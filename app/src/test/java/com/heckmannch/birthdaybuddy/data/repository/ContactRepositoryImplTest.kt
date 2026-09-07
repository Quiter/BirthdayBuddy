package com.heckmannch.birthdaybuddy.data.repository

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
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissionChecker: PermissionChecker = mock()
    private val contactDao: ContactDao = mock()
    private val labelConfigDao: LabelConfigDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private val systemContactDataSource: SystemContactDataSource = mock()
    private val settingsRepository: SettingsRepository = mock()
    private val contactDbMapper = ContactDbMapper()
    private val labelConfigMapper = LabelConfigMapper()

    // State flows to back our DAO mocks, defined and stubbed before repository initialization
    private val allContactsFlow = MutableStateFlow<List<ContactEntity>>(emptyList())
    private val labelConfigsFlow = MutableStateFlow<List<LabelConfigEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow(AppSettings())

    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setUp() {
        whenever(contactDao.getAllContacts()).thenReturn(allContactsFlow)
        whenever(labelConfigDao.getAllConfigs()).thenReturn(labelConfigsFlow)
        whenever(settingsRepository.settings).thenReturn(settingsFlow)

        repository = ContactRepositoryImpl(
            permissionChecker = permissionChecker,
            contactDao = contactDao,
            labelConfigDao = labelConfigDao,
            contactUserDataDao = contactUserDataDao,
            systemContactDataSource = systemContactDataSource,
            settingsRepository = settingsRepository,
            contactDbMapper = contactDbMapper,
            labelConfigMapper = labelConfigMapper,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
        )
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
                giftIdeas = emptyList(),
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
    fun syncContacts_catchesAndLogsGenericExceptionWithoutCrashing() = runTest {
        // Arrange
        whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
        whenever(systemContactDataSource.fetchContactGroups()).thenThrow(RuntimeException("Provider failed"))

        // Act (should not throw)
        repository.syncContacts()

        // Assert
        verify(contactDao, never()).refreshContacts(any())
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
            contactDbMapper = contactDbMapper,
            labelConfigMapper = labelConfigMapper,
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
        repository.syncContacts()

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
    fun updateLabelConfig_delegatesToLabelConfigDao() = runTest {
        val config = LabelConfig(name = "Family", isHiddenFromFilter = true, isIgnored = false)
        repository.updateLabelConfig(config)
        verify(labelConfigDao).upsertConfig(labelConfigMapper.toEntity(config))
    }

    @Test
    fun updateLabelsEnabled_delegatesToSettingsRepository() = runTest {
        repository.updateLabelsEnabled(false)
        verify(settingsRepository).updateSettings(any())
    }
}
