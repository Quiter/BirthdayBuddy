package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.domain.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class ContactRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissionChecker: PermissionChecker = mock()
    private val contactDao: ContactDao = mock()
    private val labelConfigDao: LabelConfigDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private val systemContactDataSource: SystemContactDataSource = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val widgetUpdater: WidgetUpdater = mock()
    private val contentResolver: ContentResolver = mock()
    private val clock: Clock = mock()
    private val contactDbMapper = ContactDbMapper()
    private val labelConfigMapper = LabelConfigMapper()

    // State flows to back our DAO mocks, defined and stubbed before repository initialization
    private val allContactsFlow = MutableStateFlow<List<ContactEntity>>(emptyList())
    private val labelConfigsFlow = MutableStateFlow<List<LabelConfigEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow(AppSettings())

    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        whenever(contactDao.getAllContacts()).thenReturn(allContactsFlow)
        whenever(labelConfigDao.getAllConfigs()).thenReturn(labelConfigsFlow)
        whenever(settingsRepository.settings).thenReturn(settingsFlow)
        runTest {
            whenever(settingsRepository.getSettingsImmediate()).thenReturn(AppSettings())
        }
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)
        whenever(clock.currentTimeMillis()).thenReturn(1_700_000_000_000L)

        repository = ContactRepositoryImpl(
            permissionChecker = permissionChecker,
            contactDao = contactDao,
            labelConfigDao = labelConfigDao,
            contactUserDataDao = contactUserDataDao,
            systemContactDataSource = systemContactDataSource,
            settingsRepository = settingsRepository,
            calendarSyncRepository = calendarSyncRepository,
            widgetUpdater = widgetUpdater,
            contactDbMapper = contactDbMapper,
            labelConfigMapper = labelConfigMapper,
            contentResolver = contentResolver,
            clock = clock,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun allContacts_emitsCorrectlyMappedDomainObjects() = runTest {
        // Arrange
        val entityList = listOf(
            ContactEntity(
                localId = 1L,
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10),
                isFavorite = true,
                labels = listOf("Friends"),
                giftIdeasJson = "[]",
                spouseLookupKey = "spouse_key"
            )
        )
        allContactsFlow.value = entityList

        // Act
        val result = repository.allContacts.first()

        // Assert
        assertThat(result).hasSize(1)
        val contact = result.first()
        assertThat(contact.contactId).isEqualTo("c1")
        assertThat(contact.lookupKey).isEqualTo("key1")
        assertThat(contact.fullName).isEqualTo("Alice")
        assertThat(contact.birthday).isEqualTo(LocalDate.of(1990, 5, 10))
        assertThat(contact.isFavorite).isTrue()
        assertThat(contact.labels).containsExactly("Friends")
        assertThat(contact.spouseLookupKey).isEqualTo("spouse_key")
    }

    @Test
    fun labelsEnabled_emitsCorrectValue_fromSettings() = runTest {
        settingsFlow.value = AppSettings(labelsEnabled = true)
        assertThat(repository.labelsEnabled.first()).isTrue()

        settingsFlow.value = AppSettings(labelsEnabled = false)
        assertThat(repository.labelsEnabled.first()).isFalse()
    }

    @Test
    fun syncContacts_doesNotSync_whenPermissionIsNotGranted() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(false)

        // Act
        repository.syncContacts()

        // Assert
        verify(systemContactDataSource, never()).fetchContactGroups()
        verify(contactDao, never()).getAllContactsImmediate()
        verify(widgetUpdater, never()).updateWidget()
        verify(calendarSyncRepository, never()).syncBirthdays(any())
    }

    @Test
    fun syncContacts_syncsSuccessfully_whenPermissionIsGranted() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        val systemContact = Contact(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Alice",
            birthday = LocalDate.of(1990, 1, 1),
            labels = listOf("Friends")
        )
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(systemContact)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act
        repository.syncContacts()

        // Assert
        verify(systemContactDataSource).fetchContactGroups()
        verify(systemContactDataSource).fetchContactsFromSystem(groups)
        verify(contactDao).getAllContactsImmediate()
        verify(labelConfigDao).getAllConfigsImmediate()
        verify(contactUserDataDao).getAllUserDataImmediate()
        verify(contactDao).refreshContacts(any())
        verify(settingsRepository).updateSettings(any())
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository, never()).syncBirthdays(any())
    }

    @Test
    fun syncContacts_updatesWidgetAndSyncsCalendar_whenCalendarSyncEnabledAndPermitted() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        val systemContact = Contact(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Alice",
            birthday = LocalDate.of(1990, 1, 1),
            labels = listOf("Friends")
        )
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(systemContact)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act
        repository.syncContacts()

        // Assert
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository).syncBirthdays(listOf(systemContact))
    }

    @Test
    fun syncContacts_doesNotSyncCalendar_whenCalendarPermissionMissing() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(false)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act
        repository.syncContacts()

        // Assert
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository, never()).syncBirthdays(any())
    }

    @Test
    fun syncContacts_continuesCalendarSync_whenWidgetUpdateThrowsException() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        whenever(widgetUpdater.updateWidget()).thenThrow(RuntimeException("Widget error"))
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        val systemContact = Contact(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Alice",
            birthday = LocalDate.of(1990, 1, 1)
        )
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(systemContact)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act (should not throw)
        repository.syncContacts()

        // Assert
        verify(contactDao).refreshContacts(any())
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository).syncBirthdays(listOf(systemContact))
    }

    @Test
    fun syncContacts_succeeds_whenCalendarSyncThrowsException() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        whenever(calendarSyncRepository.syncBirthdays(any()))
            .thenThrow(RuntimeException("Calendar error"))
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act (should not throw)
        repository.syncContacts()

        // Assert
        verify(contactDao).refreshContacts(any())
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository).syncBirthdays(emptyList())
    }

    @Test
    fun syncContacts_rethrowsCancellationException_fromWidgetUpdate() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(widgetUpdater.updateWidget()).thenThrow(CancellationException("Widget cancelled"))
        val groups = emptyMap<Long, GroupInfo>()
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.syncContacts()
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun syncContacts_rethrowsCancellationException_fromCalendarSync() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        whenever(calendarSyncRepository.syncBirthdays(any()))
            .thenThrow(CancellationException("Calendar cancelled"))
        val groups = emptyMap<Long, GroupInfo>()
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.syncContacts()
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun syncContacts_rethrowsCancellationException() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        whenever(systemContactDataSource.fetchContactGroups()).thenThrow(CancellationException("Job was cancelled"))

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.syncContacts()
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun syncContacts_logsAndRethrowsGenericException() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(systemContactDataSource.fetchContactGroups()).thenThrow(RuntimeException("Provider failed"))

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.syncContacts()
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(RuntimeException::class.java)
        assertThat(caught).hasMessageThat().isEqualTo("Provider failed")
        io.mockk.verify(exactly = 1) { Log.e(any(), any(), any<RuntimeException>()) }
        verify(contactDao, never()).refreshContacts(any())
        verify(widgetUpdater, never()).updateWidget()
        verify(calendarSyncRepository, never()).syncBirthdays(any())
    }

    @Test
    fun syncContacts_concurrentCalls_executeSequentiallyWithoutRaceCondition() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        val executingSyncCount = AtomicInteger(0)
        val maxConcurrentSyncs = AtomicInteger(0)
        val executionOrder = java.util.Collections.synchronizedList(mutableListOf<String>())

        val testContactDao = object : ContactDao by contactDao {
            override suspend fun refreshContacts(contacts: List<ContactEntity>) {
                val current = executingSyncCount.incrementAndGet()
                maxConcurrentSyncs.updateAndGet { maxOf(it, current) }
                executionOrder.add("start_refresh")
                delay(50.milliseconds)
                executionOrder.add("end_refresh")
                executingSyncCount.decrementAndGet()
            }
        }

        val testRepository = ContactRepositoryImpl(
            permissionChecker = permissionChecker,
            contactDao = testContactDao,
            labelConfigDao = labelConfigDao,
            contactUserDataDao = contactUserDataDao,
            systemContactDataSource = systemContactDataSource,
            settingsRepository = settingsRepository,
            calendarSyncRepository = calendarSyncRepository,
            widgetUpdater = widgetUpdater,
            contactDbMapper = contactDbMapper,
            labelConfigMapper = labelConfigMapper,
            contentResolver = contentResolver,
            clock = clock,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )

        // Act - Launch two concurrent sync calls
        val job1 = launch { testRepository.syncContacts() }
        val job2 = launch { testRepository.syncContacts() }

        job1.join()
        job2.join()

        // Assert - Mutex ensured strictly sequential execution without race conditions
        assertThat(maxConcurrentSyncs.get()).isEqualTo(1)
        assertThat(executionOrder).containsExactly(
            "start_refresh",
            "end_refresh",
            "start_refresh",
            "end_refresh"
        ).inOrder()
    }

    @Test
    fun syncContacts_releasesMutexOnException_allowingSubsequentSync() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        whenever(systemContactDataSource.fetchContactGroups())
            .thenThrow(RuntimeException("Transient system error"))
            .thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act 1 - First sync fails due to exception
        var caught: Throwable? = null
        try {
            repository.syncContacts()
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(RuntimeException::class.java)

        // Act 2 - Second sync should execute successfully because Mutex was cleanly unlocked
        repository.syncContacts()

        // Assert
        verify(contactDao).refreshContacts(any())
    }

    @Test
    fun labelConfigs_emitsCorrectlyMappedDomainObjects() = runTest {
        // Arrange
        val entityList = listOf(
            LabelConfigEntity(
                name = "Friends",
                isHiddenFromFilter = false,
                isIgnored = true,
                isSystem = false
            ),
            LabelConfigEntity(
                name = "Family",
                isHiddenFromFilter = true,
                isIgnored = false,
                isSystem = true
            )
        )
        labelConfigsFlow.value = entityList

        // Act
        val result = repository.labelConfigs.first()

        // Assert
        assertThat(result).hasSize(2)

        val config1 = result[0]
        assertThat(config1.name).isEqualTo("Friends")
        assertThat(config1.isHiddenFromFilter).isFalse()
        assertThat(config1.isIgnored).isTrue()
        assertThat(config1.isSystem).isFalse()

        val config2 = result[1]
        assertThat(config2.name).isEqualTo("Family")
        assertThat(config2.isHiddenFromFilter).isTrue()
        assertThat(config2.isIgnored).isFalse()
        assertThat(config2.isSystem).isTrue()
    }

    @Test
    fun updateLabelConfig_delegatesToLabelConfigDaoAndUpdatesWidget() = runTest {
        val config = LabelConfig(name = "Family", isHiddenFromFilter = true, isIgnored = false)
        repository.updateLabelConfig(config)

        val ordered = inOrder(labelConfigDao, widgetUpdater)
        ordered.verify(labelConfigDao).upsertConfig(labelConfigMapper.toEntity(config))
        ordered.verify(widgetUpdater).updateWidget()
    }

    @Test
    fun updateLabelConfig_succeeds_whenWidgetUpdateThrowsException() = runTest {
        val config = LabelConfig(name = "Family", isHiddenFromFilter = false, isIgnored = true)
        whenever(widgetUpdater.updateWidget()).thenThrow(RuntimeException("Widget error"))

        repository.updateLabelConfig(config)

        verify(labelConfigDao).upsertConfig(labelConfigMapper.toEntity(config))
        verify(widgetUpdater).updateWidget()
        io.mockk.verify(exactly = 1) { Log.e(any(), any(), any<RuntimeException>()) }
    }

    @Test
    fun updateLabelConfig_rethrowsCancellationException_fromWidgetUpdate() = runTest {
        val config = LabelConfig(name = "Family", showInWidget = false)
        whenever(widgetUpdater.updateWidget()).thenThrow(CancellationException("Widget cancelled"))

        var caught: Throwable? = null
        try {
            repository.updateLabelConfig(config)
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(CancellationException::class.java)
        verify(labelConfigDao).upsertConfig(labelConfigMapper.toEntity(config))
    }

    @Test
    fun updateLabelsEnabled_delegatesToSettingsRepositoryAndUpdatesWidget() = runTest {
        repository.updateLabelsEnabled(false)

        val ordered = inOrder(settingsRepository, widgetUpdater)
        ordered.verify(settingsRepository).updateSettings(any())
        ordered.verify(widgetUpdater).updateWidget()
    }

    @Test
    fun updateLabelsEnabled_succeeds_whenWidgetUpdateThrowsException() = runTest {
        whenever(widgetUpdater.updateWidget()).thenThrow(RuntimeException("Widget error"))

        repository.updateLabelsEnabled(true)

        verify(settingsRepository).updateSettings(any())
        verify(widgetUpdater).updateWidget()
        io.mockk.verify(exactly = 1) { Log.e(any(), any(), any<RuntimeException>()) }
    }

    @Test
    fun updateLabelsEnabled_rethrowsCancellationException_fromWidgetUpdate() = runTest {
        whenever(widgetUpdater.updateWidget()).thenThrow(CancellationException("Widget cancelled"))

        var caught: Throwable? = null
        try {
            repository.updateLabelsEnabled(false)
        } catch (e: Throwable) {
            caught = e
        }
        assertThat(caught).isInstanceOf(CancellationException::class.java)
        verify(settingsRepository).updateSettings(any())
    }

    @Test
    fun contactChanges_registersContentObserverAndUnregistersOnCancellation() = runTest {
        val job = launch {
            repository.contactChanges.collect {}
        }
        testScheduler.runCurrent()

        verify(contentResolver).registerContentObserver(
            eq(ContactsContract.Contacts.CONTENT_URI),
            eq(true),
            any()
        )

        job.cancel()
        testScheduler.runCurrent()

        verify(contentResolver).unregisterContentObserver(any())
    }

    @Test
    fun contactChanges_closesCleanlyWithoutUnregistering_whenRegisterThrowsSecurityException() = runTest {
        whenever(
            contentResolver.registerContentObserver(
                eq(ContactsContract.Contacts.CONTENT_URI),
                eq(true),
                any()
            )
        ).thenThrow(SecurityException("Permission denied"))

        val emissions = repository.contactChanges.toList()

        assertThat(emissions).isEmpty()
        verify(contentResolver, never()).unregisterContentObserver(any())
    }

    @Test
    fun updateContactBirthday_updatesWidgetAndSyncsCalendar_whenSuccessful() = runTest {
        // Arrange
        val birthday = LocalDate.of(1992, 8, 15)
        val groups = emptyMap<Long, GroupInfo>()
        val updatedContact = Contact(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Alice",
            birthday = birthday
        )
        whenever(systemContactDataSource.updateContactBirthday("c1", birthday)).thenReturn(true)
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(updatedContact)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act
        val result = repository.updateContactBirthday("c1", birthday)

        // Assert
        assertThat(result).isTrue()
        verify(contactDao).refreshContacts(any())
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository).syncBirthdays(listOf(updatedContact))
    }

    @Test
    fun updateContactBirthday_returnsTrue_whenWidgetAndCalendarSyncFail() = runTest {
        // Arrange
        val birthday = LocalDate.of(1992, 8, 15)
        val groups = emptyMap<Long, GroupInfo>()
        whenever(systemContactDataSource.updateContactBirthday("c1", birthday)).thenReturn(true)
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        whenever(widgetUpdater.updateWidget()).thenThrow(RuntimeException("Widget failure"))
        whenever(calendarSyncRepository.syncBirthdays(any()))
            .thenThrow(RuntimeException("Calendar failure"))

        // Act
        val result = repository.updateContactBirthday("c1", birthday)

        // Assert
        assertThat(result).isTrue()
        verify(contactDao).refreshContacts(any())
        verify(widgetUpdater).updateWidget()
        verify(calendarSyncRepository).syncBirthdays(emptyList())
    }

    @Test
    fun updateContactBirthday_doesNotSync_whenSystemUpdateFails() = runTest {
        // Arrange
        val birthday = LocalDate.of(1992, 8, 15)
        whenever(systemContactDataSource.updateContactBirthday("c1", birthday)).thenReturn(false)

        // Act
        val result = repository.updateContactBirthday("c1", birthday)

        // Assert
        assertThat(result).isFalse()
        verify(contactDao, never()).refreshContacts(any())
        verify(widgetUpdater, never()).updateWidget()
        verify(calendarSyncRepository, never()).syncBirthdays(any())
    }

    @Test
    fun syncContacts_filtersOutEmptyAndBlankLookupKeys() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(permissionChecker.hasCalendarPermission()).thenReturn(true)
        whenever(settingsRepository.getSettingsImmediate()).thenReturn(
            AppSettings(calendarSyncEnabled = true)
        )
        val groups = mapOf(1L to GroupInfo("Friends", isSystem = false))
        val emptyKeyContact = Contact(
            contactId = "c1",
            lookupKey = "",
            fullName = "Empty Key",
            birthday = LocalDate.of(1990, 1, 1),
            labels = listOf("Friends")
        )
        val blankKeyContact = Contact(
            contactId = "c2",
            lookupKey = "   ",
            fullName = "Blank Key",
            birthday = LocalDate.of(1991, 2, 2)
        )
        val validContact = Contact(
            contactId = "c3",
            lookupKey = "valid-key",
            fullName = "Valid Contact",
            birthday = LocalDate.of(1992, 3, 3)
        )
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(emptyKeyContact, blankKeyContact, validContact)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        // Act
        repository.syncContacts()

        // Assert
        val entitiesCaptor = argumentCaptor<List<ContactEntity>>()
        verify(contactDao).refreshContacts(entitiesCaptor.capture())
        val finalEntities = entitiesCaptor.firstValue
        assertThat(finalEntities).hasSize(1)
        assertThat(finalEntities.single().lookupKey).isEqualTo("valid-key")
        assertThat(finalEntities.none { it.lookupKey.isBlank() }).isTrue()

        val calendarCaptor = argumentCaptor<List<Contact>>()
        verify(calendarSyncRepository).syncBirthdays(calendarCaptor.capture())
        assertThat(calendarCaptor.firstValue).containsExactly(validContact)
    }

    @Test
    fun syncContacts_deduplicatesByLookupKey_preferringEntryWithBirthdayRegardlessOfOrder() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val groups = emptyMap<Long, GroupInfo>()
        val withoutBirthdayRich = Contact(
            contactId = "c1",
            lookupKey = "dup-key",
            fullName = "Alice",
            birthday = null,
            anniversary = LocalDate.of(2020, 6, 1),
            phoneNumber = "+491701234567",
            imageUri = "content://photo/1",
            isFavorite = true,
            hasWhatsApp = true,
            labels = listOf("Friends", "Family")
        )
        val withBirthdayMinimal = Contact(
            contactId = "c2",
            lookupKey = "dup-key",
            fullName = "Alice",
            birthday = LocalDate.of(1990, 5, 10)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)

        // Act 1: withoutBirthday first, withBirthday second
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(withoutBirthdayRich, withBirthdayMinimal)
        )
        repository.syncContacts()

        // Act 2: withBirthday first, withoutBirthday second
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(withBirthdayMinimal, withoutBirthdayRich)
        )
        repository.syncContacts()

        // Assert
        val entitiesCaptor = argumentCaptor<List<ContactEntity>>()
        verify(contactDao, org.mockito.kotlin.times(2)).refreshContacts(entitiesCaptor.capture())

        entitiesCaptor.allValues.forEach { finalEntities ->
            assertThat(finalEntities).hasSize(1)
            val entity = finalEntities.single()
            assertThat(entity.lookupKey).isEqualTo("dup-key")
            assertThat(entity.contactId).isEqualTo("c2")
            assertThat(entity.birthday).isEqualTo(LocalDate.of(1990, 5, 10))
        }
    }

    @Test
    fun syncContacts_deduplicatesByLookupKey_preferringBirthdayWithYearOverNoYear() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val groups = emptyMap<Long, GroupInfo>()
        val noYearContact = Contact(
            contactId = "c1",
            lookupKey = "dup-key",
            fullName = "Bob",
            birthday = LocalDate.of(NO_YEAR_MARKER, 7, 20),
            phoneNumber = "+491701234567",
            isFavorite = true
        )
        val withYearContact = Contact(
            contactId = "c2",
            lookupKey = "dup-key",
            fullName = "Bob",
            birthday = LocalDate.of(1988, 7, 20)
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(noYearContact, withYearContact)
        )

        // Act
        repository.syncContacts()

        // Assert
        val entitiesCaptor = argumentCaptor<List<ContactEntity>>()
        verify(contactDao).refreshContacts(entitiesCaptor.capture())
        val finalEntities = entitiesCaptor.firstValue
        assertThat(finalEntities).hasSize(1)
        assertThat(finalEntities.single().contactId).isEqualTo("c2")
        assertThat(finalEntities.single().birthday).isEqualTo(LocalDate.of(1988, 7, 20))
    }

    @Test
    fun syncContacts_deduplicatesByLookupKey_preferringMoreCompleteEntryWhenBothLackBirthday() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        val groups = emptyMap<Long, GroupInfo>()
        val sparseContact = Contact(
            contactId = "c1",
            lookupKey = "dup-key",
            fullName = "Charlie",
            birthday = null
        )
        val richContact = Contact(
            contactId = "c2",
            lookupKey = "dup-key",
            fullName = "Charlie",
            birthday = null,
            phoneNumber = "+491709876543",
            imageUri = "content://photo/2",
            labels = listOf("Work")
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)

        // Act 1: sparse first, rich second
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(sparseContact, richContact)
        )
        repository.syncContacts()

        // Act 2: rich first, sparse second
        whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
            listOf(richContact, sparseContact)
        )
        repository.syncContacts()

        // Assert
        val entitiesCaptor = argumentCaptor<List<ContactEntity>>()
        verify(contactDao, org.mockito.kotlin.times(2)).refreshContacts(entitiesCaptor.capture())

        entitiesCaptor.allValues.forEach { finalEntities ->
            assertThat(finalEntities).hasSize(1)
            val entity = finalEntities.single()
            assertThat(entity.lookupKey).isEqualTo("dup-key")
            assertThat(entity.contactId).isEqualTo("c2")
            assertThat(entity.phoneNumber).isEqualTo("+491709876543")
        }
    }

    @Test
    fun syncContacts_usesInjectedClockForLastSyncTimestamp() = runTest {
        val expectedTimestamp = 987_654_321L
        whenever(clock.currentTimeMillis()).thenReturn(expectedTimestamp)
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(systemContactDataSource.fetchContactGroups()).thenReturn(emptyMap())
        whenever(systemContactDataSource.fetchContactsFromSystem(emptyMap())).thenReturn(emptyList())
        whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
        whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

        repository.syncContacts()

        val transformCaptor = argumentCaptor<(AppSettings) -> AppSettings>()
        verify(settingsRepository).updateSettings(transformCaptor.capture())
        val updatedSettings = transformCaptor.firstValue(AppSettings())
        assertThat(updatedSettings.lastSyncTimestamp).isEqualTo(expectedTimestamp)
    }

    @Test
    fun syncContacts_excludesRedundantUppercaseLabelsUnderTurkishLocale() = runTest {
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))
        try {
            whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
            val groups = mapOf(
                1L to GroupInfo("FAVORITES", isSystem = false),
                2L to GroupInfo("STARRED IN ANDROID", isSystem = false),
                3L to GroupInfo("Friends", isSystem = false)
            )
            val systemContact = Contact(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 1, 1),
                labels = listOf("ICH", "Friends", "Work")
            )
            whenever(systemContactDataSource.fetchContactGroups()).thenReturn(groups)
            whenever(systemContactDataSource.fetchContactsFromSystem(groups)).thenReturn(
                listOf(systemContact)
            )
            whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
            whenever(labelConfigDao.getAllConfigsImmediate()).thenReturn(emptyList())
            whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())

            repository.syncContacts()

            val configsCaptor = argumentCaptor<List<LabelConfigEntity>>()
            verify(labelConfigDao).upsertConfigs(configsCaptor.capture())
            assertThat(configsCaptor.firstValue.map { it.name })
                .containsExactly("Friends", "Work")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}
