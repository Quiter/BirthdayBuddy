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
import com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/**
 * JVM Unit Tests for [GiftIdeaRepositoryImpl].
 */
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
    private val converters = GiftIdeaConverters()

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

        runBlocking {
            whenever(contactDao.getAllContactsImmediate()).thenReturn(emptyList())
            whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        }

        repository = GiftIdeaRepositoryImpl(
            contentResolver = contentResolver,
            contactDao = contactDao,
            contactUserDataDao = contactUserDataDao,
            giftIdeaBackupManager = giftIdeaBackupManager,
            contactRepository = contactRepository,
            appDatabase = appDatabase,
            settingsDatabase = settingsDatabase,
            widgetUpdater = widgetUpdater,
            giftIdeaConverters = converters,
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
            giftIdeasJson = "[]"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(null)

        val newIdea = GiftIdea(id = "idea1", text = "Book")

        // Act
        repository.addGiftIdea("key1", newIdea)

        // Assert
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).hasSize(1)
            assertThat(ideas.first().text).isEqualTo("Book")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).hasSize(1)
            assertThat(ideas.first().text).isEqualTo("Book")
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
            giftIdeasJson = "[]",
            spouseLookupKey = "spouse_key"
        )
        val existingUserData = ContactUserData(
            lookupKey = "key1",
            giftIdeasJson = "[]",
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
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).hasSize(1)
            assertThat(ideas.first().text).isEqualTo("Book")
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).hasSize(1)
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun addGiftIdea_rollsBackSettings_whenCacheUpdateFails() = runTest(createTransactionElement()) {
        // Arrange
        val prevUserData = ContactUserData(lookupKey = "key1", giftIdeasJson = "[]")
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeasJson = "[]"
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
    fun addGiftIdea_rollsBackSettingsAndRethrows_whenCacheUpdateIsCancelled() =
        runTest(createTransactionElement()) {
            // Arrange
            val prevUserData = ContactUserData(
                lookupKey = "key1",
                giftIdeasJson = "[]",
                spouseLookupKey = "spouse_key"
            )
            val contactEntity = ContactEntity(
                contactId = "c1",
                lookupKey = "key1",
                fullName = "John Doe",
                giftIdeasJson = "[]",
                spouseLookupKey = "spouse_key"
            )
            whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
                .thenThrow(CancellationException("Cancelled"))
            whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(prevUserData)

            val newIdea = GiftIdea(id = "idea1", text = "Book")

            // Act
            var caught: Throwable? = null
            try {
                repository.addGiftIdea("key1", newIdea)
            } catch (e: Throwable) {
                caught = e
            }

            // Assert
            assertThat(caught).isInstanceOf(CancellationException::class.java)
            verify(contactUserDataDao).upsertUserData(prevUserData)
            verify(widgetUpdater, never()).updateWidget()
        }

    @Test
    fun toggleGiftIdea_updatesContactGiftIdeas_whenIdeaBelongsToContact() = runTest(createTransactionElement()) {
        // Arrange
        val idea = GiftIdea(id = "idea1", text = "Book", isChecked = false)
        val contactEntity = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeasJson = converters.fromGiftIdeaList(listOf(idea))
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(
            ContactUserData(lookupKey = "key1", giftIdeasJson = converters.fromGiftIdeaList(listOf(idea)))
        )

        // Act
        repository.toggleGiftIdea("key1", idea, isChecked = true)

        // Assert
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().isChecked).isTrue()
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().isChecked).isTrue()
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
            giftIdeasJson = "[]",
            spouseLookupKey = "spouse_key"
        )
        val spouseEntity = ContactEntity(
            contactId = "c2",
            lookupKey = "spouse_key",
            fullName = "Erika",
            giftIdeasJson = converters.fromGiftIdeaList(listOf(spouseIdea)),
            spouseLookupKey = "key1"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactDao.getContactByLookupKey("spouse_key")).thenReturn(spouseEntity)
        whenever(contactUserDataDao.getUserDataForContact("spouse_key")).thenReturn(
            ContactUserData(
                lookupKey = "spouse_key",
                giftIdeasJson = converters.fromGiftIdeaList(listOf(spouseIdea)),
                spouseLookupKey = "key1"
            )
        )

        // Act - Call toggle using primary contact key1
        repository.toggleGiftIdea("key1", spouseIdea, isChecked = true)

        // Assert - Updates should be applied to spouse_key
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().isChecked).isTrue()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().isChecked).isTrue()
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
            giftIdeasJson = converters.fromGiftIdeaList(listOf(idea))
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(
            ContactUserData(lookupKey = "key1", giftIdeasJson = converters.fromGiftIdeaList(listOf(idea)))
        )

        // Act
        repository.deleteGiftIdea("key1", "idea1")

        // Assert
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).isEmpty()
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).isEmpty()
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
            giftIdeasJson = "[]",
            spouseLookupKey = "spouse_key"
        )
        val spouseEntity = ContactEntity(
            contactId = "c2",
            lookupKey = "spouse_key",
            fullName = "Erika",
            giftIdeasJson = converters.fromGiftIdeaList(listOf(spouseIdea)),
            spouseLookupKey = "key1"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactDao.getContactByLookupKey("spouse_key")).thenReturn(spouseEntity)
        whenever(contactUserDataDao.getUserDataForContact("spouse_key")).thenReturn(
            ContactUserData(
                lookupKey = "spouse_key",
                giftIdeasJson = converters.fromGiftIdeaList(listOf(spouseIdea)),
                spouseLookupKey = "key1"
            )
        )

        // Act - Call delete using primary contact key1
        repository.deleteGiftIdea("key1", "idea_spouse")

        // Assert - Deletion should happen on spouse_key
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).isEmpty()
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas).isEmpty()
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
            giftIdeasJson = converters.fromGiftIdeaList(listOf(idea))
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(
            ContactUserData(lookupKey = "key1", giftIdeasJson = converters.fromGiftIdeaList(listOf(idea)))
        )

        // Act
        repository.updateGiftIdeaText("key1", "idea1", "Updated text")

        // Assert
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().text).isEqualTo("Updated text")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("key1")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().text).isEqualTo("Updated text")
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
            giftIdeasJson = "[]",
            spouseLookupKey = "spouse_key"
        )
        val spouseEntity = ContactEntity(
            contactId = "c2",
            lookupKey = "spouse_key",
            fullName = "Erika",
            giftIdeasJson = converters.fromGiftIdeaList(listOf(spouseIdea)),
            spouseLookupKey = "key1"
        )
        whenever(contactDao.getContactByLookupKey("key1")).thenReturn(contactEntity)
        whenever(contactDao.getContactByLookupKey("spouse_key")).thenReturn(spouseEntity)
        whenever(contactUserDataDao.getUserDataForContact("spouse_key")).thenReturn(
            ContactUserData(
                lookupKey = "spouse_key",
                giftIdeasJson = converters.fromGiftIdeaList(listOf(spouseIdea)),
                spouseLookupKey = "key1"
            )
        )

        // Act - Call update using primary contact key1
        repository.updateGiftIdeaText("key1", "idea_spouse", "Updated text")

        // Assert - Update should happen on spouse_key
        verify(contactUserDataDao).upsertUserData(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().text).isEqualTo("Updated text")
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(contactDao).upsertContact(check {
            assertThat(it.lookupKey).isEqualTo("spouse_key")
            val ideas = converters.toGiftIdeaList(it.giftIdeasJson)
            assertThat(ideas.first().text).isEqualTo("Updated text")
            assertThat(it.spouseLookupKey).isEqualTo("key1")
        })
        verify(widgetUpdater).updateWidget()
    }

    @Test
    fun exportGiftIdeas_writesBackupToOutputStreamWithTruncateModeAndUtf8() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "{\"ideas\": [\"Bücher & Geschenke 🎁\"]}"
            whenever(giftIdeaBackupManager.exportGiftIdeas()).thenReturn(json)
            val outputStream = ByteArrayOutputStream()
            whenever(contentResolver.openOutputStream(mockUri, "wt")).thenReturn(outputStream)

            repository.exportGiftIdeas(uriString)

            verify(giftIdeaBackupManager).exportGiftIdeas()
            verify(contentResolver).openOutputStream(mockUri, "wt")
            assertThat(outputStream.toByteArray()).isEqualTo(json.toByteArray(Charsets.UTF_8))
        }
    }

    @Test
    fun exportGiftIdeas_throwsIOException_whenOpenOutputStreamReturnsNull() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            whenever(giftIdeaBackupManager.exportGiftIdeas()).thenReturn("[]")
            whenever(contentResolver.openOutputStream(mockUri, "wt")).thenReturn(null)

            var thrown: Throwable? = null
            try {
                repository.exportGiftIdeas(uriString)
            } catch (e: Throwable) {
                thrown = e
            }

            assertThat(thrown).isInstanceOf(IOException::class.java)
            assertThat(thrown?.message).isEqualTo("Could not open output stream for URI: $mockUri")
            verify(contentResolver).openOutputStream(mockUri, "wt")
        }
    }

    @Test
    fun exportGiftIdeas_fallsBackToWriteMode_whenProviderRejectsTruncateMode() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "{\"ideas\": []}"
            whenever(giftIdeaBackupManager.exportGiftIdeas()).thenReturn(json)
            val outputStream = ByteArrayOutputStream()
            whenever(contentResolver.openOutputStream(mockUri, "wt"))
                .thenThrow(IllegalArgumentException("Unsupported mode: wt"))
            whenever(contentResolver.openOutputStream(mockUri, "w")).thenReturn(outputStream)

            repository.exportGiftIdeas(uriString)

            verify(contentResolver).openOutputStream(mockUri, "wt")
            verify(contentResolver).openOutputStream(mockUri, "w")
            assertThat(outputStream.toByteArray()).isEqualTo(json.toByteArray(Charsets.UTF_8))
        }
    }

    @Test
    fun importGiftIdeas_readsBackupFromInputStreamAndReturnsCount() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "{\"ideas\": []}"
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            whenever(contentResolver.openInputStream(mockUri)).thenReturn(inputStream)
            whenever(giftIdeaBackupManager.importGiftIdeas(json)).thenReturn(3)

            val result = repository.importGiftIdeas(uriString)

            assertThat(result).isEqualTo(3)
            verify(contentResolver).openInputStream(mockUri)
            verify(giftIdeaBackupManager).importGiftIdeas(json)
            verify(contactRepository).syncContacts()
        }
    }

    @Test
    fun importGiftIdeas_updatesCachedContactsGiftIdeas_evenWithoutContactsPermission() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "[{\"lookupKey\":\"key1\"}]"
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            whenever(contentResolver.openInputStream(mockUri)).thenReturn(inputStream)
            whenever(giftIdeaBackupManager.importGiftIdeas(json)).thenReturn(1)
            // Simulate contactRepository.syncContacts() returning early due to missing permission (no-op)
            whenever(contactRepository.syncContacts()).thenReturn(Unit)

            val importedJson = converters.fromGiftIdeaList(listOf(GiftIdea(id = "i1", text = "Blumen")))
            val contact1 = ContactEntity(
                localId = 1L,
                contactId = "c1",
                lookupKey = "key1",
                fullName = "Anna",
                giftIdeasJson = "[]"
            )
            val contact2 = ContactEntity(
                localId = 2L,
                contactId = "c2",
                lookupKey = "key2",
                fullName = "Ben",
                giftIdeasJson = "[]"
            )
            whenever(contactDao.getAllContactsImmediate()).thenReturn(listOf(contact1, contact2))
            whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(
                listOf(
                    ContactUserData(lookupKey = "key1", giftIdeasJson = importedJson),
                    ContactUserData(lookupKey = "key2", giftIdeasJson = "[]")
                )
            )

            val result = repository.importGiftIdeas(uriString)

            assertThat(result).isEqualTo(1)
            verify(contactRepository).syncContacts()
            verify(contactDao).upsertContacts(check { updated ->
                assertThat(updated).hasSize(1)
                assertThat(updated.first().lookupKey).isEqualTo("key1")
                assertThat(updated.first().giftIdeasJson).isEqualTo(importedJson)
            })
            verify(widgetUpdater).updateWidget()
        }
    }

    @Test
    fun importGiftIdeas_doesNotSyncCache_whenImportCountIsZero() = runTest {
        val uriString = "content://test_uri"
        val mockUri: Uri = mock()
        mockStatic(Uri::class.java).use { mockedStatic ->
            mockedStatic.`when`<Uri> { Uri.parse(uriString) }.thenReturn(mockUri)

            val json = "[]"
            val inputStream = ByteArrayInputStream(json.toByteArray(Charsets.UTF_8))
            whenever(contentResolver.openInputStream(mockUri)).thenReturn(inputStream)
            whenever(giftIdeaBackupManager.importGiftIdeas(json)).thenReturn(0)

            val result = repository.importGiftIdeas(uriString)

            assertThat(result).isEqualTo(0)
            verify(contactRepository, never()).syncContacts()
            verify(contactDao, never()).upsertContacts(any())
            verify(widgetUpdater, never()).updateWidget()
        }
    }

    @Test
    fun concurrentMutations_areSerializedByMutex_withoutLostUpdates() = runTest(createTransactionElement()) {
        // Arrange: in-memory backing state for ContactDao and ContactUserDataDao
        var storedContact = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeasJson = "[]"
        )
        var storedUserData: ContactUserData? = null
        val writeCount = AtomicInteger(0)
        val firstWriteEntered = CompletableDeferred<Unit>()
        val allowFirstWriteToFinish = CompletableDeferred<Unit>()

        whenever(contactDao.getContactByLookupKey("key1")).doSuspendableAnswer { storedContact }
        whenever(contactUserDataDao.getUserDataForContact("key1")).doSuspendableAnswer { storedUserData }
        whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { invocation ->
            val data = invocation.getArgument<ContactUserData>(0)
            if (writeCount.incrementAndGet() == 1) {
                firstWriteEntered.complete(Unit)
                allowFirstWriteToFinish.await()
            }
            storedUserData = data
        }
        whenever(contactDao.upsertContact(any())).doSuspendableAnswer { invocation ->
            storedContact = invocation.getArgument(0)
        }

        val idea1 = GiftIdea(id = "idea1", text = "Book", isChecked = false)
        val idea2 = GiftIdea(id = "idea2", text = "Flowers", isChecked = false)

        // Act: start first mutation and suspend mid-write, then fire concurrent mutations
        val job1 = launch { repository.addGiftIdea("key1", idea1) }
        firstWriteEntered.await()

        val job2 = launch { repository.addGiftIdea("key1", idea2) }
        val job3 = launch { repository.toggleGiftIdea("key1", idea1, isChecked = true) }
        val job4 = launch { repository.updateGiftIdeaText("key1", "idea2", "Red Flowers") }

        allowFirstWriteToFinish.complete(Unit)

        job1.join()
        job2.join()
        job3.join()
        job4.join()

        // Assert: all mutations were applied sequentially without lost updates
        val finalCacheIdeas = converters.toGiftIdeaList(storedContact.giftIdeasJson)
        val finalSettingsIdeas = converters.toGiftIdeaList(storedUserData!!.giftIdeasJson)

        assertThat(finalCacheIdeas).hasSize(2)
        assertThat(finalCacheIdeas.find { it.id == "idea1" }?.isChecked).isTrue()
        assertThat(finalCacheIdeas.find { it.id == "idea1" }?.text).isEqualTo("Book")
        assertThat(finalCacheIdeas.find { it.id == "idea2" }?.text).isEqualTo("Red Flowers")
        assertThat(finalSettingsIdeas).isEqualTo(finalCacheIdeas)
    }

    @Test
    fun concurrentAddAndRemove_areSerializedByMutex_withoutLostUpdates() = runTest(createTransactionElement()) {
        // Arrange
        val idea1 = GiftIdea(id = "idea1", text = "Book")
        val idea2 = GiftIdea(id = "idea2", text = "Mug")
        val idea3 = GiftIdea(id = "idea3", text = "Headphones")
        val initialJson = converters.fromGiftIdeaList(listOf(idea1, idea2))

        var storedContact = ContactEntity(
            contactId = "c1",
            lookupKey = "key1",
            fullName = "John Doe",
            giftIdeasJson = initialJson
        )
        var storedUserData: ContactUserData? = ContactUserData(
            lookupKey = "key1",
            giftIdeasJson = initialJson
        )
        val writeCount = AtomicInteger(0)
        val firstWriteEntered = CompletableDeferred<Unit>()
        val allowFirstWriteToFinish = CompletableDeferred<Unit>()

        whenever(contactDao.getContactByLookupKey("key1")).doSuspendableAnswer { storedContact }
        whenever(contactUserDataDao.getUserDataForContact("key1")).doSuspendableAnswer { storedUserData }
        whenever(contactUserDataDao.upsertUserData(any())).doSuspendableAnswer { invocation ->
            val data = invocation.getArgument<ContactUserData>(0)
            if (writeCount.incrementAndGet() == 1) {
                firstWriteEntered.complete(Unit)
                allowFirstWriteToFinish.await()
            }
            storedUserData = data
        }
        whenever(contactDao.upsertContact(any())).doSuspendableAnswer { invocation ->
            storedContact = invocation.getArgument(0)
        }

        // Act
        val removeJob = launch { repository.removeGiftIdea("key1", "idea1") }
        firstWriteEntered.await()

        val addJob = launch { repository.addGiftIdea("key1", idea3) }
        allowFirstWriteToFinish.complete(Unit)

        removeJob.join()
        addJob.join()

        // Assert: idea1 removed AND idea3 preserved
        val finalIdeas = converters.toGiftIdeaList(storedContact.giftIdeasJson)
        assertThat(finalIdeas.map { it.id }).containsExactly("idea2", "idea3")
        assertThat(converters.toGiftIdeaList(storedUserData!!.giftIdeasJson)).isEqualTo(finalIdeas)
    }
}
