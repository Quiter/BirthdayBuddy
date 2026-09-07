package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementation of [CoupleRepository] managing couple relations, suggestions, and 2-phase
 * commit synchronization across persistent storage and database caches.
 *
 * Implements rollback semantics across [SettingsDatabase] and [AppDatabase] to guarantee
 * relational consistency between the user-data store and fast contact query cache.
 */
class CoupleRepositoryImpl @Inject constructor(
    private val contactDao: ContactDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val settingsRepository: SettingsRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val widgetUpdater: WidgetUpdater,
    private val appDatabase: AppDatabase,
    private val settingsDatabase: SettingsDatabase,
    private val contactDbMapper: ContactDbMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CoupleRepository {

    override val potentialCouples: Flow<List<CoupleSuggestion>> = contactDao.getPotentialCouples()
        .map { list ->
            list.map { projection ->
                CoupleSuggestion(
                    firstLookupKey = projection.firstLookupKey,
                    firstName = projection.firstName,
                    firstImageUri = projection.firstImageUri,
                    secondLookupKey = projection.secondLookupKey,
                    secondName = projection.secondName,
                    secondImageUri = projection.secondImageUri
                )
            }
        }
        .distinctUntilChanged()

    override val ignoredCouples: Flow<List<String>> = settingsRepository.settings
        .map { it.ignoredCouplePairs }
        .distinctUntilChanged()

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

    override suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String) {
        withContext(ioDispatcher) {
            executeWithSettingsRollback(
                readPreviousState = {
                    val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey1)
                    val prevUserData2 = contactUserDataDao.getUserDataForContact(lookupKey2)
                    prevUserData1 to prevUserData2
                },
                writeSettings = { (prevUserData1, prevUserData2) ->
                    val userData1 = prevUserData1 ?: ContactUserData(lookupKey = lookupKey1)
                    val userData2 = prevUserData2 ?: ContactUserData(lookupKey = lookupKey2)
                    contactUserDataDao.upsertUserData(userData1.copy(spouseLookupKey = lookupKey2))
                    contactUserDataDao.upsertUserData(userData2.copy(spouseLookupKey = lookupKey1))
                },
                rollbackSettings = { (prevUserData1, prevUserData2) ->
                    if (prevUserData1 != null) {
                        contactUserDataDao.upsertUserData(prevUserData1)
                    } else {
                        contactUserDataDao.getUserDataForContact(lookupKey1)?.let {
                            contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
                        }
                    }
                    if (prevUserData2 != null) {
                        contactUserDataDao.upsertUserData(prevUserData2)
                    } else {
                        contactUserDataDao.getUserDataForContact(lookupKey2)?.let {
                            contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
                        }
                    }
                },
                updateAppDbCache = {
                    contactDao.getContactByLookupKey(lookupKey1)?.let { contact ->
                        contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey2))
                    }
                    contactDao.getContactByLookupKey(lookupKey2)?.let { contact ->
                        contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey1))
                    }
                },
                errorMessage = "Cache update failed for linkAsCouple, rolling back settings"
            )
        }
        updateWidgetAndSyncCalendar()
    }

    override suspend fun unlinkCouple(lookupKey: String) {
        withContext(ioDispatcher) {
            val contact = contactDao.getContactByLookupKey(lookupKey) ?: return@withContext
            val spouseKey = contact.spouseLookupKey ?: return@withContext

            executeWithSettingsRollback(
                readPreviousState = {
                    val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey)
                    val prevUserData2 = contactUserDataDao.getUserDataForContact(spouseKey)
                    prevUserData1 to prevUserData2
                },
                writeSettings = { (prevUserData1, prevUserData2) ->
                    prevUserData1?.let { contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null)) }
                    prevUserData2?.let { contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null)) }
                },
                rollbackSettings = { (prevUserData1, prevUserData2) ->
                    prevUserData1?.let { contactUserDataDao.upsertUserData(it) }
                    prevUserData2?.let { contactUserDataDao.upsertUserData(it) }
                },
                updateAppDbCache = {
                    contactDao.getContactByLookupKey(lookupKey)?.let {
                        contactDao.upsertContact(it.copy(spouseLookupKey = null))
                    }
                    contactDao.getContactByLookupKey(spouseKey)?.let {
                        contactDao.upsertContact(it.copy(spouseLookupKey = null))
                    }
                },
                errorMessage = "Cache update failed for unlinkCouple, rolling back settings"
            )
        }
        updateWidgetAndSyncCalendar()
    }

    private suspend fun updateWidgetAndSyncCalendar(): Unit = withContext(ioDispatcher) {
        widgetUpdater.updateWidget()
        val allContacts = contactDao.getAllContactsImmediate().map { contactDbMapper.toDomain(it) }
        val currentSettings = settingsRepository.getSettingsImmediate()
        if (currentSettings.calendarSyncEnabled) {
            calendarSyncRepository.syncBirthdays(allContacts)
        }
    }

    override suspend fun ignoreSuggestion(lookupKey1: String, lookupKey2: String) {
        withContext(ioDispatcher) {
            val pairKey =
                if (lookupKey1 < lookupKey2) "$lookupKey1:$lookupKey2" else "$lookupKey2:$lookupKey1"
            settingsRepository.updateSettings { currentSettings ->
                if (!currentSettings.ignoredCouplePairs.contains(pairKey)) {
                    val updatedList = currentSettings.ignoredCouplePairs + pairKey
                    currentSettings.copy(ignoredCouplePairs = updatedList)
                } else {
                    currentSettings
                }
            }
        }
    }

    override suspend fun clearIgnoredCouplePairs() {
        withContext(ioDispatcher) {
            settingsRepository.updateSettings { currentSettings ->
                currentSettings.copy(ignoredCouplePairs = emptyList())
            }
        }
    }

    companion object {
        private const val TAG = "CoupleRepository"
    }
}
