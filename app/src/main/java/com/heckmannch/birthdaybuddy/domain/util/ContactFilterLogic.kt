package com.heckmannch.birthdaybuddy.domain.util

import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig

/**
 * Pure domain utility encapsulating filtering business rules for contacts based on labels.
 *
 * Rules:
 * 1. Ignored Labels: If a contact is assigned to at least one label marked as [LabelConfig.isIgnored],
 *    the contact is completely ignored (excluded from notifications and widgets).
 * 2. Hidden Labels: If a contact is assigned to multiple labels and none are ignored,
 *    it is only hidden if ALL of its assigned labels are hidden (disabled).
 *    If at least one assigned label is active (or if the contact has no labels at all),
 *    the contact remains included.
 */
object ContactFilterLogic {

    /**
     * Extracts the set of label names configured as ignored.
     */
    fun extractIgnoredLabels(configs: Collection<LabelConfig>): Set<String> =
        configs.asSequence()
            .filter { it.isIgnored }
            .map { it.name }
            .toSet()

    /**
     * Extracts the set of label names with disabled notifications.
     */
    fun extractDisabledNotificationLabels(configs: Collection<LabelConfig>): Set<String> =
        configs.asSequence()
            .filter { !it.notificationsEnabled }
            .map { it.name }
            .toSet()

    /**
     * Extracts the set of label names hidden from the widget.
     */
    fun extractDisabledWidgetLabels(configs: Collection<LabelConfig>): Set<String> =
        configs.asSequence()
            .filter { !it.showInWidget }
            .map { it.name }
            .toSet()

    /**
     * Evaluates whether a [Contact] is eligible for notifications based on its assigned labels.
     */
    fun isEligibleForNotifications(
        contact: Contact,
        labelsEnabled: Boolean,
        ignoredLabels: Set<String>,
        disabledNotificationLabels: Set<String>
    ): Boolean {
        if (!labelsEnabled) return true
        if (contact.labels.any { it in ignoredLabels }) return false
        if (contact.labels.isEmpty()) return true
        return contact.labels.any { it !in disabledNotificationLabels }
    }

    /**
     * Evaluates whether a [Contact] is eligible to be displayed in the widget.
     */
    fun isEligibleForWidget(
        contact: Contact,
        labelsEnabled: Boolean,
        ignoredLabels: Set<String>,
        disabledWidgetLabels: Set<String>
    ): Boolean {
        if (contact.birthday == null) return false
        if (!labelsEnabled) return true
        if (contact.labels.any { it in ignoredLabels }) return false
        if (contact.labels.isEmpty()) return true
        return contact.labels.any { it !in disabledWidgetLabels }
    }

    /**
     * Filters a collection of [Contact]s for notifications in a single batch.
     */
    fun filterForNotifications(
        contacts: List<Contact>,
        labelsEnabled: Boolean,
        configs: Collection<LabelConfig>
    ): List<Contact> {
        if (!labelsEnabled) return contacts
        val ignoredLabels = extractIgnoredLabels(configs)
        val disabledLabels = extractDisabledNotificationLabels(configs)
        return contacts.filter { contact ->
            isEligibleForNotifications(contact, labelsEnabled, ignoredLabels, disabledLabels)
        }
    }

    /**
     * Filters a collection of [Contact]s for display in the widget in a single batch.
     */
    fun filterForWidget(
        contacts: List<Contact>,
        labelsEnabled: Boolean,
        configs: Collection<LabelConfig>
    ): List<Contact> {
        val ignoredLabels = if (!labelsEnabled) emptySet() else extractIgnoredLabels(configs)
        val disabledLabels = if (!labelsEnabled) emptySet() else extractDisabledWidgetLabels(configs)
        return contacts.filter { contact ->
            isEligibleForWidget(contact, labelsEnabled, ignoredLabels, disabledLabels)
        }
    }
}
