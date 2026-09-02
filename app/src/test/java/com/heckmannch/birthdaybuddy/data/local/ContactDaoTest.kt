package com.heckmannch.birthdaybuddy.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContactDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ContactDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.contactDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun refreshContacts_emptyDatabase_insertsAllContacts() = runTest {
        // Arrange
        val contacts = listOf(
            ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice"),
            ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
        )

        // Act
        dao.refreshContacts(contacts)

        // Assert
        val stored = dao.getAllContactsImmediate()
        assertThat(stored).hasSize(2)
        assertThat(dao.getAllLookupKeys()).containsExactly("k1", "k2")
    }

    @Test
    fun refreshContacts_emptyList_deletesAllContacts() = runTest {
        // Arrange
        dao.upsertContacts(
            listOf(
                ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice"),
                ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob")
            )
        )

        // Act
        dao.refreshContacts(emptyList())

        // Assert
        assertThat(dao.getAllContactsImmediate()).isEmpty()
        assertThat(dao.getAllLookupKeys()).isEmpty()
    }

    @Test
    fun refreshContacts_updatesExistingAndDeletesStaleContacts() = runTest {
        // Arrange
        dao.upsertContacts(
            listOf(
                ContactEntity(contactId = "c1", lookupKey = "k1", fullName = "Alice", birthday = LocalDate.of(1990, 1, 1)),
                ContactEntity(contactId = "c2", lookupKey = "k2", fullName = "Bob"),
                ContactEntity(contactId = "c3", lookupKey = "k3", fullName = "Charlie")
            )
        )

        val initialK1 = dao.getContactByLookupKey("k1")!!
        val initialK3 = dao.getContactByLookupKey("k3")!!

        // New list: k1 updated, k2 removed, k3 kept, k4 added (preserving localIds like repository does)
        val newContacts = listOf(
            ContactEntity(localId = initialK1.localId, contactId = "c1", lookupKey = "k1", fullName = "Alice Updated", birthday = LocalDate.of(1990, 1, 2)),
            ContactEntity(localId = initialK3.localId, contactId = "c3", lookupKey = "k3", fullName = "Charlie"),
            ContactEntity(localId = 0L, contactId = "c4", lookupKey = "k4", fullName = "David")
        )

        // Act
        dao.refreshContacts(newContacts)

        // Assert
        val stored = dao.getAllContactsImmediate().associateBy { it.lookupKey }
        assertThat(stored.keys).containsExactly("k1", "k3", "k4")
        assertThat(stored["k1"]?.fullName).isEqualTo("Alice Updated")
        assertThat(stored["k1"]?.birthday).isEqualTo(LocalDate.of(1990, 1, 2))
        assertThat(stored["k2"]).isNull()
    }

    @Test
    fun refreshContacts_withLargeDataset_chunksDeletionsWithoutExceedingSqliteVariableLimits() = runTest {
        // Arrange: Insert 1200 contacts into DB
        val initialContacts = (1..1200).map { i ->
            ContactEntity(
                contactId = "c$i",
                lookupKey = "k$i",
                fullName = "Person $i"
            )
        }
        dao.upsertContacts(initialContacts)
        assertThat(dao.getAllLookupKeys()).hasSize(1200)

        // New sync has only 200 contacts (1000 contacts need to be deleted, exceeding 999 parameter limit)
        val survivingContacts = (1..200).map { i ->
            ContactEntity(
                localId = i.toLong(),
                contactId = "c$i",
                lookupKey = "k$i",
                fullName = "Person $i Updated"
            )
        }

        // Act
        dao.refreshContacts(survivingContacts)

        // Assert
        val stored = dao.getAllContactsImmediate()
        assertThat(stored).hasSize(200)
        assertThat(dao.getAllLookupKeys()).containsExactlyElementsIn((1..200).map { "k$it" })
    }
}
