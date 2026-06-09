package com.heckmannch.birthdaybuddy.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
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
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getOrCreateCalendar(type: CalendarType): Long? = withContext(Dispatchers.IO) {
        val existingId = findCalendarIdByName(type.calendarName)
        if (existingId != null) {
            return@withContext existingId
        }

        val newId = createLocalCalendar(type)
        newId
    }


    private fun deleteCalendarById(calendarId: Long, accountName: String, accountType: String) {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
        val uri = builder.build()
        try {
            context.contentResolver.delete(
                uri,
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calendarId.toString())
            )
            Log.d(
                "CalendarSyncRepo",
                "Successfully deleted calendar ID: $calendarId ($accountName, $accountType)"
            )
        } catch (e: Exception) {
            Log.e(
                "CalendarSyncRepo",
                "Failed to delete calendar ID: $calendarId ($accountName, $accountType)",
                e
            )
        }
    }

    private fun findCalendarIdByName(calendarName: String): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection =
            "${CalendarContract.Calendars.NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs =
            arrayOf(calendarName, "BirthdayBuddy", CalendarContract.ACCOUNT_TYPE_LOCAL)
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getLong(0)
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error finding calendar by name: $calendarName", e)
        }
        return null
    }

    suspend fun cleanCalendars(): Unit = withContext(Dispatchers.IO) {
        val activeNames = setOf(
            CalendarType.BIRTHDAY.calendarName,
            CalendarType.ANNIVERSARY.calendarName,
            CalendarType.NAMEDAY.calendarName
        )
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.NAME
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val accNameCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val nameCol = cursor.getColumnIndex(CalendarContract.Calendars.NAME)

                val seenActiveIds = mutableMapOf<String, Long>()

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accountName = cursor.getString(accNameCol)
                    val accountType = cursor.getString(accTypeCol)
                    when (val name = cursor.getString(nameCol)) {
                        // Lösche veraltete BirthdayBuddyCalendar (unter phone account)
                        "BirthdayBuddyCalendar" -> {
                            deleteCalendarById(id, accountName, accountType)
                        }

                        in activeNames -> {
                            if (accountName == "BirthdayBuddy" && accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
                                val existingId = seenActiveIds[name]
                                if (existingId == null) {
                                    seenActiveIds[name] = id
                                } else {
                                    // Duplikat löschen
                                    deleteCalendarById(id, accountName, accountType)
                                }
                            } else {
                                // Falscher Account-Name/Typ - löschen
                                deleteCalendarById(id, accountName, accountType)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error cleaning calendars", e)
        }
    }

    private suspend fun createLocalCalendar(type: CalendarType): Long? {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "BirthdayBuddy")
        builder.appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        val uri = builder.build()

        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
        val preferredColor = when (type) {
            CalendarType.BIRTHDAY -> currentSettings.birthdayCalendarColor
            CalendarType.ANNIVERSARY -> currentSettings.anniversaryCalendarColor
            CalendarType.NAMEDAY -> currentSettings.nameDayCalendarColor
        }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "BirthdayBuddy")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, type.calendarName)
            put(
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                context.getString(type.displayNameRes)
            )
            put(CalendarContract.Calendars.CALENDAR_COLOR, preferredColor)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "birthdaybuddy@local")
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1)
            put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        try {
            val resultUri = context.contentResolver.insert(uri, values)
            val insertedId = resultUri?.lastPathSegment?.toLongOrNull()
            Log.d(
                "CalendarSyncRepo",
                "Successfully created local calendar ${type.calendarName} with ID: $insertedId"
            )
            return insertedId
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Failed to create local calendar ${type.calendarName}", e)
        }
        return null
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

            val calendarId = findCalendarIdByName(type.calendarName)
            if (calendarId != null) {
                val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "BirthdayBuddy")
                    .appendQueryParameter(
                        CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.ACCOUNT_TYPE_LOCAL
                    )
                    .build()

                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.CALENDAR_COLOR, newColor)
                }

                try {
                    val updatedRows = context.contentResolver.update(
                        uri,
                        values,
                        "${CalendarContract.Calendars._ID} = ?",
                        arrayOf(calendarId.toString())
                    )
                    return@withContext updatedRows > 0
                } catch (e: Exception) {
                    Log.e("CalendarSyncRepo", "Error updating color for ${type.calendarName}", e)
                }
            }
            false
        }

    suspend fun deleteCalendar(): Boolean = withContext(Dispatchers.IO) {
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()

        var deletedAny = false
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.NAME
        )
        val allTargetNames = setOf(
            "BirthdayBuddyCalendar",
            CalendarType.BIRTHDAY.calendarName,
            CalendarType.ANNIVERSARY.calendarName,
            CalendarType.NAMEDAY.calendarName
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val accNameCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val nameCol = cursor.getColumnIndex(CalendarContract.Calendars.NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accountName = cursor.getString(accNameCol)
                    val accountType = cursor.getString(accTypeCol)
                    val name = cursor.getString(nameCol)

                    if (name in allTargetNames) {
                        deleteCalendarById(id, accountName, accountType)
                        deletedAny = true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error deleting calendars", e)
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
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.NAME,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.VISIBLE
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                Log.d("CalendarSyncRepo", "=== START DEBUG PRINT ALL CALENDARS ===")
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val accNameCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val nameCol = cursor.getColumnIndex(CalendarContract.Calendars.NAME)
                val dispNameCol =
                    cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val visibleCol = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accName = cursor.getString(accNameCol)
                    val accType = cursor.getString(accTypeCol)
                    val name = cursor.getString(nameCol)
                    val dispName = cursor.getString(dispNameCol)
                    val visible = cursor.getInt(visibleCol)
                    Log.d(
                        "CalendarSyncRepo",
                        "Calendar ID: $id | AccName: $accName | AccType: $accType | Name: $name | DispName: $dispName | Visible: $visible"
                    )
                }
                Log.d("CalendarSyncRepo", "=== END DEBUG PRINT ALL CALENDARS ===")
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Failed to query calendars", e)
        }
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
            // Hilfsfunktion zum permanenten Löschen der Termine eines Kalenders via Sync-Adapter
            fun clearCalendarEvents(calId: Long) {
                val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "BirthdayBuddy")
                    .appendQueryParameter(
                        CalendarContract.Calendars.ACCOUNT_TYPE,
                        CalendarContract.ACCOUNT_TYPE_LOCAL
                    )
                    .build()

                context.contentResolver.delete(
                    deleteUri,
                    "${CalendarContract.Events.CALENDAR_ID} = ?",
                    arrayOf(calId.toString())
                )
            }

            // Geburtstage leeren
            clearCalendarEvents(birthdayCalId)

            // Hochzeitstage leeren oder Kalender löschen falls deaktiviert
            if (anniversaryCalId != null) {
                clearCalendarEvents(anniversaryCalId)
            } else {
                findCalendarIdByName(CalendarType.ANNIVERSARY.calendarName)?.let { id ->
                    deleteCalendarById(id, "BirthdayBuddy", CalendarContract.ACCOUNT_TYPE_LOCAL)
                }
            }

            // Namenstage leeren oder Kalender löschen falls deaktiviert
            if (nameDayCalId != null) {
                clearCalendarEvents(nameDayCalId)
            } else {
                findCalendarIdByName(CalendarType.NAMEDAY.calendarName)?.let { id ->
                    deleteCalendarById(id, "BirthdayBuddy", CalendarContract.ACCOUNT_TYPE_LOCAL)
                }
            }

            val operations = ArrayList<ContentProviderOperation>()

            fun addEvent(
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

                val op = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
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
                    context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
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
                context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
            }
            true
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error syncing events to calendars", e)
            false
        }
    }
}
