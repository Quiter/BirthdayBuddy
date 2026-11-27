package com.heckmannch.birthdaybuddy.data.repository

import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.SettingsDatabase
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GiftIdeaBackupManagerTest {

    private val contactDao: ContactDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private val settingsDatabase: SettingsDatabase = mock()
    private lateinit var manager: GiftIdeaBackupManager

    @Before
    fun setup() {
        mockkStatic("androidx.room.RoomDatabaseKt")
        mockkStatic(android.util.Log::class)
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.w(any(), any<String>(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        coEvery { settingsDatabase.withTransaction<Any?>(any()) } coAnswers {
            val block = secondArg<suspend () -> Any?>()
            block()
        }
        runBlocking {
            whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(emptyList())
        }

        manager = GiftIdeaBackupManager(
            contactDao = contactDao,
            contactUserDataDao = contactUserDataDao,
            settingsDatabase = settingsDatabase,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `exportGiftIdeas should produce correct JSON`() = runTest {
        // Given
        val giftIdeas = listOf(GiftIdea(text = "Book"))
        val userData = listOf(ContactUserData(lookupKey = "key1", giftIdeasJson = com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters().fromGiftIdeaList(giftIdeas)))
        val contacts =
            listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))

        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(userData)
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val json = manager.exportGiftIdeas()

        // Then
        val root = JSONArray(json)
        assertThat(root.length()).isEqualTo(1)
        val obj = root.getJSONObject(0)
        assertThat(obj.getString("lookupKey")).isEqualTo("key1")
        assertThat(obj.getString("fullName")).isEqualTo("John Doe")
        val ideasArray = obj.getJSONArray("giftIdeas")
        assertThat(ideasArray.length()).isEqualTo(1)
        assertThat(ideasArray.getJSONObject(0).getString("text")).isEqualTo("Book")
    }

    @Test
    fun `importGiftIdeas should delegate to repository and match by lookupKey`() = runTest {
        // Given
        val json = "[{\"lookupKey\": \"key1\", \"fullName\": \"John Doe\", \"giftIdeas\": [{\"text\": \"Book\"}]}]"
        val contacts =
            listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserDataList(any())
    }

    @Test
    fun `importGiftIdeas should support legacy string giftIdeas format`() = runTest {
        // Given
        val json = "[{\"lookupKey\": \"key1\", \"fullName\": \"John Doe\", \"giftIdeas\": \"0|Legacy Book\"}]"
        val contacts =
            listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(1)
            val ideas = com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters().toGiftIdeaList(list[0].giftIdeasJson)
            assertThat(ideas).hasSize(1)
            assertThat(ideas[0].text).isEqualTo("Legacy Book")
        })
    }

    @Test
    fun `importGiftIdeas should match by name as fallback`() = runTest {
        // Given
        val json =
            "[{\"lookupKey\": \"wrong_key\", \"fullName\": \"John Doe\", \"giftIdeas\": [{\"text\": \"Book\"}]}]"
        val contacts =
            listOf(ContactEntity(contactId = "1", lookupKey = "correct_key", fullName = "John Doe"))
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(1)
            assertThat(list[0].lookupKey).isEqualTo("correct_key")
        })
    }

    @Test
    fun `importGiftIdeas should preserve existing spouseLookupKey`() = runTest {
        // Given
        val json = "[{\"lookupKey\": \"key1\", \"fullName\": \"John Doe\", \"giftIdeas\": [{\"text\": \"Book\"}]}]"
        val contacts =
            listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))
        val existingUserData = ContactUserData(
            lookupKey = "key1",
            giftIdeasJson = "[]",
            spouseLookupKey = "spouse_key"
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)
        whenever(contactUserDataDao.getAllUserDataImmediate()).thenReturn(listOf(existingUserData))

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(1)
            assertThat(list[0].lookupKey).isEqualTo("key1")
            assertThat(list[0].spouseLookupKey).isEqualTo("spouse_key")
        })
    }

    @Test
    fun `importGiftIdeas should return negative on failure`() = runTest {
        // Given
        val json = "invalid json"

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(-1)
    }

    @Test
    fun `importGiftIdeas should skip entry with JsonObject giftIdeas and import valid entries`() = runTest {
        // Given
        val json = """
            [
                {"lookupKey": "key1", "fullName": "John Doe", "giftIdeas": {"unexpected": "object"}},
                {"lookupKey": "key2", "fullName": "Jane Doe", "giftIdeas": [{"text": "Book"}]}
            ]
        """.trimIndent()
        val contacts = listOf(
            ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"),
            ContactEntity(contactId = "2", lookupKey = "key2", fullName = "Jane Doe"),
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        io.mockk.verify(atLeast = 1) { android.util.Log.w(any(), any<String>()) }
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(1)
            assertThat(list[0].lookupKey).isEqualTo("key2")
        })
    }

    @Test
    fun `importGiftIdeas should skip entry with non-primitive lookupKey or fullName and import valid entries`() = runTest {
        // Given
        val json = """
            [
                {"lookupKey": {"bad": 1}, "fullName": ["bad"], "giftIdeas": [{"text": "Ignored"}]},
                {"lookupKey": "key1", "fullName": "John Doe", "giftIdeas": [{"text": "Book"}]}
            ]
        """.trimIndent()
        val contacts = listOf(
            ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"),
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(1)
            assertThat(list[0].lookupKey).isEqualTo("key1")
        })
    }

    @Test
    fun `importGiftIdeas should not match blank lookupKey or blank fullName`() = runTest {
        // Given
        val json = """
            [
                {"lookupKey": "", "fullName": "", "giftIdeas": [{"text": "Book"}]},
                {"lookupKey": "   ", "fullName": "   ", "giftIdeas": [{"text": "Flowers"}]},
                {"lookupKey": "unknown_key", "fullName": "", "giftIdeas": [{"text": "Mug"}]}
            ]
        """.trimIndent()
        val contacts = listOf(
            ContactEntity(contactId = "1", lookupKey = "", fullName = ""),
            ContactEntity(contactId = "2", lookupKey = "key_blank_name", fullName = ""),
            ContactEntity(contactId = "3", lookupKey = "key_ws_name", fullName = "   "),
            ContactEntity(contactId = "4", lookupKey = "key1", fullName = "John Doe"),
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(0)
        verify(contactUserDataDao, never()).upsertUserDataList(any())
    }

    @Test
    fun `importGiftIdeas should fallback to fullName when lookupKey is blank`() = runTest {
        // Given
        val json = """
            [
                {"lookupKey": "   ", "fullName": "John Doe", "giftIdeas": [{"text": "Book"}]}
            ]
        """.trimIndent()
        val contacts = listOf(
            ContactEntity(contactId = "1", lookupKey = "   ", fullName = "Other"),
            ContactEntity(contactId = "2", lookupKey = "key1", fullName = "John Doe"),
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(1)
            assertThat(list[0].lookupKey).isEqualTo("key1")
        })
    }

    @Test
    fun `importGiftIdeas should deduplicate multiple entries for same targetLookupKey`() = runTest {
        // Given
        val json = """
            [
                {"lookupKey": "key1", "fullName": "John Doe", "giftIdeas": [{"text": "First Idea"}]},
                {"lookupKey": "wrong_key", "fullName": "John Doe", "giftIdeas": [{"text": "Second Idea"}]},
                {"lookupKey": "key2", "fullName": "Jane Doe", "giftIdeas": [{"text": "Flowers"}]}
            ]
        """.trimIndent()
        val contacts = listOf(
            ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"),
            ContactEntity(contactId = "2", lookupKey = "key2", fullName = "Jane Doe"),
        )
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(2)
        verify(contactUserDataDao).upsertUserDataList(check { list ->
            assertThat(list).hasSize(2)
            assertThat(list.map { it.lookupKey }).containsExactly("key1", "key2").inOrder()
            val converters = com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters()
            val johnIdeas = converters.toGiftIdeaList(list[0].giftIdeasJson)
            assertThat(johnIdeas).hasSize(1)
            assertThat(johnIdeas[0].text).isEqualTo("Second Idea")
            val janeIdeas = converters.toGiftIdeaList(list[1].giftIdeasJson)
            assertThat(janeIdeas).hasSize(1)
            assertThat(janeIdeas[0].text).isEqualTo("Flowers")
        })
    }
}
