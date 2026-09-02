package com.heckmannch.birthdaybuddy.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.EventType
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NotificationTextFormatterTest {

    private lateinit var context: Context
    private lateinit var formatter: NotificationTextFormatter
    private val fixedToday = LocalDate.of(2026, 6, 15)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        formatter = NotificationTextFormatter(context)
    }

    // =========================================================================
    // Couple Anniversary Detection Tests
    // =========================================================================

    @Test
    fun isCoupleAnniversary_mutuallyLinkedSpousesWithAnniversaryType_returnsTrue() {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "key2"
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            spouseLookupKey = "key1"
        )

        val result = formatter.isCoupleAnniversary(
            listOf(contact1, contact2),
            EventType.ANNIVERSARY
        )

        assertThat(result).isTrue()
    }

    @Test
    fun isCoupleAnniversary_birthdayEventType_returnsFalse() {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "key2"
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            spouseLookupKey = "key1"
        )

        val result = formatter.isCoupleAnniversary(
            listOf(contact1, contact2),
            EventType.BIRTHDAY
        )

        assertThat(result).isFalse()
    }

    @Test
    fun isCoupleAnniversary_singleContact_returnsFalse() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "key2"
        )

        val result = formatter.isCoupleAnniversary(
            listOf(contact),
            EventType.ANNIVERSARY
        )

        assertThat(result).isFalse()
    }

    @Test
    fun isCoupleAnniversary_unmatchedSpouseKeys_returnsFalse() {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "other_key"
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            spouseLookupKey = "key1"
        )

        val result = formatter.isCoupleAnniversary(
            listOf(contact1, contact2),
            EventType.ANNIVERSARY
        )

        assertThat(result).isFalse()
    }

    // =========================================================================
    // Birthday Notification Title Tests
    // =========================================================================

    @Test
    fun buildTitle_birthday_singleContactWithYear_daysBefore0() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1996, 6, 15) // Turns 30
        )

        val title = formatter.buildTitle(
            contacts = listOf(contact),
            daysBefore = 0,
            eventType = EventType.BIRTHDAY,
            today = fixedToday
        )

        val expected = context.resources.getQuantityString(
            R.plurals.notif_title_today_age,
            30,
            "Max Mustermann",
            30
        )
        assertThat(title).isEqualTo(expected)
    }

    @Test
    fun buildTitle_birthday_singleContactWithYear_daysBefore1() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1996, 6, 16) // Turns 30 tomorrow
        )

        val title = formatter.buildTitle(
            contacts = listOf(contact),
            daysBefore = 1,
            eventType = EventType.BIRTHDAY,
            today = fixedToday
        )

        val expected = context.resources.getQuantityString(
            R.plurals.notif_title_tomorrow_age,
            30,
            "Max Mustermann",
            30
        )
        assertThat(title).isEqualTo(expected)
    }

    @Test
    fun buildTitle_birthday_singleContactWithYear_daysBefore7() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1996, 6, 22) // Turns 30 in a week
        )

        val title = formatter.buildTitle(
            contacts = listOf(contact),
            daysBefore = 7,
            eventType = EventType.BIRTHDAY,
            today = fixedToday
        )

        val expected = context.getString(
            R.string.notif_title_week_age,
            "Max Mustermann",
            30
        )
        assertThat(title).isEqualTo(expected)
    }

    @Test
    fun buildTitle_birthday_singleContactWithYear_daysBeforeArbitrary() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1996, 6, 18) // Turns 30 in 3 days
        )

        val title = formatter.buildTitle(
            contacts = listOf(contact),
            daysBefore = 3,
            eventType = EventType.BIRTHDAY,
            today = fixedToday
        )

        val expected = context.resources.getQuantityString(
            R.plurals.notif_title_days_age,
            3,
            3,
            "Max Mustermann",
            30
        )
        assertThat(title).isEqualTo(expected)
    }

    @Test
    fun buildTitle_birthday_singleContactWithoutYear_allOffsets() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(NO_YEAR_MARKER, 6, 15)
        )

        val titleToday = formatter.buildTitle(listOf(contact), 0, EventType.BIRTHDAY, fixedToday)
        val titleTomorrow = formatter.buildTitle(listOf(contact), 1, EventType.BIRTHDAY, fixedToday)
        val titleWeek = formatter.buildTitle(listOf(contact), 7, EventType.BIRTHDAY, fixedToday)
        val titleDays = formatter.buildTitle(listOf(contact), 5, EventType.BIRTHDAY, fixedToday)

        assertThat(titleToday).isEqualTo(context.getString(R.string.notif_title_today_named, "Max Mustermann"))
        assertThat(titleTomorrow).isEqualTo(context.getString(R.string.notif_title_tomorrow_named, "Max Mustermann"))
        assertThat(titleWeek).isEqualTo(context.getString(R.string.notif_title_week_named, "Max Mustermann"))
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(R.plurals.notif_title_days_named, 5, 5, "Max Mustermann")
        )
    }

    @Test
    fun buildTitle_birthday_groupContacts_allOffsets() {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "key1", fullName = "Max"),
            Contact(contactId = "2", lookupKey = "key2", fullName = "Erika"),
            Contact(contactId = "3", lookupKey = "key3", fullName = "John")
        )

        val titleToday = formatter.buildTitle(contacts, 0, EventType.BIRTHDAY, fixedToday)
        val titleTomorrow = formatter.buildTitle(contacts, 1, EventType.BIRTHDAY, fixedToday)
        val titleWeek = formatter.buildTitle(contacts, 7, EventType.BIRTHDAY, fixedToday)
        val titleDays = formatter.buildTitle(contacts, 4, EventType.BIRTHDAY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.resources.getQuantityString(R.plurals.notif_title_today_plural, 3, 3)
        )
        assertThat(titleTomorrow).isEqualTo(
            context.resources.getQuantityString(R.plurals.notif_title_tomorrow_plural, 3, 3)
        )
        assertThat(titleWeek).isEqualTo(
            context.resources.getQuantityString(R.plurals.notif_title_week_plural, 3, 3)
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(R.plurals.notif_title_days_plural, 4, 4, 3)
        )
    }

    // =========================================================================
    // Anniversary Notification Title Tests
    // =========================================================================

    @Test
    fun buildTitle_anniversary_singleContactWithYear_allOffsets() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Anna Schmidt",
            anniversary = LocalDate.of(2016, 6, 15) // 10th anniversary
        )

        val titleToday = formatter.buildTitle(listOf(contact), 0, EventType.ANNIVERSARY, fixedToday)
        val titleTomorrow = formatter.buildTitle(listOf(contact), 1, EventType.ANNIVERSARY, fixedToday)
        val titleWeek = formatter.buildTitle(listOf(contact), 7, EventType.ANNIVERSARY, fixedToday)
        val titleDays = formatter.buildTitle(listOf(contact), 2, EventType.ANNIVERSARY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.getString(R.string.notif_title_today_anniversary_age, "Anna Schmidt", 10)
        )
        assertThat(titleTomorrow).isEqualTo(
            context.getString(R.string.notif_title_tomorrow_anniversary_age, "Anna Schmidt", 10)
        )
        assertThat(titleWeek).isEqualTo(
            context.getString(R.string.notif_title_week_anniversary_age, "Anna Schmidt", 10)
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_anniversary_age,
                2,
                2,
                "Anna Schmidt",
                10
            )
        )
    }

    @Test
    fun buildTitle_anniversary_singleContactWithoutYear_allOffsets() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Anna Schmidt",
            anniversary = LocalDate.of(NO_YEAR_MARKER, 6, 15)
        )

        val titleToday = formatter.buildTitle(listOf(contact), 0, EventType.ANNIVERSARY, fixedToday)
        val titleTomorrow = formatter.buildTitle(listOf(contact), 1, EventType.ANNIVERSARY, fixedToday)
        val titleWeek = formatter.buildTitle(listOf(contact), 7, EventType.ANNIVERSARY, fixedToday)
        val titleDays = formatter.buildTitle(listOf(contact), 2, EventType.ANNIVERSARY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.getString(R.string.notif_title_today_anniversary, "Anna Schmidt")
        )
        assertThat(titleTomorrow).isEqualTo(
            context.getString(R.string.notif_title_tomorrow_anniversary, "Anna Schmidt")
        )
        assertThat(titleWeek).isEqualTo(
            context.getString(R.string.notif_title_week_anniversary, "Anna Schmidt")
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_anniversary,
                2,
                2,
                "Anna Schmidt"
            )
        )
    }

    @Test
    fun buildTitle_anniversary_coupleWithYear_mergesNamesAndIncludesAge() {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "key2",
            anniversary = LocalDate.of(2016, 6, 15)
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            spouseLookupKey = "key1",
            anniversary = LocalDate.of(2016, 6, 15)
        )
        val mergedName = "Max & Erika Mustermann"

        val titleToday = formatter.buildTitle(listOf(contact1, contact2), 0, EventType.ANNIVERSARY, fixedToday)
        val titleTomorrow = formatter.buildTitle(listOf(contact1, contact2), 1, EventType.ANNIVERSARY, fixedToday)
        val titleWeek = formatter.buildTitle(listOf(contact1, contact2), 7, EventType.ANNIVERSARY, fixedToday)
        val titleDays = formatter.buildTitle(listOf(contact1, contact2), 4, EventType.ANNIVERSARY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.getString(R.string.notif_title_today_anniversary_age, mergedName, 10)
        )
        assertThat(titleTomorrow).isEqualTo(
            context.getString(R.string.notif_title_tomorrow_anniversary_age, mergedName, 10)
        )
        assertThat(titleWeek).isEqualTo(
            context.getString(R.string.notif_title_week_anniversary_age, mergedName, 10)
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_anniversary_age,
                4,
                4,
                mergedName,
                10
            )
        )
    }

    @Test
    fun buildTitle_anniversary_coupleWithoutYear_usesCoupleStrings() {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "key2",
            anniversary = LocalDate.of(NO_YEAR_MARKER, 6, 15)
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            spouseLookupKey = "key1",
            anniversary = LocalDate.of(NO_YEAR_MARKER, 6, 15)
        )
        val mergedName = "Max & Erika Mustermann"

        val titleToday = formatter.buildTitle(listOf(contact1, contact2), 0, EventType.ANNIVERSARY, fixedToday)
        val titleTomorrow = formatter.buildTitle(listOf(contact1, contact2), 1, EventType.ANNIVERSARY, fixedToday)
        val titleWeek = formatter.buildTitle(listOf(contact1, contact2), 7, EventType.ANNIVERSARY, fixedToday)
        val titleDays = formatter.buildTitle(listOf(contact1, contact2), 3, EventType.ANNIVERSARY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.getString(R.string.notif_title_today_anniversary_couple, mergedName)
        )
        assertThat(titleTomorrow).isEqualTo(
            context.getString(R.string.notif_title_tomorrow_anniversary_couple, mergedName)
        )
        assertThat(titleWeek).isEqualTo(
            context.getString(R.string.notif_title_week_anniversary_couple, mergedName)
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_anniversary_couple,
                3,
                3,
                mergedName
            )
        )
    }

    @Test
    fun buildTitle_anniversary_groupContacts_allOffsets() {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "key1", fullName = "Alice"),
            Contact(contactId = "2", lookupKey = "key2", fullName = "Bob"),
            Contact(contactId = "3", lookupKey = "key3", fullName = "Charlie")
        )

        val titleToday = formatter.buildTitle(contacts, 0, EventType.ANNIVERSARY, fixedToday)
        val titleTomorrow = formatter.buildTitle(contacts, 1, EventType.ANNIVERSARY, fixedToday)
        val titleWeek = formatter.buildTitle(contacts, 7, EventType.ANNIVERSARY, fixedToday)
        val titleDays = formatter.buildTitle(contacts, 6, EventType.ANNIVERSARY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_today_anniversary_plural,
                3,
                3
            )
        )
        assertThat(titleTomorrow).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_tomorrow_anniversary_plural,
                3,
                3
            )
        )
        assertThat(titleWeek).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_week_anniversary_plural,
                3,
                3
            )
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_anniversary_plural,
                6,
                6,
                3
            )
        )
    }

    // =========================================================================
    // Name Day Notification Title Tests
    // =========================================================================

    @Test
    fun buildTitle_nameDay_singleContact_allOffsets() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Johannes"
        )

        val titleToday = formatter.buildTitle(listOf(contact), 0, EventType.NAME_DAY, fixedToday)
        val titleTomorrow = formatter.buildTitle(listOf(contact), 1, EventType.NAME_DAY, fixedToday)
        val titleWeek = formatter.buildTitle(listOf(contact), 7, EventType.NAME_DAY, fixedToday)
        val titleDays = formatter.buildTitle(listOf(contact), 4, EventType.NAME_DAY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.getString(R.string.notif_title_today_nameday, "Johannes")
        )
        assertThat(titleTomorrow).isEqualTo(
            context.getString(R.string.notif_title_tomorrow_nameday, "Johannes")
        )
        assertThat(titleWeek).isEqualTo(
            context.getString(R.string.notif_title_week_nameday, "Johannes")
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_nameday,
                4,
                4,
                "Johannes"
            )
        )
    }

    @Test
    fun buildTitle_nameDay_groupContacts_allOffsets() {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "key1", fullName = "Johannes"),
            Contact(contactId = "2", lookupKey = "key2", fullName = "Maria")
        )

        val titleToday = formatter.buildTitle(contacts, 0, EventType.NAME_DAY, fixedToday)
        val titleTomorrow = formatter.buildTitle(contacts, 1, EventType.NAME_DAY, fixedToday)
        val titleWeek = formatter.buildTitle(contacts, 7, EventType.NAME_DAY, fixedToday)
        val titleDays = formatter.buildTitle(contacts, 2, EventType.NAME_DAY, fixedToday)

        assertThat(titleToday).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_today_nameday_plural,
                2,
                2
            )
        )
        assertThat(titleTomorrow).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_tomorrow_nameday_plural,
                2,
                2
            )
        )
        assertThat(titleWeek).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_week_nameday_plural,
                2,
                2
            )
        )
        assertThat(titleDays).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.notif_title_days_nameday_plural,
                2,
                2,
                2
            )
        )
    }

    // =========================================================================
    // Empty Contact List Edge Case
    // =========================================================================

    @Test
    fun buildTitle_emptyContacts_returnsEmptyString() {
        val title = formatter.buildTitle(emptyList(), 0, EventType.BIRTHDAY, fixedToday)
        assertThat(title).isEmpty()
    }

    // =========================================================================
    // Content Text Tests
    // =========================================================================

    @Test
    fun buildContentText_singleBirthdayContact_withoutAndWithHint() {
        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max Mustermann")

        val textWithoutHint = formatter.buildContentText(
            contacts = listOf(contact),
            eventType = EventType.BIRTHDAY,
            showHint = false
        )
        val textWithHint = formatter.buildContentText(
            contacts = listOf(contact),
            eventType = EventType.BIRTHDAY,
            showHint = true
        )

        assertThat(textWithoutHint).isEqualTo(context.getString(R.string.notif_desc_named))
        assertThat(textWithHint).isEqualTo(context.getString(R.string.notif_hint_persistent))
    }

    @Test
    fun buildContentText_singleAnniversaryContact_withoutAndWithHint() {
        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Max Mustermann")

        val textWithoutHint = formatter.buildContentText(
            contacts = listOf(contact),
            eventType = EventType.ANNIVERSARY,
            showHint = false
        )
        val textWithHint = formatter.buildContentText(
            contacts = listOf(contact),
            eventType = EventType.ANNIVERSARY,
            showHint = true
        )

        assertThat(textWithoutHint).isEqualTo(context.getString(R.string.notif_desc_anniversary))
        assertThat(textWithHint).isEqualTo(context.getString(R.string.notif_hint_persistent))
    }

    @Test
    fun buildContentText_singleNameDayContact_withoutAndWithHint() {
        val contact = Contact(contactId = "1", lookupKey = "key1", fullName = "Johannes")

        val textWithoutHint = formatter.buildContentText(
            contacts = listOf(contact),
            eventType = EventType.NAME_DAY,
            showHint = false
        )
        val textWithHint = formatter.buildContentText(
            contacts = listOf(contact),
            eventType = EventType.NAME_DAY,
            showHint = true
        )

        assertThat(textWithoutHint).isEqualTo(context.getString(R.string.notif_desc_nameday))
        assertThat(textWithHint).isEqualTo(context.getString(R.string.notif_hint_persistent))
    }

    @Test
    fun buildContentText_coupleAnniversary_withoutAndWithHint() {
        val contact1 = Contact(
            contactId = "1",
            lookupKey = "key1",
            fullName = "Max Mustermann",
            spouseLookupKey = "key2"
        )
        val contact2 = Contact(
            contactId = "2",
            lookupKey = "key2",
            fullName = "Erika Mustermann",
            spouseLookupKey = "key1"
        )

        val textWithoutHint = formatter.buildContentText(
            contacts = listOf(contact1, contact2),
            eventType = EventType.ANNIVERSARY,
            showHint = false
        )
        val textWithHint = formatter.buildContentText(
            contacts = listOf(contact1, contact2),
            eventType = EventType.ANNIVERSARY,
            showHint = true
        )

        assertThat(textWithoutHint).isEqualTo(context.getString(R.string.notif_desc_anniversary))
        assertThat(textWithHint).isEqualTo(context.getString(R.string.notif_hint_persistent))
    }

    @Test
    fun buildContentText_groupContacts_withoutAndWithHint() {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "key1", fullName = "Alice"),
            Contact(contactId = "2", lookupKey = "key2", fullName = "Bob")
        )

        val textWithoutHint = formatter.buildContentText(
            contacts = contacts,
            eventType = EventType.BIRTHDAY,
            showHint = false
        )
        val textWithHint = formatter.buildContentText(
            contacts = contacts,
            eventType = EventType.BIRTHDAY,
            showHint = true
        )

        assertThat(textWithoutHint).isEqualTo("Alice, Bob")
        assertThat(textWithHint).isEqualTo("${context.getString(R.string.notif_hint_persistent)} (Alice, Bob)")
    }
}
