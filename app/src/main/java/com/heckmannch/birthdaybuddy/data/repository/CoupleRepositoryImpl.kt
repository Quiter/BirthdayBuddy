package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val permissionChecker: PermissionChecker,
    private val contactDao: ContactDao,
    private val contactUserDataDao: ContactUserDataDao,
    private val settingsRepository: SettingsRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val widgetUpdater: WidgetUpdater,
    private val appDatabase: AppDatabase,
    private val settingsDatabase: SettingsDatabase,
    private val contactDbMapper: ContactDbMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : CoupleRepository {

    /**
     * Mutex serializing [linkAsCouple] and [unlinkCouple] mutations so concurrent updates
     * cannot interleave and leave asymmetric spouse links across databases.
     */
    private val coupleMutex = Mutex()

    private data class LinkPreviousState(
        val prevUserData1: ContactUserData?,
        val prevUserData2: ContactUserData?,
        val oldSpouseUserData: Map<String, ContactUserData?>,
    )

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
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override val ignoredCouples: Flow<List<String>> = settingsRepository.settings
        .map { it.ignoredCouplePairs }
        .distinctUntilChanged()

    override suspend fun linkAsCouple(lookupKey1: String, lookupKey2: String): Unit =
        withContext(ioDispatcher) {
            if (lookupKey1 == lookupKey2) return@withContext

            coupleMutex.withLock {
                executeWithSettingsRollback(
                    settingsDatabase = settingsDatabase,
                    appDatabase = appDatabase,
                    logTag = TAG,
                    errorMessage = "Cache update failed for linkAsCouple, rolling back settings",
                    readPreviousState = {
                        val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey1)
                        val prevUserData2 = contactUserDataDao.getUserDataForContact(lookupKey2)
                        val oldSpouseKeys = buildList {
                            prevUserData1?.spouseLookupKey
                                ?.takeIf { it != lookupKey2 && it != lookupKey1 }
                                ?.let(::add)
                            prevUserData2?.spouseLookupKey
                                ?.takeIf { it != lookupKey1 && it != lookupKey2 }
                                ?.let(::add)
                        }.distinct()
                        val oldSpouseUserData = oldSpouseKeys.associateWith { oldKey ->
                            contactUserDataDao.getUserDataForContact(oldKey)
                        }
                        LinkPreviousState(
                            prevUserData1 = prevUserData1,
                            prevUserData2 = prevUserData2,
                            oldSpouseUserData = oldSpouseUserData,
                        )
                    },
                    writeSettings = { prevState ->
                        prevState.oldSpouseUserData.values.filterNotNull().forEach { oldSpouseData ->
                            contactUserDataDao.upsertUserData(oldSpouseData.copy(spouseLookupKey = null))
                        }
                        val userData1 = prevState.prevUserData1 ?: ContactUserData(lookupKey = lookupKey1)
                        val userData2 = prevState.prevUserData2 ?: ContactUserData(lookupKey = lookupKey2)
                        contactUserDataDao.upsertUserData(userData1.copy(spouseLookupKey = lookupKey2))
                        contactUserDataDao.upsertUserData(userData2.copy(spouseLookupKey = lookupKey1))
                    },
                    rollbackSettings = { prevState ->
                        if (prevState.prevUserData1 != null) {
                            contactUserDataDao.upsertUserData(prevState.prevUserData1)
                        } else {
                            contactUserDataDao.getUserDataForContact(lookupKey1)?.let {
                                contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
                            }
                        }
                        if (prevState.prevUserData2 != null) {
                            contactUserDataDao.upsertUserData(prevState.prevUserData2)
                        } else {
                            contactUserDataDao.getUserDataForContact(lookupKey2)?.let {
                                contactUserDataDao.upsertUserData(it.copy(spouseLookupKey = null))
                            }
                        }
                        prevState.oldSpouseUserData.values.filterNotNull().forEach { oldSpouseData ->
                            contactUserDataDao.upsertUserData(oldSpouseData)
                        }
                    },
                    updateAppDbCache = { prevState ->
                        val contact1 = contactDao.getContactByLookupKey(lookupKey1)
                        val contact2 = contactDao.getContactByLookupKey(lookupKey2)
                        val oldSpouseKeysToClear = buildSet {
                            addAll(prevState.oldSpouseUserData.keys)
                            contact1?.spouseLookupKey
                                ?.takeIf { it != lookupKey2 && it != lookupKey1 }
                                ?.let(::add)
                            contact2?.spouseLookupKey
                                ?.takeIf { it != lookupKey1 && it != lookupKey2 }
                                ?.let(::add)
                        }
                        oldSpouseKeysToClear.forEach { oldSpouseKey ->
                            contactDao.getContactByLookupKey(oldSpouseKey)?.let { oldSpouseContact ->
                                contactDao.upsertContact(oldSpouseContact.copy(spouseLookupKey = null))
                            }
                        }
                        contact1?.let { contact ->
                            contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey2))
                        }
                        contact2?.let { contact ->
                            contactDao.upsertContact(contact.copy(spouseLookupKey = lookupKey1))
                        }
                    },
                )
            }
            updateWidgetAndSyncCalendar()
        }

    override suspend fun unlinkCouple(lookupKey: String): Unit = withContext(ioDispatcher) {
        val unlinked = coupleMutex.withLock {
            val contact = contactDao.getContactByLookupKey(lookupKey)
            val spouseKey = contact?.spouseLookupKey
                ?: contactUserDataDao.getUserDataForContact(lookupKey)?.spouseLookupKey
                ?: return@withLock false

            executeWithSettingsRollback(
                settingsDatabase = settingsDatabase,
                appDatabase = appDatabase,
                logTag = TAG,
                errorMessage = "Cache update failed for unlinkCouple, rolling back settings",
                readPreviousState = {
                    val prevUserData1 = contactUserDataDao.getUserDataForContact(lookupKey)
                    val prevUserData2 = if (spouseKey != lookupKey) {
                        contactUserDataDao.getUserDataForContact(spouseKey)
                    } else {
                        null
                    }
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
                    if (spouseKey != lookupKey) {
                        contactDao.getContactByLookupKey(spouseKey)?.let {
                            contactDao.upsertContact(it.copy(spouseLookupKey = null))
                        }
                    }
                },
            )
            true
        }
        if (unlinked) {
            updateWidgetAndSyncCalendar()
        }
    }

    /**
     * Triggers an immediate home screen widget refresh and, when calendar sync is enabled
     * and calendar permissions are granted, synchronizes contact events with Android's system
     * calendar.
     *
     * Both side effects are independently guarded so a failure in either operation cannot
     * abort the other or break couple link/unlink persistence. [CancellationException] is always
     * rethrown to preserve structured concurrency.
     */
    private suspend fun updateWidgetAndSyncCalendar(): Unit = withContext(ioDispatcher) {
        try {
            widgetUpdater.updateWidget()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget after couple update: ${e.message}", e)
        }

        try {
            val currentSettings = settingsRepository.getSettingsImmediate()
            if (currentSettings.calendarSyncEnabled && permissionChecker.hasCalendarPermission()) {
                val allContacts = contactDao.getAllContactsImmediate().map { contactDbMapper.toDomain(it) }
                calendarSyncRepository.syncBirthdays(allContacts)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing calendar after couple update: ${e.message}", e)
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
