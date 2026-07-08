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
    fun whenLabelsEnabled_includesAllCategoriesCorrectlySortedAndOrdered() = runTest {
        // Alice: birthday = null, has "Family" label
        // Bob: has birthday, anniversary, and "Colleagues" label
        // Charlie: has nameDay and a hidden/ignored label "Secret"
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "k1", fullName = "Alice", birthday = null, labels = listOf("Family")),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Bob",
                birthday = LocalDate.of(1995, 1, 1),
                anniversary = LocalDate.of(2020, 10, 10),
                labels = listOf("Colleagues")
            ),
            Contact(
                contactId = "3",
                lookupKey = "k3",
                fullName = "Charlie",
                nameDay = LocalDate.of(2000, 3, 3),
                labels = listOf("Secret")
            )
        )

        val configs = listOf(
            LabelConfig(name = "Family", isHiddenFromFilter = false, isIgnored = false, isSystem = false),
            LabelConfig(name = "Colleagues", isHiddenFromFilter = false, isIgnored = false, isSystem = false),
            LabelConfig(name = "Secret", isHiddenFromFilter = true, isIgnored = false, isSystem = false),
            LabelConfig(name = ContactLabels.LABEL_NO_BIRTHDAY, isHiddenFromFilter = false, isIgnored = false, isSystem = true)
        )

        val result = useCase(
            contacts = MutableStateFlow(contacts),
            configs = MutableStateFlow(configs),
            otherEventsEnabled = MutableStateFlow(true),
            labelsEnabled = MutableStateFlow(true)
        ).first()

        // 1. User labels (Family, Colleagues) are included and sorted alphabetically -> Colleagues, Family
        // 2. Secret is hidden, so excluded
        // 3. No Birthday is included because Alice has birthday = null -> LABEL_NO_BIRTHDAY
        // 4. Anniversary is included because Bob has anniversary != null -> LABEL_ANNIVERSARY
        // 5. Name Day is included because Charlie has nameDay != null -> LABEL_NAME_DAY
        // Expected order: User Labels sorted, then No Birthday, then Anniversary, then Name Day
        assertThat(result).containsExactly(
            "Colleagues",
            "Family",
            ContactLabels.LABEL_NO_BIRTHDAY,
            ContactLabels.LABEL_ANNIVERSARY,
            ContactLabels.LABEL_NAME_DAY
        ).inOrder()
    }
}
