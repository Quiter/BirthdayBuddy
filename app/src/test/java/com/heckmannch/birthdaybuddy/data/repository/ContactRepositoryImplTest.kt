package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.AppSettingsDao
import com.heckmannch.birthdaybuddy.data.local.AppSettingsEntity
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.model.PotentialCouple
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ContactRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val permissionChecker: PermissionChecker = mock()
    private val contentResolver: ContentResolver = mock()
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
            permissionChecker = permissionChecker,
            contentResolver = contentResolver,
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
            labelConfigMapper = labelConfigMapper,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
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
        assertThat(contact.localId).isEqualTo(1L)
        assertThat(contact.contactId).isEqualTo("c1")
        assertThat(contact.lookupKey).isEqualTo("key1")
        assertThat(contact.fullName).isEqualTo("Alice")
        assertThat(contact.birthday).isEqualTo(LocalDate.of(1990, 5, 10))
        assertThat(contact.isFavorite).isTrue()
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
        whenever(permissionChecker.hasContactsPermission()).thenReturn(false)

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
    fun addGiftIdea_preservesSpouseLookupKey() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeas = emptyList(),
            spouseLookupKey = "spouse_key"
        )
        val existingUserData = ContactUserData(
            lookupKey = "key1",
            giftIdeas = emptyList(),
            spouseLookupKey = "spouse_key"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(existingUserData)

        val newIdea = GiftIdea(id = "idea1", text = "Book")

        // Act
        repository.addGiftIdea("key1", newIdea)

        // Assert
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.giftIdeas.first().text).isEqualTo("Book")
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun toggleGiftIdea_updatesContactGiftIdeas_whenIdeaBelongsToContact() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val idea = GiftIdea(id = "idea1", text = "Book", isChecked = false)
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeas = listOf(idea)
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(
            ContactUserData(lookupKey = "key1", giftIdeas = listOf(idea))
        )

        // Act
        repository.toggleGiftIdea("key1", idea, isChecked = true)

        // Assert
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun toggleGiftIdea_updatesSpouseGiftIdeas_whenIdeaBelongsToSpouse() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val spouseIdea = GiftIdea(id = "idea_spouse", text = "Perfume", isChecked = false)
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Max",
            giftIdeas = emptyList(),
            spouseLookupKey = "spouse_key"
        )
        val spouseEntity = ContactEntity(
            contactId = "c2",
            lookupKey = "spouse_key",
            fullName = "Erika",
            giftIdeas = listOf(spouseIdea),
            spouseLookupKey = "key1"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactDao.getContactByLookupKey("spouse_key")).thenReturn(spouseEntity)
        whenever(contactUserDataDao.getUserDataForContact("spouse_key")).thenReturn(
            ContactUserData(lookupKey = "spouse_key", giftIdeas = listOf(spouseIdea), spouseLookupKey = "key1")
        )

        // Act - Call toggle using primary contact key1
        repository.toggleGiftIdea("key1", spouseIdea, isChecked = true)

        // Assert - Updates should be applied to spouse_key
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun deleteGiftIdea_deletesFromContact_whenIdeaBelongsToContact() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val idea = GiftIdea(id = "idea1", text = "Book")
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeas = listOf(idea)
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(
            ContactUserData(lookupKey = "key1", giftIdeas = listOf(idea))
        )

        // Act
        repository.deleteGiftIdea("key1", "idea1")

        // Assert
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).isEmpty()
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).isEmpty()
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun deleteGiftIdea_deletesFromSpouse_whenIdeaBelongsToSpouse() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val spouseIdea = GiftIdea(id = "idea_spouse", text = "Perfume")
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Max",
            giftIdeas = emptyList(),
            spouseLookupKey = "spouse_key"
        )
        val spouseEntity = ContactEntity(
            contactId = "c2",
            lookupKey = "spouse_key",
            fullName = "Erika",
            giftIdeas = listOf(spouseIdea),
            spouseLookupKey = "key1"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactDao.getContactByLookupKey("spouse_key")).thenReturn(spouseEntity)
        whenever(contactUserDataDao.getUserDataForContact("spouse_key")).thenReturn(
            ContactUserData(lookupKey = "spouse_key", giftIdeas = listOf(spouseIdea), spouseLookupKey = "key1")
        )

        // Act - Call delete using primary contact key1
        repository.deleteGiftIdea("key1", "idea_spouse")
        
        // Assert - Deletion should happen on spouse_key
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas).isEmpty()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas).isEmpty()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun updateGiftIdeaText_updatesContactGiftIdea_whenIdeaBelongsToContact() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val idea = GiftIdea(id = "idea1", text = "Old text")
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeas = listOf(idea)
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(
            ContactUserData(lookupKey = "key1", giftIdeas = listOf(idea))
        )

        // Act
        repository.updateGiftIdeaText("key1", "idea1", "Updated text")

        // Assert
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun updateGiftIdeaText_updatesSpouseGiftIdea_whenIdeaBelongsToSpouse() = runTest(
        createTransactionElement()
    ) {
        // Arrange
        val spouseIdea = GiftIdea(id = "idea_spouse", text = "Old text")
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "Max",
            giftIdeas = emptyList(),
            spouseLookupKey = "spouse_key"
        )
        val spouseEntity = ContactEntity(
            contactId = "c2",
            lookupKey = "spouse_key",
            fullName = "Erika",
            giftIdeas = listOf(spouseIdea),
            spouseLookupKey = "key1"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactDao.getContactByLookupKey("spouse_key")).thenReturn(spouseEntity)
        whenever(contactUserDataDao.getUserDataForContact("spouse_key")).thenReturn(
            ContactUserData(lookupKey = "spouse_key", giftIdeas = listOf(spouseIdea), spouseLookupKey = "key1")
        )

        // Act - Call update using primary contact key1
        repository.updateGiftIdeaText("key1", "idea_spouse", "Updated text")

        // Assert - Update should happen on spouse_key
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
            assertThat(it.spouseLookupKey).isEqualTo("key1")
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

    @Test
    fun exportGiftIdeas_writesBackupToOutputStream() = runTest {
        // Arrange
        val uri: Uri = mock()
        val json = "{\"ideas\": []}"
        whenever(giftIdeaBackupManager.exportGiftIdeas()).thenReturn(json)
        val outputStream = ByteArrayOutputStream()
        whenever(contentResolver.openOutputStream(uri)).thenReturn(outputStream)

        // Act
        repository.exportGiftIdeas(uri)

        // Assert
        verify(giftIdeaBackupManager).exportGiftIdeas()
        verify(contentResolver).openOutputStream(uri)
        assertThat(outputStream.toString()).isEqualTo(json)
    }

    @Test
    fun importGiftIdeas_readsBackupFromInputStreamAndTriggersSync() = runTest {
        // Arrange
        val uri: Uri = mock()
        val json = "{\"ideas\": []}"
        val inputStream = ByteArrayInputStream(json.toByteArray())
        whenever(contentResolver.openInputStream(uri)).thenReturn(inputStream)
        whenever(giftIdeaBackupManager.importGiftIdeas(json)).thenReturn(3)

        // Stub syncContacts requirements
        whenever(permissionChecker.hasContactsPermission()).thenReturn(false)

        // Act
        val result = repository.importGiftIdeas(uri)

        // Assert
        verify(contentResolver).openInputStream(uri)
        verify(giftIdeaBackupManager).importGiftIdeas(json)
        assertThat(result).isEqualTo(3)
    }
}
