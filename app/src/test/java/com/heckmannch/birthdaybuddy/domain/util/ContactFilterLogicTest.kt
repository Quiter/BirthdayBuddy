package com.heckmannch.birthdaybuddy.domain.util

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import org.junit.Test
import java.time.LocalDate

class ContactFilterLogicTest {

    private val activeLabel = LabelConfig(name = "Family", isIgnored = false, notificationsEnabled = true, showInWidget = true)
    private val ignoredLabel = LabelConfig(name = "Ex-Colleagues", isIgnored = true, notificationsEnabled = true, showInWidget = true)
    private val hiddenNotifLabel = LabelConfig(name = "Work", isIgnored = false, notificationsEnabled = false, showInWidget = true)
    private val hiddenWidgetLabel = LabelConfig(name = "Acquaintances", isIgnored = false, notificationsEnabled = true, showInWidget = false)
    private val fullyHiddenLabel = LabelConfig(name = "Gym", isIgnored = false, notificationsEnabled = false, showInWidget = false)

    private val configs = listOf(activeLabel, ignoredLabel, hiddenNotifLabel, hiddenWidgetLabel, fullyHiddenLabel)

    // --- Notification Eligibility Tests ---

    @Test
    fun `isEligibleForNotifications returns true when labels are disabled`() {
        val contact = createContact(labels = listOf("Ex-Colleagues", "Work"))
        val result = ContactFilterLogic.filterForNotifications(listOf(contact), labelsEnabled = false, configs = configs)
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `isEligibleForNotifications returns true for contact without labels`() {
        val contact = createContact(labels = emptyList())
        val result = ContactFilterLogic.filterForNotifications(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `isEligibleForNotifications returns false when contact has at least one ignored label`() {
        val contact = createContact(labels = listOf("Family", "Ex-Colleagues"))
        val result = ContactFilterLogic.filterForNotifications(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).isEmpty()
    }

    @Test
    fun `isEligibleForNotifications returns true when contact has mixed hidden and active labels`() {
        val contact = createContact(labels = listOf("Family", "Work"))
        val result = ContactFilterLogic.filterForNotifications(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `isEligibleForNotifications returns false when contact has exclusively hidden notification labels`() {
        val contact = createContact(labels = listOf("Work", "Gym"))
        val result = ContactFilterLogic.filterForNotifications(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).isEmpty()
    }

    // --- Widget Eligibility Tests ---

    @Test
    fun `isEligibleForWidget returns false when contact has no birthday`() {
        val contact = createContact(birthday = null, labels = listOf("Family"))
        val result = ContactFilterLogic.filterForWidget(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).isEmpty()
    }

    @Test
    fun `isEligibleForWidget returns true when labels are disabled and contact has birthday`() {
        val contact = createContact(labels = listOf("Ex-Colleagues"))
        val result = ContactFilterLogic.filterForWidget(listOf(contact), labelsEnabled = false, configs = configs)
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `isEligibleForWidget returns true for contact without labels`() {
        val contact = createContact(labels = emptyList())
        val result = ContactFilterLogic.filterForWidget(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `isEligibleForWidget returns false when contact has at least one ignored label`() {
        val contact = createContact(labels = listOf("Family", "Ex-Colleagues"))
        val result = ContactFilterLogic.filterForWidget(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).isEmpty()
    }

    @Test
    fun `isEligibleForWidget returns true when contact has mixed hidden and active widget labels`() {
        val contact = createContact(labels = listOf("Family", "Acquaintances"))
        val result = ContactFilterLogic.filterForWidget(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).containsExactly(contact)
    }

    @Test
    fun `isEligibleForWidget returns false when contact has exclusively hidden widget labels`() {
        val contact = createContact(labels = listOf("Acquaintances", "Gym"))
        val result = ContactFilterLogic.filterForWidget(listOf(contact), labelsEnabled = true, configs = configs)
        assertThat(result).isEmpty()
    }

    private fun createContact(
        birthday: LocalDate? = LocalDate.of(1990, 5, 15),
        labels: List<String> = emptyList()
    ): Contact = Contact(
        contactId = "1",
        lookupKey = "key1",
        fullName = "Test Contact",
        birthday = birthday,
        labels = labels
    )
}
