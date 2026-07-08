package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.ContactLabels
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.domain.model.Contact
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
 *
 * All inputs are supplied as [MutableStateFlow]s so each test can verify a single
 * configuration in isolation without the Android framework or any mocks.
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
        useCase = GetContactsUseCase(ContactMapper())
    }

    // ---------------------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------------------

    private fun invoke(
        contacts: List<Contact>,
        keywords: List<String> = emptyList(),
        selectedLabel: String? = null,
        settings: GetContactsUseCase.LabelSettingsState = defaultSettings,
    ) = useCase(
        contacts = MutableStateFlow(contacts),
        currentDate = MutableStateFlow(today),
        searchKeywords = MutableStateFlow(keywords),
        selectedLabel = MutableStateFlow(selectedLabel),
        labelSettings = MutableStateFlow(settings),
    )

    // ---------------------------------------------------------------------------
    // Sorting
    // ---------------------------------------------------------------------------

    @Test
    fun sorting_byDaysUntilNext_thenByName() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Zara", birthday = today.plusDays(10)),
            Contact(contactId = "2", lookupKey = "k2", fullName = "Anna", birthday = today.plusDays(10)),
            Contact(contactId = "3", lookupKey = "k3", fullName = "Bob", birthday = today.plusDays(1)),
        )

        val result = invoke(contacts).first()

        assertThat(result.map { it.fullName }).containsExactly("Bob", "Anna", "Zara").inOrder()
    }

    @Test
    fun sorting_contactsWithNullDaysUntilNext_areLastInList() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "No Date"),          // birthday = null
            Contact(contactId = "2", lookupKey = "k2", fullName = "Has Date", birthday = today.plusDays(5)),
        )

        val result = invoke(contacts, keywords = listOf("Date")).first()

        assertThat(result.first().fullName).isEqualTo("Has Date")
        assertThat(result.last().fullName).isEqualTo("No Date")
    }

    // ---------------------------------------------------------------------------
    // Keyword search
    // ---------------------------------------------------------------------------

    @Test
    fun search_singleKeyword_matchesPartialName() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Max Mustermann", birthday = today),
            Contact(contactId = "2", lookupKey = "k2", fullName = "Erika Muster", birthday = today),
        )

        val result = invoke(contacts, keywords = listOf("Mustermann")).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun search_multipleKeywords_allMustMatch() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Max Mustermann", birthday = today),
            Contact(contactId = "2", lookupKey = "k2", fullName = "Erika Mustermann", birthday = today),
        )

        // "Max" AND "Mustermann" -> only Max Mustermann
        val result = invoke(contacts, keywords = listOf("Max", "Mustermann")).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun search_isCaseInsensitive() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Max Mustermann", birthday = today),
        )

        val result = invoke(contacts, keywords = listOf("max")).first()

        assertThat(result).hasSize(1)
    }

    @Test
    fun search_contactWithNoDate_isIncludedWhenSearchingBirthdays() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "No Date Person"),  // birthday = null
        )

        val result = invoke(contacts, keywords = listOf("No Date")).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("No Date Person")
    }

    @Test
    fun noSearch_contactWithNoDate_isHidden() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "No Date Person"),  // birthday = null
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
            Contact(contactId = "1", lookupKey = "k1", fullName = "Friend", birthday = today, labels = listOf("Freunde")),
            Contact(contactId = "2", lookupKey = "k2", fullName = "Family", birthday = today, labels = listOf("Familie")),
        )

        val result = invoke(contacts, selectedLabel = "Freunde").first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Friend")
    }

    @Test
    fun ignoredLabel_hidesContactWhenNotSearching() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Visible", birthday = today, labels = listOf("Normal")),
            Contact(contactId = "2", lookupKey = "k2", fullName = "Hidden", birthday = today, labels = listOf("Ignored")),
        )
        val settings = defaultSettings.copy(ignoredLabels = setOf("Ignored"))

        val result = invoke(contacts, settings = settings).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("Visible")
    }

    @Test
    fun ignoredLabel_doesNotHideContactDuringSearch() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Hidden But Searched", birthday = today, labels = listOf("Ignored")),
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
            Contact(contactId = "2", lookupKey = "k2", fullName = "No Date"),          // birthday = null
        )

        val result = invoke(contacts, selectedLabel = ContactLabels.LABEL_NO_BIRTHDAY).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().fullName).isEqualTo("No Date")
    }

    // ---------------------------------------------------------------------------
    // Anniversary pairing
    // ---------------------------------------------------------------------------

    @Test
    fun anniversary_twoCoupledContacts_areMergedIntoOneEntry() = runTest {
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

        // Two partners merged into a single couple entry
        assertThat(result).hasSize(1)
        assertThat(result.first().isCouple).isTrue()
        assertThat(result.first().secondFullName).isNotNull()
    }

    @Test
    fun anniversary_singleContactWithNoSpouse_isNotMerged() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1", lookupKey = "k1", fullName = "Single",
                anniversary = today.plusDays(5),
            ),
        )
        val settings = defaultSettings.copy(otherEventsEnabled = true)

        val result = invoke(
            contacts = contacts,
            selectedLabel = ContactLabels.LABEL_ANNIVERSARY,
            settings = settings,
        ).first()

        assertThat(result).hasSize(1)
        assertThat(result.first().isCouple).isFalse()
    }

    // ---------------------------------------------------------------------------
    // Labels-disabled mode
    // ---------------------------------------------------------------------------

    @Test
    fun labelsDisabled_allEventTypesCombined() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Birthday Person", birthday = today.plusDays(1)),
            Contact(contactId = "2", lookupKey = "k2", fullName = "NameDay Person", nameDay = today.plusDays(2)),
            Contact(contactId = "3", lookupKey = "k3", fullName = "Anniversary Person", anniversary = today.plusDays(3)),
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
            .inOrder()
    }

    @Test
    fun labelsDisabled_contactLabelsStrippedFromOutput() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1", lookupKey = "k1", fullName = "Tagged",
                birthday = today, labels = listOf("VIP"),
            ),
        )
        val settings = GetContactsUseCase.LabelSettingsState(
            ignoredLabels = emptySet(),
            labelsEnabled = false,
            otherEventsEnabled = false,
        )

        val result = invoke(contacts, settings = settings).first()

        assertThat(result.first().labels).isEmpty()
    }

    @Test
    fun labelsDisabled_otherEventsDisabled_onlyBirthdaysShown() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Birthday Person", birthday = today.plusDays(1)),
            Contact(contactId = "2", lookupKey = "k2", fullName = "NameDay Person", nameDay = today.plusDays(2)),
            Contact(contactId = "3", lookupKey = "k3", fullName = "Anniversary Person", anniversary = today.plusDays(3)),
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
