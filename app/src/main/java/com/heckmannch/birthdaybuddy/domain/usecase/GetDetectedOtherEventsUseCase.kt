package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Pure domain representation of a non-birthday date-field category discovered across contacts.
 *
 * @property eventKey Canonical event key (e.g. [ContactLabels.LABEL_ANNIVERSARY],
 *   [ContactLabels.LABEL_NAME_DAY], [ContactLabels.LABEL_OTHER_EVENT], or `custom_event:<Label>`).
 * @property contactCount Number of contacts that have a date for this event.
 * @property isEnabled Whether this specific event category is currently active
 *   (not listed in `disabledOtherEventLabels`).
 */
data class DetectedOtherEvent(
    val eventKey: String,
    val contactCount: Int,
    val isEnabled: Boolean,
)

/**
 * Single source of truth for discovering and canonically ordering all non-birthday date-field
 * event labels present in the user's contacts.
 *
 * Shared across:
 * - Other Events settings screen (`OtherEventsViewModel`)
 * - Calendar settings & calendar synchronization (`CalendarViewModel`, `CalendarSyncRepositoryImpl`)
 * - Label categorization (`GetCategorizedLabelsUseCase`)
 * - Home filter bar (`GetAvailableLabelsUseCase`)
 *
 * Canonical order:
 * 1. [ContactLabels.LABEL_ANNIVERSARY] (if present)
 * 2. [ContactLabels.LABEL_NAME_DAY] (if present)
 * 3. Custom date-field labels (`custom_event:*`) sorted alphabetically (case-insensitive)
 * 4. [ContactLabels.LABEL_OTHER_EVENT] (if present)
 */
@Reusable
class GetDetectedOtherEventsUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Observes the canonically ordered list of detected non-birthday events across [contacts].
     */
    operator fun invoke(
        contacts: Flow<List<Contact>>,
        disabledEventKeys: Flow<Set<String>>,
    ): Flow<List<DetectedOtherEvent>> = combine(
        contacts,
        disabledEventKeys,
    ) { contactList, disabledKeys ->
        detect(contactList, disabledKeys)
    }.flowOn(defaultDispatcher)

    companion object {
        /**
         * Pure, synchronous discovery of all non-birthday date-field events present in [contacts].
         *
         * @param contacts Unfiltered list of contacts.
         * @param disabledEventKeys Set of event keys explicitly disabled by the user.
         * @return Canonically ordered list of [DetectedOtherEvent] items with contact counts.
         */
        fun detect(
            contacts: List<Contact>,
            disabledEventKeys: Set<String> = emptySet(),
        ): List<DetectedOtherEvent> {
            val result = mutableListOf<DetectedOtherEvent>()

            val anniversaryCount = contacts.count { it.anniversary != null }
            if (anniversaryCount > 0) {
                result.add(
                    DetectedOtherEvent(
                        eventKey = ContactLabels.LABEL_ANNIVERSARY,
                        contactCount = anniversaryCount,
                        isEnabled = ContactLabels.LABEL_ANNIVERSARY !in disabledEventKeys,
                    )
                )
            }

            val nameDayCount = contacts.count { it.nameDay != null }
            if (nameDayCount > 0) {
                result.add(
                    DetectedOtherEvent(
                        eventKey = ContactLabels.LABEL_NAME_DAY,
                        contactCount = nameDayCount,
                        isEnabled = ContactLabels.LABEL_NAME_DAY !in disabledEventKeys,
                    )
                )
            }

            val customCounts = mutableMapOf<String, Int>()
            var otherEventCount = 0
            for (contact in contacts) {
                for (key in contact.customEvents.keys) {
                    if (key == ContactLabels.LABEL_OTHER_EVENT) {
                        otherEventCount++
                    } else if (ContactLabels.isCustomEventLabel(key)) {
                        customCounts[key] = (customCounts[key] ?: 0) + 1
                    }
                }
            }

            customCounts.entries
                .sortedBy {
                    (ContactLabels.extractCustomEventName(it.key) ?: it.key).lowercase()
                }
                .forEach { (key, count) ->
                    result.add(
                        DetectedOtherEvent(
                            eventKey = key,
                            contactCount = count,
                            isEnabled = key !in disabledEventKeys,
                        )
                    )
                }

            if (otherEventCount > 0) {
                result.add(
                    DetectedOtherEvent(
                        eventKey = ContactLabels.LABEL_OTHER_EVENT,
                        contactCount = otherEventCount,
                        isEnabled = ContactLabels.LABEL_OTHER_EVENT !in disabledEventKeys,
                    )
                )
            }

            return result
        }
    }
}
