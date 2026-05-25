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
    @ApplicationContext private val context: Context,
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
            val accountName = getCalendarAccountName(calendarId)
            if (accountName != null && accountName != "phone") {
                Log.d("CalendarSyncRepo", "Upgrading legacy calendar account '$accountName' to 'phone'...")
                deleteCalendarById(calendarId, accountName)
                appSettingsDao.upsertSettings(currentSettings.copy(calendarId = null))
            } else {
                val exists = checkCalendarExists(calendarId)
                if (exists) {
                    return@withContext calendarId
                }
            }
        }

        val existingId = findExistingCalendarId()
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

    private fun deleteCalendarById(calendarId: Long, accountName: String) {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        val uri = builder.build()
        try {
            context.contentResolver.delete(
                uri,
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calendarId.toString())
            )
            Log.d("CalendarSyncRepo", "Successfully deleted calendar ID: $calendarId ($accountName)")
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Failed to delete calendar ID: $calendarId ($accountName)", e)
        }
    }

    private fun findExistingCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.NAME
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(CalendarContract.ACCOUNT_TYPE_LOCAL)

        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val accNameCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val nameCol = cursor.getColumnIndex(CalendarContract.Calendars.NAME)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accountName = cursor.getString(accNameCol)
                    val name = cursor.getString(nameCol)

                    if (name == "BirthdayBuddyCalendar") {
                        if (accountName == "phone") {
                            return id
                        } else {
                            // Legacy calendar - delete it!
                            deleteCalendarById(id, accountName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CalendarSyncRepo", "Error finding calendar", e)
        }
        return null
    }

    private fun createLocalCalendar(): Long? {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "phone")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        val uri = builder.build()

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "phone")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "BirthdayBuddyCalendar")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "BirthdayBuddy")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFFE91E63.toInt()) // Premium pink color
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
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
        val calendarId = currentSettings.calendarId ?: findExistingCalendarId()

        var deleted = false
        if (calendarId != null) {
            val accountName = getCalendarAccountName(calendarId) ?: "phone"

            val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
            builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            val uri = builder.build()

            try {
                val deletedRows = context.contentResolver.delete(
                    uri,
                    "${CalendarContract.Calendars._ID} = ?",
                    arrayOf(calendarId.toString())
                )
                deleted = deletedRows > 0
            } catch (e: Exception) {
                Log.e("CalendarSyncRepo", "Failed to delete calendar", e)
            }
        }

        // Update local settings in database
        appSettingsDao.upsertSettings(
            currentSettings.copy(
                calendarSyncEnabled = false,
                calendarId = null
            )
        )
        deleted
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
                val dispNameCol = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val visibleCol = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val accName = cursor.getString(accNameCol)
                    val accType = cursor.getString(accTypeCol)
                    val name = cursor.getString(nameCol)
                    val dispName = cursor.getString(dispNameCol)
                    val visible = cursor.getInt(visibleCol)
                    Log.d("CalendarSyncRepo", "Calendar ID: $id | AccName: $accName | AccType: $accType | Name: $name | DispName: $dispName | Visible: $visible")
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
            // Delete all existing events on our specific calendarId to avoid drift
            context.contentResolver.delete(
                CalendarContract.Events.CONTENT_URI,
                "${CalendarContract.Events.CALENDAR_ID} = ?",
                arrayOf(calendarId.toString())
            )

            val contactsWithBirthday = contacts.filter { it.birthday != null }
            if (contactsWithBirthday.isEmpty()) return@withContext true

            val operations = ArrayList<ContentProviderOperation>()
            for (contact in contactsWithBirthday) {
                val birthday = contact.birthday ?: continue
                val year = if (birthday.hasYear) birthday.year else 2000

                // Get UTC midnight start
                val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, birthday.monthValue - 1, birthday.dayOfMonth, 0, 0, 0)
                }
                val dtStart = startCal.timeInMillis

                val title = context.getString(R.string.calendar_event_title, contact.fullName)
                val description = if (birthday.hasYear) {
                    context.getString(R.string.calendar_event_birth_year, birthday.year)
                } else {
                    context.getString(R.string.calendar_event_no_year)
                }

                val op = ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                    .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                    .withValue(CalendarContract.Events.TITLE, title)
                    .withValue(CalendarContract.Events.DESCRIPTION, description)
                    .withValue(CalendarContract.Events.DTSTART, dtStart)
                    .withValue(CalendarContract.Events.DURATION, "P1D") // 1-day duration
                    .withValue(CalendarContract.Events.RRULE, "FREQ=YEARLY")
                    .withValue(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    .withValue(CalendarContract.Events.ALL_DAY, 1)
                    .withValue(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                    .build()

                operations.add(op)

                if (operations.size >= 400) {
                    context.contentResolver.applyBatch(CalendarContract.AUTHORITY, operations)
                    operations.clear()
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
