package com.heckmannch.birthdaybuddy.data.repository

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.CoupleProjection
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.CoupleSuggestion
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify as mockkVerify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicInteger

/**
 * JVM Unit Tests for [CoupleRepositoryImpl].
 */
class CoupleRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissionChecker: PermissionChecker = mock()
    private val contactDao: ContactDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val widgetUpdater: WidgetUpdater = mock()
    private val appDatabase: AppDatabase = mock()
    private val settingsDatabase: SettingsDatabase = mock()
    private val contactDbMapper = ContactDbMapper()

    private val potentialCouplesFlow = MutableStateFlow<List<CoupleProjection>>(emptyList())
    private val settingsFlow = MutableStateFlow(AppSettings())

    private lateinit var repository: CoupleRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)
        whenever(contactDao.getPotentialCouples()).thenReturn(potentialCouplesFlow)
        whenever(settingsRepository.settings).thenReturn(settingsFlow)

        val executor = java.util.concurrent.Executor { it.run() }
        whenever(appDatabase.transactionExecutor).thenReturn(executor)
        whenever(appDatabase.queryExecutor).thenReturn(executor)
        whenever(settingsDatabase.transactionExecutor).thenReturn(executor)
        whenever(settingsDatabase.queryExecutor).thenReturn(executor)

        repository = CoupleRepositoryImpl(
            permissionChecker = permissionChecker,
            contactDao = contactDao,
            contactUserDataDao = contactUserDataDao,
            settingsRepository = settingsRepository,
            calendarSyncRepository = calendarSyncRepository,
            widgetUpdater = widgetUpdater,
            appDatabase = appDatabase,
            settingsDatabase = settingsDatabase,
            contactDbMapper = contactDbMapper,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun createTransactionElement(): kotlin.coroutines.CoroutineContext {
        val clazz = Class.forName("androidx.room.TransactionElement")
        val constructor = clazz.constructors.first()
        return constructor.newInstance(Dispatchers.Unconfined) as kotlin.coroutines.CoroutineContext
    }

    @Test
    fun potentialCouples_emitsFromContactDao() = runTest {
        val projections = listOf(
            CoupleProjection("k1", "Alice", null, "k2", "Bob", null)
        )
        potentialCouplesFlow.value = projections

        val expected = listOf(
            CoupleSuggestion("k1", "Alice", null, "k2", "Bob", null)
        )
        val result = repository.potentialCouples.first()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun ignoredCouples_emitsFromSettingsRepository() = runTest {
        val list = listOf("k1:k2")
        settingsFlow.value = AppSettings(ignoredCouplePairs = list)

        val result = repository.ignoredCouples.first()
        assertThat(result).isEqualTo(list)
    }

    @Test
    fun ignoredCouplePairs_emitsFromSettingsRepository() = runTest {
        val list = listOf("k1:k2")
        settingsFlow.value = AppSettings(ignoredCouplePairs = list)

        val result = repository.ignoredCouplePairs.first()
        assertThat(result).isEqualTo(list)
    }

    @Test
    fun linkAsCouple_doesNothing_whenLinkingContactToSelf() = runTest(createTransactionElement()) {
        repository.linkAsCouple("k1", "k1")

        verify(contactUserDataDao, never()).getUserDataForContact(any())
        verify(contactUserDataDao, never()).upsertUserData(any())
        verify(contactDao, never()).upsertContact(any())
        verify(widgetUpdater, never()).updateWidget()
        verify(calendarSyncRepository, never()).syncBirthdays(any())
    }

    @Test
    fun linkAsCouple_linksBothContactsAndUpdatesCache() = runTest(createTransactionElement()) {
        // Arrange
        val c1 = ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice")
        val c2 = ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
        whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
        whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
        whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(null)
        whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(null)
        whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = false)
        )

        // Act
        repository.linkAsCouple("k1", "k2")

        // Assert
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("k1")
            assertThat(it.spouseLookupKey).isEqualTo("k2")
        })
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("k2")
            assertThat(it.spouseLookupKey).isEqualTo("k1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("k1")
            assertThat(it.spouseLookupKey).isEqualTo("k2")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("k2")
            assertThat(it.spouseLookupKey).isEqualTo("k1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun linkAsCouple_clearsPreviousPartnerInBothDbs_whenRelinkingContact() =
        runTest(createTransactionElement()) {
            // Arrange: A <-> C exists, B is unlinked. Now link A <-> B.
            val userDataMap = mutableMapOf(
                "A" to ContactUserData(lookupKey = "A", spouseLookupKey = "C"),
                "C" to ContactUserData(lookupKey = "C", spouseLookupKey = "A"),
            )
            val contactMap = mutableMapOf(
                "A" to ContactEntity(contactId = "1", lookupKey = "A", fullName = "Alice", spouseLookupKey = "C"),
                "B" to ContactEntity(contactId = "2", lookupKey = "B", fullName = "Bob", spouseLookupKey = null),
                "C" to ContactEntity(contactId = "3", lookupKey = "C", fullName = "Carol", spouseLookupKey = "A"),
            )
            whenever(contactUserDataDao.getUserDataForContact(any())).doSuspendableAnswer { inv ->
                userDataMap[inv.getArgument(0)]
            }
            whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { inv ->
                val data = inv.getArgument<ContactUserData>(0)
                userDataMap[data.lookupKey] = data
            }
            whenever(contactDao.getContactByLookupKey(any())).doSuspendableAnswer { inv ->
                contactMap[inv.getArgument(0)]
            }
            whenever(contactDao.upsertContact(any())).doSuspendableAnswer { inv ->
                val entity = inv.getArgument<ContactEntity>(0)
                contactMap[entity.lookupKey] = entity
            }
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = false)
            )

            // Act
            repository.linkAsCouple("A", "B")

            // Assert: SettingsDB and AppDB both have A <-> B and C -> null (no orphaned C -> A)
            assertThat(userDataMap["A"]?.spouseLookupKey).isEqualTo("B")
            assertThat(userDataMap["B"]?.spouseLookupKey).isEqualTo("A")
            assertThat(userDataMap["C"]?.spouseLookupKey).isNull()

            assertThat(contactMap["A"]?.spouseLookupKey).isEqualTo("B")
            assertThat(contactMap["B"]?.spouseLookupKey).isEqualTo("A")
            assertThat(contactMap["C"]?.spouseLookupKey).isNull()
        }

    @Test
    fun linkAsCouple_clearsBothPreviousPartnersInBothDbs_whenRelinkingTwoAlreadyLinkedContacts() =
        runTest(createTransactionElement()) {
            // Arrange: A <-> C and B <-> D exist. Now link A <-> B.
            val userDataMap = mutableMapOf(
                "A" to ContactUserData(lookupKey = "A", spouseLookupKey = "C"),
                "C" to ContactUserData(lookupKey = "C", spouseLookupKey = "A"),
                "B" to ContactUserData(lookupKey = "B", spouseLookupKey = "D"),
                "D" to ContactUserData(lookupKey = "D", spouseLookupKey = "B"),
            )
            val contactMap = mutableMapOf(
                "A" to ContactEntity(contactId = "1", lookupKey = "A", fullName = "Alice", spouseLookupKey = "C"),
                "B" to ContactEntity(contactId = "2", lookupKey = "B", fullName = "Bob", spouseLookupKey = "D"),
                "C" to ContactEntity(contactId = "3", lookupKey = "C", fullName = "Carol", spouseLookupKey = "A"),
                "D" to ContactEntity(contactId = "4", lookupKey = "D", fullName = "Dave", spouseLookupKey = "B"),
            )
            whenever(contactUserDataDao.getUserDataForContact(any())).doSuspendableAnswer { inv ->
                userDataMap[inv.getArgument(0)]
            }
            whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { inv ->
                val data = inv.getArgument<ContactUserData>(0)
                userDataMap[data.lookupKey] = data
            }
            whenever(contactDao.getContactByLookupKey(any())).doSuspendableAnswer { inv ->
                contactMap[inv.getArgument(0)]
            }
            whenever(contactDao.upsertContact(any())).doSuspendableAnswer { inv ->
                val entity = inv.getArgument<ContactEntity>(0)
                contactMap[entity.lookupKey] = entity
            }
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = false)
            )

            // Act
            repository.linkAsCouple("A", "B")

            // Assert
            assertThat(userDataMap["A"]?.spouseLookupKey).isEqualTo("B")
            assertThat(userDataMap["B"]?.spouseLookupKey).isEqualTo("A")
            assertThat(userDataMap["C"]?.spouseLookupKey).isNull()
            assertThat(userDataMap["D"]?.spouseLookupKey).isNull()

            assertThat(contactMap["A"]?.spouseLookupKey).isEqualTo("B")
            assertThat(contactMap["B"]?.spouseLookupKey).isEqualTo("A")
            assertThat(contactMap["C"]?.spouseLookupKey).isNull()
            assertThat(contactMap["D"]?.spouseLookupKey).isNull()
        }

    @Test
    fun linkAsCouple_rollsBackSettingsIncludingPreviousPartner_whenCacheUpdateFails() =
        runTest(createTransactionElement()) {
            // Arrange: A <-> C exists, B is unlinked. Cache fails during linkAsCouple(A, B).
            val userDataMap = mutableMapOf(
                "A" to ContactUserData(lookupKey = "A", spouseLookupKey = "C"),
                "C" to ContactUserData(lookupKey = "C", spouseLookupKey = "A"),
            )
            whenever(contactUserDataDao.getUserDataForContact(any())).doSuspendableAnswer { inv ->
                userDataMap[inv.getArgument(0)]
            }
            whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { inv ->
                val data = inv.getArgument<ContactUserData>(0)
                userDataMap[data.lookupKey] = data
            }
            whenever(contactDao.getContactByLookupKey("A")).thenThrow(RuntimeException("AppDB error"))

            // Act
            var caught: Throwable? = null
            try {
                repository.linkAsCouple("A", "B")
            } catch (e: Throwable) {
                caught = e
            }

            // Assert
            assertThat(caught).isInstanceOf(RuntimeException::class.java)
            assertThat(userDataMap["A"]?.spouseLookupKey).isEqualTo("C")
            assertThat(userDataMap["C"]?.spouseLookupKey).isEqualTo("A")
            assertThat(userDataMap["B"]?.spouseLookupKey).isNull()
            verify(widgetUpdater, never()).updateWidget()
        }

    @Test
    fun linkAsCouple_rollsBackSettingsAndRethrows_whenCacheUpdateIsCancelled() =
        runTest(createTransactionElement()) {
            // Arrange: A <-> C exists, cache update is cancelled.
            val userDataMap = mutableMapOf(
                "A" to ContactUserData(lookupKey = "A", spouseLookupKey = "C"),
                "C" to ContactUserData(lookupKey = "C", spouseLookupKey = "A"),
            )
            whenever(contactUserDataDao.getUserDataForContact(any())).doSuspendableAnswer { inv ->
                userDataMap[inv.getArgument(0)]
            }
            whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { inv ->
                val data = inv.getArgument<ContactUserData>(0)
                userDataMap[data.lookupKey] = data
            }
            whenever(contactDao.getContactByLookupKey("A")).thenThrow(CancellationException("Cancelled"))

            // Act
            var caught: Throwable? = null
            try {
                repository.linkAsCouple("A", "B")
            } catch (e: Throwable) {
                caught = e
            }

            // Assert
            assertThat(caught).isInstanceOf(CancellationException::class.java)
            assertThat(userDataMap["A"]?.spouseLookupKey).isEqualTo("C")
            assertThat(userDataMap["C"]?.spouseLookupKey).isEqualTo("A")
            assertThat(userDataMap["B"]?.spouseLookupKey).isNull()
            verify(widgetUpdater, never()).updateWidget()
        }

    @Test
    fun linkAsCouple_rollsBackSettings_whenCacheUpdateFails() = runTest(createTransactionElement()) {
        // Arrange
        val prev1 = ContactUserData(lookupKey = "k1", spouseLookupKey = null)
        val prev2 = ContactUserData(lookupKey = "k2", spouseLookupKey = null)
        whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(prev1)
        whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(prev2)
        whenever(contactDao.getContactByLookupKey("k1")).thenThrow(RuntimeException("AppDB error"))

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.linkAsCouple("k1", "k2")
        } catch (e: Throwable) {
            caught = e
        }

        assertThat(caught).isNotNull()
        // Verify rollback was performed with previous data
        verify(contactUserDataDao).upsertUserData(prev1)
        verify(contactUserDataDao).upsertUserData(prev2)
    }

    @Test
    fun concurrentLinkCalls_areSerializedByMutex_preservingSymmetry() =
        runTest(createTransactionElement()) {
            // Arrange: in-memory backing stores for SettingsDB and AppDB
            val userDataMap = mutableMapOf<String, ContactUserData>()
            val contactMap = mutableMapOf(
                "A" to ContactEntity(contactId = "1", lookupKey = "A", fullName = "Alice"),
                "B" to ContactEntity(contactId = "2", lookupKey = "B", fullName = "Bob"),
                "C" to ContactEntity(contactId = "3", lookupKey = "C", fullName = "Carol"),
            )
            val writeCount = AtomicInteger(0)
            val firstWriteEntered = CompletableDeferred<Unit>()
            val allowFirstWriteToFinish = CompletableDeferred<Unit>()

            whenever(contactUserDataDao.getUserDataForContact(any())).doSuspendableAnswer { inv ->
                userDataMap[inv.getArgument(0)]
            }
            whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { inv ->
                val data = inv.getArgument<ContactUserData>(0)
                if (writeCount.incrementAndGet() == 1) {
                    firstWriteEntered.complete(Unit)
                    allowFirstWriteToFinish.await()
                }
                userDataMap[data.lookupKey] = data
            }
            whenever(contactDao.getContactByLookupKey(any())).doSuspendableAnswer { inv ->
                contactMap[inv.getArgument(0)]
            }
            whenever(contactDao.upsertContact(any())).doSuspendableAnswer { inv ->
                val entity = inv.getArgument<ContactEntity>(0)
                contactMap[entity.lookupKey] = entity
            }
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = false)
            )

            // Act: start link(A, B), suspend mid-write, then fire concurrent link(A, C)
            val job1 = launch { repository.linkAsCouple("A", "B") }
            firstWriteEntered.await()

            val job2 = launch { repository.linkAsCouple("A", "C") }
            allowFirstWriteToFinish.complete(Unit)

            job1.join()
            job2.join()

            // Assert: final state is strictly symmetric (A <-> C, B -> null) in both DBs
            assertThat(userDataMap["A"]?.spouseLookupKey).isEqualTo("C")
            assertThat(userDataMap["C"]?.spouseLookupKey).isEqualTo("A")
            assertThat(userDataMap["B"]?.spouseLookupKey).isNull()

            assertThat(contactMap["A"]?.spouseLookupKey).isEqualTo("C")
            assertThat(contactMap["C"]?.spouseLookupKey).isEqualTo("A")
            assertThat(contactMap["B"]?.spouseLookupKey).isNull()
        }

    @Test
    fun unlinkCouple_unlinksBothContactsAndUpdatesCache() = runTest(createTransactionElement()) {
        // Arrange
        val c1 = ContactEntity(
            contactId = "c1",
            lookupKey = "k1",
            fullName = "Alice",
            spouseLookupKey = "k2"
        )
        val c2 = ContactEntity(
            contactId = "c2",
            lookupKey = "k2",
            fullName = "Bob",
            spouseLookupKey = "k1"
        )
        whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
        whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
        val u1 = ContactUserData(lookupKey = "k1", spouseLookupKey = "k2")
        val u2 = ContactUserData(lookupKey = "k2", spouseLookupKey = "k1")
        whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(u1)
        whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(u2)
        whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = false)
        )

        // Act
        repository.unlinkCouple("k1")

        // Assert
        val userDataCaptor = org.mockito.kotlin.argumentCaptor<ContactUserData>()
        verify(contactUserDataDao, org.mockito.kotlin.times(2)).upsertUserData(userDataCaptor.capture())
        val savedUserData = userDataCaptor.allValues
        assertThat(savedUserData.map { it.lookupKey }).containsExactly("k1", "k2")
        assertThat(savedUserData.all { it.spouseLookupKey == null }).isTrue()

        val contactCaptor = org.mockito.kotlin.argumentCaptor<ContactEntity>()
        verify(contactDao, org.mockito.kotlin.times(2)).upsertContact(contactCaptor.capture())
        val savedContacts = contactCaptor.allValues
        assertThat(savedContacts.map { it.lookupKey }).containsExactly("k1", "k2")
        assertThat(savedContacts.all { it.spouseLookupKey == null }).isTrue()

        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun unlinkCouple_rollsBackSettings_whenCacheUpdateFails() = runTest(createTransactionElement()) {
        // Arrange
        val c1 = ContactEntity(
            contactId = "c1",
            lookupKey = "k1",
            fullName = "Alice",
            spouseLookupKey = "k2"
        )
        whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            .thenThrow(RuntimeException("AppDB error"))
        val u1 = ContactUserData(lookupKey = "k1", spouseLookupKey = "k2")
        val u2 = ContactUserData(lookupKey = "k2", spouseLookupKey = "k1")
        whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(u1)
        whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(u2)

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.unlinkCouple("k1")
        } catch (e: Throwable) {
            caught = e
        }

        assertThat(caught).isNotNull()
        // Verify rollback was performed with original previous state
        verify(contactUserDataDao).upsertUserData(u1)
        verify(contactUserDataDao).upsertUserData(u2)
    }

    @Test
    fun linkAsCouple_skipsCalendarSync_whenCalendarPermissionIsNotGranted() =
        runTest(createTransactionElement()) {
            val c1 = ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice")
            val c2 = ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
            whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
            whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(null)
            whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(null)
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = true)
            )
            whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)

            repository.linkAsCouple("k1", "k2")

            verify(widgetUpdater).updateWidget()
            verify(calendarSyncRepository, never()).syncBirthdays(any())
        }

    @Test
    fun linkAsCouple_syncsCalendar_whenEnabledAndPermissionGranted() =
        runTest(createTransactionElement()) {
            val c1 = ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice")
            val c2 = ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
            whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
            whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(null)
            whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(null)
            whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = true)
            )
            whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)

            repository.linkAsCouple("k1", "k2")

            verify(widgetUpdater).updateWidget()
            verify(calendarSyncRepository).syncBirthdays(any())
        }

    @Test
    fun linkAsCouple_doesNotFail_whenCalendarSyncThrows() =
        runTest(createTransactionElement()) {
            val c1 = ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice")
            val c2 = ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
            whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
            whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(null)
            whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(null)
            whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = true)
            )
            whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
            whenever(calendarSyncRepository.syncBirthdays(any()))
                .thenThrow(RuntimeException("Calendar provider unavailable"))

            // Should not throw
            repository.linkAsCouple("k1", "k2")

            verify(widgetUpdater).updateWidget()
            verify(calendarSyncRepository).syncBirthdays(any())
            mockkVerify(exactly = 1) { Log.e("CoupleRepository", any(), any()) }
        }

    @Test
    fun linkAsCouple_stillSyncsCalendar_whenWidgetUpdateThrows() =
        runTest(createTransactionElement()) {
            val c1 = ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice")
            val c2 = ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
            whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
            whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(null)
            whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(null)
            whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = true)
            )
            whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
            whenever(widgetUpdater.updateWidget()).thenThrow(RuntimeException("Widget error"))

            // Should not throw and should still execute calendar sync
            repository.linkAsCouple("k1", "k2")

            verify(widgetUpdater).updateWidget()
            verify(calendarSyncRepository).syncBirthdays(any())
            mockkVerify(exactly = 1) { Log.e("CoupleRepository", any(), any()) }
        }

    @Test
    fun unlinkCouple_doesNotFail_whenCalendarSyncThrows() =
        runTest(createTransactionElement()) {
            val c1 = ContactEntity(
                contactId = "c1",
                lookupKey = "k1",
                fullName = "Alice",
                spouseLookupKey = "k2"
            )
            val c2 = ContactEntity(
                contactId = "c2",
                lookupKey = "k2",
                fullName = "Bob",
                spouseLookupKey = "k1"
            )
            whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
            whenever(contactUserDataDao.getUserDataForContact("k1"))
                .thenReturn(ContactUserData(lookupKey = "k1", spouseLookupKey = "k2"))
            whenever(contactUserDataDao.getUserDataForContact("k2"))
                .thenReturn(ContactUserData(lookupKey = "k2", spouseLookupKey = "k1"))
            whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = true)
            )
            whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
            whenever(calendarSyncRepository.syncBirthdays(any()))
                .thenThrow(SecurityException("Calendar permission revoked"))

            // Should not throw
            repository.unlinkCouple("k1")

            verify(widgetUpdater).updateWidget()
            verify(calendarSyncRepository).syncBirthdays(any())
            mockkVerify(exactly = 1) { Log.e("CoupleRepository", any(), any()) }
        }

    @Test
    fun linkAsCouple_rethrowsCancellationException_fromCalendarSync() =
        runTest(createTransactionElement()) {
            val c1 = ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice")
            val c2 = ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
            whenever(contactDao.getContactByLookupKey("k1")).thenReturn(c1)
            whenever(contactDao.getContactByLookupKey("k2")).thenReturn(c2)
            whenever(contactUserDataDao.getUserDataForContact("k1")).thenReturn(null)
            whenever(contactUserDataDao.getUserDataForContact("k2")).thenReturn(null)
            whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(c1, c2))
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(
                AppSettings(calendarSyncEnabled = true)
            )
            whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
            whenever(calendarSyncRepository.syncBirthdays(any()))
                .thenThrow(CancellationException("Sync cancelled"))

            var caught: Throwable? = null
            try {
                repository.linkAsCouple("k1", "k2")
            } catch (e: Throwable) {
                caught = e
            }

            assertThat(caught).isInstanceOf(CancellationException::class.java)
            mockkVerify(exactly = 0) { Log.e(any(), any(), any()) }
        }

    @Test
    fun ignoreCoupleSuggestion_delegatesToSettingsRepository() = runTest {
        repository.ignoreCoupleSuggestion("key1", "key2")
        verify(settingsRepository).updateSettings(any())
    }

    @Test
    fun clearIgnoredCouplePairs_delegatesToSettingsRepository() = runTest {
        repository.clearIgnoredCouplePairs()
        verify(settingsRepository).updateSettings(any())
    }
}
