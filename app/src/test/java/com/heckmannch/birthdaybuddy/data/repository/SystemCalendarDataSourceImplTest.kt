package com.heckmannch.birthdaybuddy.data.repository

import android.Manifest
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemCalendarDataSourceImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk(relaxed = true)

    private lateinit var dataSource: SystemCalendarDataSourceImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        mockkStatic(ContextCompat::class)

        every { context.contentResolver } returns contentResolver

        dataSource = SystemCalendarDataSourceImpl(
            context = context,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // =========================================================================
    // Permission Tests
    // =========================================================================

    @Test
    fun `hasCalendarPermissions - when both READ and WRITE permissions are granted - returns true`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED

        assertThat(dataSource.hasCalendarPermissions()).isTrue()
    }

    @Test
    fun `hasCalendarPermissions - when READ_CALENDAR is denied - returns false`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED

        assertThat(dataSource.hasCalendarPermissions()).isFalse()
    }

    @Test
    fun `hasCalendarPermissions - when WRITE_CALENDAR is denied - returns false`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED

        assertThat(dataSource.hasCalendarPermissions()).isFalse()
    }

    // =========================================================================
    // findCalendarIdByName Tests
    // =========================================================================

    @Test
    fun `findCalendarIdByName - when calendar exists - queries with correct projection and returns id`() = runTest {
        val cursor: Cursor = mockk(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(0) } returns 42L
        every { cursor.close() } returns Unit

        val projectionSlot = slot<Array<String>>()
        val selectionSlot = slot<String>()
        val selectionArgsSlot = slot<Array<String>>()

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                capture(projectionSlot),
                capture(selectionSlot),
                capture(selectionArgsSlot),
                null
            )
        } returns cursor

        val calendarId = dataSource.findCalendarIdByName("BirthdayBuddy_Birthdays")

        assertThat(calendarId).isEqualTo(42L)
        assertThat(projectionSlot.captured).asList().containsExactly(CalendarContract.Calendars._ID)
        assertThat(selectionSlot.captured).contains(CalendarContract.Calendars.NAME)
        assertThat(selectionSlot.captured).contains(CalendarContract.Calendars.ACCOUNT_NAME)
        assertThat(selectionSlot.captured).contains(CalendarContract.Calendars.ACCOUNT_TYPE)
        assertThat(selectionArgsSlot.captured).asList().containsExactly(
            "BirthdayBuddy_Birthdays",
            SystemCalendarDataSource.ACCOUNT_NAME,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
    }

    @Test
    fun `findCalendarIdByName - when calendar not found - returns null`() = runTest {
        val cursor: Cursor = mockk(relaxed = true)
        every { cursor.moveToFirst() } returns false
        every { cursor.close() } returns Unit

        every {
            contentResolver.query(any(), any(), any(), any(), null)
        } returns cursor

        val calendarId = dataSource.findCalendarIdByName("NonExistentCalendar")

        assertThat(calendarId).isNull()
    }

    @Test
    fun `findCalendarIdByName - when cursor is null - returns null`() = runTest {
        every {
            contentResolver.query(any(), any(), any(), any(), null)
        } returns null

        val calendarId = dataSource.findCalendarIdByName("UnknownCalendar")

        assertThat(calendarId).isNull()
    }

    @Test
    fun `findCalendarIdByName - when query throws generic exception - catches and returns null`() = runTest {
        every {
            contentResolver.query(any(), any(), any(), any(), null)
        } throws RuntimeException("ContentResolver query failed")

        val calendarId = dataSource.findCalendarIdByName("ErrorCalendar")

        assertThat(calendarId).isNull()
    }

    @Test
    fun `findCalendarIdByName - when query throws CancellationException - rethrows CancellationException`() = runTest {
        every {
            contentResolver.query(any(), any(), any(), any(), null)
        } throws CancellationException("Coroutine cancelled")

        var thrown: CancellationException? = null
        try {
            dataSource.findCalendarIdByName("CancelledCalendar")
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Coroutine cancelled")
    }

    // =========================================================================
    // createLocalCalendar Tests
    // =========================================================================

    @Test
    fun `createLocalCalendar - inserts local calendar with correct uri and values and returns id`() = runTest {
        val uriSlot = slot<Uri>()
        val valuesSlot = slot<ContentValues>()

        every {
            contentResolver.insert(capture(uriSlot), capture(valuesSlot))
        } returns Uri.parse("content://com.android.calendar/calendars/123")

        val newId = dataSource.createLocalCalendar(
            calendarName = "BirthdayBuddy_Birthdays",
            displayName = "Geburtstage",
            color = 0xFF5722
        )

        assertThat(newId).isEqualTo(123L)

        // Verify SyncAdapter parameters in URI
        val uri = uriSlot.captured
        assertThat(uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo(SystemCalendarDataSource.ACCOUNT_NAME)
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)

        // Verify ContentValues
        val values = valuesSlot.captured
        assertThat(values.getAsString(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo(SystemCalendarDataSource.ACCOUNT_NAME)
        assertThat(values.getAsString(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)
        assertThat(values.getAsString(CalendarContract.Calendars.NAME)).isEqualTo("BirthdayBuddy_Birthdays")
        assertThat(values.getAsString(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)).isEqualTo("Geburtstage")
        assertThat(values.getAsInteger(CalendarContract.Calendars.CALENDAR_COLOR)).isEqualTo(0xFF5722)
        assertThat(values.getAsInteger(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)).isEqualTo(CalendarContract.Calendars.CAL_ACCESS_OWNER)
        assertThat(values.getAsString(CalendarContract.Calendars.OWNER_ACCOUNT)).isEqualTo(SystemCalendarDataSource.OWNER_ACCOUNT)
        assertThat(values.getAsInteger(CalendarContract.Calendars.CAN_ORGANIZER_RESPOND)).isEqualTo(1)
        assertThat(values.getAsInteger(CalendarContract.Calendars.CAN_MODIFY_TIME_ZONE)).isEqualTo(1)
        assertThat(values.getAsInteger(CalendarContract.Calendars.SYNC_EVENTS)).isEqualTo(1)
        assertThat(values.getAsInteger(CalendarContract.Calendars.VISIBLE)).isEqualTo(1)
    }

    @Test
    fun `createLocalCalendar - when insert returns null uri - returns null`() = runTest {
        every { contentResolver.insert(any(), any()) } returns null

        val result = dataSource.createLocalCalendar("Birthdays", "Geburtstage", 0xFF0000)

        assertThat(result).isNull()
    }

    @Test
    fun `createLocalCalendar - when insert throws CancellationException - rethrows CancellationException`() = runTest {
        every {
            contentResolver.insert(any(), any())
        } throws CancellationException("Cancelled during calendar creation")

        var thrown: CancellationException? = null
        try {
            dataSource.createLocalCalendar("Birthdays", "Geburtstage", 0xFF0000)
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Cancelled during calendar creation")
    }

    // =========================================================================
    // getOrCreateCalendar Tests (Lookup + Create)
    // =========================================================================

    @Test
    fun `getOrCreateCalendar - lookup hit - returns existing calendar id without inserting new calendar`() = runTest {
        val cursor: Cursor = mockk(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(0) } returns 88L
        every { cursor.close() } returns Unit

        every {
            contentResolver.query(any(), any(), any(), any(), null)
        } returns cursor

        val calendarId = dataSource.getOrCreateCalendar("BirthdayBuddy_Birthdays", "Geburtstage", 0xFF0000)

        assertThat(calendarId).isEqualTo(88L)
        verify(exactly = 0) { contentResolver.insert(any(), any()) }
    }

    @Test
    fun `getOrCreateCalendar - lookup miss - creates local calendar with correct values and returns new id`() = runTest {
        val cursor: Cursor = mockk(relaxed = true)
        every { cursor.moveToFirst() } returns false
        every { cursor.close() } returns Unit

        every {
            contentResolver.query(any(), any(), any(), any(), null)
        } returns cursor

        every {
            contentResolver.insert(any(), any())
        } returns Uri.parse("content://com.android.calendar/calendars/99")

        val calendarId = dataSource.getOrCreateCalendar("BirthdayBuddy_Anniversaries", "Jahrestage", 0x00FF00)

        assertThat(calendarId).isEqualTo(99L)
        verify(exactly = 1) { contentResolver.insert(any(), any()) }
    }

    // =========================================================================
    // getAllCalendars (queryAllCalendars) Tests
    // =========================================================================

    @Test
    fun `getAllCalendars - queries ContentResolver and maps cursor rows to SystemCalendarInfo objects`() = runTest {
        val cursor: Cursor = mockk(relaxed = true)

        every { cursor.getColumnIndex(CalendarContract.Calendars._ID) } returns 0
        every { cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME) } returns 1
        every { cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE) } returns 2
        every { cursor.getColumnIndex(CalendarContract.Calendars.NAME) } returns 3
        every { cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME) } returns 4
        every { cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE) } returns 5

        // Two rows in cursor
        every { cursor.moveToNext() } returnsMany listOf(true, true, false)
        every { cursor.getLong(0) } returnsMany listOf(1L, 2L)
        every { cursor.getString(1) } returnsMany listOf("BirthdayBuddy", "Google")
        every { cursor.getString(2) } returnsMany listOf("LOCAL", "com.google")
        every { cursor.getString(3) } returnsMany listOf("BirthdayBuddy_Birthdays", "Work")
        every { cursor.getString(4) } returnsMany listOf("Geburtstage", "Arbeit")
        every { cursor.getInt(5) } returnsMany listOf(1, 0)
        every { cursor.close() } returns Unit

        every {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                any(),
                null,
                null,
                null
            )
        } returns cursor

        val calendars = dataSource.queryAllCalendars()

        assertThat(calendars).hasSize(2)
        assertThat(calendars[0]).isEqualTo(
            SystemCalendarInfo(
                id = 1L,
                name = "BirthdayBuddy_Birthdays",
                accountName = "BirthdayBuddy",
                accountType = "LOCAL",
                displayName = "Geburtstage",
                visible = 1
            )
        )
        assertThat(calendars[1]).isEqualTo(
            SystemCalendarInfo(
                id = 2L,
                name = "Work",
                accountName = "Google",
                accountType = "com.google",
                displayName = "Arbeit",
                visible = 0
            )
        )
    }

    @Test
    fun `getAllCalendars - when cursor is null - returns empty list`() = runTest {
        every {
            contentResolver.query(any(), any(), null, null, null)
        } returns null

        val calendars = dataSource.queryAllCalendars()

        assertThat(calendars).isEmpty()
    }

    @Test
    fun `getAllCalendars - when query throws CancellationException - rethrows CancellationException`() = runTest {
        every {
            contentResolver.query(any(), any(), null, null, null)
        } throws CancellationException("Cancelled")

        var thrown: CancellationException? = null
        try {
            dataSource.queryAllCalendars()
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Cancelled")
    }

    // =========================================================================
    // deleteAllEvents (clearCalendarEvents) Tests
    // =========================================================================

    @Test
    fun `deleteAllEvents - clearCalendarEvents deletes all events for specified calendar id with sync adapter uri`() = runTest {
        val uriSlot = slot<Uri>()
        val selectionSlot = slot<String>()
        val selectionArgsSlot = slot<Array<String>>()

        every {
            contentResolver.delete(
                capture(uriSlot),
                capture(selectionSlot),
                capture(selectionArgsSlot)
            )
        } returns 5

        val result = dataSource.clearCalendarEvents(42L)

        assertThat(result).isTrue()

        val uri = uriSlot.captured
        assertThat(uri.authority).isEqualTo(CalendarContract.AUTHORITY)
        assertThat(uri.path).isEqualTo("/events")
        assertThat(uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo(SystemCalendarDataSource.ACCOUNT_NAME)
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)

        assertThat(selectionSlot.captured).isEqualTo("${CalendarContract.Events.CALENDAR_ID} = ?")
        assertThat(selectionArgsSlot.captured).asList().containsExactly("42")
    }

    @Test
    fun `deleteAllEvents - when delete throws CancellationException - rethrows CancellationException`() = runTest {
        every {
            contentResolver.delete(any(), any(), any())
        } throws CancellationException("Cancelled clearing events")

        var thrown: CancellationException? = null
        try {
            dataSource.clearCalendarEvents(42L)
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Cancelled clearing events")
    }

    @Test
    fun `deleteAllEvents - when delete throws generic exception - returns false`() = runTest {
        every {
            contentResolver.delete(any(), any(), any())
        } throws RuntimeException("Delete failed")

        val result = dataSource.clearCalendarEvents(42L)

        assertThat(result).isFalse()
    }

    // =========================================================================
    // deleteCalendarById Tests
    // =========================================================================

    @Test
    fun `deleteCalendarById - deletes calendar with sync adapter uri and returns true on success`() = runTest {
        val uriSlot = slot<Uri>()
        val selectionSlot = slot<String>()
        val selectionArgsSlot = slot<Array<String>>()

        every {
            contentResolver.delete(
                capture(uriSlot),
                capture(selectionSlot),
                capture(selectionArgsSlot)
            )
        } returns 1

        val result = dataSource.deleteCalendarById(15L, "CustomAccount", "CustomType")

        assertThat(result).isTrue()
        val uri = uriSlot.captured
        assertThat(uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo("CustomAccount")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo("CustomType")
        assertThat(selectionSlot.captured).isEqualTo("${CalendarContract.Calendars._ID} = ?")
        assertThat(selectionArgsSlot.captured).asList().containsExactly("15")
    }

    @Test
    fun `deleteCalendarById - when no rows deleted - returns false`() = runTest {
        every { contentResolver.delete(any(), any(), any()) } returns 0

        val result = dataSource.deleteCalendarById(999L, "acc", "type")

        assertThat(result).isFalse()
    }

    @Test
    fun `deleteCalendarById - when delete throws CancellationException - rethrows CancellationException`() = runTest {
        every {
            contentResolver.delete(any(), any(), any())
        } throws CancellationException("Delete cancelled")

        var thrown: CancellationException? = null
        try {
            dataSource.deleteCalendarById(15L, "acc", "type")
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Delete cancelled")
    }

    // =========================================================================
    // updateCalendarColor Tests
    // =========================================================================

    @Test
    fun `updateCalendarColor - updates color with sync adapter uri and returns true when rows updated`() = runTest {
        val uriSlot = slot<Uri>()
        val valuesSlot = slot<ContentValues>()
        val selectionSlot = slot<String>()
        val selectionArgsSlot = slot<Array<String>>()

        every {
            contentResolver.update(
                capture(uriSlot),
                capture(valuesSlot),
                capture(selectionSlot),
                capture(selectionArgsSlot)
            )
        } returns 1

        val result = dataSource.updateCalendarColor(20L, 0x123456)

        assertThat(result).isTrue()
        val uri = uriSlot.captured
        assertThat(uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo(SystemCalendarDataSource.ACCOUNT_NAME)
        assertThat(uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)

        val values = valuesSlot.captured
        assertThat(values.getAsInteger(CalendarContract.Calendars.CALENDAR_COLOR)).isEqualTo(0x123456)

        assertThat(selectionSlot.captured).isEqualTo("${CalendarContract.Calendars._ID} = ?")
        assertThat(selectionArgsSlot.captured).asList().containsExactly("20")
    }

    @Test
    fun `updateCalendarColor - when update throws CancellationException - rethrows CancellationException`() = runTest {
        every {
            contentResolver.update(any(), any(), any(), any())
        } throws CancellationException("Update cancelled")

        var thrown: CancellationException? = null
        try {
            dataSource.updateCalendarColor(20L, 0x123456)
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Update cancelled")
    }

    // =========================================================================
    // applyBatch & buildBirthdayEventOps / buildAnniversaryEventOps Tests
    // =========================================================================

    @Test
    fun `applyBatch - when operations list is empty - returns true immediately without calling ContentResolver`() = runTest {
        val result = dataSource.applyBatch(emptyList())

        assertThat(result).isTrue()
        verify(exactly = 0) { contentResolver.applyBatch(any(), any()) }
    }

    @Test
    fun `applyBatch - buildBirthdayEventOps - applies batch operations and verifies ContentProviderOperation URIs and values`() = runTest {
        val birthdayOp = buildBirthdayEventOps(
            calendarId = 10L,
            title = "Max Mustermann",
            description = "Geboren 1990",
            dtStart = 642384000000L
        )

        val opsSlot = slot<ArrayList<ContentProviderOperation>>()
        every { contentResolver.applyBatch(CalendarContract.AUTHORITY, capture(opsSlot)) } returns arrayOf()

        val result = dataSource.applyBatch(listOf(birthdayOp))

        assertThat(result).isTrue()
        verify(exactly = 1) { contentResolver.applyBatch(CalendarContract.AUTHORITY, any()) }

        val capturedList = opsSlot.captured
        assertThat(capturedList).hasSize(1)
        val capturedOp = capturedList[0]

        // Verify URI
        assertThat(capturedOp.uri.authority).isEqualTo(CalendarContract.AUTHORITY)
        assertThat(capturedOp.uri.path).isEqualTo("/events")
        assertThat(capturedOp.uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(capturedOp.uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo(SystemCalendarDataSource.ACCOUNT_NAME)
        assertThat(capturedOp.uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)
        assertThat(capturedOp.isInsert).isTrue()
    }

    @Test
    fun `applyBatch - buildAnniversaryEventOps - applies batch operations and verifies ContentProviderOperation URIs and values`() = runTest {
        val anniversaryOp = buildAnniversaryEventOps(
            calendarId = 20L,
            title = "Max & Erika Hochzeitstag",
            description = "10. Hochzeitstag",
            dtStart = 1434326400000L
        )

        val opsSlot = slot<ArrayList<ContentProviderOperation>>()
        every { contentResolver.applyBatch(CalendarContract.AUTHORITY, capture(opsSlot)) } returns arrayOf()

        val result = dataSource.applyBatch(listOf(anniversaryOp))

        assertThat(result).isTrue()
        verify(exactly = 1) { contentResolver.applyBatch(CalendarContract.AUTHORITY, any()) }

        val capturedList = opsSlot.captured
        assertThat(capturedList).hasSize(1)
        val capturedOp = capturedList[0]

        // Verify URI & Operation type
        assertThat(capturedOp.uri.authority).isEqualTo(CalendarContract.AUTHORITY)
        assertThat(capturedOp.uri.path).isEqualTo("/events")
        assertThat(capturedOp.uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER)).isEqualTo("true")
        assertThat(capturedOp.uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME)).isEqualTo(SystemCalendarDataSource.ACCOUNT_NAME)
        assertThat(capturedOp.uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE)).isEqualTo(CalendarContract.ACCOUNT_TYPE_LOCAL)
        assertThat(capturedOp.isInsert).isTrue()
    }

    @Test
    fun `applyBatch - when applyBatch throws generic exception - logs and returns false`() = runTest {
        val op = buildBirthdayEventOps(1L, "Title", "Desc", 0L)
        every {
            contentResolver.applyBatch(any(), any())
        } throws RuntimeException("Remote batch failure")

        val result = dataSource.applyBatch(listOf(op))

        assertThat(result).isFalse()
    }

    @Test
    fun `applyBatch - when applyBatch throws CancellationException - rethrows CancellationException`() = runTest {
        val op = buildBirthdayEventOps(1L, "Title", "Desc", 0L)
        every {
            contentResolver.applyBatch(any(), any())
        } throws CancellationException("Batch cancelled")

        var thrown: CancellationException? = null
        try {
            dataSource.applyBatch(listOf(op))
        } catch (e: CancellationException) {
            thrown = e
        }
        assertThat(thrown).isNotNull()
        assertThat(thrown).hasMessageThat().contains("Batch cancelled")
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun buildBirthdayEventOps(
        calendarId: Long,
        title: String,
        description: String,
        dtStart: Long
    ): ContentProviderOperation {
        val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
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

        return ContentProviderOperation.newInsert(uri)
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
    }

    private fun buildAnniversaryEventOps(
        calendarId: Long,
        title: String,
        description: String,
        dtStart: Long
    ): ContentProviderOperation {
        val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
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

        return ContentProviderOperation.newInsert(uri)
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
    }
}
