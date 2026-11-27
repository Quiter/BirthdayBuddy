package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.util.CalendarStringProvider
import com.heckmannch.birthdaybuddy.domain.util.NO_YEAR_MARKER
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

class CalendarSyncRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val systemCalendarDataSource: SystemCalendarDataSource = mockk(relaxed = true)
    private val calendarStringProvider: CalendarStringProvider = mockk(relaxed = true)

    private lateinit var repository: CalendarSyncRepositoryImpl

    private val mockUri = mockk<Uri>(relaxed = true)
    private val mockUriBuilder = mockk<Uri.Builder>(relaxed = true)
    private val mockOpBuilder = mockk<ContentProviderOperation.Builder>(relaxed = true)
    private val mockOp = mockk<ContentProviderOperation>(relaxed = true)

    @Before
    fun setUp() {
        // Mock static Uri, ContentProviderOperation and Log to intercept static initializers and calls
        mockkStatic(Uri::class)
        mockkStatic(ContentProviderOperation::class)
        mockkStatic(Log::class)

        every { Uri.parse(any()) } returns mockUri
        every { mockUri.buildUpon() } returns mockUriBuilder
        every { mockUriBuilder.appendQueryParameter(any(), any()) } returns mockUriBuilder
        every { mockUriBuilder.build() } returns mockUri

        every { ContentProviderOperation.newInsert(any()) } returns mockOpBuilder
        every { mockOpBuilder.withValue(any(), any()) } returns mockOpBuilder
        every { mockOpBuilder.build() } returns mockOp

        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } answers {
            println("Log.e: ${arg<String>(0)} - ${arg<String>(1)}")
            0
        }
        every { Log.e(any(), any(), any()) } answers {
            println("Log.e: ${arg<String>(0)} - ${arg<String>(1)}")
            arg<Throwable>(2).printStackTrace()
            0
        }
        // Use sun.misc.Unsafe via reflection to bypass Java 17 static final field restrictions and mock CONTENT_URI fields
        try {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val theUnsafeField = unsafeClass.getDeclaredField("theUnsafe")
            theUnsafeField.isAccessible = true
            val unsafe = theUnsafeField.get(null)

            val staticFieldBaseMethod =
                unsafeClass.getMethod("staticFieldBase", java.lang.reflect.Field::class.java)
            val staticFieldOffsetMethod =
                unsafeClass.getMethod("staticFieldOffset", java.lang.reflect.Field::class.java)
            val putObjectMethod = unsafeClass.getMethod(
                "putObject",
                Any::class.java,
                java.lang.Long.TYPE,
                Any::class.java
            )

            val eventsField = CalendarContract.Events::class.java.getDeclaredField("CONTENT_URI")
            val eventsBase = staticFieldBaseMethod.invoke(unsafe, eventsField)
            val eventsOffset = staticFieldOffsetMethod.invoke(unsafe, eventsField) as Long
            putObjectMethod.invoke(unsafe, eventsBase, eventsOffset, mockUri)

            val calendarsField =
                CalendarContract.Calendars::class.java.getDeclaredField("CONTENT_URI")
            val calendarsBase = staticFieldBaseMethod.invoke(unsafe, calendarsField)
            val calendarsOffset = staticFieldOffsetMethod.invoke(unsafe, calendarsField) as Long
            putObjectMethod.invoke(unsafe, calendarsBase, calendarsOffset, mockUri)
        } catch (e: Exception) {
            println("Unsafe reflection failed: ${e.message}")
            e.printStackTrace()
        }

        repository = CalendarSyncRepositoryImpl(
            settingsRepository = settingsRepository,
            systemCalendarDataSource = systemCalendarDataSource,
            calendarStringProvider = calendarStringProvider,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun hasCalendarPermissions_delegatesToDataSource() {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true

        // Act & Assert
        assertThat(repository.hasCalendarPermissions()).isTrue()

        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns false

        // Act & Assert
        assertThat(repository.hasCalendarPermissions()).isFalse()
    }

    @Test
    fun updateCalendarColor_whenCalendarExists_updatesColorViaSettingsRepositoryAndDataSource() =
        runTest {
            // Arrange
            val type = CalendarSyncRepository.CalendarType.BIRTHDAY
            val newColor = 0xFF123456.toInt()
            val transformSlot = slot<(AppSettings) -> AppSettings>()
            coEvery { settingsRepository.updateSettings(capture(transformSlot)) } returns Unit
            coEvery { systemCalendarDataSource.findCalendarIdByName("BirthdayBuddy_Birthdays") } returns 123L
            coEvery { systemCalendarDataSource.updateCalendarColor(123L, newColor) } returns true

            // Act
            val result = repository.updateCalendarColor(type, newColor)

            // Assert
            assertThat(result).isTrue()
            val updated = transformSlot.captured(AppSettings(birthdayCalendarColor = 0))
            assertThat(updated.birthdayCalendarColor).isEqualTo(newColor)
            coVerify { systemCalendarDataSource.updateCalendarColor(123L, newColor) }
        }

    @Test
    fun updateCalendarColor_whenCalendarDoesNotExist_updatesSettingsRepositoryAndReturnsFalse() =
        runTest {
            // Arrange
            val type = CalendarSyncRepository.CalendarType.ANNIVERSARY
            val newColor = 0xFF654321.toInt()
            val transformSlot = slot<(AppSettings) -> AppSettings>()
            coEvery { settingsRepository.updateSettings(capture(transformSlot)) } returns Unit
            coEvery { systemCalendarDataSource.findCalendarIdByName("BirthdayBuddy_Anniversaries") } returns null

            // Act
            val result = repository.updateCalendarColor(type, newColor)

            // Assert
            assertThat(result).isFalse()
            val updated = transformSlot.captured(AppSettings(anniversaryCalendarColor = 0))
            assertThat(updated.anniversaryCalendarColor).isEqualTo(newColor)
            coVerify(exactly = 0) { systemCalendarDataSource.updateCalendarColor(any(), any()) }
        }

    @Test
    fun updateCalendarColor_nameDay_updatesNameDayColorViaSettingsRepository() = runTest {
        // Arrange
        val type = CalendarSyncRepository.CalendarType.NAMEDAY
        val newColor = 0xFF777777.toInt()
        val transformSlot = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.updateSettings(capture(transformSlot)) } returns Unit
        coEvery { systemCalendarDataSource.findCalendarIdByName("BirthdayBuddy_NameDays") } returns 456L
        coEvery { systemCalendarDataSource.updateCalendarColor(456L, newColor) } returns true

        // Act
        val result = repository.updateCalendarColor(type, newColor)

        // Assert
        assertThat(result).isTrue()
        val updated = transformSlot.captured(AppSettings(nameDayCalendarColor = 0))
        assertThat(updated.nameDayCalendarColor).isEqualTo(newColor)
    }

    @Test
    fun updateCustomEventCalendarColor_atomicallyMergesColorViaSettingsRepository() = runTest {
        // Arrange
        val eventKey = "custom:graduation"
        val newColor = 0xFF00AAFF.toInt()
        val transformSlot = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.updateSettings(capture(transformSlot)) } returns Unit
        coEvery {
            systemCalendarDataSource.findCalendarIdByName("BirthdayBuddy_Event_custom:graduation")
        } returns 789L
        coEvery { systemCalendarDataSource.updateCalendarColor(789L, newColor) } returns true

        // Act
        val result = repository.updateCustomEventCalendarColor(eventKey, newColor)

        // Assert
        assertThat(result).isTrue()
        val initialSettings = AppSettings(
            customEventCalendarColors = mapOf("custom:existing" to 0xFF111111.toInt())
        )
        val updated = transformSlot.captured(initialSettings)
        assertThat(updated.customEventCalendarColors).containsExactly(
            "custom:existing", 0xFF111111.toInt(),
            "custom:graduation", newColor
        )
        coVerify { systemCalendarDataSource.updateCalendarColor(789L, newColor) }
    }

    @Test
    fun deleteCalendar_whenTargetCalendarsExist_deletesThemBeforeAtomicallyUpdatingSettings() =
        runTest {
            // Arrange
            val transformSlot = slot<(AppSettings) -> AppSettings>()
            coEvery { settingsRepository.updateSettings(capture(transformSlot)) } returns Unit
            val calendars = listOf(
                SystemCalendarInfo(
                    id = 1L,
                    name = "BirthdayBuddyCalendar",
                    accountName = "acc",
                    accountType = "type",
                    displayName = "disp",
                    visible = 1
                ),
                SystemCalendarInfo(
                    id = 2L,
                    name = "BirthdayBuddy_Birthdays",
                    accountName = "BirthdayBuddy",
                    accountType = "LOCAL",
                    displayName = "disp",
                    visible = 1
                ),
                SystemCalendarInfo(
                    id = 3L,
                    name = "OtherCalendar",
                    accountName = "other",
                    accountType = "other",
                    displayName = "disp",
                    visible = 1
                )
            )
            coEvery { systemCalendarDataSource.queryAllCalendars() } returns calendars
            coEvery {
                systemCalendarDataSource.deleteCalendarById(
                    any(),
                    any(),
                    any()
                )
            } returns true

            // Act
            val result = repository.deleteCalendar()

            // Assert
            assertThat(result).isTrue()
            coVerifyOrder {
                systemCalendarDataSource.deleteCalendarById(1L, "acc", "type")
                systemCalendarDataSource.deleteCalendarById(2L, "BirthdayBuddy", "LOCAL")
                settingsRepository.updateSettings(any())
            }
            coVerify(exactly = 0) { systemCalendarDataSource.deleteCalendarById(3L, any(), any()) }

            val updated = transformSlot.captured(
                AppSettings(
                    calendarSyncEnabled = true,
                    calendarId = 999L,
                    notificationsEnabled = true
                )
            )
            assertThat(updated.calendarSyncEnabled).isFalse()
            assertThat(updated.calendarId).isNull()
            assertThat(updated.notificationsEnabled).isTrue()
        }

    @Test
    fun deleteCalendar_whenNoTargetCalendarsExist_updatesSettingsAndReturnsFalse() = runTest {
        // Arrange
        val transformSlot = slot<(AppSettings) -> AppSettings>()
        coEvery { settingsRepository.updateSettings(capture(transformSlot)) } returns Unit
        val calendars = listOf(
            SystemCalendarInfo(
                id = 3L,
                name = "OtherCalendar",
                accountName = "other",
                accountType = "other",
                displayName = "disp",
                visible = 1
            )
        )
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns calendars

        // Act
        val result = repository.deleteCalendar()

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { systemCalendarDataSource.deleteCalendarById(any(), any(), any()) }
        val updated = transformSlot.captured(
            AppSettings(calendarSyncEnabled = true, calendarId = 999L)
        )
        assertThat(updated.calendarSyncEnabled).isFalse()
        assertThat(updated.calendarId).isNull()
    }

    @Test
    fun syncBirthdays_whenNoPermissions_returnsFalse() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns false

        // Act
        val result = repository.syncBirthdays(emptyList())

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { systemCalendarDataSource.queryAllCalendars() }
    }

    @Test
    fun syncBirthdays_cleansCalendarsCorrectly() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        val calendars = listOf(
            SystemCalendarInfo(10L, "BirthdayBuddyCalendar", "oldAcc", "oldType", "disp", 1),
            SystemCalendarInfo(11L, "BirthdayBuddy_Birthdays", "BirthdayBuddy", "LOCAL", "disp", 1),
            SystemCalendarInfo(12L, "BirthdayBuddy_Birthdays", "BirthdayBuddy", "LOCAL", "disp", 1),
            SystemCalendarInfo(13L, "BirthdayBuddy_Anniversaries", "WrongAcc", "LOCAL", "disp", 1),
            SystemCalendarInfo(
                14L,
                "BirthdayBuddy_NameDays",
                "BirthdayBuddy",
                "WrongType",
                "disp",
                1
            ),
            SystemCalendarInfo(15L, "OtherCalendar", "other", "other", "disp", 1)
        )
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns calendars
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery { systemCalendarDataSource.getOrCreateCalendar(any(), any(), any()) } returns 11L
        coEvery { systemCalendarDataSource.clearCalendarEvents(any()) } returns true
        coEvery { systemCalendarDataSource.applyBatch(any()) } returns true

        // Act
        val result = repository.syncBirthdays(emptyList())

        // Assert
        assertThat(result).isTrue()
        // Verify deletions in cleanCalendars
        coVerify { systemCalendarDataSource.deleteCalendarById(10L, "oldAcc", "oldType") }
        coVerify { systemCalendarDataSource.deleteCalendarById(12L, "BirthdayBuddy", "LOCAL") }
        coVerify { systemCalendarDataSource.deleteCalendarById(13L, "WrongAcc", "LOCAL") }
        coVerify { systemCalendarDataSource.deleteCalendarById(14L, "BirthdayBuddy", "WrongType") }
        coVerify(exactly = 0) { systemCalendarDataSource.deleteCalendarById(11L, any(), any()) }
        coVerify(exactly = 0) { systemCalendarDataSource.deleteCalendarById(15L, any(), any()) }
    }

    @Test
    fun syncBirthdays_whenBirthdayCalendarCreationFails_returnsFalse() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings()
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns null

        // Act
        val result = repository.syncBirthdays(emptyList())

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun syncBirthdays_whenOtherEventsDisabled_syncsBirthdaysOnlyAndDeletesOtherCalendars() =
        runTest {
            // Arrange
            every { systemCalendarDataSource.hasCalendarPermissions() } returns true
            coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
            coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
                otherEventsEnabled = false
            )
            coEvery {
                systemCalendarDataSource.getOrCreateCalendar(
                    "BirthdayBuddy_Birthdays",
                    any(),
                    any()
                )
            } returns 11L
            coEvery { systemCalendarDataSource.findCalendarIdByName("BirthdayBuddy_Anniversaries") } returns 22L
            coEvery { systemCalendarDataSource.findCalendarIdByName("BirthdayBuddy_NameDays") } returns 33L
            coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } returns true
            coEvery { systemCalendarDataSource.deleteCalendarById(22L, any(), any()) } returns true
            coEvery { systemCalendarDataSource.deleteCalendarById(33L, any(), any()) } returns true
            coEvery { systemCalendarDataSource.applyBatch(any()) } returns true

            val contacts = listOf(
                Contact(
                    contactId = "c1",
                    lookupKey = "key1",
                    fullName = "Alice",
                    birthday = LocalDate.of(1990, 5, 10)
                ),
                Contact(
                    contactId = "c2",
                    lookupKey = "key2",
                    fullName = "Bob",
                    birthday = LocalDate.of(NO_YEAR_MARKER, 10, 20)
                )
            )

            every { calendarStringProvider.calendarEventNoYear() } returns "MockNoYear"
            every { calendarStringProvider.calendarEventBirthYear(any()) } returns "MockWithArgs"
            every { calendarStringProvider.calendarEventTitle(any()) } returns "MockWithArgs"

            // Act
            val result = repository.syncBirthdays(contacts)

            // Assert
            assertThat(result).isTrue()
            coVerify { systemCalendarDataSource.clearCalendarEvents(11L) }
            coVerify { systemCalendarDataSource.deleteCalendarById(22L, "BirthdayBuddy", "LOCAL") }
            coVerify { systemCalendarDataSource.deleteCalendarById(33L, "BirthdayBuddy", "LOCAL") }
            coVerify { systemCalendarDataSource.applyBatch(any()) }
        }

    @Test
    fun syncBirthdays_whenOtherEventsEnabled_syncsAllEventsIncludingSpouseMergingAndNameDays() =
        runTest {
            // Arrange
            every { systemCalendarDataSource.hasCalendarPermissions() } returns true
            coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
            coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
                otherEventsEnabled = true
            )
            coEvery {
                systemCalendarDataSource.getOrCreateCalendar(
                    "BirthdayBuddy_Birthdays",
                    any(),
                    any()
                )
            } returns 11L
            coEvery {
                systemCalendarDataSource.getOrCreateCalendar(
                    "BirthdayBuddy_Anniversaries",
                    any(),
                    any()
                )
            } returns 22L
            coEvery {
                systemCalendarDataSource.getOrCreateCalendar(
                    "BirthdayBuddy_NameDays",
                    any(),
                    any()
                )
            } returns 33L
            coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } returns true
            coEvery { systemCalendarDataSource.clearCalendarEvents(22L) } returns true
            coEvery { systemCalendarDataSource.clearCalendarEvents(33L) } returns true
            coEvery { systemCalendarDataSource.applyBatch(any()) } returns true

            val contacts = listOf(
                // Alice & Bob: Spouse couple (both have same anniversary and point to each other)
                Contact(
                    contactId = "c1",
                    lookupKey = "keyA",
                    fullName = "Alice Mustermann",
                    birthday = LocalDate.of(1990, 5, 10),
                    anniversary = LocalDate.of(2015, 6, 15),
                    spouseLookupKey = "keyB"
                ),
                Contact(
                    contactId = "c2",
                    lookupKey = "keyB",
                    fullName = "Bob Mustermann",
                    birthday = LocalDate.of(1988, 3, 12),
                    anniversary = LocalDate.of(2015, 6, 15),
                    spouseLookupKey = "keyA"
                ),
                // Charlie: Has spouse key but spouse is not in the contacts list
                Contact(
                    contactId = "c3",
                    lookupKey = "keyC",
                    fullName = "Charlie Schmidt",
                    anniversary = LocalDate.of(2010, 8, 20),
                    spouseLookupKey = "keyNonExistent"
                ),
                // Eva: Has name day
                Contact(
                    contactId = "c5",
                    lookupKey = "keyE",
                    fullName = "Eva",
                    nameDay = LocalDate.of(NO_YEAR_MARKER, 12, 24)
                )
            )

            every { calendarStringProvider.calendarEventNoYear() } returns "MockNoYear"
            every { calendarStringProvider.calendarEventBirthYear(any()) } returns "MockWithArgs"
            every { calendarStringProvider.calendarEventTitle(any()) } returns "MockWithArgs"

            // Act
            val result = repository.syncBirthdays(contacts)

            // Assert
            assertThat(result).isTrue()

            // Verifications
            coVerify { systemCalendarDataSource.clearCalendarEvents(11L) }
            coVerify { systemCalendarDataSource.clearCalendarEvents(22L) }
            coVerify { systemCalendarDataSource.clearCalendarEvents(33L) }
            coVerify { systemCalendarDataSource.applyBatch(any()) }
        }

    @Test
    fun syncBirthdays_batchingThreshold_appliesBatchInChunksOf400() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns 11L
        coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } returns true
        coEvery { systemCalendarDataSource.applyBatch(any()) } returns true

        // Create 450 contacts, each with a birthday to trigger 450 events
        val contacts = List(450) { index ->
            Contact(
                contactId = "c$index",
                lookupKey = "key$index",
                fullName = "Contact $index",
                birthday = LocalDate.of(1990, 1, 1)
            )
        }

        every { calendarStringProvider.calendarEventNoYear() } returns "MockNoYear"
        every { calendarStringProvider.calendarEventBirthYear(any()) } returns "MockWithArgs"
        every { calendarStringProvider.calendarEventTitle(any()) } returns "MockWithArgs"

        // Act
        val result = repository.syncBirthdays(contacts)

        // Assert
        assertThat(result).isTrue()
        // applyBatch should be called twice (once at size 400, once at the remaining 50)
        coVerify(exactly = 2) { systemCalendarDataSource.applyBatch(any()) }
    }

    @Test
    fun syncBirthdays_whenFinalApplyBatchReturnsFalse_returnsFalse() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns 11L
        coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } returns true
        coEvery { systemCalendarDataSource.applyBatch(any()) } returns false

        val contacts = listOf(
            Contact(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10)
            )
        )

        every { calendarStringProvider.calendarEventNoYear() } returns "MockNoYear"
        every { calendarStringProvider.calendarEventBirthYear(any()) } returns "MockWithArgs"
        every { calendarStringProvider.calendarEventTitle(any()) } returns "MockWithArgs"

        // Act
        val result = repository.syncBirthdays(contacts)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun syncBirthdays_whenIntermediateApplyBatchReturnsFalse_returnsFalse() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns 11L
        coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } returns true
        coEvery { systemCalendarDataSource.applyBatch(any()) } returnsMany listOf(false, true)

        val contacts = List(450) { index ->
            Contact(
                contactId = "c$index",
                lookupKey = "key$index",
                fullName = "Contact $index",
                birthday = LocalDate.of(1990, 1, 1)
            )
        }

        every { calendarStringProvider.calendarEventNoYear() } returns "MockNoYear"
        every { calendarStringProvider.calendarEventBirthYear(any()) } returns "MockWithArgs"
        every { calendarStringProvider.calendarEventTitle(any()) } returns "MockWithArgs"

        // Act
        val result = repository.syncBirthdays(contacts)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 2) { systemCalendarDataSource.applyBatch(any()) }
    }

    @Test
    fun syncBirthdays_whenExceptionThrown_returnsFalse() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns 11L
        coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } returns true
        coEvery { systemCalendarDataSource.applyBatch(any()) } throws RuntimeException("Batch failed")

        val contacts = listOf(
            Contact(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10)
            )
        )

        every { calendarStringProvider.calendarEventNoYear() } returns "MockNoYear"
        every { calendarStringProvider.calendarEventBirthYear(any()) } returns "MockWithArgs"
        every { calendarStringProvider.calendarEventTitle(any()) } returns "MockWithArgs"

        // Act
        val result = repository.syncBirthdays(contacts)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun syncBirthdays_concurrentCalls_areSerializedWithoutInterleavingClearAndBatch() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { systemCalendarDataSource.queryAllCalendars() } returns emptyList()
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns 11L

        val events = mutableListOf<String>()
        val clearCounter = AtomicInteger(0)
        val batchCounter = AtomicInteger(0)
        val firstClearEntered = CompletableDeferred<Unit>()
        val allowFirstClearToFinish = CompletableDeferred<Unit>()

        coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } coAnswers {
            val n = clearCounter.incrementAndGet()
            events.add("clear_start_$n")
            if (n == 1) {
                firstClearEntered.complete(Unit)
                allowFirstClearToFinish.await()
            }
            events.add("clear_end_$n")
            true
        }
        coEvery { systemCalendarDataSource.applyBatch(any()) } coAnswers {
            val n = batchCounter.incrementAndGet()
            events.add("batch_$n")
            true
        }

        val contacts = listOf(
            Contact(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10)
            )
        )

        every { calendarStringProvider.calendarEventBirthYear(any()) } returns "1990"
        every { calendarStringProvider.calendarEventTitle(any()) } returns "Alice"

        // Act
        val firstSync = async { repository.syncBirthdays(contacts) }
        firstClearEntered.await()

        val secondSync = async { repository.syncBirthdays(contacts) }

        // While first sync holds the mutex, second sync must not have entered clearCalendarEvents
        assertThat(events).containsExactly("clear_start_1")

        allowFirstClearToFinish.complete(Unit)

        assertThat(firstSync.await()).isTrue()
        assertThat(secondSync.await()).isTrue()

        // Assert strict sequential execution without interleaving
        assertThat(events).containsExactly(
            "clear_start_1",
            "clear_end_1",
            "batch_1",
            "clear_start_2",
            "clear_end_2",
            "batch_2"
        ).inOrder()
    }

    @Test
    fun syncBirthdays_andDeleteCalendar_areSerializedByMutex() = runTest {
        // Arrange
        every { systemCalendarDataSource.hasCalendarPermissions() } returns true
        coEvery { settingsRepository.getSettingsImmediate() } returns AppSettings(
            otherEventsEnabled = false
        )
        coEvery {
            systemCalendarDataSource.getOrCreateCalendar(
                "BirthdayBuddy_Birthdays",
                any(),
                any()
            )
        } returns 11L

        val events = mutableListOf<String>()
        val firstClearEntered = CompletableDeferred<Unit>()
        val allowFirstClearToFinish = CompletableDeferred<Unit>()

        coEvery { systemCalendarDataSource.queryAllCalendars() } returns listOf(
            SystemCalendarInfo(11L, "BirthdayBuddy_Birthdays", "BirthdayBuddy", "LOCAL", "Birthdays", 1)
        )
        coEvery { systemCalendarDataSource.clearCalendarEvents(11L) } coAnswers {
            events.add("sync_clear_start")
            firstClearEntered.complete(Unit)
            allowFirstClearToFinish.await()
            events.add("sync_clear_end")
            true
        }
        coEvery { systemCalendarDataSource.applyBatch(any()) } coAnswers {
            events.add("sync_batch")
            true
        }
        coEvery { systemCalendarDataSource.deleteCalendarById(11L, "BirthdayBuddy", "LOCAL") } coAnswers {
            events.add("delete_calendar")
            true
        }
        coEvery { settingsRepository.updateSettings(any()) } coAnswers {
            events.add("delete_update_settings")
        }

        val contacts = listOf(
            Contact(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10)
            )
        )

        every { calendarStringProvider.calendarEventBirthYear(any()) } returns "1990"
        every { calendarStringProvider.calendarEventTitle(any()) } returns "Alice"

        // Act
        val syncDeferred = async { repository.syncBirthdays(contacts) }
        firstClearEntered.await()

        val deleteDeferred = async { repository.deleteCalendar() }

        // Delete must wait until sync finishes
        assertThat(events).containsExactly("sync_clear_start")

        allowFirstClearToFinish.complete(Unit)

        assertThat(syncDeferred.await()).isTrue()
        assertThat(deleteDeferred.await()).isTrue()

        // Assert delete ran strictly after sync finished
        assertThat(events).containsExactly(
            "sync_clear_start",
            "sync_clear_end",
            "sync_batch",
            "delete_calendar",
            "delete_update_settings"
        ).inOrder()
    }
}
