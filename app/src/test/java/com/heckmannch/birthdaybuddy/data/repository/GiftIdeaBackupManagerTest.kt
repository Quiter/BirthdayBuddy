package com.heckmannch.birthdaybuddy.data.repository

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.ContactUserData
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GiftIdeaBackupManagerTest {

    private val contactDao: ContactDao = mock()
    private val contactUserDataDao: ContactUserDataDao = mock()
    private lateinit var manager: GiftIdeaBackupManager

    @Before
    fun setup() {
        manager = GiftIdeaBackupManager(contactDao, contactUserDataDao)
    }

    @Test
    fun `exportGiftIdeas should produce correct JSON`() = runTest {
        // Given
        val giftIdeas = listOf(GiftIdea(text = "Book"))
        val userData = listOf(ContactUserData(lookupKey = "key1", giftIdeas = giftIdeas))
        val contacts = listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))

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
        assertThat(obj.getString("giftIdeas")).contains("Book")
    }

    @Test
    fun `importGiftIdeas should delegate to repository and match by lookupKey`() = runTest {
        // Given
        val json = "[{\"lookupKey\": \"key1\", \"fullName\": \"John Doe\", \"giftIdeas\": \"[]\"}]"
        val contacts = listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserData(any())
    }

    @Test
    fun `importGiftIdeas should match by name as fallback`() = runTest {
        // Given
        val json =
            "[{\"lookupKey\": \"wrong_key\", \"fullName\": \"John Doe\", \"giftIdeas\": \"[]\"}]"
        val contacts =
            listOf(ContactEntity(contactId = "1", lookupKey = "correct_key", fullName = "John Doe"))
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("correct_key")
        })
    }

    @Test
    fun `importGiftIdeas should preserve existing spouseLookupKey`() = runTest {
        // Given
        val json = "[{\"lookupKey\": \"key1\", \"fullName\": \"John Doe\", \"giftIdeas\": \"[]\"}]"
        val contacts = listOf(ContactEntity(contactId = "1", lookupKey = "key1", fullName = "John Doe"))
        val existingUserData = ContactUserData(lookupKey = "key1", giftIdeas = emptyList(), spouseLookupKey = "spouse_key")
        whenever(contactDao.getAllContactsImmediate()).thenReturn(contacts)
        whenever(contactUserDataDao.getUserDataForContact("key1")).thenReturn(existingUserData)

        // When
        val count = manager.importGiftIdeas(json)

        // Then
        assertThat(count).isEqualTo(1)
        verify(contactUserDataDao).upsertUserData(org.mockito.kotlin.check {
            assertThat(it.lookupKey).isEqualTo("key1")
            assertThat(it.spouseLookupKey).isEqualTo("spouse_key")
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
}
