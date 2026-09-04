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
 * Encapsulates the filtering logic for the Home screen: keyword search,
 * ignored labels, and selected labels filter, returning domain [Contact] models.
 *
 * All reactive inputs are accepted as [Flow] parameters so that the ViewModel
 * stays decoupled from the filter transformations and this class can be unit-tested
 * without any Android framework dependencies.
 */
@Reusable
class GetContactsUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Settings snapshot derived from label & event configuration flows.
     * Kept internal – the ViewModel builds this from repository flows.
     */
    data class LabelSettingsState(
        val ignoredLabels: Set<String>,
        val labelsEnabled: Boolean,
        val otherEventsEnabled: Boolean,
    )

    /**
     * Returns a [Flow] that emits the filtered list of domain [Contact] models
     * whenever any of the upstream inputs changes.
     *
     * The returned flow is pinned to [Dispatchers.Default] via [flowOn] so that
     * the heavy CPU work is always offloaded from the Main thread.
     * Callers MUST NOT add an additional [flowOn] on top of this flow.
     */
    operator fun invoke(
        contacts: Flow<List<Contact>>,
        searchKeywords: Flow<List<String>>,
        selectedLabel: Flow<String?>,
        labelSettings: Flow<LabelSettingsState>,
    ): Flow<List<Contact>> = combine(
        contacts,
        searchKeywords,
        selectedLabel,
        labelSettings,
    ) { rawContacts, keywords, label, settingsState ->
        buildContactList(rawContacts, keywords, label, settingsState)
    }.flowOn(defaultDispatcher)

    private fun buildContactList(
        rawContacts: List<Contact>,
        keywords: List<String>,
        label: String?,
        settingsState: LabelSettingsState,
    ): List<Contact> {
        val isSearching = keywords.isNotEmpty()
        val ignoredLabels = settingsState.ignoredLabels
        val labelsEnabled = settingsState.labelsEnabled
        val otherEventsEnabled = settingsState.otherEventsEnabled

        val contactMap = rawContacts.associateBy { it.lookupKey }

        return rawContacts.filter { contact ->
            // 1. Keyword search filter (checks self or spouse)
            if (isSearching && !keywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true) ||
                            (contact.spouseLookupKey?.let { contactMap[it]?.fullName }
                                ?.contains(keyword, ignoreCase = true) ?: false)
                }
            ) return@filter false

            // 2. Ignored labels filter (only when not searching and labels are enabled)
            if (labelsEnabled && !isSearching && contact.labels.any { it in ignoredLabels }) {
                return@filter false
            }

            // 3. Label / Event type filter
            if (!labelsEnabled) {
                val hasBirthday = contact.birthday != null
                val hasNameDay = otherEventsEnabled && contact.nameDay != null
                val hasAnniversary = otherEventsEnabled && contact.anniversary != null
                hasBirthday || hasNameDay || hasAnniversary || isSearching
            } else {
                when (label) {
                    null -> {
                        // All contacts with a birthday, or during search, contacts with no events too
                        contact.birthday != null || isSearching
                    }

                    ContactLabels.LABEL_NO_BIRTHDAY -> {
                        // Contacts without birthday
                        contact.birthday == null
                    }

                    ContactLabels.LABEL_ANNIVERSARY -> {
                        // Contacts with anniversary
                        contact.anniversary != null
                    }

                    ContactLabels.LABEL_NAME_DAY -> {
                        // Contacts with name day
                        contact.nameDay != null
                    }

                    else -> {
                        // Contacts with specific label category and birthday (unless searching)
                        contact.labels.contains(label) && (contact.birthday != null || isSearching)
                    }
                }
            }
        }
    }
}
