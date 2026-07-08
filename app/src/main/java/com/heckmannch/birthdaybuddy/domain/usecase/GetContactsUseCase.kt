package com.heckmannch.birthdaybuddy.domain.usecase

import android.util.Log
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.local.ContactLabels
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.EventType
import com.heckmannch.birthdaybuddy.util.mergeNames
import dagger.Reusable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

/**
 * Encapsulates the display-list logic for the Home screen: filtering, couple-merging
 * and sorting of [ContactUiModel]s.
 *
 * Accepts all required reactive inputs as [Flow] parameters so that the ViewModel
 * stays decoupled from the heavy transformation and this class can be unit-tested
 * without any Android framework dependencies.
 *
 * @param mapper Maps [Contact] DB entities to [ContactUiModel]s.
 */
@Reusable
class GetContactsUseCase @Inject constructor(
    private val mapper: ContactMapper,
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
     * Returns a [Flow] that emits the filtered, merged and sorted list of
     * [ContactUiModel]s whenever any of the upstream inputs changes.
     *
     * @param contacts        Raw contacts from the DB.
     * @param currentDate     Today's date (auto-refreshes at midnight).
     * @param searchKeywords  Debounced, trimmed keyword list from the search field.
     * @param selectedLabel   Currently active label filter chip (null = all).
     * @param labelSettings   Aggregated label & event-visibility settings.
     */
    operator fun invoke(
        contacts: Flow<List<Contact>>,
        currentDate: Flow<LocalDate>,
        searchKeywords: Flow<List<String>>,
        selectedLabel: Flow<String?>,
        labelSettings: Flow<LabelSettingsState>,
    ): Flow<List<ContactUiModel>> = combine(
        contacts,
        currentDate,
        searchKeywords,
        selectedLabel,
        labelSettings,
    ) { rawContacts, today, keywords, label, settingsState ->
        val startTime = System.currentTimeMillis()
        val result = buildContactList(rawContacts, today, keywords, label, settingsState)

        if (rawContacts.size > 1000) {
            Log.d(
                TAG,
                "Filtering ${rawContacts.size} -> ${result.size} contacts took " +
                    "${System.currentTimeMillis() - startTime}ms",
            )
        }
        result
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun buildContactList(
        rawContacts: List<Contact>,
        today: LocalDate,
        keywords: List<String>,
        label: String?,
        settingsState: LabelSettingsState,
    ): List<ContactUiModel> {
        val isSearching = keywords.isNotEmpty()
        val ignoredLabels = settingsState.ignoredLabels
        val labelsEnabled = settingsState.labelsEnabled
        val otherEventsEnabled = settingsState.otherEventsEnabled

        val uiList = if (!labelsEnabled) {
            buildListWithoutLabels(rawContacts, today, keywords, isSearching, otherEventsEnabled)
        } else {
            buildListWithLabels(rawContacts, today, keywords, label, isSearching, ignoredLabels)
        }

        return uiList.sortedWith(
            compareBy<ContactUiModel, Long?>(nullsLast(naturalOrder())) { it.daysUntilNext }
                .thenBy { it.fullName },
        )
    }

    /**
     * Builds the contact list when label management is disabled.
     * All three event types (birthday, name day, anniversary) are merged into a single list.
     */
    private fun buildListWithoutLabels(
        rawContacts: List<Contact>,
        today: LocalDate,
        keywords: List<String>,
        isSearching: Boolean,
        otherEventsEnabled: Boolean,
    ): List<ContactUiModel> {
        // 1. Birthdays
        val birthdays = rawContacts.asSequence()
            .filter { it.birthday != null }
            .filter { contact ->
                !isSearching || keywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true)
                }
            }
            .map {
                mapper.toUiModelForEvent(it, today, EventType.BIRTHDAY)
                    .copy(labels = emptyList())
            }
            .toList()

        // 2. Name days (only when other events are enabled)
        val nameDays = if (otherEventsEnabled) {
            rawContacts.asSequence()
                .filter { it.nameDay != null }
                .filter { contact ->
                    !isSearching || keywords.all { keyword ->
                        contact.fullName.contains(keyword, ignoreCase = true)
                    }
                }
                .map {
                    mapper.toUiModelForEvent(it, today, EventType.NAME_DAY)
                        .copy(labels = emptyList())
                }
                .toList()
        } else emptyList()

        // 3. Anniversaries with couple-pairing (only when other events are enabled)
        val pairedAnniversaries = if (otherEventsEnabled) {
            buildAnniversaryList(
                rawContacts = rawContacts,
                today = today,
                keywords = keywords,
                isSearching = isSearching,
                applySearchFilter = true,
                mergeLabels = false,
            )
        } else emptyList()

        // 4. Contacts without any event – only visible during search
        val contactsWithNoEvent = if (isSearching) {
            rawContacts.asSequence()
                .filter { contact ->
                    val hasNoAnniversary = !otherEventsEnabled || contact.anniversary == null
                    val hasNoNameDay = !otherEventsEnabled || contact.nameDay == null
                    contact.birthday == null && hasNoAnniversary && hasNoNameDay
                }
                .filter { contact ->
                    keywords.all { keyword ->
                        contact.fullName.contains(keyword, ignoreCase = true)
                    }
                }
                .map {
                    mapper.toUiModelForEvent(it, today, EventType.BIRTHDAY)
                        .copy(labels = emptyList())
                }
                .toList()
        } else emptyList()

        return birthdays + nameDays + pairedAnniversaries + contactsWithNoEvent
    }

    /**
     * Builds the contact list when label management is active.
     * Applies label and event-type filters before mapping and merging.
     */
    private fun buildListWithLabels(
        rawContacts: List<Contact>,
        today: LocalDate,
        keywords: List<String>,
        label: String?,
        isSearching: Boolean,
        ignoredLabels: Set<String>,
    ): List<ContactUiModel> {
        val displayEventType: EventType = when (label) {
            ContactLabels.LABEL_ANNIVERSARY -> EventType.ANNIVERSARY
            ContactLabels.LABEL_NAME_DAY -> EventType.NAME_DAY
            else -> EventType.BIRTHDAY
        }

        val preFilteredRaw = if (displayEventType != EventType.ANNIVERSARY) {
            rawContacts.asSequence().filter { contact ->
                if (isSearching && !keywords.all { keyword ->
                        contact.fullName.contains(keyword, ignoreCase = true)
                    }
                ) return@filter false

                if (label != null &&
                    label != ContactLabels.LABEL_NO_BIRTHDAY &&
                    label != ContactLabels.LABEL_NAME_DAY &&
                    !contact.labels.contains(label)
                ) return@filter false

                if (!isSearching && contact.labels.any { it in ignoredLabels }) return@filter false

                val hasEvent = if (displayEventType == EventType.NAME_DAY) {
                    contact.nameDay != null
                } else {
                    contact.birthday != null
                }
                if (!hasEvent) {
                    if (displayEventType != EventType.BIRTHDAY) return@filter false
                    if (!isSearching && label != ContactLabels.LABEL_NO_BIRTHDAY) return@filter false
                } else if (label == ContactLabels.LABEL_NO_BIRTHDAY) {
                    return@filter false
                }
                true
            }.toList()
        } else {
            rawContacts
        }

        val uiListTemp = if (displayEventType == EventType.ANNIVERSARY) {
            buildAnniversaryList(
                rawContacts = rawContacts,
                today = today,
                keywords = keywords,
                isSearching = isSearching,
                applySearchFilter = false,
                mergeLabels = true,
            )
        } else {
            preFilteredRaw.map { mapper.toUiModelForEvent(it, today, displayEventType) }
        }

        return uiListTemp.filter {
            shouldShowContact(it, keywords, label, ignoredLabels, displayEventType)
        }
    }

    /**
     * Builds the anniversary list with couple-pairing logic.
     *
     * @param applySearchFilter When true, applies keyword search during iteration
     *                          (used in the no-labels path where pre-filtering has not
     *                          yet happened).
     * @param mergeLabels       When true, merges both partners' labels; otherwise
     *                          clears labels (no-labels path).
     */
    private fun buildAnniversaryList(
        rawContacts: List<Contact>,
        today: LocalDate,
        keywords: List<String>,
        isSearching: Boolean,
        applySearchFilter: Boolean,
        mergeLabels: Boolean,
    ): List<ContactUiModel> {
        val processedKeys = mutableSetOf<String>()
        val list = mutableListOf<ContactUiModel>()
        val contactMap = rawContacts.associateBy { it.lookupKey }

        for (contact in rawContacts) {
            if (contact.anniversary == null) continue
            if (processedKeys.contains(contact.lookupKey)) continue

            // Optional search filter (only applied in the no-labels path)
            if (applySearchFilter && isSearching && !keywords.all { keyword ->
                    contact.fullName.contains(keyword, ignoreCase = true) ||
                        (contact.spouseLookupKey?.let { contactMap[it]?.fullName }
                            ?.contains(keyword, ignoreCase = true) ?: false)
                }
            ) continue

            val spouseKey = contact.spouseLookupKey
            val spouse = if (spouseKey != null) contactMap[spouseKey] else null

            if (spouse != null && spouse.anniversary != null) {
                processedKeys.add(contact.lookupKey)
                processedKeys.add(spouse.lookupKey)

                val uiModelA = mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                val uiModelB = mapper.toUiModelForEvent(spouse, today, EventType.ANNIVERSARY)

                val mergedModel = ContactUiModel(
                    id = "${contact.lookupKey}_${spouse.lookupKey}",
                    contactId = contact.contactId,
                    lookupKey = contact.lookupKey,
                    fullName = mergeNames(contact.fullName, spouse.fullName),
                    dateText = uiModelA.dateText,
                    monthName = uiModelA.monthName,
                    imageUri = contact.imageUri,
                    phoneNumber = contact.phoneNumber,
                    initials = uiModelA.initials,
                    nextAge = uiModelA.nextAge,
                    daysUntilNext = uiModelA.daysUntilNext,
                    isToday = uiModelA.isToday,
                    hasWhatsApp = contact.hasWhatsApp || spouse.hasWhatsApp,
                    hasSignal = contact.hasSignal || spouse.hasSignal,
                    labels = if (mergeLabels) {
                        (contact.labels + spouse.labels).distinct()
                    } else emptyList(),
                    giftIdeas = uiModelA.giftIdeas + uiModelB.giftIdeas,
                    birthday = contact.birthday,
                    secondImageUri = spouse.imageUri,
                    secondInitials = uiModelB.initials,
                    secondFullName = spouse.fullName,
                    isCouple = true,
                )
                list.add(mergedModel)
            } else {
                processedKeys.add(contact.lookupKey)
                val single = mapper.toUiModelForEvent(contact, today, EventType.ANNIVERSARY)
                list.add(if (mergeLabels) single else single.copy(labels = emptyList()))
            }
        }
        return list
    }

    /**
     * Post-mapping filter gate applied in the label-enabled path.
     * Checks event presence, ignored-label status and keyword/label matches.
     */
    private fun shouldShowContact(
        contact: ContactUiModel,
        keywords: List<String>,
        label: String?,
        ignoredLabels: Set<String>,
        displayEventType: EventType,
    ): Boolean {
        val isSearching = keywords.isNotEmpty()
        val isMissingEvent = contact.dateText == "-"
        val isNoBirthdayFilter = label == ContactLabels.LABEL_NO_BIRTHDAY

        // Hide event-less contacts (except during birthday search)
        if (isMissingEvent) {
            if (displayEventType != EventType.BIRTHDAY) return false
            if (!isSearching && !isNoBirthdayFilter) return false
        }

        // Hide contacts with ignored labels (unless actively searching)
        if (contact.labels.any { it in ignoredLabels } && !isSearching) return false

        // Keyword match
        if (isSearching && !keywords.all { keyword ->
                contact.fullName.contains(keyword, ignoreCase = true)
            }
        ) return false

        // Label match
        return when (label) {
            null -> true
            ContactLabels.LABEL_NO_BIRTHDAY -> isMissingEvent
            // Already filtered upstream via isMissingEvent + displayEventType
            ContactLabels.LABEL_ANNIVERSARY -> true
            ContactLabels.LABEL_NAME_DAY -> true
            else -> contact.labels.contains(label)
        }
    }

    private companion object {
        private const val TAG = "GetContactsUseCase"
    }
}
