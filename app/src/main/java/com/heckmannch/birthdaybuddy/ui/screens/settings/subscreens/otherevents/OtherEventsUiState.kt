package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.otherevents

import androidx.compose.runtime.Immutable

/**
 * UI representation of a single non-birthday date-field category discovered in the user's contacts.
 *
 * @property eventKey Canonical event key (e.g. `special:anniversary`, `special:name_day`,
 *   `special:other_event`, or `custom_event:<Label>`).
 * @property contactCount Number of contacts that have a date for this event.
 * @property isEnabled Whether this event category is currently enabled by the user.
 */
@Immutable
data class DetectedOtherEventUiModel(
    val eventKey: String,
    val contactCount: Int,
    val isEnabled: Boolean,
)

/**
 * Coherent UI state for the "Other Events" settings subpage.
 *
 * @property otherEventsEnabled Global toggle controlling whether non-birthday events are active.
 * @property detectedEvents All non-birthday date-field categories found in the user's contacts,
 *   in canonical display order.
 */
@Immutable
data class OtherEventsUiState(
    val otherEventsEnabled: Boolean = false,
    val detectedEvents: List<DetectedOtherEventUiModel> = emptyList(),
)
