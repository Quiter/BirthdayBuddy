package com.heckmannch.birthdaybuddy.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.model.PotentialCouple
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mock()
    private val contactDao: ContactDao = mock()
    private val labelConfigDao: LabelConfigDao = mock()
    private val appSettingsDao: AppSettingsDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private val systemContactDataSource: SystemContactDataSource = mock()
    private val giftIdeaBackupManager: GiftIdeaBackupManager = mock()
    private val calendarSyncRepository: CalendarSyncRepository = mock()
    private val widgetUpdater: WidgetUpdater = mock()
    private val appDatabase: AppDatabase = mock()
    private val settingsDatabase: SettingsDatabase = mock()
    private val contactDbMapper = ContactDbMapper()
    private val labelConfigMapper = LabelConfigMapper()

    // State flows to back our DAO mocks, defined and stubbed before repository initialization
    private val allContactsFlow = MutableStateFlow<List<ContactEntity>>(emptyList())
    private val potentialCouplesFlow = MutableStateFlow<List<PotentialCouple>>(emptyList())
    private val labelConfigsFlow = MutableStateFlow<List<LabelConfigEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow<AppSettingsEntity?>(null)

    private lateinit var repository: ContactRepositoryImpl

    @Before
    fun setUp() {
        // Stub the flows with default values before instantiating repository to prevent NullPointerException
        whenever(contactDao.getAllContacts()).thenReturn(allContactsFlow)
        whenever(contactDao.getPotentialCouples()).thenReturn(potentialCouplesFlow)
        whenever(labelConfigDao.getAllConfigs()).thenReturn(labelConfigsFlow)
        whenever(appSettingsDao.getSettings()).thenReturn(settingsFlow)

        // Stub the database transaction executors to execute blocks synchronously on JVM
        val executor = java.util.concurrent.Executor { it.run() }
        whenever(appDatabase.transactionExecutor).thenReturn(executor)
        whenever(appDatabase.queryExecutor).thenReturn(executor)
        whenever(settingsDatabase.transactionExecutor).thenReturn(executor)
        whenever(settingsDatabase.queryExecutor).thenReturn(executor)

        repository = ContactRepositoryImpl(
            context = context,
            contactDao = contactDao,
            labelConfigDao = labelConfigDao,
            appSettingsDao = appSettingsDao,
            contactUserDataDao = contactUserDataDao,
            systemContactDataSource = systemContactDataSource,
            giftIdeaBackupManager = giftIdeaBackupManager,
            calendarSyncRepository = calendarSyncRepository,
            widgetUpdater = widgetUpdater,
            appDatabase = appDatabase,
            settingsDatabase = settingsDatabase,
            contactDbMapper = contactDbMapper,
            labelConfigMapper = labelConfigMapper
        )
    }

    /**
     * Creates a Room TransactionElement using reflection to bypass Room's transaction manager connection pool.
     */
    private fun createTransactionElement(): kotlin.coroutines.CoroutineContext {
        val clazz = Class.forName("androidx.room.TransactionElement")
        val constructor = clazz.constructors.first()
        return constructor.newInstance(Dispatchers.Unconfined) as kotlin.coroutines.CoroutineContext
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
        assertThat(contact.localId).isEqualTo(1L)
        assertThat(contact.contactId).isEqualTo("c1")
        assertThat(contact.lookupKey).isEqualTo("key1")
        assertThat(contact.fullName).isEqualTo("Alice")
        assertThat(contact.birthday).isEqualTo(LocalDate.of(1990, 5, 10))
        assertThat(contact.labels).containsExactly("Friends")
        assertThat(contact.spouseLookupKey).isEqualTo("spouse_key")
    }

    @Test
    fun labelsEnabled_emitsTrue_whenSettingsEntityDoesNotExist() = runTest {
        // Arrange
        settingsFlow.value = null

        // Act
        val result = repository.labelsEnabled.first()

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun labelsEnabled_emitsCorrectValue_whenSettingsEntityExists() = runTest {
        // Arrange & Act & Assert
        settingsFlow.value = AppSettingsEntity(labelsEnabled = true)
        assertThat(repository.labelsEnabled.first()).isTrue()

        settingsFlow.value = AppSettingsEntity(labelsEnabled = false)
        assertThat(repository.labelsEnabled.first()).isFalse()
    }

    @Test
    fun syncContacts_doesNotSync_whenPermissionIsNotGranted() = runTest {
        // Arrange
        whenever(context.checkPermission(eq(Manifest.permission.READ_CONTACTS), any(), any()))
            .thenReturn(PackageManager.PERMISSION_DENIED)

        // Act
        repository.syncContacts()

        // Assert
        verify(systemContactDataSource, never()).fetchContactGroups()
        verify(contactDao, never()).getAllContactsImmediate()
    }

    @Test
    fun addGiftIdea_delegatesToContactUserDataDaoAndUpdatesCache() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeas = emptyList()
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(null)

        val newIdea = GiftIdea(id = "idea1", text = "Book")

        // Act
        repository.addGiftIdea("key1", newIdea)

        // Assert
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.giftIdeas.first().text).isEqualTo("Book")
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.giftIdeas.first().text).isEqualTo("Book")
        })
        verify(widgetUpdater).updateWidget()
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
}
