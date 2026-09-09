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
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * JVM Unit Tests for [CoupleRepositoryImpl].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoupleRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

        whenever(contactDao.getPotentialCouples()).thenReturn(potentialCouplesFlow)
        whenever(settingsRepository.settings).thenReturn(settingsFlow)

        val executor = java.util.concurrent.Executor { it.run() }
        whenever(appDatabase.transactionExecutor).thenReturn(executor)
        whenever(appDatabase.queryExecutor).thenReturn(executor)
        whenever(settingsDatabase.transactionExecutor).thenReturn(executor)
        whenever(settingsDatabase.queryExecutor).thenReturn(executor)

        repository = CoupleRepositoryImpl(
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
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("k1")
            assertThat(it.spouseLookupKey).isNull()
        })
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("k2")
            assertThat(it.spouseLookupKey).isNull()
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("k1")
            assertThat(it.spouseLookupKey).isNull()
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("k2")
            assertThat(it.spouseLookupKey).isNull()
        })
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
