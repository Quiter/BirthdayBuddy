package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.provider.CalendarContract
import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.util.CalendarStringProvider
import com.heckmannch.birthdaybuddy.util.hasYear
import com.heckmannch.birthdaybuddy.util.mergeNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * Implementation of [CalendarSyncRepository] that synchronizes contact events
 * (such as birthdays, anniversaries, and name days) with Android's system calendar database.
 *
 * It uses content providers to manage custom application-specific calendars ("BirthdayBuddy")
 * and schedules recurring sync operations. Design decisions include offloading the heavy ContentProvider
 * batch operations to [kotlinx.coroutines.Dispatchers.IO] to keep UI threads responsive.
 */
class CalendarSyncRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao,
    private val systemCalendarDataSource: SystemCalendarDataSource,
    private val calendarStringProvider: CalendarStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CalendarSyncRepository {

    private enum class LocalCalendarType(val calendarName: String) {
        BIRTHDAY("BirthdayBuddy_Birthdays"),
        ANNIVERSARY("BirthdayBuddy_Anniversaries"),
        NAMEDAY("BirthdayBuddy_NameDays");

        companion object {
            fun fromDomain(type: CalendarSyncRepository.CalendarType): LocalCalendarType =
                when (type) {
                    CalendarSyncRepository.CalendarType.BIRTHDAY -> BIRTHDAY
                    CalendarSyncRepository.CalendarType.ANNIVERSARY -> ANNIVERSARY
                    CalendarSyncRepository.CalendarType.NAMEDAY -> NAMEDAY
                }
        }
    }

    override fun hasCalendarPermissions(): Boolean {
        return systemCalendarDataSource.hasCalendarPermissions()
    }

    private suspend fun getOrCreateCalendar(type: LocalCalendarType): Long? =
        withContext(ioDispatcher) {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
            val preferredColor = when (type) {
                LocalCalendarType.BIRTHDAY -> currentSettings.birthdayCalendarColor
                LocalCalendarType.ANNIVERSARY -> currentSettings.anniversaryCalendarColor
                LocalCalendarType.NAMEDAY -> currentSettings.nameDayCalendarColor
            }
            val displayName = when (type) {
                LocalCalendarType.BIRTHDAY -> calendarStringProvider.calendarNameBirthdays()
                LocalCalendarType.ANNIVERSARY -> calendarStringProvider.calendarNameAnniversaries()
                LocalCalendarType.NAMEDAY -> calendarStringProvider.calendarNameNameDays()
            }
            systemCalendarDataSource.getOrCreateCalendar(
                type.calendarName,
                displayName,
                preferredColor
            )
        }

    private suspend fun cleanCalendars(): Unit = withContext(ioDispatcher) {
        val activeNames = setOf(
            LocalCalendarType.BIRTHDAY.calendarName,
            LocalCalendarType.ANNIVERSARY.calendarName,
            LocalCalendarType.NAMEDAY.calendarName
        )
        val calendars = systemCalendarDataSource.queryAllCalendars()
        val seenActiveIds = mutableMapOf<String, Long>()

        for (calendar in calendars) {
            val id = calendar.id
            val accountName = calendar.accountName
            val accountType = calendar.accountType

            when (val name = calendar.name) {
                // Delete legacy BirthdayBuddyCalendar (under phone account)
                SystemCalendarDataSource.LEGACY_CALENDAR_NAME -> {
                    systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
                }

                in activeNames -> {
                    if (accountName == SystemCalendarDataSource.ACCOUNT_NAME && accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
                        val existingId = seenActiveIds[name]
                        if (existingId == null) {
                            seenActiveIds[name] = id
                        } else {
                            // Delete duplicate
                            systemCalendarDataSource.deleteCalendarById(
                                id,
                                accountName,
                                accountType
                            )
                        }
                    } else {
                        // Incorrect account name/type - delete
                        systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
                    }
                }
            }
        }
    }

    private suspend fun prepareOptionalCalendar(calendarId: Long?, type: LocalCalendarType) {
        if (calendarId != null) {
            systemCalendarDataSource.clearCalendarEvents(calendarId)
        } else {
            systemCalendarDataSource.findCalendarIdByName(type.calendarName)?.let { id ->
                systemCalendarDataSource.deleteCalendarById(
                    id,
                    SystemCalendarDataSource.ACCOUNT_NAME,
                    CalendarContract.ACCOUNT_TYPE_LOCAL
                )
            }
        }
    }

    private fun formatAnniversaryDescription(anniversary: LocalDate): String {
        return if (anniversary.hasYear) {
            calendarStringProvider.calendarEventAnniversaryYear(anniversary.year)
        } else {
            calendarStringProvider.calendarEventAnniversaryNoYear()
        }
    }

    override suspend fun updateCalendarColor(
        type: CalendarSyncRepository.CalendarType,
        newColor: Int
    ): Boolean =
        withContext(ioDispatcher) {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
            val localType = LocalCalendarType.fromDomain(type)
            val updatedSettings = when (localType) {
                LocalCalendarType.BIRTHDAY -> currentSettings.copy(birthdayCalendarColor = newColor)
                LocalCalendarType.ANNIVERSARY -> currentSettings.copy(anniversaryCalendarColor = newColor)
                LocalCalendarType.NAMEDAY -> currentSettings.copy(nameDayCalendarColor = newColor)
            }
            appSettingsDao.upsertSettings(updatedSettings)

            val calendarId = systemCalendarDataSource.findCalendarIdByName(localType.calendarName)
            if (calendarId != null) {
                systemCalendarDataSource.updateCalendarColor(calendarId, newColor)
            } else {
                false
            }
        }

    override suspend fun deleteCalendar(): Boolean = withContext(ioDispatcher) {
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()

        var deletedAny = false
        val allTargetNames = setOf(
            SystemCalendarDataSource.LEGACY_CALENDAR_NAME,
            LocalCalendarType.BIRTHDAY.calendarName,
            LocalCalendarType.ANNIVERSARY.calendarName,
            LocalCalendarType.NAMEDAY.calendarName
        )
        val calendars = systemCalendarDataSource.queryAllCalendars()
        for (calendar in calendars) {
            val id = calendar.id
            val accountName = calendar.accountName
            val accountType = calendar.accountType
            val name = calendar.name

            if (name in allTargetNames) {
                systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
                deletedAny = true
            }
        }

        // Update local settings in database
        appSettingsDao.upsertSettings(
            currentSettings.copy(
                calendarSyncEnabled = false,
                calendarId = null
            )
        )
        deletedAny
    }

    override suspend fun syncBirthdays(contacts: List<Contact>): Boolean =
        withContext(ioDispatcher) {
            if (!hasCalendarPermissions()) return@withContext false

            // Clean up outdated or duplicate calendars prior to syncing
            cleanCalendars()

            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
            val otherEventsEnabled = currentSettings.otherEventsEnabled

            // Retrieve or create IDs for all active calendars
            val birthdayCalId =
                getOrCreateCalendar(LocalCalendarType.BIRTHDAY) ?: return@withContext false
            val anniversaryCalId =
                if (otherEventsEnabled) getOrCreateCalendar(LocalCalendarType.ANNIVERSARY) else null
            val nameDayCalId =
                if (otherEventsEnabled) getOrCreateCalendar(LocalCalendarType.NAMEDAY) else null

            try {
                // Clear birthdays
                systemCalendarDataSource.clearCalendarEvents(birthdayCalId)

                // Clear anniversaries and name days, or delete calendar if disabled
                prepareOptionalCalendar(anniversaryCalId, LocalCalendarType.ANNIVERSARY)
                prepareOptionalCalendar(nameDayCalId, LocalCalendarType.NAMEDAY)

                val operations = ArrayList<ContentProviderOperation>()

                suspend fun addEvent(
                    calId: Long,
                    date: LocalDate,
                    title: String,
                    description: String
                ) {
                    val year = if (date.hasYear) date.year else DEFAULT_EVENT_YEAR
                    val dtStart =
                        date.withYear(year).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                    val insertUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                        .appendQueryParameter(
                            CalendarContract.Calendars.ACCOUNT_NAME,
                            SystemCalendarDataSource.ACCOUNT_NAME
                        )
                        .appendQueryParameter(
                            CalendarContract.Calendars.ACCOUNT_TYPE,
                            CalendarContract.ACCOUNT_TYPE_LOCAL
                        )
                        .build()

                    val op = ContentProviderOperation.newInsert(insertUri)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calId)
                        .withValue(CalendarContract.Events.TITLE, title)
                        .withValue(CalendarContract.Events.DESCRIPTION, description)
                        .withValue(CalendarContract.Events.DTSTART, dtStart)
                        .withValue(CalendarContract.Events.DURATION, "P1D")
                        .withValue(CalendarContract.Events.RRULE, "FREQ=YEARLY")
                        .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                        .withValue(CalendarContract.Events.ALL_DAY, 1)
                        .withValue(
                            CalendarContract.Events.STATUS,
                            CalendarContract.Events.STATUS_CONFIRMED
                        )
                        .build()

                    operations.add(op)

                    if (operations.size >= BATCH_SIZE) {
                        systemCalendarDataSource.applyBatch(operations)
                        operations.clear()
                    }
                }

                val contactsByLookupKey = contacts.associateBy { it.lookupKey }
                val processedAnniversaries = HashSet<String>()

                for (contact in contacts) {
                    // 1. Insert birthdays into the birthday calendar
                    contact.birthday?.let { birthday ->
                        val title =
                            calendarStringProvider.calendarEventTitle(contact.fullName)
                        val description = if (birthday.hasYear) {
                            calendarStringProvider.calendarEventBirthYear(birthday.year)
                        } else {
                            calendarStringProvider.calendarEventNoYear()
                        }
                        addEvent(birthdayCalId, birthday, title, description)
                    }

                    // 2. Insert anniversaries into the anniversary calendar (if enabled)
                    if (otherEventsEnabled && anniversaryCalId != null) {
                        contact.anniversary?.let { anniversary ->
                            val description = formatAnniversaryDescription(anniversary)
                            val spouseKey = contact.spouseLookupKey
                            if (spouseKey != null) {
                                if (!processedAnniversaries.contains(contact.lookupKey)) {
                                    val spouse =
                                        contactsByLookupKey[spouseKey]?.takeIf { it.anniversary != null }
                                    if (spouse != null) {
                                        val mergedName = mergeNames(
                                            contact.fullName,
                                            spouse.fullName
                                        )
                                        val title = calendarStringProvider.calendarEventAnniversaryTitleCouple(
                                            mergedName
                                        )
                                        addEvent(anniversaryCalId, anniversary, title, description)
                                        processedAnniversaries.add(contact.lookupKey)
                                        processedAnniversaries.add(spouse.lookupKey)
                                    } else {
                                        val title = calendarStringProvider.calendarEventAnniversaryTitle(
                                            contact.fullName
                                        )
                                        addEvent(anniversaryCalId, anniversary, title, description)
                                        processedAnniversaries.add(contact.lookupKey)
                                    }
                                }
                            } else {
                                val title = calendarStringProvider.calendarEventAnniversaryTitle(
                                    contact.fullName
                                )
                                addEvent(anniversaryCalId, anniversary, title, description)
                            }
                        }
                    }

                    // 3. Insert name days into the name day calendar (if enabled)
                    if (otherEventsEnabled && nameDayCalId != null) {
                        contact.nameDay?.let { nameDay ->
                            val title = calendarStringProvider.calendarEventNameDayTitle(
                                contact.fullName
                            )
                            val description = calendarStringProvider.calendarEventNameDayDescription(
                                contact.fullName
                            )
                            addEvent(nameDayCalId, nameDay, title, description)
                        }
                    }
                }

                if (operations.isNotEmpty()) {
                    systemCalendarDataSource.applyBatch(operations)
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing events to calendars", e)
                false
            }
        }

    companion object {
        private const val TAG = "CalendarSyncRepo"
        private const val BATCH_SIZE = 400
        private const val DEFAULT_EVENT_YEAR = 2000
    }
}
