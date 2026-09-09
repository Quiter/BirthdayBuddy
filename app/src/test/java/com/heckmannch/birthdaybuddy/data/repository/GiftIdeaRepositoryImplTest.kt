package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.AppDatabase
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * JVM Unit Tests for [GiftIdeaRepositoryImpl].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GiftIdeaRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contentResolver: ContentResolver = mock()
    private val contactDao: ContactDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private val giftIdeaBackupManager: GiftIdeaBackupManager = mock()
    private val contactRepository: ContactRepository = mock()
    private val widgetUpdater: WidgetUpdater = mock()
    private val appDatabase: AppDatabase = mock()
    private val settingsDatabase: SettingsDatabase = mock()

    private lateinit var repository: GiftIdeaRepositoryImpl

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        val executor = java.util.concurrent.Executor { it.run() }
        whenever(appDatabase.transactionExecutor).thenReturn(executor)
        whenever(appDatabase.queryExecutor).thenReturn(executor)
        whenever(settingsDatabase.transactionExecutor).thenReturn(executor)
        whenever(settingsDatabase.queryExecutor).thenReturn(executor)

        repository = GiftIdeaRepositoryImpl(
            contentResolver = contentResolver,
            contactDao = contactDao,
            contactUserDataDao = contactUserDataDao,
            giftIdeaBackupManager = giftIdeaBackupManager,
            contactRepository = contactRepository,
            appDatabase = appDatabase,
            settingsDatabase = settingsDatabase,
            widgetUpdater = widgetUpdater,
            ioDispatcher = mainDispatcherRule.testDispatcher,
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
    fun addGiftIdea_delegatesToContactUserDataDaoAndUpdatesCache() = runTest(createTransactionElement()) {
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
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.giftIdeas.first().text).isEqualTo("Book")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.giftIdeas.first().text).isEqualTo("Book")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun addGiftIdea_preservesSpouseLookupKey() = runTest(createTransactionElement()) {
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
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.giftIdeas.first().text).isEqualTo("Book")
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).hasSize(1)
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun addGiftIdea_rollsBackSettings_whenCacheUpdateFails() = runTest(createTransactionElement()) {
        // Arrange
        val prevUserData = ContactUserData(lookupKey = "key1", giftIdeas = emptyList())
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeas = emptyList()
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
            .thenThrow(RuntimeException("Cache error"))
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(prevUserData)

        val newIdea = GiftIdea(id = "idea1", text = "Book")

        // Act & Assert
        var caught: Throwable? = null
        try {
            repository.addGiftIdea("key1", newIdea)
        } catch (e: Throwable) {
            caught = e
        }

        assertThat(caught).isNotNull()
        verify(contactUserDataDao).upsertUserData(prevUserData)
    }

    @Test
    fun toggleGiftIdea_updatesContactGiftIdeas_whenIdeaBelongsToContact() = runTest(createTransactionElement()) {
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
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun toggleGiftIdea_updatesSpouseGiftIdeas_whenIdeaBelongsToSpouse() = runTest(createTransactionElement()) {
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
            ContactUserData(
                lookupKey = "spouse_key",
                giftIdeas = listOf(spouseIdea),
                spouseLookupKey = "key1"
            )
        )

        // Act - Call toggle using primary contact key1
        repository.toggleGiftIdea("key1", spouseIdea, isChecked = true)

        // Assert - Updates should be applied to spouse_key
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().isChecked).isTrue()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun deleteGiftIdea_deletesFromContact_whenIdeaBelongsToContact() = runTest(createTransactionElement()) {
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
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).isEmpty()
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas).isEmpty()
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun deleteGiftIdea_deletesFromSpouse_whenIdeaBelongsToSpouse() = runTest(createTransactionElement()) {
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
            ContactUserData(
                lookupKey = "spouse_key",
                giftIdeas = listOf(spouseIdea),
                spouseLookupKey = "key1"
            )
        )

        // Act - Call delete using primary contact key1
        repository.deleteGiftIdea("key1", "idea_spouse")

        // Assert - Deletion should happen on spouse_key
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas).isEmpty()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas).isEmpty()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun updateGiftIdeaText_updatesContactGiftIdea_whenIdeaBelongsToContact() = runTest(createTransactionElement()) {
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
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun updateGiftIdeaText_updatesSpouseGiftIdea_whenIdeaBelongsToSpouse() = runTest(createTransactionElement()) {
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
            ContactUserData(
                lookupKey = "spouse_key",
                giftIdeas = listOf(spouseIdea),
                spouseLookupKey = "key1"
            )
        )

        // Act - Call update using primary contact key1
        repository.updateGiftIdeaText("key1", "idea_spouse", "Updated text")

        // Assert - Update should happen on spouse_key
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            assertThat(it.giftIdeas.first().text).isEqualTo("Updated text")
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun exportGiftIdeas_writesBackupToOutputStream() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "{\"ideas\": []}"
            whenever(giftIdeaBackupManager.exportGiftIdeas()).thenReturn(json)
            val outputStream = ByteArrayOutputStream()
            whenever(contentResolver.openOutputStream(mockUri)).thenReturn(outputStream)

            repository.exportGiftIdeas(uriString)

            verify(giftIdeaBackupManager).exportGiftIdeas()
            verify(contentResolver).openOutputStream(mockUri)
            assertThat(outputStream.toString()).isEqualTo(json)
        }
    }

    @Test
    fun importGiftIdeas_readsBackupFromInputStreamAndReturnsCount() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "{\"ideas\": []}"
            val inputStream = ByteArrayInputStream(json.toByteArray())
            whenever(contentResolver.openInputStream(mockUri)).thenReturn(inputStream)
            whenever(giftIdeaBackupManager.importGiftIdeas(json)).thenReturn(3)

            val result = repository.importGiftIdeas(uriString)

            assertThat(result).isEqualTo(3)
            verify(contentResolver).openInputStream(mockUri)
            verify(giftIdeaBackupManager).importGiftIdeas(json)
            verify(contactRepository).syncContacts()
        }
    }
}
