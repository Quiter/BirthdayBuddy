package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Pure domain entry pairing a [LabelConfig] with all unfiltered contacts assigned to it.
 *
 * @property config Persisted or default configuration for this label or pseudo-label.
 * @property assignedContacts Unfiltered contacts matched by this label, sorted alphabetically by name.
 * @property isOtherEvent True when this entry represents a pseudo-label for an "Other Event"
 *   (anniversary, name day, custom date-field label, or other event).
 */
data class CategorizedLabel(
    val config: LabelConfig,
    val assignedContacts: List<Contact>,
    val isOtherEvent: Boolean = false,
)

/**
 * Structured, sectioned representation of all labels for the label management screen,
 * completely decoupled from UI rendering.
 *
 * Each label appears in exactly one mutually exclusive section:
 * 1. [activeLabels]: Active regular labels (alphabetical) + [ContactLabels.LABEL_NO_BIRTHDAY] at the end
 *    (when neither hidden nor ignored).
 * 2. [otherEventLabels]: Active pseudo-labels for enabled "Other Events" in canonical order
 *    (when neither hidden nor ignored).
 * 3. [hiddenLabels]: All labels where `isHiddenFromFilter == true && !isIgnored`.
 * 4. [ignoredLabels]: All labels where `isIgnored == true`.
 *
 * @property totalLabeledContactsCount Total count of contacts that have at least one regular label.
 */
data class CategorizedLabels(
    val activeLabels: List<CategorizedLabel> = emptyList(),
    val otherEventLabels: List<CategorizedLabel> = emptyList(),
    val hiddenLabels: List<CategorizedLabel> = emptyList(),
    val ignoredLabels: List<CategorizedLabel> = emptyList(),
    val totalLabeledContactsCount: Int = 0,
)

/**
 * Encapsulates construction, pseudo-label generation, contact assignment, sorting, and section
 * categorization for the label management screen.
 *
 * Reuses [GetDetectedOtherEventsUseCase.detect] as the single source of truth for non-birthday
 * event pseudo-labels so Settings, Calendar Sync, Home filters, and Labels stay strictly in sync.
 */
@Reusable
class GetCategorizedLabelsUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Observes the categorized label sections computed from unfiltered [contacts], persisted
     * [configs], and "Other Events" settings.
     */
    operator fun invoke(
        contacts: Flow<List<Contact>>,
        configs: Flow<List<LabelConfig>>,
        otherEventsEnabled: Flow<Boolean>,
        disabledOtherEventLabels: Flow<Set<String>>,
    ): Flow<CategorizedLabels> = combine(
        contacts,
        configs,
        otherEventsEnabled,
        disabledOtherEventLabels,
    ) { contactList, configList, otherEventsOn, disabledEvents ->
        categorize(
            contacts = contactList,
            configs = configList,
            otherEventsEnabled = otherEventsOn,
            disabledOtherEventLabels = disabledEvents,
        )
    }.flowOn(defaultDispatcher)

    companion object {
        /**
         * Pure, synchronous categorization of all regular labels and pseudo-labels.
         *
         * @param contacts Unfiltered list of contacts.
         * @param configs Persisted [LabelConfig] rows from the database.
         * @param otherEventsEnabled Whether global "Other Events" support is enabled.
         * @param disabledOtherEventLabels Set of individually disabled non-birthday event keys.
         */
        fun categorize(
            contacts: List<Contact>,
            configs: List<LabelConfig>,
            otherEventsEnabled: Boolean,
            disabledOtherEventLabels: Set<String> = emptySet(),
        ): CategorizedLabels {
            val configMap = configs.associateBy { it.name }

            val activeLabels = mutableListOf<CategorizedLabel>()
            val otherEventLabels = mutableListOf<CategorizedLabel>()
            val hiddenLabels = mutableListOf<CategorizedLabel>()
            val ignoredLabels = mutableListOf<CategorizedLabel>()

            fun addEntry(entry: CategorizedLabel) {
                when {
                    entry.config.isIgnored -> ignoredLabels.add(entry)
                    entry.config.isHiddenFromFilter -> hiddenLabels.add(entry)
                    entry.isOtherEvent -> otherEventLabels.add(entry)
                    else -> activeLabels.add(entry)
                }
            }

            // 1. Regular contact labels currently assigned to at least one contact (alphabetical)
            val contactsByLabel = mutableMapOf<String, MutableList<Contact>>()
            for (contact in contacts) {
                for (labelName in contact.labels) {
                    if (labelName == ContactLabels.LABEL_NO_BIRTHDAY ||
                        ContactLabels.isOtherEventLabel(labelName)
                    ) {
                        continue
                    }
                    contactsByLabel.getOrPut(labelName) { mutableListOf() }.add(contact)
                }
            }

            contactsByLabel.keys
                .sortedBy { it.lowercase() }
                .forEach { labelName ->
                    val config = configMap[labelName] ?: LabelConfig(name = labelName)
                    val assigned = contactsByLabel[labelName]
                        .orEmpty()
                        .distinctBy { it.lookupKey }
                        .sortedBy { it.fullName.lowercase() }
                    addEntry(
                        CategorizedLabel(
                            config = config,
                            assignedContacts = assigned,
                            isOtherEvent = false,
                        )
                    )
                }

            // 2. "No Birthday" pseudo-label (appended after regular labels when applicable)
            val contactsWithoutBirthday = contacts
                .filter { it.birthday == null }
                .distinctBy { it.lookupKey }
                .sortedBy { it.fullName.lowercase() }

            if (contactsWithoutBirthday.isNotEmpty()) {
                val noBirthdayConfig = configMap[ContactLabels.LABEL_NO_BIRTHDAY]
                    ?: LabelConfig(name = ContactLabels.LABEL_NO_BIRTHDAY)
                addEntry(
                    CategorizedLabel(
                        config = noBirthdayConfig,
                        assignedContacts = contactsWithoutBirthday,
                        isOtherEvent = false,
                    )
                )
            }

            // 3. Enabled "Other Events" pseudo-labels from shared discovery source (canonical order)
            if (otherEventsEnabled) {
                val detectedEvents = GetDetectedOtherEventsUseCase.detect(
                    contacts = contacts,
                    disabledEventKeys = disabledOtherEventLabels,
                )
                for (event in detectedEvents) {
                    if (!event.isEnabled) continue
                    val eventConfig = configMap[event.eventKey]
                        ?: LabelConfig(name = event.eventKey)
                    val assigned = contacts
                        .filter { it.hasEvent(event.eventKey) }
                        .distinctBy { it.lookupKey }
                        .sortedBy { it.fullName.lowercase() }
                    addEntry(
                        CategorizedLabel(
                            config = eventConfig,
                            assignedContacts = assigned,
                            isOtherEvent = true,
                        )
                    )
                }
            }

            return CategorizedLabels(
                activeLabels = activeLabels,
                otherEventLabels = otherEventLabels,
                hiddenLabels = hiddenLabels,
                ignoredLabels = ignoredLabels,
                totalLabeledContactsCount = contacts.count { it.labels.isNotEmpty() },
            )
        }
    }
}
