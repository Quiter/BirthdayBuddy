package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.util.Log
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.GiftIdeaRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Implementation of [GiftIdeaRepository] managing the persistence, export, import,
 * and 2-phase transactional updates of gift ideas.
 *
 * Persists user-authored gift ideas to [SettingsDatabase] as the single source of truth
 * while maintaining a synchronized cached copy in [AppDatabase]. Home screen widgets
 * are notified of changes through [WidgetUpdater].
 */
class GiftIdeaRepositoryImpl @Inject constructor(
    private val contentResolver: ContentResolver,
    private val contactDao: ContactDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val giftIdeaBackupManager: GiftIdeaBackupManager,
    private val contactRepository: ContactRepository,
    private val appDatabase: AppDatabase,
    private val settingsDatabase: SettingsDatabase,
    private val widgetUpdater: WidgetUpdater,
    private val giftIdeaConverters: GiftIdeaConverters = GiftIdeaConverters(),
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GiftIdeaRepository {

    /**
     * Mutex serializing read-modify-write operations on gift ideas to prevent lost updates
     * when multiple mutations occur concurrently.
     */
    private val giftIdeaMutex = Mutex()

    /**
     * Executes a 2-phase transactional update:
     * 1. Reads previous state from SettingsDB / Room.
     * 2. Writes updated data to SettingsDB (Source of Truth).
     * 3. Updates AppDB cache. If this fails, rolls back the SettingsDB change and rethrows.
     */
    private suspend fun <T> executeWithSettingsRollback(
        readPreviousState: suspend () -> T,
        writeSettings: suspend (T) -> Unit,
        rollbackSettings: suspend (T) -> Unit,
        updateAppDbCache: suspend () -> Unit,
        errorMessage: String
    ) {
        val previousState = readPreviousState()
        settingsDatabase.withTransaction {
            writeSettings(previousState)
        }
        try {
            appDatabase.withTransaction {
                updateAppDbCache()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "$errorMessage: ${e.message}", e)
            settingsDatabase.withTransaction {
                rollbackSettings(previousState)
            }
            throw e
        }
    }

    private suspend fun updateGiftIdeas(lookupKey: String, ideas: List<GiftIdea>) {
        withContext(ioDispatcher) {
            executeWithSettingsRollback(
                readPreviousState = {
                    contactUserDataDao.getUserDataForContact(lookupKey)
                },
                writeSettings = { prevUserData ->
                    contactUserDataDao.upsertUserData(
                        ContactUserData(
                            lookupKey = lookupKey,
                            giftIdeasJson = giftIdeaConverters.fromGiftIdeaList(ideas),
                            spouseLookupKey = prevUserData?.spouseLookupKey
                        )
                    )
                },
                rollbackSettings = { prevUserData ->
                    val rollbackData = prevUserData
                        ?: ContactUserData(lookupKey = lookupKey, giftIdeasJson = "[]")
                    contactUserDataDao.upsertUserData(rollbackData)
                },
                updateAppDbCache = {
                    contactDao.getContactByLookupKey(lookupKey)?.let { contact ->
                        contactDao.upsertContact(contact.copy(giftIdeasJson = giftIdeaConverters.fromGiftIdeaList(ideas)))
                    }
                },
                errorMessage = "Failed to update gift idea cache, rolling back"
            )
        }
        widgetUpdater.updateWidget()
    }

    private suspend fun resolveContactForGiftIdea(
        lookupKey: String,
        ideaId: String
    ): Pair<String, ContactEntity>? {
        val contact = contactDao.getContactByLookupKey(lookupKey) ?: return null
        val contactIdeas = giftIdeaConverters.toGiftIdeaList(contact.giftIdeasJson)
        if (contactIdeas.any { it.id == ideaId }) {
            return lookupKey to contact
        }
        val spouseKey = contact.spouseLookupKey ?: return null
        val spouseContact = contactDao.getContactByLookupKey(spouseKey) ?: return null
        val spouseIdeas = giftIdeaConverters.toGiftIdeaList(spouseContact.giftIdeasJson)
        if (spouseIdeas.any { it.id == ideaId }) {
            return spouseKey to spouseContact
        }
        return null
    }

    override suspend fun addGiftIdea(lookupKey: String, newIdea: GiftIdea) =
        withContext(ioDispatcher) {
            giftIdeaMutex.withLock {
                val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withLock
                val ideas = giftIdeaConverters.toGiftIdeaList(contact.giftIdeasJson)
                val updatedIdeas = GiftIdea.withNewIdea(ideas, newIdea)
                updateGiftIdeas(lookupKey, updatedIdeas)
            }
        }

    override suspend fun removeGiftIdea(lookupKey: String, ideaId: String) =
        withContext(ioDispatcher) {
            giftIdeaMutex.withLock {
                val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, ideaId)
                    ?: return@withLock
                val ideas = giftIdeaConverters.toGiftIdeaList(targetContact.giftIdeasJson)
                val updatedIdeas = ideas.filter { it.id != ideaId }
                updateGiftIdeas(targetKey, updatedIdeas)
            }
        }

    override suspend fun deleteGiftIdea(lookupKey: String, ideaId: String) =
        removeGiftIdea(lookupKey, ideaId)

    override suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        withContext(ioDispatcher) {
            giftIdeaMutex.withLock {
                val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, idea.id)
                    ?: return@withLock
                val ideas = giftIdeaConverters.toGiftIdeaList(targetContact.giftIdeasJson)
                val updatedIdeas = GiftIdea.withToggledIdea(ideas, idea, isChecked)
                updateGiftIdeas(targetKey, updatedIdeas)
            }
        }

    override suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        withContext(ioDispatcher) {
            giftIdeaMutex.withLock {
                val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, ideaId)
                    ?: return@withLock
                val ideas = giftIdeaConverters.toGiftIdeaList(targetContact.giftIdeasJson)
                val updatedIdeas = ideas.map {
                    if (it.id == ideaId) it.copy(text = newText) else it
                }
                updateGiftIdeas(targetKey, updatedIdeas)
            }
        }

    override suspend fun exportGiftIdeas(uriString: String): Unit = withContext(ioDispatcher) {
        val json = giftIdeaBackupManager.exportGiftIdeas()
        val uri = uriString.toUri()
        // Open with "wt" (write + truncate) so overwriting an existing SAF document does not leave trailing bytes.
        // Fall back to "w" only if a provider explicitly rejects the "wt" mode.
        val outputStream = try {
            contentResolver.openOutputStream(uri, "wt")
        } catch (_: IllegalArgumentException) {
            contentResolver.openOutputStream(uri, "w")
        } catch (_: UnsupportedOperationException) {
            contentResolver.openOutputStream(uri, "w")
        } ?: throw IOException("Could not open output stream for URI: $uri")

        outputStream.use { stream ->
            stream.write(json.toByteArray(Charsets.UTF_8))
        }
    }

    override suspend fun importGiftIdeas(uriString: String): Int = withContext(ioDispatcher) {
        val uri = uriString.toUri()
        val json = contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        } ?: return@withContext -1
        val count = giftIdeaBackupManager.importGiftIdeas(json)
        if (count > 0) {
            contactRepository.syncContacts()
            syncCachedContactGiftIdeas()
        }
        count
    }

    /**
     * Synchronizes the cached `giftIdeasJson` fields in [AppDatabase] from [SettingsDatabase]
     * for all contacts currently present in the local cache. This ensures imported gift ideas
     * become immediately visible even when system contacts permission is not granted and
     * [ContactRepository.syncContacts] returns early.
     */
    private suspend fun syncCachedContactGiftIdeas() {
        giftIdeaMutex.withLock {
            val cachedContacts = contactDao.getAllContactsImmediate()
            if (cachedContacts.isEmpty()) return

            val userDataByKey = contactUserDataDao.getAllUserDataImmediate()
                .associateBy { it.lookupKey }
            if (userDataByKey.isEmpty()) return

            val contactsToUpdate = cachedContacts.mapNotNull { contact ->
                val userData = userDataByKey[contact.lookupKey] ?: return@mapNotNull null
                if (contact.giftIdeasJson != userData.giftIdeasJson) {
                    contact.copy(giftIdeasJson = userData.giftIdeasJson)
                } else {
                    null
                }
            }

            if (contactsToUpdate.isNotEmpty()) {
                contactDao.upsertContacts(contactsToUpdate)
                widgetUpdater.updateWidget()
            }
        }
    }

    companion object {
        private const val TAG = "GiftIdeaRepository"
    }
}
