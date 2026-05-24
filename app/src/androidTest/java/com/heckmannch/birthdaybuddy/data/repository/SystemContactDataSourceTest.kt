package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SystemContactDataSourceTest {

    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private lateinit var dataSource: SystemContactDataSource

    @Before
    fun setUp() {
        whenever(context.contentResolver).doReturn(contentResolver)
        dataSource = SystemContactDataSource(context)
    }

    @Test
    fun updateContactBirthday_whenBirthdayExists_performsUpdate() = runTest {
        val contactId = "123"
        val birthday = LocalDate.of(1990, 5, 24)

        // Mock query to find existing birthday
        val cursor: Cursor = mock {
            on { moveToFirst() } doReturn true
            on { getLong(0) } doReturn 456L // existingDataId
        }

        whenever(
            contentResolver.query(
                eq(ContactsContract.Data.CONTENT_URI),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).doReturn(cursor)

        // Mock applyBatch to succeed
        whenever(contentResolver.applyBatch(eq(ContactsContract.AUTHORITY), any())).doReturn(emptyArray())

        val result = dataSource.updateContactBirthday(contactId, birthday)

        assertThat(result).isTrue()

        // Verify we checked for existing birthday
        verify(contentResolver).query(
            eq(ContactsContract.Data.CONTENT_URI),
            argThat { contentEquals(arrayOf(ContactsContract.Data._ID)) },
            anyOrNull(),
            argThat { contentEquals(arrayOf(contactId, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString())) },
            anyOrNull()
        )

        // Verify applyBatch was called with an update operation
        verify(contentResolver).applyBatch(
            eq(ContactsContract.AUTHORITY),
            argThat {
                size == 1
            }
        )
    }

    @Test
    fun updateContactBirthday_whenNoBirthdayExists_performsInsertWithWriteableRawContact() = runTest {
        val contactId = "123"
        val birthday = LocalDate.of(1990, 5, 24)

        // Mock query to find existing birthday returns empty
        val cursorEmpty: Cursor = mock {
            on { moveToFirst() } doReturn false
        }

        // Mock query for raw contacts: returns WhatsApp (read-only) and Google (writeable)
        val cursorRawContacts: Cursor = mock {
            var counter = 0
            on { moveToNext() } doAnswer {
                counter++ < 2
            }
            on { getColumnIndex(ContactsContract.RawContacts._ID) } doReturn 0
            on { getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE) } doReturn 1
            on { getLong(0) } doAnswer {
                if (counter == 1) 111L else 222L // rawContactId
            }
            on { getString(1) } doAnswer {
                if (counter == 1) "com.whatsapp" else "com.google" // accountType
            }
        }

        // Return empty cursor when checking existing birthday (first query)
        whenever(
            contentResolver.query(
                eq(ContactsContract.Data.CONTENT_URI),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).doReturn(cursorEmpty)

        // Return raw contacts when querying raw contacts (second query)
        whenever(
            contentResolver.query(
                eq(ContactsContract.RawContacts.CONTENT_URI),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).doReturn(cursorRawContacts)

        // Mock applyBatch to succeed
        whenever(contentResolver.applyBatch(eq(ContactsContract.AUTHORITY), any())).doReturn(emptyArray())

        val result = dataSource.updateContactBirthday(contactId, birthday)

        assertThat(result).isTrue()

        // Verify we queried for raw contacts
        verify(contentResolver).query(
            eq(ContactsContract.RawContacts.CONTENT_URI),
            argThat { contentEquals(arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_TYPE, ContactsContract.RawContacts.DELETED)) },
            anyOrNull(),
            argThat { contentEquals(arrayOf(contactId)) },
            anyOrNull()
        )

        // Verify applyBatch was called with an insert operation
        verify(contentResolver).applyBatch(
            eq(ContactsContract.AUTHORITY),
            argThat {
                size == 1
            }
        )
    }
}
