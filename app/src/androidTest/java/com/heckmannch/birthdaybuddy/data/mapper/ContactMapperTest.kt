package com.heckmannch.birthdaybuddy.data.mapper

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class ContactMapperTest {

    private val mapper = ContactMapper()
    private val today = LocalDate.of(2024, 5, 15)

    @Test
    fun toUiModel_mapsBasicFieldsCorrectly() {
        val contact = Contact(
            contactId = "123",
            lookupKey = "abc",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 20),
            phoneNumber = "0123456789",
            hasWhatsApp = true,
            hasSignal = false,
            labels = listOf("Freunde")
        )

        val uiModel = mapper.toUiModel(contact, today)

        assertThat(uiModel.contactId).isEqualTo("123")
        assertThat(uiModel.lookupKey).isEqualTo("abc")
        assertThat(uiModel.fullName).isEqualTo("Max Mustermann")
        assertThat(uiModel.phoneNumber).isEqualTo("0123456789")
        assertThat(uiModel.hasWhatsApp).isTrue()
        assertThat(uiModel.hasSignal).isFalse()
        assertThat(uiModel.labels).containsExactly("Freunde")
        assertThat(uiModel.initials).isEqualTo("MM")
    }

    @Test
    fun toUiModel_calculatesAgeAndDaysUntilNextCorrectly() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "k1",
            fullName = "Birthday Boy",
            birthday = LocalDate.of(1990, 5, 20) // 5 days after 'today'
        )

        val uiModel = mapper.toUiModel(contact, today)

        assertThat(uiModel.daysUntilNext).isEqualTo(5)
        assertThat(uiModel.nextAge).isEqualTo(34) // 2024 - 1990
        assertThat(uiModel.isToday).isFalse()
    }

    @Test
    fun toUiModel_handlesTodayCorrectly() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "k1",
            fullName = "Today Person",
            birthday = LocalDate.of(1990, 5, 15)
        )

        val uiModel = mapper.toUiModel(contact, today)

        assertThat(uiModel.daysUntilNext).isEqualTo(0)
        assertThat(uiModel.isToday).isTrue()
        assertThat(uiModel.nextAge).isEqualTo(34)
    }

    @Test
    fun toUiModel_handlesNoYearMarkerCorrectly() {
        val contact = Contact(
            contactId = "1",
            lookupKey = "k1",
            fullName = "No Year Person",
            birthday = LocalDate.of(NO_YEAR_MARKER, 5, 20)
        )

        val uiModel = mapper.toUiModel(contact, today)

        assertThat(uiModel.nextAge).isNull()
        assertThat(uiModel.daysUntilNext).isEqualTo(5)
        // Date text should not contain the year
        assertThat(uiModel.dateText).doesNotContain(NO_YEAR_MARKER.toString())
    }

    @Test
    fun toUiModel_generatesInitialsCorrectly() {
        val contact1 =
            Contact(contactId = "1", lookupKey = "1", fullName = "max mustermann", birthday = today)
        val contact2 = Contact(contactId = "2", lookupKey = "2", fullName = "  ", birthday = today)

        assertThat(mapper.toUiModel(contact1, today).initials).isEqualTo("MM")
        assertThat(mapper.toUiModel(contact2, today).initials).isEqualTo("?")
    }

    @Test
    fun toUiModel_handlesNullBirthdayCorrectly() {
        val contact = Contact(
            contactId = "null_bday",
            lookupKey = "null_key",
            fullName = "No Birthday",
            birthday = null
        )

        val uiModel = mapper.toUiModel(contact, today)

        assertThat(uiModel.daysUntilNext).isEqualTo(Long.MAX_VALUE)
        assertThat(uiModel.nextAge).isNull()
        assertThat(uiModel.isToday).isFalse()
        assertThat(uiModel.dateText).isEqualTo("-")
    }

    @Test
    fun toUiModelForEvent_mapsAnniversaryCorrectly() {
        val contact = Contact(
            contactId = "anniv_1",
            lookupKey = "anniv_key",
            fullName = "Married Couple",
            birthday = LocalDate.of(1990, 5, 20),
            anniversary = LocalDate.of(2020, 5, 25) // 10 days after today (May 15)
        )

        val uiModel = mapper.toUiModelForEvent(contact, today, "anniversary")

        assertThat(uiModel.daysUntilNext).isEqualTo(10)
        assertThat(uiModel.nextAge).isEqualTo(4) // 2024 - 2020
        assertThat(uiModel.isToday).isFalse()
        // Dates with years should be formatted with standard date formatter
        assertThat(uiModel.dateText).contains("2020")
    }

    @Test
    fun toUiModelForEvent_mapsNameDayCorrectly() {
        val contact = Contact(
            contactId = "nd_1",
            lookupKey = "nd_key",
            fullName = "Saint Name",
            nameDay = LocalDate.of(NO_YEAR_MARKER, 5, 18) // 3 days after today (May 15)
        )

        val uiModel = mapper.toUiModelForEvent(contact, today, "name_day")

        assertThat(uiModel.daysUntilNext).isEqualTo(3)
        assertThat(uiModel.nextAge).isNull() // Name day with NO_YEAR_MARKER has no age
        assertThat(uiModel.isToday).isFalse()
        // No year should be included in the text
        assertThat(uiModel.dateText).doesNotContain(NO_YEAR_MARKER.toString())
    }

    @Test
    fun toUiModelForEvent_handlesNullEventsCorrectly() {
        val contact = Contact(
            contactId = "empty_1",
            lookupKey = "empty_key",
            fullName = "Empty Event Person",
            anniversary = null,
            nameDay = null
        )

        val uiModelAnniv = mapper.toUiModelForEvent(contact, today, "anniversary")
        assertThat(uiModelAnniv.daysUntilNext).isEqualTo(Long.MAX_VALUE)
        assertThat(uiModelAnniv.dateText).isEqualTo("-")

        val uiModelNameDay = mapper.toUiModelForEvent(contact, today, "name_day")
        assertThat(uiModelNameDay.daysUntilNext).isEqualTo(Long.MAX_VALUE)
        assertThat(uiModelNameDay.dateText).isEqualTo("-")
    }
}
