package com.heckmannch.birthdaybuddy.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [GetAvailableLabelsUseCase].
 *
 * Verifies that label categorization, visibility rules, sorting, and ordering are
 * correctly computed based on contacts, label configurations, and active event settings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GetAvailableLabelsUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var useCase: GetAvailableLabelsUseCase

    @Before
    fun setUp() {
        useCase = GetAvailableLabelsUseCase()
    }

    @Test
    fun whenLabelsDisabled_returnsEmptyList() = runTest {
        // Scenario 1: labelsEnabled = false -> empty list returned
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Alice", labels = listOf("Family"))
        )
        val configs = listOf(
            LabelConfig(name = "Family", isHiddenFromFilter = false, isIgnored = false, isSystem = false)
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(true),
            labelsEnabled = MutableStateFlow(false)
        ).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun whenLabelsEnabled_noActiveLabelsOrEvents_returnsEmptyList() = runTest {
        // Verify that with active contacts but no labels, pseudo-labels, or other events, the list is empty
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Alice", birthday = LocalDate.of(1990, 5, 10))
        )
        val configs = emptyList<LabelConfig>()

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(false),
            labelsEnabled = MutableStateFlow(true)
        ).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun whenAllLabelsAreIgnored_returnsEmptyList() = runTest {
        // Scenario 2: All labels are marked as isIgnored = true -> empty list
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10),
                labels = listOf("Family", "Work")
            )
        )
        val configs = listOf(
            LabelConfig(name = "Family", isHiddenFromFilter = false, isIgnored = true, isSystem = false),
            LabelConfig(name = "Work", isHiddenFromFilter = false, isIgnored = true, isSystem = false)
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(false),
            labelsEnabled = MutableStateFlow(true)
        ).first()

        assertThat(result).isEmpty()
    }

    @Test
    fun whenOtherEventsEnabledAndContactsWithAnniversary_includesAnniversaryLabel() = runTest {
        // Scenario 3: otherEventsEnabled = true + contacts with anniversary -> LABEL_ANNIVERSARY appears in the list
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                birthday = LocalDate.of(1990, 5, 10),
                anniversary = LocalDate.of(2020, 10, 10)
            )
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(emptyList()),
            otherEventsEnabled = MutableStateFlow(true),
            labelsEnabled = MutableStateFlow(true)
        ).first()

        assertThat(result).containsExactly(ContactLabels.LABEL_ANNIVERSARY)
    }

    @Test
    fun whenContactsWithoutBirthdayAndPseudoLabelNotHidden_includesNoBirthdayLabel() = runTest {
        // Scenario 4: Contacts without birthday present + pseudo-label not hidden -> LABEL_NO_BIRTHDAY appears
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Alice", birthday = null)
        )
        val configs = listOf(
            LabelConfig(name = ContactLabels.LABEL_NO_BIRTHDAY, isHiddenFromFilter = false, isIgnored = false, isSystem = true)
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(false),
            labelsEnabled = MutableStateFlow(true)
        ).first()

        assertThat(result).containsExactly(ContactLabels.LABEL_NO_BIRTHDAY)
    }

    @Test
    fun whenMixOfUserLabelsPseudoLabelAndSystemLabels_returnsCorrectlyOrderedList() = runTest {
        // Scenario 5: Mixture of User-Labels, Pseudo-Label and System-Labels -> correct order
        // Order: User-Labels alphabetically -> LABEL_NO_BIRTHDAY -> LABEL_ANNIVERSARY -> LABEL_NAME_DAY
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                birthday = null, // triggers LABEL_NO_BIRTHDAY
                anniversary = LocalDate.of(2020, 10, 10), // triggers LABEL_ANNIVERSARY
                nameDay = LocalDate.of(2020, 10, 10), // triggers LABEL_NAME_DAY
                labels = listOf("Work", "Family") // user labels (unsorted order to verify alphabetic sort)
            )
        )
        val configs = listOf(
            LabelConfig(name = "Family", isHiddenFromFilter = false, isIgnored = false, isSystem = false),
            LabelConfig(name = "Work", isHiddenFromFilter = false, isIgnored = false, isSystem = false),
            LabelConfig(name = ContactLabels.LABEL_NO_BIRTHDAY, isHiddenFromFilter = false, isIgnored = false, isSystem = true)
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(true),
            labelsEnabled = MutableStateFlow(true)
        ).first()

        assertThat(result).containsExactly(
            "Family",
            "Work",
            ContactLabels.LABEL_NO_BIRTHDAY,
            ContactLabels.LABEL_ANNIVERSARY,
            ContactLabels.LABEL_NAME_DAY
        ).inOrder()
    }
}
