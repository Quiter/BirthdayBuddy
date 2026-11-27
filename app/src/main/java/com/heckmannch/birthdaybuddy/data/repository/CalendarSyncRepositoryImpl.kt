package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.provider.CalendarContract
import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.Converters
import com.heckmannch.birthdaybuddy.di.IoDispatcher
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetDetectedOtherEventsUseCase
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
 * (birthdays, anniversaries, name days, and dynamic custom date-field events)
 * with Android's system calendar database.
 *
 * Uses [GetDetectedOtherEventsUseCase] as the shared source of truth for non-birthday
 * event labels, ensuring full consistency with the "Other Events" settings screen.
 */
class CalendarSyncRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao,
    private val systemCalendarDataSource: SystemCalendarDataSource,
    private val calendarStringProvider: CalendarStringProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val converters: Converters = Converters(),
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

    private fun customEventCalendarName(eventKey: String): String =
        "$CUSTOM_EVENT_CALENDAR_PREFIX$eventKey"

    private fun resolveCustomEventDisplayName(eventKey: String): String = when {
        eventKey == ContactLabels.LABEL_OTHER_EVENT -> calendarStringProvider.calendarNameOtherEvent()
        ContactLabels.isCustomEventLabel(eventKey) ->
            ContactLabels.extractCustomEventName(eventKey) ?: eventKey
        else -> eventKey
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

    private suspend fun getOrCreateCustomEventCalendar(
        eventKey: String,
        customColors: Map<String, Int>,
    ): Long? = withContext(ioDispatcher) {
        val preferredColor = customColors[eventKey] ?: AppSettings.DEFAULT_CUSTOM_EVENT_CALENDAR_COLOR
        val displayName = resolveCustomEventDisplayName(eventKey)
        systemCalendarDataSource.getOrCreateCalendar(
            customEventCalendarName(eventKey),
            displayName,
            preferredColor
        )
    }

    private suspend fun cleanCalendars(): Unit = withContext(ioDispatcher) {
        val fixedNames = setOf(
            LocalCalendarType.BIRTHDAY.calendarName,
            LocalCalendarType.ANNIVERSARY.calendarName,
            LocalCalendarType.NAMEDAY.calendarName
        )
        val calendars = systemCalendarDataSource.queryAllCalendars()
        val seenActiveIds = mutableMapOf<String, Long>()

        for ((id, name, accountName, accountType) in calendars) {
            val isManagedName = name in fixedNames || name.startsWith(CUSTOM_EVENT_CALENDAR_PREFIX)
            when {
                // Delete legacy BirthdayBuddyCalendar (under phone account)
                name == SystemCalendarDataSource.LEGACY_CALENDAR_NAME -> {
                    systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
                }

                isManagedName -> {
                    if (accountName == SystemCalendarDataSource.ACCOUNT_NAME &&
                        accountType == CalendarContract.ACCOUNT_TYPE_LOCAL
                    ) {
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

    private suspend fun deleteStaleCustomEventCalendars(activeCustomCalendarNames: Set<String>) {
        val calendars = systemCalendarDataSource.queryAllCalendars()
        for ((id, name, accountName, accountType) in calendars) {
            if (name.startsWith(CUSTOM_EVENT_CALENDAR_PREFIX) && name !in activeCustomCalendarNames) {
                systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
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

    private fun formatCustomEventDescription(eventName: String, date: LocalDate): String {
        return if (date.hasYear) {
            calendarStringProvider.calendarEventCustomYear(eventName, date.year)
        } else {
            calendarStringProvider.calendarEventCustomNoYear(eventName)
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
            calendarId != null && systemCalendarDataSource.updateCalendarColor(calendarId, newColor)
        }

    override suspend fun updateCustomEventCalendarColor(
        eventKey: String,
        newColor: Int
    ): Boolean = withContext(ioDispatcher) {
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()
        val currentColors = converters.toIntMap(currentSettings.customEventCalendarColorsJson).toMutableMap()
        currentColors[eventKey] = newColor
        appSettingsDao.upsertSettings(
            currentSettings.copy(
                customEventCalendarColorsJson = converters.fromIntMap(currentColors)
            )
        )

        val calendarId = systemCalendarDataSource.findCalendarIdByName(customEventCalendarName(eventKey))
        calendarId != null && systemCalendarDataSource.updateCalendarColor(calendarId, newColor)
    }

    override suspend fun deleteCalendar(): Boolean = withContext(ioDispatcher) {
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettingsEntity()

        var deletedAny = false
        val fixedTargetNames = setOf(
            SystemCalendarDataSource.LEGACY_CALENDAR_NAME,
            LocalCalendarType.BIRTHDAY.calendarName,
            LocalCalendarType.ANNIVERSARY.calendarName,
            LocalCalendarType.NAMEDAY.calendarName
        )
        val calendars = systemCalendarDataSource.queryAllCalendars()
        for ((id, name, accountName, accountType) in calendars) {
            if (name in fixedTargetNames || name.startsWith(CUSTOM_EVENT_CALENDAR_PREFIX)) {
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
            val disabledOtherEventLabels = currentSettings.disabledOtherEventLabels.toSet()
            val customColors = converters.toIntMap(currentSettings.customEventCalendarColorsJson)

            val anniversaryEnabled = otherEventsEnabled &&
                ContactLabels.LABEL_ANNIVERSARY !in disabledOtherEventLabels
            val nameDayEnabled = otherEventsEnabled &&
                ContactLabels.LABEL_NAME_DAY !in disabledOtherEventLabels

            // Discover active dynamic custom/other events from the shared source of truth
            val activeCustomEventKeys = if (otherEventsEnabled) {
                GetDetectedOtherEventsUseCase.detect(contacts, disabledOtherEventLabels)
                    .asSequence()
                    .filter { it.isEnabled && ContactLabels.isDynamicCustomEventKey(it.eventKey) }
                    .map { it.eventKey }
                    .toList()
            } else {
                emptyList()
            }

            // Retrieve or create IDs for all active calendars
            val birthdayCalId =
                getOrCreateCalendar(LocalCalendarType.BIRTHDAY) ?: return@withContext false
            val anniversaryCalId =
                if (anniversaryEnabled) getOrCreateCalendar(LocalCalendarType.ANNIVERSARY) else null
            val nameDayCalId =
                if (nameDayEnabled) getOrCreateCalendar(LocalCalendarType.NAMEDAY) else null

            val customEventCalIds = mutableMapOf<String, Long>()
            for (eventKey in activeCustomEventKeys) {
                getOrCreateCustomEventCalendar(eventKey, customColors)?.let { calId ->
                    customEventCalIds[eventKey] = calId
                }
            }

            try {
                // Clear birthdays
                systemCalendarDataSource.clearCalendarEvents(birthdayCalId)

                // Clear anniversaries and name days, or delete calendar if disabled
                prepareOptionalCalendar(anniversaryCalId, LocalCalendarType.ANNIVERSARY)
                prepareOptionalCalendar(nameDayCalId, LocalCalendarType.NAMEDAY)

                // Clear active custom event calendars and delete stale/disabled ones
                customEventCalIds.values.forEach { calId ->
                    systemCalendarDataSource.clearCalendarEvents(calId)
                }
                deleteStaleCustomEventCalendars(
                    activeCustomEventKeys.mapTo(mutableSetOf()) { customEventCalendarName(it) }
                )

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

                // Deduplicate contacts by lookupKey to avoid creating duplicate events for the same contact
                val uniqueContacts = contacts.distinctBy { it.lookupKey }

                for (contact in uniqueContacts) {
                    val lookupKey = contact.lookupKey
                    val fullName = contact.fullName
                    val spouseKey = contact.spouseLookupKey

                    // 1. Insert birthdays into the birthday calendar
                    contact.birthday?.let { birthday ->
                        val title = calendarStringProvider.calendarEventTitle(fullName)
                        val description = if (birthday.hasYear) {
                            calendarStringProvider.calendarEventBirthYear(birthday.year)
                        } else {
                            calendarStringProvider.calendarEventNoYear()
                        }
                        addEvent(birthdayCalId, birthday, title, description)
                    }

                    // 2. Insert anniversaries into the anniversary calendar (if enabled)
                    if (anniversaryEnabled && anniversaryCalId != null) {
                        contact.anniversary?.let { anniversary ->
                            val description = formatAnniversaryDescription(anniversary)
                            if (spouseKey != null) {
                                if (!processedAnniversaries.contains(lookupKey)) {
                                    val spouse =
                                        contactsByLookupKey[spouseKey]?.takeIf { it.anniversary != null }
                                    if (spouse != null) {
                                        val mergedName = mergeNames(
                                            fullName,
                                            spouse.fullName
                                        )
                                        val title = calendarStringProvider.calendarEventAnniversaryTitleCouple(
                                            mergedName
                                        )
                                        addEvent(anniversaryCalId, anniversary, title, description)
                                        processedAnniversaries.add(lookupKey)
                                        processedAnniversaries.add(spouse.lookupKey)
                                    } else {
                                        val title = calendarStringProvider.calendarEventAnniversaryTitle(
                                            fullName
                                        )
                                        addEvent(anniversaryCalId, anniversary, title, description)
                                        processedAnniversaries.add(lookupKey)
                                    }
                                }
                            } else {
                                val title = calendarStringProvider.calendarEventAnniversaryTitle(
                                    fullName
                                )
                                addEvent(anniversaryCalId, anniversary, title, description)
                            }
                        }
                    }

                    // 3. Insert name days into the name day calendar (if enabled)
                    if (nameDayEnabled && nameDayCalId != null) {
                        contact.nameDay?.let { nameDay ->
                            val title = calendarStringProvider.calendarEventNameDayTitle(fullName)
                            val description = calendarStringProvider.calendarEventNameDayDescription(fullName)
                            addEvent(nameDayCalId, nameDay, title, description)
                        }
                    }

                    // 4. Insert enabled custom/other date-field events into their respective calendars
                    if (customEventCalIds.isNotEmpty()) {
                        for ((eventKey, eventDate) in contact.customEvents) {
                            val calId = customEventCalIds[eventKey] ?: continue
                            val eventDisplayName = resolveCustomEventDisplayName(eventKey)
                            val title = calendarStringProvider.calendarEventCustomTitle(
                                fullName,
                                eventDisplayName
                            )
                            val description = formatCustomEventDescription(eventDisplayName, eventDate)
                            addEvent(calId, eventDate, title, description)
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
        internal const val CUSTOM_EVENT_CALENDAR_PREFIX = "BirthdayBuddy_Event_"
    }
}
