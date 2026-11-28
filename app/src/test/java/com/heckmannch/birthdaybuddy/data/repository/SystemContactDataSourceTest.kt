package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.Context
import android.database.MatrixCursor
import android.provider.ContactsContract
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.hasYear
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SystemContactDataSourceTest {

    private val contentResolver: ContentResolver = mockk(relaxed = true)
    private lateinit var realContext: Context
    private lateinit var context: Context
    private lateinit var dataSource: SystemContactDataSource
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        realContext = ApplicationProvider.getApplicationContext()
        context = mockk(relaxed = true)
        every { context.contentResolver } returns contentResolver
        every { context.getString(R.string.contact_unknown) } answers {
            realContext.getString(R.string.contact_unknown)
        }

        dataSource = SystemContactDataSource(context, UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        unmockkStatic(Log::class)
    }

    @Test
    fun `parseDate parses leap day without year to NO_YEAR_MARKER`() {
        val result = dataSource.parseDate("--02-29")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 2, 29))
        assertThat(result!!.hasYear).isFalse()
    }

    @Test
    fun `parseDate parses compact leap day without year to NO_YEAR_MARKER`() {
        val result = dataSource.parseDate("--0229")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 2, 29))
        assertThat(result!!.hasYear).isFalse()
    }

    @Test
    fun `parseDate parses standard date without year to NO_YEAR_MARKER`() {
        val result = dataSource.parseDate("--05-15")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 5, 15))
        assertThat(result!!.hasYear).isFalse()
    }

    @Test
    fun `parseDate parses ISO date with leap year correctly`() {
        val result = dataSource.parseDate("2024-02-29")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(result!!.hasYear).isTrue()
    }

    @Test
    fun `parseDate parses standard ISO date correctly`() {
        val result = dataSource.parseDate("1990-05-15")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(1990, 5, 15))
        assertThat(result!!.hasYear).isTrue()
    }

    @Test
    fun `parseDate parses yyyyMMdd date format correctly`() {
        val result = dataSource.parseDate("19900515")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(1990, 5, 15))
        assertThat(result!!.hasYear).isTrue()
    }

    @Test
    fun `parseDate parses ISO date with timestamp correctly`() {
        val resultUtc = dataSource.parseDate("1990-05-15T00:00:00.000Z")
        assertThat(resultUtc).isNotNull()
        assertThat(resultUtc).isEqualTo(LocalDate.of(1990, 5, 15))
        assertThat(resultUtc!!.hasYear).isTrue()

        val resultSpace = dataSource.parseDate("1990-05-15 12:30:00")
        assertThat(resultSpace).isNotNull()
        assertThat(resultSpace).isEqualTo(LocalDate.of(1990, 5, 15))

        val resultOffset = dataSource.parseDate("1990-05-15T15:30:00+02:00")
        assertThat(resultOffset).isNotNull()
        assertThat(resultOffset).isEqualTo(LocalDate.of(1990, 5, 15))
    }

    @Test
    fun `parseDate returns null for invalid date strings`() {
        assertThat(dataSource.parseDate(null)).isNull()
        assertThat(dataSource.parseDate("")).isNull()
        assertThat(dataSource.parseDate("invalid-date")).isNull()
        assertThat(dataSource.parseDate("--02-30")).isNull()
        assertThat(dataSource.parseDate("--13-01")).isNull()
    }

    @Test
    fun `updateContactBirthday formats no-year date with ASCII digits under Arabic locale`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("ar-EG-u-nu-arab"))

        val existingCursor = MatrixCursor(arrayOf(ContactsContract.Data._ID)).apply {
            addRow(arrayOf(99L))
        }
        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns existingCursor

        val opsSlot = slot<ArrayList<ContentProviderOperation>>()
        every {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, capture(opsSlot))
        } returns emptyArray()

        val result = dataSource.updateContactBirthday("1", LocalDate.of(NO_YEAR_MARKER, 5, 9))

        assertThat(result).isTrue()
        assertThat(opsSlot.captured).hasSize(1)
        val resolvedValues = opsSlot.captured[0].resolveValueBackReferences(emptyArray(), 0)
        assertThat(resolvedValues?.getAsString(ContactsContract.CommonDataKinds.Event.START_DATE))
            .isEqualTo("--05-09")
    }

    @Test
    fun `fetchContactsFromSystem handles null display name and null cursor columns without NPE`() = runTest {
        val contactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.STARRED
            )
        ).apply {
            addRow(arrayOf<Any?>("1", "key-1", null, null, 0))
            addRow(arrayOf<Any?>(null, "ignored", "ShouldSkip", null, 0))
        }

        val eventsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.TYPE,
                ContactsContract.CommonDataKinds.Event.LABEL
            )
        ).apply {
            addRow(
                arrayOf<Any?>(
                    "1",
                    "1990-05-15",
                    ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY,
                    null
                )
            )
        }

        val dataCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
            )
        ).apply {
            // Null contact ID and null mimetype rows must be safely skipped
            addRow(arrayOf<Any?>(null, "vnd.android.cursor.item/vnd.com.whatsapp.profile", 10L))
            addRow(arrayOf<Any?>("1", null, 10L))
            addRow(arrayOf<Any?>("1", "vnd.android.cursor.item/vnd.com.whatsapp.profile", 10L))
        }

        val phoneCursor = MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
            )
        ).apply {
            addRow(arrayOf<Any?>(null, "+49123456", 1))
            addRow(arrayOf<Any?>("1", null, 1))
            addRow(arrayOf<Any?>("1", "+491701234567", 0))
        }

        every {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns contactsCursor

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                match { it.contains(ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE) || it == "${ContactsContract.Data.MIMETYPE} = ?" },
                match { it.isNotEmpty() && it[0] == ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE },
                any()
            )
        } returns eventsCursor

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                match { it.contains("IN (?, ?)") },
                any(),
                any()
            )
        } returns dataCursor

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                match { it.contains("${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} IN") },
                any(),
                any()
            )
        } returns MatrixCursor(
            arrayOf(
                ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID,
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
            )
        ).apply {
            addRow(arrayOf<Any?>(null, 10L))
            addRow(arrayOf<Any?>("1", 10L))
        }

        every {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns phoneCursor

        val groups = mapOf(10L to GroupInfo("Friends", isSystem = false))
        val contacts = dataSource.fetchContactsFromSystem(groups)

        assertThat(contacts).hasSize(1)
        val contact = contacts.first()
        assertThat(contact.contactId).isEqualTo("1")
        assertThat(contact.lookupKey).isEqualTo("key-1")
        assertThat(contact.fullName).isEqualTo(realContext.getString(R.string.contact_unknown))
        assertThat(contact.birthday).isEqualTo(LocalDate.of(1990, 5, 15))
        assertThat(contact.phoneNumber).isEqualTo("+491701234567")
        assertThat(contact.hasWhatsApp).isTrue()
        assertThat(contact.hasSignal).isFalse()
        assertThat(contact.labels).containsExactly("Friends")
    }

    @Test
    fun `fetchContactsFromSystem skips contacts with null, empty, or blank lookupKey and logs warning`() = runTest {
        val contactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.STARRED
            )
        ).apply {
            addRow(arrayOf<Any?>("1", null, "NullKey", null, 0))
            addRow(arrayOf<Any?>("2", "", "EmptyKey", null, 0))
            addRow(arrayOf<Any?>("3", "   ", "BlankKey", null, 0))
            addRow(arrayOf<Any?>("4", "valid-key", "ValidContact", null, 1))
        }

        every {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns contactsCursor

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns null

        every {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns null

        val contacts = dataSource.fetchContactsFromSystem(emptyMap())

        assertThat(contacts).hasSize(1)
        assertThat(contacts.single().contactId).isEqualTo("4")
        assertThat(contacts.single().lookupKey).isEqualTo("valid-key")
        assertThat(contacts.single().fullName).isEqualTo("ValidContact")
        verify(exactly = 3) { Log.w(any(), any<String>()) }
    }

    @Test
    fun `fetchContactsFromSystem skips all contacts and logs warning when LOOKUP_KEY column is missing`() = runTest {
        val contactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
            )
        ).apply {
            addRow(arrayOf<Any?>("1", "Alice"))
        }

        every {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns contactsCursor

        val contacts = dataSource.fetchContactsFromSystem(emptyMap())

        assertThat(contacts).isEmpty()
        verify(exactly = 1) { Log.w(any(), any<String>()) }
    }

    @Test
    fun `fetchContactsFromSystem matches uppercase ANNIVERSARY label under Turkish locale`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        val contactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                ContactsContract.Contacts.STARRED
            )
        ).apply {
            addRow(arrayOf<Any?>("1", "key-1", "Alice", null, 0))
        }

        val eventsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Event.START_DATE,
                ContactsContract.CommonDataKinds.Event.TYPE,
                ContactsContract.CommonDataKinds.Event.LABEL
            )
        ).apply {
            addRow(
                arrayOf<Any?>(
                    "1",
                    "2018-08-20",
                    ContactsContract.CommonDataKinds.Event.TYPE_CUSTOM,
                    "ANNIVERSARY"
                )
            )
        }

        every {
            contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns contactsCursor

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                match { it == "${ContactsContract.Data.MIMETYPE} = ?" },
                any(),
                any()
            )
        } returns eventsCursor

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                match { it != "${ContactsContract.Data.MIMETYPE} = ?" },
                any(),
                any()
            )
        } returns null

        every {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns null

        val contacts = dataSource.fetchContactsFromSystem(emptyMap())

        assertThat(contacts).hasSize(1)
        val contact = contacts.single()
        assertThat(contact.anniversary).isEqualTo(LocalDate.of(2018, 8, 20))
        assertThat(contact.customEvents).isEmpty()
    }

    @Test
    fun `fetchContactGroups marks system groups independently of Turkish locale`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        val groupsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.Groups._ID,
                ContactsContract.Groups.TITLE
            )
        ).apply {
            addRow(arrayOf<Any?>(1L, "MY CONTACTS"))
            addRow(arrayOf<Any?>(2L, "All Contacts"))
            addRow(arrayOf<Any?>(3L, "Friends"))
        }

        every {
            contentResolver.query(
                ContactsContract.Groups.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns groupsCursor

        val groups = dataSource.fetchContactGroups()

        assertThat(groups[1L]).isEqualTo(GroupInfo("MY CONTACTS", isSystem = true))
        assertThat(groups[2L]).isEqualTo(GroupInfo("All Contacts", isSystem = true))
        assertThat(groups[3L]).isEqualTo(GroupInfo("Friends", isSystem = false))
    }

    @Test
    fun `updateContactBirthday skips uppercase read-only Viber account under Turkish locale`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("tr-TR"))

        val emptyExistingBirthdayCursor = MatrixCursor(arrayOf(ContactsContract.Data._ID))
        val rawContactsCursor = MatrixCursor(
            arrayOf(
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.DELETED
            )
        ).apply {
            addRow(arrayOf<Any?>(10L, "COM.VIBER.VOIP", 0))
            addRow(arrayOf<Any?>(20L, "com.google", 0))
        }

        every {
            contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns emptyExistingBirthdayCursor

        every {
            contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                any(),
                any(),
                any(),
                any()
            )
        } returns rawContactsCursor

        val opsSlot = slot<ArrayList<ContentProviderOperation>>()
        every {
            contentResolver.applyBatch(ContactsContract.AUTHORITY, capture(opsSlot))
        } returns emptyArray()

        val result = dataSource.updateContactBirthday("1", LocalDate.of(1990, 5, 15))

        assertThat(result).isTrue()
        assertThat(opsSlot.captured).hasSize(1)
        val resolvedValues = opsSlot.captured[0].resolveValueBackReferences(emptyArray(), 0)
        assertThat(resolvedValues?.getAsLong(ContactsContract.Data.RAW_CONTACT_ID)).isEqualTo(20L)
    }
}
