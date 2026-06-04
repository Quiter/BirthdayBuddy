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

    suspend fun getOrCreateCalendar(): Long? = withContext(Dispatchers.IO) {
        val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
        val calendarId = currentSettings.calendarId

        if (calendarId != null) {
            val exists = checkCalendarExists(calendarId)
            val accountName = getCalendarAccountName(calendarId)
            if (exists && accountName == "phone") {
                // Bereinige etwaige Duplikate im Hintergrund
                cleanDuplicateCalendarsAndGetId()
                return@withContext calendarId
            } else if (exists) {
                // Upgrade/Bereinigung von Altkalendern
                val accountType = getCalendarAccountType(calendarId) ?: CalendarContract.ACCOUNT_TYPE_LOCAL
                deleteCalendarById(calendarId, accountName ?: "phone", accountType)
                appSettingsDao.upsertSettings(currentSettings.copy(calendarId = null))
            }
        }

        val existingId = cleanDuplicateCalendarsAndGetId()
        if (existingId != null) {
            appSettingsDao.upsertSettings(currentSettings.copy(calendarId = existingId))
            return@withContext existingId
        }

        val newId = createLocalCalendar()
        if (newId != null) {
            appSettingsDao.upsertSettings(currentSettings.copy(calendarId = newId))
        }
        newId
    }

    private fun checkCalendarExists(calendarId: Long): Boolean {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                return cursor.moveToFirst()
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error checking if calendar exists", e)
        }
        return false
    }

    private fun getCalendarAccountName(calendarId: Long): String? {
        val projection = arrayOf(CalendarContract.Calendars.ACCOUNT_NAME)
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error getting calendar account name", e)
        }
        return null
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

    private fun getCalendarAccountType(calendarId: Long): String? {
        val projection = arrayOf(CalendarContract.Calendars.ACCOUNT_TYPE)
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error getting calendar account type", e)
        }
        return null
    }

    private fun cleanDuplicateCalendarsAndGetId(): Long? {
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

                var firstValidId: Long? = null

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accountName = cursor.getString(accNameCol)
                    val accountType = cursor.getString(accTypeCol)
                    val name = cursor.getString(nameCol)

                    if (name == "BirthdayBuddyCalendar") {
                        if (accountName == "phone" && accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
                            if (firstValidId == null) {
                                firstValidId = id
                            } else {
                                // Dies ist ein Duplikat, löschen!
                                deleteCalendarById(id, accountName, accountType)
                            }
                        } else {
                            // Altkalender (falscher Account oder Typ) - löschen!
                            deleteCalendarById(id, accountName, accountType)
                        }
                    }
                }
                return firstValidId
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error cleaning duplicate calendars", e)
        }
        return null
    }

    private fun createLocalCalendar(): Long? {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "phone")
        builder.appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        val uri = builder.build()

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "phone")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "BirthdayBuddyCalendar")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "BirthdayBuddy")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFFE91E63.toInt()) // Premium pink color
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "phone@local")
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
            put(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND, 1)
            put(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
        }

        try {
            val resultUri = context.contentResolver.insert(uri, values)
            val insertedId = resultUri?.lastPathSegment?.toLongOrNull()
            Log.d("CalendarSyncRepo", "Successfully created local calendar with ID: $insertedId")
            return insertedId
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Failed to create local calendar", e)
        }
        return null
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

                    if (name == "BirthdayBuddyCalendar") {
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

        val calendarId = getOrCreateCalendar() ?: return@withContext false

        try {
            // Events permanent als Sync-Adapter löschen, um Datenmüll/Tombstones in lokalen Kalendern zu verhindern
            val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "phone")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build()

            context.contentResolver.delete(
                deleteUri,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )

            val currentSettings = appSettingsDao.getSettingsImmediate() ?: AppSettings()
            val otherEventsEnabled = currentSettings.otherEventsEnabled

            val hasEvents = contacts.any {
                it.birthday != null || (otherEventsEnabled && (it.anniversary != null || it.nameDay != null))
            }
            if (!hasEvents) return@withContext true

            val operations = ArrayList<ContentProviderOperation>()

            fun addEvent(date: java.time.LocalDate, title: String, description: String) {
                val year = if (date.hasYear) date.year else 2000
                val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, date.monthValue - 1, date.dayOfMonth, 0, 0, 0)
                }
                val dtStart = startCal.timeInMillis

                val op = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
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
                // Birthdays
                contact.birthday?.let { birthday ->
                    val title = context.getString(R.string.calendar_event_title, contact.fullName)
                    val description = if (birthday.hasYear) {
                        context.getString(R.string.calendar_event_birth_year, birthday.year)
                    } else {
                        context.getString(R.string.calendar_event_no_year)
                    }
                    addEvent(birthday, title, description)
                }

                // Anniversaries (if enabled)
                if (otherEventsEnabled) {
                    contact.anniversary?.let { anniversary ->
                        val spouseKey = contact.spouseLookupKey
                        if (spouseKey != null) {
                            if (!processedAnniversaries.contains(contact.lookupKey)) {
                                val spouse = contacts.find { it.lookupKey == spouseKey && it.anniversary != null }
                                if (spouse != null) {
                                    val mergedName = com.heckmannch.birthdaybuddy.util.mergeNames(contact.fullName, spouse.fullName)
                                    val title = context.getString(R.string.calendar_event_anniversary_title, mergedName)
                                    val description = if (anniversary.hasYear) {
                                        context.getString(R.string.calendar_event_anniversary_year, anniversary.year)
                                    } else {
                                        context.getString(R.string.calendar_event_anniversary_no_year)
                                    }
                                    addEvent(anniversary, title, description)
                                    processedAnniversaries.add(contact.lookupKey)
                                    processedAnniversaries.add(spouse.lookupKey)
                                } else {
                                    val title = context.getString(R.string.calendar_event_anniversary_title, contact.fullName)
                                    val description = if (anniversary.hasYear) {
                                        context.getString(R.string.calendar_event_anniversary_year, anniversary.year)
                                    } else {
                                        context.getString(R.string.calendar_event_anniversary_no_year)
                                    }
                                    addEvent(anniversary, title, description)
                                    processedAnniversaries.add(contact.lookupKey)
                                }
                            }
                        } else {
                            val title = context.getString(R.string.calendar_event_anniversary_title, contact.fullName)
                            val description = if (anniversary.hasYear) {
                                context.getString(R.string.calendar_event_anniversary_year, anniversary.year)
                            } else {
                                context.getString(R.string.calendar_event_anniversary_no_year)
                            }
                            addEvent(anniversary, title, description)
                        }
                    }
                }

                // Name Days (if enabled)
                if (otherEventsEnabled) {
                    contact.nameDay?.let { nameDay ->
                        val title = context.getString(R.string.calendar_event_nameday_title, contact.fullName)
                        val description = context.getString(R.string.calendar_event_nameday_description, contact.fullName)
                        addEvent(nameDay, title, description)
                    }
                }
            }

            if (operations.isNotEmpty()) {
                context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
            }
            true
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error syncing birthdays to calendar", e)
            false
        }
    }
}
