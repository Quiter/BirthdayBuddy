package com.heckmannch.birthdaybuddy.util

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.navigation.AppAction
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class IntentParserTest {

    @Test
    fun `parse returns null when intent is null`() {
        val result = IntentParser.parse(null)
        assertThat(result).isNull()
    }

    @Test
    fun `parse returns NavigateToNotifications when extra is present and true`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns true
        every { intent.getBooleanExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS, false) } returns true

        val result = IntentParser.parse(intent)
        assertThat(result).isEqualTo(AppAction.NavigateToNotifications)
    }

    @Test
    fun `parse returns OpenBirthdayPicker with full date when appfn extras are provided`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns false
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } returns "contact_lookup_42"
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_YEAR) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_YEAR, NO_YEAR_MARKER) } returns 1995
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1) } returns 7
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_DAY) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_DAY, -1) } returns 15

        val result = IntentParser.parse(intent)
        assertThat(result).isInstanceOf(AppAction.OpenBirthdayPicker::class.java)

        val pickerAction = result as AppAction.OpenBirthdayPicker
        assertThat(pickerAction.contactLookupKey).isEqualTo("contact_lookup_42")
        assertThat(pickerAction.year).isEqualTo(1995)
        assertThat(pickerAction.month).isEqualTo(7)
        assertThat(pickerAction.day).isEqualTo(15)
    }

    @Test
    fun `parse returns OpenBirthdayPicker with null year when NO_YEAR_MARKER is used`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns false
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns true
        every { intent.getStringExtra(IntentExtras.APPFN_CONTACT_ID) } returns "contact_lookup_42"
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_YEAR) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_YEAR, NO_YEAR_MARKER) } returns NO_YEAR_MARKER
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_MONTH) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_MONTH, -1) } returns 12
        every { intent.hasExtra(IntentExtras.APPFN_BIRTHDAY_DAY) } returns true
        every { intent.getIntExtra(IntentExtras.APPFN_BIRTHDAY_DAY, -1) } returns 31

        val result = IntentParser.parse(intent)
        assertThat(result).isInstanceOf(AppAction.OpenBirthdayPicker::class.java)

        val pickerAction = result as AppAction.OpenBirthdayPicker
        assertThat(pickerAction.contactLookupKey).isEqualTo("contact_lookup_42")
        assertThat(pickerAction.year).isNull()
        assertThat(pickerAction.month).isEqualTo(12)
        assertThat(pickerAction.day).isEqualTo(31)
    }

    @Test
    fun `parse returns OpenSearch when extra is present and true`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns false
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns false
        every { intent.hasExtra(IntentExtras.OPEN_SEARCH) } returns true
        every { intent.getBooleanExtra(IntentExtras.OPEN_SEARCH, false) } returns true

        val result = IntentParser.parse(intent)
        assertThat(result).isEqualTo(AppAction.OpenSearch)
    }

    @Test
    fun `parse returns ScrollToTop when extra is present and true`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns false
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns false
        every { intent.hasExtra(IntentExtras.OPEN_SEARCH) } returns false
        every { intent.hasExtra(IntentExtras.SCROLL_TO_TOP) } returns true
        every { intent.getBooleanExtra(IntentExtras.SCROLL_TO_TOP, false) } returns true

        val result = IntentParser.parse(intent)
        assertThat(result).isEqualTo(AppAction.ScrollToTop)
    }

    @Test
    fun `parse returns OpenAddContact when extra is present and true`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(IntentExtras.NAVIGATE_TO_NOTIFICATIONS) } returns false
        every { intent.hasExtra(IntentExtras.APPFN_CONTACT_ID) } returns false
        every { intent.hasExtra(IntentExtras.OPEN_SEARCH) } returns false
        every { intent.hasExtra(IntentExtras.SCROLL_TO_TOP) } returns false
        every { intent.hasExtra(IntentExtras.OPEN_ADD_CONTACT) } returns true
        every { intent.getBooleanExtra(IntentExtras.OPEN_ADD_CONTACT, false) } returns true

        val result = IntentParser.parse(intent)
        assertThat(result).isEqualTo(AppAction.OpenAddContact)
    }

    @Test
    fun `parse returns null when intent has no recognized extras`() {
        val intent = mockk<Intent>()
        every { intent.hasExtra(any()) } returns false

        val result = IntentParser.parse(intent)
        assertThat(result).isNull()
    }
}
