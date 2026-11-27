package com.heckmannch.birthdaybuddy.domain.model

import java.util.Locale

/**
 * Domain-defined special label identifiers used to represent pseudo-categories
 * (contacts without a birthday, anniversaries, name days, and custom date-field events)
 * in the label filter, settings, and [LabelConfig] system.
 *
 * Placed in the domain layer to decouple UI and UseCases from data layer details.
 */
object ContactLabels {
    const val LABEL_NO_BIRTHDAY = "special:no_birthday"
    const val LABEL_ANNIVERSARY = "special:anniversary"
    const val LABEL_NAME_DAY = "special:name_day"
    const val LABEL_OTHER_EVENT = "special:other_event"

    /**
     * Prefix for user-defined contact date-field labels (`Event.TYPE_CUSTOM`) to prevent
     * collisions with regular contact group labels of the same name.
     */
    const val CUSTOM_EVENT_PREFIX = "custom_event:"

    /**
     * Lowercase names of redundant Android system/favorites contact groups that must never
     * appear as regular user labels or filter chips.
     */
    val REDUNDANT_LABELS: Set<String> = setOf(
        "my contacts",
        "contacts",
        "all contacts",
        "ich",
        "me",
        "starred in android",
        "starred",
        "favorites",
        "favoriten",
    )

    /**
     * Creates a canonical pseudo-label key for a custom contact date-field label.
     */
    fun customEventLabel(rawLabel: String): String = "$CUSTOM_EVENT_PREFIX${rawLabel.trim()}"

    /**
     * Returns `true` if [label] represents a user-defined custom date-field event.
     */
    fun isCustomEventLabel(label: String): Boolean = label.startsWith(CUSTOM_EVENT_PREFIX)

    /**
     * Returns `true` if [label] represents any non-birthday "Other event" category
     * (anniversary, name day, generic other event, or custom date-field label).
     */
    fun isOtherEventLabel(label: String): Boolean =
        label == LABEL_ANNIVERSARY ||
            label == LABEL_NAME_DAY ||
            label == LABEL_OTHER_EVENT ||
            isCustomEventLabel(label)

    /**
     * Returns `true` if [label] matches a redundant Android system or favorites group name
     * (case-insensitive using [Locale.ROOT]).
     */
    fun isRedundantLabel(label: String): Boolean =
        label.trim().lowercase(Locale.ROOT) in REDUNDANT_LABELS

    /**
     * Returns `true` if [label] is stored in [Contact.customEvents]
     * (i.e. [LABEL_OTHER_EVENT] or a prefixed custom event label).
     */
    fun isDynamicCustomEventKey(label: String): Boolean =
        label == LABEL_OTHER_EVENT || isCustomEventLabel(label)

    /**
     * Extracts the raw display name from a prefixed custom event key,
     * or returns `null` if [label] is not a prefixed custom event.
     */
    fun extractCustomEventName(label: String): String? =
        if (isCustomEventLabel(label)) label.removePrefix(CUSTOM_EVENT_PREFIX) else null
}
