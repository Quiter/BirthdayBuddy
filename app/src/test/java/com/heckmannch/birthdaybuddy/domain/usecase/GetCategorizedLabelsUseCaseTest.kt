package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [GetCategorizedLabelsUseCase].
 */
class GetCategorizedLabelsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var useCase: GetCategorizedLabelsUseCase

    @Before
    fun setUp() {
        useCase = GetCategorizedLabelsUseCase(mainDispatcherRule.testDispatcher)
    }

    @Test
    fun categorize_splitsRegularNoBirthdayOtherEventsHiddenAndIgnoredIntoExactSections() = runTest {
        val customActive = ContactLabels.customEventLabel("Jahrestag")
        val customHidden = ContactLabels.customEventLabel("Verlobung")
        val customDisabled = ContactLabels.customEventLabel("Ausgeschaltet")

        val alice = Contact(
            contactId = "1",
            lookupKey = "k1",
            fullName = "Alice",
            birthday = LocalDate.of(1990, 5, 10),
            anniversary = LocalDate.of(2019, 6, 1),
            nameDay = LocalDate.of(2020, 7, 26),
            customEvents = mapOf(
                customActive to LocalDate.of(2021, 1, 1),
                customHidden to LocalDate.of(2018, 2, 14),
                customDisabled to LocalDate.of(2017, 3, 3),
                ContactLabels.LABEL_OTHER_EVENT to LocalDate.of(2022, 4, 4),
            ),
            labels = listOf("Work", "Family"),
        )
        val bob = Contact(
            contactId = "2",
            lookupKey = "k2",
            fullName = "Bob",
            birthday = null,
            anniversary = LocalDate.of(2019, 6, 1),
            labels = listOf("HiddenGroup", "SpamGroup"),
        )

        val configs = listOf(
            LabelConfig(name = "Family"),
            LabelConfig(name = "Work"),
            LabelConfig(name = "HiddenGroup", isHiddenFromFilter = true, isIgnored = false),
            LabelConfig(name = "SpamGroup", isHiddenFromFilter = true, isIgnored = true),
            LabelConfig(name = customHidden, isHiddenFromFilter = true, isIgnored = false),
            LabelConfig(name = ContactLabels.LABEL_NAME_DAY, isIgnored = true),
        )

        val result = useCase(
            contacts = MutableStateFlow(listOf(alice, bob)),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(true),
            disabledOtherEventLabels = MutableStateFlow(setOf(customDisabled)),
        ).first()

        // Section 1: Active regular labels (alphabetical) + LABEL_NO_BIRTHDAY at end
        assertThat(result.activeLabels.map { it.config.name }).containsExactly(
            "Family",
            "Work",
            ContactLabels.LABEL_NO_BIRTHDAY,
        ).inOrder()
        assertThat(result.activeLabels[2].assignedContacts.map { it.fullName })
            .containsExactly("Bob")

        // Section 2: Active enabled "Other Events" in canonical order (disabled/hidden/ignored excluded)
        assertThat(result.otherEventLabels.map { it.config.name }).containsExactly(
            ContactLabels.LABEL_ANNIVERSARY,
            customActive,
            ContactLabels.LABEL_OTHER_EVENT,
        ).inOrder()
        assertThat(result.otherEventLabels[0].assignedContacts.map { it.fullName })
            .containsExactly("Alice", "Bob").inOrder()
        assertThat(result.otherEventLabels[1].assignedContacts.map { it.fullName })
            .containsExactly("Alice")
        assertThat(result.otherEventLabels[2].assignedContacts.map { it.fullName })
            .containsExactly("Alice")

        // Section 3: Hidden (isHiddenFromFilter && !isIgnored) — includes both regular & hidden other-events
        assertThat(result.hiddenLabels.map { it.config.name }).containsExactly(
            "HiddenGroup",
            customHidden,
        ).inOrder()
        assertThat(result.hiddenLabels[0].assignedContacts.map { it.fullName })
            .containsExactly("Bob")
        assertThat(result.hiddenLabels[1].assignedContacts.map { it.fullName })
            .containsExactly("Alice")

        // Section 4: Ignored (isIgnored == true takes precedence over isHiddenFromFilter)
        assertThat(result.ignoredLabels.map { it.config.name }).containsExactly(
            "SpamGroup",
            ContactLabels.LABEL_NAME_DAY,
        ).inOrder()
        assertThat(result.ignoredLabels[0].assignedContacts.map { it.fullName })
            .containsExactly("Bob")
        assertThat(result.ignoredLabels[1].assignedContacts.map { it.fullName })
            .containsExactly("Alice")

        // Total labeled contacts count from unfiltered contacts
        assertThat(result.totalLabeledContactsCount).isEqualTo(2)
    }

    @Test
    fun categorize_excludesAllOtherEventsWhenGlobalToggleIsOff() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10),
                anniversary = LocalDate.of(2020, 6, 1),
                nameDay = LocalDate.of(2020, 7, 26),
                customEvents = mapOf(ContactLabels.customEventLabel("Jahrestag") to LocalDate.of(2021, 1, 1)),
                labels = listOf("Family"),
            )
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(emptyList()),
            otherEventsEnabled = MutableStateFlow(false),
            disabledOtherEventLabels = MutableStateFlow(emptySet()),
        ).first()

        assertThat(result.activeLabels.map { it.config.name }).containsExactly("Family")
        assertThat(result.otherEventLabels).isEmpty()
        assertThat(result.hiddenLabels).isEmpty()
        assertThat(result.ignoredLabels).isEmpty()
    }

    @Test
    fun categorize_ignoresUnusedOrphanLabelConfigs() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10),
                labels = listOf("Family"),
            )
        )
        val configs = listOf(
            LabelConfig("Family"),
            LabelConfig("DeletedGroup"),
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(false),
            disabledOtherEventLabels = MutableStateFlow(emptySet()),
        ).first()

        assertThat(result.activeLabels.map { it.config.name }).containsExactly("Family")
    }
}
