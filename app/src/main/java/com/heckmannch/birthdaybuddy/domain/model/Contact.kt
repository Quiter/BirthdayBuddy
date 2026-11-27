package com.heckmannch.birthdaybuddy.domain.model

import java.time.LocalDate

/**
 * Represents a contact with birthday and event details in the domain layer.
 * Completely decoupled from database implementation details.
 *
 * @property customEvents Map of canonical event keys (e.g. [ContactLabels.LABEL_OTHER_EVENT] or
 *   `custom_event:<Label>`) to their corresponding [LocalDate] values from the system contacts provider.
 */
data class Contact(
    val contactId: String,
    val lookupKey: String,
    val fullName: String,
    val birthday: LocalDate? = null,
    val anniversary: LocalDate? = null,
    val nameDay: LocalDate? = null,
    val customEvents: Map<String, LocalDate> = emptyMap(),
    val imageUri: String? = null,
    val phoneNumber: String? = null,
    val isFavorite: Boolean = false,
    val hasWhatsApp: Boolean = false,
    val hasSignal: Boolean = false,
    val labels: List<String> = emptyList(),
    val giftIdeas: List<GiftIdea> = emptyList(),
    val spouseLookupKey: String? = null
) {
    /**
     * Returns the date for the given non-birthday [eventKey], or `null` if this contact
     * does not have an event of that type.
     */
    fun getEventDate(eventKey: String): LocalDate? = when (eventKey) {
        ContactLabels.LABEL_ANNIVERSARY -> anniversary
        ContactLabels.LABEL_NAME_DAY -> nameDay
        else -> customEvents[eventKey]
    }

    /**
     * Returns `true` if this contact has a date for the given non-birthday [eventKey].
     */
    fun hasEvent(eventKey: String): Boolean = getEventDate(eventKey) != null
}
