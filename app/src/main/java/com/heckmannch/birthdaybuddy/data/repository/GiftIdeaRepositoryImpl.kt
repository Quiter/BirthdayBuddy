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
import kotlinx.coroutines.withContext
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
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val ideas = giftIdeaConverters.toGiftIdeaList(contact.giftIdeasJson)
            val updatedIdeas = GiftIdea.withNewIdea(ideas, newIdea)
            updateGiftIdeas(lookupKey, updatedIdeas)
        }

    override suspend fun removeGiftIdea(lookupKey: String, ideaId: String) =
        withContext(ioDispatcher) {
            val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, ideaId)
                ?: return@withContext
            val ideas = giftIdeaConverters.toGiftIdeaList(targetContact.giftIdeasJson)
            val updatedIdeas = ideas.filter { it.id != ideaId }
            updateGiftIdeas(targetKey, updatedIdeas)
        }

    override suspend fun deleteGiftIdea(lookupKey: String, ideaId: String) =
        removeGiftIdea(lookupKey, ideaId)

    override suspend fun toggleGiftIdea(lookupKey: String, idea: GiftIdea, isChecked: Boolean) =
        withContext(ioDispatcher) {
            val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, idea.id)
                ?: return@withContext
            val ideas = giftIdeaConverters.toGiftIdeaList(targetContact.giftIdeasJson)
            val updatedIdeas = GiftIdea.withToggledIdea(ideas, idea, isChecked)
            updateGiftIdeas(targetKey, updatedIdeas)
        }

    override suspend fun updateGiftIdeaText(lookupKey: String, ideaId: String, newText: String) =
        withContext(ioDispatcher) {
            val (targetKey, targetContact) = resolveContactForGiftIdea(lookupKey, ideaId)
                ?: return@withContext
            val ideas = giftIdeaConverters.toGiftIdeaList(targetContact.giftIdeasJson)
            val updatedIdeas = ideas.map {
                if (it.id == ideaId) it.copy(text = newText) else it
            }
            updateGiftIdeas(targetKey, updatedIdeas)
        }

    override suspend fun exportGiftIdeas(uriString: String): Unit = withContext(ioDispatcher) {
        val json = giftIdeaBackupManager.exportGiftIdeas()
        val uri = uriString.toUri()
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(json.toByteArray())
        }
    }

    override suspend fun importGiftIdeas(uriString: String): Int {
        val uri = uriString.toUri()
        val json = withContext(ioDispatcher) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            }
        } ?: return -1
        val count = giftIdeaBackupManager.importGiftIdeas(json)
        if (count > 0) {
            contactRepository.syncContacts() // Update cache
        }
        return count
    }

    companion object {
        private const val TAG = "GiftIdeaRepository"
    }
}
