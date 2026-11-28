package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [GetContactsUseCase].
 * Verifies domain-level filtering logic.
 */
class GetContactsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var useCase: GetContactsUseCase

    private val today = LocalDate.of(2024, 5, 15)

    /** Default settings: labels enabled, other events disabled, no ignored labels. */
    private val defaultSettings = GetContactsUseCase.LabelSettingsState(
        ignoredLabels = emptySet(),
        labelsEnabled = true,
        otherEventsEnabled = false,
    )

    @Before
    fun setUp() {
        useCase = GetContactsUseCase(mainDispatcherRule.testDispatcher)
    }

    private fun invoke(
        contacts: List<Contact>,
        keywords: List<String> = emptyList(),
        selectedLabel: String? = null,
        settings: GetContactsUseCase.LabelSettingsState = defaultSettings,
    ) = useCase(
        contacts = MutableStateFlow(contacts),
        searchKeywords = MutableStateFlow(keywords),
        selectedLabel = MutableStateFlow(selectedLabel),
        labelSettings = MutableStateFlow(settings),
    )

    // ---------------------------------------------------------------------------
    // Keyword search
    // ---------------------------------------------------------------------------

    @Test
    fun search_singleKeyword_matchesPartialName() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Max Mustermann",
                birthday = today
            ),
            Contact(contactId = "2", lookupKey = "k2", fullName = "Erika Muster", birthday = today),
        )

        val result = invoke(contacts, keywords = listOf("Mustermann")).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun search_multipleKeywords_allMustMatch() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Max Mustermann",
                birthday = today
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Erika Mustermann",
                birthday = today
            ),
        )

        val result = invoke(contacts, keywords = listOf("Max", "Mustermann")).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun search_isCaseInsensitive() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Max Mustermann",
                birthday = today
            ),
        )

        val result = invoke(contacts, keywords = listOf("max")).first()

        assertThat(result).hasSize(1)
    }

    @Test
    fun search_contactWithNoDate_isIncludedWhenSearchingBirthdays() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "No Date Person"
            ),  // birthday = null
        )

        val result = invoke(contacts, keywords = listOf("No Date")).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("No Date Person")
    }

    @Test
    fun noSearch_contactWithNoDate_isHidden() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "No Date Person"
            ),  // birthday = null
            Contact(contactId = "2", lookupKey = "k2", fullName = "Has Date", birthday = today),
        )

        val result = invoke(contacts, keywords = emptyList()).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Has Date")
    }

    // ---------------------------------------------------------------------------
    // Label filtering
    // ---------------------------------------------------------------------------

    @Test
    fun labelFilter_showsOnlyContactsWithMatchingLabel() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Friend",
                birthday = today,
                labels = listOf("Freunde")
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Family",
                birthday = today,
                labels = listOf("Familie")
            ),
        )

        val result = invoke(contacts, selectedLabel = "Freunde").first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Friend")
    }

    @Test
    fun ignoredLabel_hidesContactWhenNotSearching() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Visible",
                birthday = today,
                labels = listOf("Normal")
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Hidden",
                birthday = today,
                labels = listOf("Ignored")
            ),
        )
        val settings = defaultSettings.copy(ignoredLabels = setOf("Ignored"))

        val result = invoke(contacts, settings = settings).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Visible")
    }

    @Test
    fun ignoredLabel_doesNotHideContactDuringSearch() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Hidden But Searched",
                birthday = today,
                labels = listOf("Ignored")
            ),
        )
        val settings = defaultSettings.copy(ignoredLabels = setOf("Ignored"))

        val result = invoke(contacts, keywords = listOf("Hidden"), settings = settings).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Hidden But Searched")
    }

    @Test
    fun ignoredLabel_whenSelected_showsMatchingContactsWithBirthdayLikeNormalLabel() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Ignored With Birthday",
                birthday = today,
                labels = listOf("Ignored"),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Ignored Without Birthday",
                birthday = null,
                labels = listOf("Ignored"),
            ),
            Contact(
                contactId = "3",
                lookupKey = "k3",
                fullName = "Ignored And Normal",
                birthday = today,
                labels = listOf("Normal", "Ignored"),
            ),
            Contact(
                contactId = "4",
                lookupKey = "k4",
                fullName = "Two Ignored Labels",
                birthday = today,
                labels = listOf("Ignored", "OtherIgnored"),
            ),
            Contact(
                contactId = "5",
                lookupKey = "k5",
                fullName = "Only Normal",
                birthday = today,
                labels = listOf("Normal"),
            ),
        )
        val settings = defaultSettings.copy(ignoredLabels = setOf("Ignored", "OtherIgnored"))

        val result = invoke(
            contacts = contacts,
            selectedLabel = "Ignored",
            settings = settings,
        ).first()

        assertThat(result.map { it.fullName }).containsExactly(
            "Ignored With Birthday",
            "Ignored And Normal",
            "Two Ignored Labels",
        )
    }

    @Test
    fun normalLabel_whenSelected_stillHidesContactsThatAlsoHaveIgnoredLabel() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Only Normal",
                birthday = today,
                labels = listOf("Normal"),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Normal And Ignored",
                birthday = today,
                labels = listOf("Normal", "Ignored"),
            ),
        )
        val settings = defaultSettings.copy(ignoredLabels = setOf("Ignored"))

        val result = invoke(
            contacts = contacts,
            selectedLabel = "Normal",
            settings = settings,
        ).first()

        assertThat(result.map { it.fullName }).containsExactly("Only Normal")
    }

    @Test
    fun ignoredPseudoLabel_whenSelected_showsMatchingContacts() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Anniversary Contact",
                anniversary = today.plusDays(2),
                labels = listOf("Ignored"),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "No Birthday Contact",
                birthday = null,
                labels = listOf("Ignored"),
            ),
        )
        val settings = defaultSettings.copy(
            ignoredLabels = setOf(
                "Ignored",
                ContactLabels.LABEL_ANNIVERSARY,
                ContactLabels.LABEL_NO_BIRTHDAY,
            ),
            otherEventsEnabled = true,
        )

        val anniversaryResult = invoke(
            contacts = contacts,
            selectedLabel = ContactLabels.LABEL_ANNIVERSARY,
            settings = settings,
        ).first()
        assertThat(anniversaryResult.map { it.fullName }).containsExactly("Anniversary Contact")

        val noBirthdayResult = invoke(
            contacts = contacts,
            selectedLabel = ContactLabels.LABEL_NO_BIRTHDAY,
            settings = settings,
        ).first()
        assertThat(noBirthdayResult.map { it.fullName }).containsExactly(
            "Anniversary Contact",
            "No Birthday Contact",
        )
    }

    @Test
    fun labelFilter_noBirthday_showsOnlyContactsWithoutDate() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Has Date", birthday = today),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "No Date"
            ),          // birthday = null
        )

        val result = invoke(contacts, selectedLabel = ContactLabels.LABEL_NO_BIRTHDAY).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("No Date")
    }

    // ---------------------------------------------------------------------------
    // Anniversary filtering
    // ---------------------------------------------------------------------------

    @Test
    fun anniversary_contactsAreReturned() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1", lookupKey = "k1", fullName = "Alice",
                anniversary = today.plusDays(3), spouseLookupKey = "k2",
            ),
            Contact(
                contactId = "2", lookupKey = "k2", fullName = "Bob",
                anniversary = today.plusDays(3), spouseLookupKey = "k1",
            ),
        )
        val settings = defaultSettings.copy(otherEventsEnabled = true)

        val result = invoke(
            contacts = contacts,
            selectedLabel = ContactLabels.LABEL_ANNIVERSARY,
            settings = settings,
        ).first()

        assertThat(result).hasSize(2)
        assertThat(result.map { it.fullName }).containsExactly("Alice", "Bob")
    }

    // ---------------------------------------------------------------------------
    // Labels-disabled mode
    // ---------------------------------------------------------------------------

    @Test
    fun labelsDisabled_allEventTypesCombined() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Birthday Person",
                birthday = today.plusDays(1)
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "NameDay Person",
                nameDay = today.plusDays(2)
            ),
            Contact(
                contactId = "3",
                lookupKey = "k3",
                fullName = "Anniversary Person",
                anniversary = today.plusDays(3)
            ),
        )
        val settings = GetContactsUseCase.LabelSettingsState(
            ignoredLabels = emptySet(),
            labelsEnabled = false,
            otherEventsEnabled = true,
        )

        val result = invoke(contacts, settings = settings).first()

        assertThat(result).hasSize(3)
        assertThat(result.map { it.fullName })
            .containsExactly("Birthday Person", "NameDay Person", "Anniversary Person")
    }

    @Test
    fun labelsDisabled_otherEventsDisabled_onlyBirthdaysShown() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Birthday Person",
                birthday = today.plusDays(1)
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "NameDay Person",
                nameDay = today.plusDays(2)
            ),
            Contact(
                contactId = "3",
                lookupKey = "k3",
                fullName = "Anniversary Person",
                anniversary = today.plusDays(3)
            ),
        )
        val settings = GetContactsUseCase.LabelSettingsState(
            ignoredLabels = emptySet(),
            labelsEnabled = false,
            otherEventsEnabled = false,
        )

        val result = invoke(contacts, settings = settings).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Birthday Person")
    }

    @Test
    fun labelFilter_dynamicCustomEvent_showsMatchingContactsUnlessDisabled() = runTest {
        val customKey = ContactLabels.customEventLabel("Jahrestag")
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Custom Person",
                customEvents = mapOf(customKey to today.plusDays(4)),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Birthday Only",
                birthday = today.plusDays(1),
            ),
        )

        val enabledResult = invoke(
            contacts = contacts,
            selectedLabel = customKey,
            settings = defaultSettings.copy(otherEventsEnabled = true),
        ).first()
        assertThat(enabledResult.map { it.fullName }).containsExactly("Custom Person")

        val disabledResult = invoke(
            contacts = contacts,
            selectedLabel = customKey,
            settings = defaultSettings.copy(
                otherEventsEnabled = true,
                disabledOtherEventLabels = setOf(customKey),
            ),
        ).first()
        assertThat(disabledResult).isEmpty()
    }

    @Test
    fun labelsDisabled_includesEnabledCustomEventsAndExcludesDisabledOnes() = runTest {
        val enabledKey = ContactLabels.customEventLabel("Jahrestag")
        val disabledKey = ContactLabels.customEventLabel("DisabledEvent")
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Enabled Custom",
                customEvents = mapOf(enabledKey to today.plusDays(2)),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Disabled Custom",
                customEvents = mapOf(disabledKey to today.plusDays(3)),
            ),
        )
        val settings = GetContactsUseCase.LabelSettingsState(
            ignoredLabels = emptySet(),
            labelsEnabled = false,
            otherEventsEnabled = true,
            disabledOtherEventLabels = setOf(disabledKey),
        )

        val result = invoke(contacts, settings = settings).first()

        assertThat(result.map { it.fullName }).containsExactly("Enabled Custom")
    }
}
