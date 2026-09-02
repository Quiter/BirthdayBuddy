package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
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
        useCase = GetContactsUseCase()
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
}
