package com.heckmannch.birthdaybuddy.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone
import javax.inject.Inject

class SystemCalendarDataSourceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SystemCalendarDataSource {

    override fun hasCalendarPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun findCalendarIdByName(calendarName: String): Long? =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(CalendarContract.Calendars._ID)
            val selection =
                "${CalendarContract.Calendars.NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
            val selectionArgs =
                arrayOf(
                    calendarName,
                    SystemCalendarDataSource.ACCOUNT_NAME,
                    CalendarContract.ACCOUNT_TYPE_LOCAL
                )
            try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        return@withContext cursor.getLong(0)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SystemCalendarDS", "Error finding calendar by name: $calendarName", e)
            }
            null
        }

    override suspend fun createLocalCalendar(
        calendarName: String,
        displayName: String,
        color: Int
    ): Long? = withContext(Dispatchers.IO) {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_NAME,
            SystemCalendarDataSource.ACCOUNT_NAME
        )
        builder.appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        val uri = builder.build()

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, SystemCalendarDataSource.ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, calendarName)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
            put(CalendarContract.Calendars.CALENDAR_COLOR, color)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_OWNER
            )
            put(
                CalendarContract.Calendars.OWNER_ACCOUNT,
                SystemCalendarDataSource.OWNER_ACCOUNT
            )
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
                "SystemCalendarDS",
                "Successfully created local calendar $calendarName with ID: $insertedId"
            )
            return@withContext insertedId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("SystemCalendarDS", "Failed to create local calendar $calendarName", e)
        }
        null
    }

    override suspend fun getOrCreateCalendar(
        calendarName: String,
        displayName: String,
        color: Int
    ): Long? {
        val existingId = findCalendarIdByName(calendarName)
        if (existingId != null) {
            return existingId
        }
        return createLocalCalendar(calendarName, displayName, color)
    }

    override suspend fun deleteCalendarById(
        calendarId: Long,
        accountName: String,
        accountType: String
    ): Boolean = withContext(Dispatchers.IO) {
        val builder = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        builder.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        builder.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
        val uri = builder.build()
        try {
            val deletedRows = context.contentResolver.delete(
                uri,
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calendarId.toString())
            )
            Log.d(
                "SystemCalendarDS",
                "Successfully deleted calendar ID: $calendarId ($accountName, $accountType)"
            )
            return@withContext deletedRows > 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                "SystemCalendarDS",
                "Failed to delete calendar ID: $calendarId ($accountName, $accountType)",
                e
            )
            false
        }
    }

    override suspend fun updateCalendarColor(calendarId: Long, newColor: Int): Boolean =
        withContext(Dispatchers.IO) {
            val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SystemCalendarDS", "Error updating color for calendar: $calendarId", e)
                false
            }
        }

    override suspend fun queryAllCalendars(): List<SystemCalendarInfo> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.NAME,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.VISIBLE
            )
            val list = mutableListOf<SystemCalendarInfo>()
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
                    val dispNameCol =
                        cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                    val visibleCol = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val accName = cursor.getString(accNameCol)
                        val accType = cursor.getString(accTypeCol)
                        val name = cursor.getString(nameCol) ?: ""
                        val dispName = cursor.getString(dispNameCol)
                        val visible = cursor.getInt(visibleCol)
                        list.add(
                            SystemCalendarInfo(
                                id = id,
                                name = name,
                                accountName = accName,
                                accountType = accType,
                                displayName = dispName,
                                visible = visible
                            )
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SystemCalendarDS", "Failed to query calendars", e)
            }
            list
        }

    override suspend fun clearCalendarEvents(calendarId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val deleteUri = CalendarContract.Events.CONTENT_URI.buildUpon()
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
            try {
                val deletedRows = context.contentResolver.delete(
                    deleteUri,
                    "${CalendarContract.Events.CALENDAR_ID} = ?",
                    arrayOf(calendarId.toString())
                )
                return@withContext deletedRows >= 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SystemCalendarDS", "Error clearing events for calendar: $calendarId", e)
                false
            }
        }

    override suspend fun applyBatch(operations: List<ContentProviderOperation>): Boolean =
        withContext(Dispatchers.IO) {
            if (operations.isEmpty()) return@withContext true
            try {
                val arrayList = ArrayList<ContentProviderOperation>(operations)
                context.contentResolver.applyBatch(CalendarContract.AUTHORITY, arrayList)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SystemCalendarDS", "Error applying batch operations", e)
                false
            }
        }
}
