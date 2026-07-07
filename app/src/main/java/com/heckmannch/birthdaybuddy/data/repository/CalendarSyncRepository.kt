package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.util.hasYear
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appSettingsDao: AppSettingsDao,
    private val systemCalendarDataSource: SystemCalendarDataSource,
) {

    enum class CalendarType(val calendarName: String, val displayNameRes: Int, val color: Int) {
        BIRTHDAY("BirthdayBuddy_Birthdays", R.string.calendar_name_birthdays, 0xFFE91E63.toInt()),
        ANNIVERSARY(
            "BirthdayBuddy_Anniversaries",
            R.string.calendar_name_anniversaries,
            0xFF9C27B0.toInt()
        ),
        NAMEDAY("BirthdayBuddy_NameDays", R.string.calendar_name_namedays, 0xFFFF9800.toInt())
    }

    fun hasCalendarPermissions(): Boolean {
        return systemCalendarDataSource.hasCalendarPermissions()
    }

    private suspend fun getOrCreateCalendar(type: CalendarType): Long? =
        withContext(Dispatchers.IO) {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
            val preferredColor = when (type) {
                CalendarType.BIRTHDAY -> currentSettings.birthdayCalendarColor
                CalendarType.ANNIVERSARY -> currentSettings.anniversaryCalendarColor
                CalendarType.NAMEDAY -> currentSettings.nameDayCalendarColor
            }
            systemCalendarDataSource.getOrCreateCalendar(
                type.calendarName,
                context.getString(type.displayNameRes),
                preferredColor
            )
        }

    private suspend fun cleanCalendars(): Unit = withContext(Dispatchers.IO) {
        val activeNames = setOf(
            CalendarType.BIRTHDAY.calendarName,
            CalendarType.ANNIVERSARY.calendarName,
            CalendarType.NAMEDAY.calendarName
        )
        val calendars = systemCalendarDataSource.queryAllCalendars()
        val seenActiveIds = mutableMapOf<String, Long>()

        for (calendar in calendars) {
            val id = calendar.id
            val accountName = calendar.accountName
            val accountType = calendar.accountType

            when (val name = calendar.name) {
                // Lösche veraltete BirthdayBuddyCalendar (unter phone account)
                "BirthdayBuddyCalendar" -> {
                    systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
                }

                in activeNames -> {
                    if (accountName == "BirthdayBuddy" && accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
                        val existingId = seenActiveIds[name]
                        if (existingId == null) {
                            seenActiveIds[name] = id
                        } else {
                            // Duplikat löschen
                            systemCalendarDataSource.deleteCalendarById(
                                id,
                                accountName,
                                accountType
                            )
                        }
                    } else {
                        // Falscher Account-Name/Typ - löschen
                        systemCalendarDataSource.deleteCalendarById(id, accountName, accountType)
                    }
                }
            }
        }
    }

    suspend fun updateCalendarColor(type: CalendarType, newColor: Int): Boolean =
        withContext(Dispatchers.IO) {
            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
            val updatedSettings = when (type) {
                CalendarType.BIRTHDAY -> currentSettings.copy(birthdayCalendarColor = newColor)
                CalendarType.ANNIVERSARY -> currentSettings.copy(anniversaryCalendarColor = newColor)
                CalendarType.NAMEDAY -> currentSettings.copy(nameDayCalendarColor = newColor)
            }
            appSettingsDao.upsertSettings(updatedSettings)

            val calendarId = systemCalendarDataSource.findCalendarIdByName(type.calendarName)
            if (calendarId != null) {
                systemCalendarDataSource.updateCalendarColor(calendarId, newColor)
            } else {
                false
            }
        }

    suspend fun deleteCalendar(): Boolean = withContext(Dispatchers.IO) {
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()

        var deletedAny = false
        val allTargetNames = setOf(
            "BirthdayBuddyCalendar",
            CalendarType.BIRTHDAY.calendarName,
            CalendarType.ANNIVERSARY.calendarName,
            CalendarType.NAMEDAY.calendarName
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

    fun debugPrintAllCalendars() {
        Log.d("CalendarSyncRepo", "=== START DEBUG PRINT ALL CALENDARS ===")
        val calendars =
            kotlinx.coroutines.runBlocking { systemCalendarDataSource.queryAllCalendars() }
        for (calendar in calendars) {
            Log.d(
                "CalendarSyncRepo",
                "Calendar ID: ${calendar.id} | AccName: ${calendar.accountName} | AccType: ${calendar.accountType} | Name: ${calendar.name} | DispName: ${calendar.displayName} | Visible: ${calendar.visible}"
            )
        }
        Log.d("CalendarSyncRepo", "=== END DEBUG PRINT ALL CALENDARS ===")
    }

    suspend fun syncBirthdays(contacts: List<Contact>): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermissions()) return@withContext false

        // Aufräumen veralteter oder doppelter Kalender vor dem Sync
        cleanCalendars()

        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
        val otherEventsEnabled = currentSettings.otherEventsEnabled

        // IDs für alle aktiven Kalender abrufen oder erstellen
        val birthdayCalId = getOrCreateCalendar(CalendarType.BIRTHDAY) ?: return@withContext false
        val anniversaryCalId =
            if (otherEventsEnabled) getOrCreateCalendar(CalendarType.ANNIVERSARY) else null
        val nameDayCalId =
            if (otherEventsEnabled) getOrCreateCalendar(CalendarType.NAMEDAY) else null

        try {
            // Geburtstage leeren
            systemCalendarDataSource.clearCalendarEvents(birthdayCalId)

            // Hochzeitstage leeren oder Kalender löschen, falls deaktiviert
            if (anniversaryCalId != null) {
                systemCalendarDataSource.clearCalendarEvents(anniversaryCalId)
            } else {
                systemCalendarDataSource.findCalendarIdByName(CalendarType.ANNIVERSARY.calendarName)
                    ?.let { id ->
                        systemCalendarDataSource.deleteCalendarById(
                            id,
                            "BirthdayBuddy",
                            CalendarContract.ACCOUNT_TYPE_LOCAL
                        )
                    }
            }

            // Namenstage leeren oder Kalender löschen, falls deaktiviert
            if (nameDayCalId != null) {
                systemCalendarDataSource.clearCalendarEvents(nameDayCalId)
            } else {
                systemCalendarDataSource.findCalendarIdByName(CalendarType.NAMEDAY.calendarName)
                    ?.let { id ->
                        systemCalendarDataSource.deleteCalendarById(
                            id,
                            "BirthdayBuddy",
                            CalendarContract.ACCOUNT_TYPE_LOCAL
                        )
                    }
            }

            val operations = ArrayList<ContentProviderOperation>()

            suspend fun addEvent(
                calId: Long,
                date: java.time.LocalDate,
                title: String,
                description: String
            ) {
                val year = if (date.hasYear) date.year else 2000
                val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, date.monthValue - 1, date.dayOfMonth, 0, 0, 0)
                }
                val dtStart = startCal.timeInMillis

                val insertUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "BirthdayBuddy")
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

                if (operations.size >= 400) {
                    systemCalendarDataSource.applyBatch(operations)
                    operations.clear()
                }
            }

            val processedAnniversaries = HashSet<String>()

            for (contact in contacts) {
                // 1. Geburtstage in den Geburtstags-Kalender eintragen
                contact.birthday?.let { birthday ->
                    val title = context.getString(R.string.calendar_event_title, contact.fullName)
                    val description = if (birthday.hasYear) {
                        context.getString(R.string.calendar_event_birth_year, birthday.year)
                    } else {
                        context.getString(R.string.calendar_event_no_year)
                    }
                    addEvent(birthdayCalId, birthday, title, description)
                }

                // 2. Hochzeitstage in den Hochzeits-Kalender eintragen (falls aktiviert)
                if (otherEventsEnabled && anniversaryCalId != null) {
                    contact.anniversary?.let { anniversary ->
                        val spouseKey = contact.spouseLookupKey
                        if (spouseKey != null) {
                            if (!processedAnniversaries.contains(contact.lookupKey)) {
                                val spouse =
                                    contacts.find { it.lookupKey == spouseKey && it.anniversary != null }
                                if (spouse != null) {
                                    val mergedName = com.heckmannch.birthdaybuddy.util.mergeNames(
                                        contact.fullName,
                                        spouse.fullName
                                    )
                                    val title = context.getString(
                                        R.string.calendar_event_anniversary_title,
                                        mergedName
                                    )
                                    val description = if (anniversary.hasYear) {
                                        context.getString(
                                            R.string.calendar_event_anniversary_year,
                                            anniversary.year
                                        )
                                    } else {
                                        context.getString(R.string.calendar_event_anniversary_no_year)
                                    }
                                    addEvent(anniversaryCalId, anniversary, title, description)
                                    processedAnniversaries.add(contact.lookupKey)
                                    processedAnniversaries.add(spouse.lookupKey)
                                } else {
                                    val title = context.getString(
                                        R.string.calendar_event_anniversary_title,
                                        contact.fullName
                                    )
                                    val description = if (anniversary.hasYear) {
                                        context.getString(
                                            R.string.calendar_event_anniversary_year,
                                            anniversary.year
                                        )
                                    } else {
                                        context.getString(R.string.calendar_event_anniversary_no_year)
                                    }
                                    addEvent(anniversaryCalId, anniversary, title, description)
                                    processedAnniversaries.add(contact.lookupKey)
                                }
                            }
                        } else {
                            val title = context.getString(
                                R.string.calendar_event_anniversary_title,
                                contact.fullName
                            )
                            val description = if (anniversary.hasYear) {
                                context.getString(
                                    R.string.calendar_event_anniversary_year,
                                    anniversary.year
                                )
                            } else {
                                context.getString(R.string.calendar_event_anniversary_no_year)
                            }
                            addEvent(anniversaryCalId, anniversary, title, description)
                        }
                    }
                }

                // 3. Namenstage in den Namenstags-Kalender eintragen (falls aktiviert)
                if (otherEventsEnabled && nameDayCalId != null) {
                    contact.nameDay?.let { nameDay ->
                        val title = context.getString(
                            R.string.calendar_event_nameday_title,
                            contact.fullName
                        )
                        val description = context.getString(
                            R.string.calendar_event_nameday_description,
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
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error syncing events to calendars", e)
            false
        }
    }
}
