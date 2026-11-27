package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.labels

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetCategorizedLabelsUseCase
import com.heckmannch.birthdaybuddy.ui.mapper.ContactUiMapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class LabelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private val timeRepository: TimeRepository = mock()
    private val contactUiMapper = ContactUiMapper()
    private lateinit var getCategorizedLabelsUseCase: GetCategorizedLabelsUseCase

    private val today = LocalDate.of(2026, 5, 15)

    @Before
    fun setUp() {
        getCategorizedLabelsUseCase = GetCategorizedLabelsUseCase(mainDispatcherRule.testDispatcher)
        whenever(timeRepository.currentDate).thenReturn(flowOf(today))
        whenever(contactRepository.otherEventsEnabled).thenReturn(flowOf(false))
        whenever(contactRepository.disabledOtherEventLabels).thenReturn(flowOf(emptySet()))
    }

    private fun createViewModel() = LabelViewModel(
        contactRepository = contactRepository,
        getCategorizedLabelsUseCase = getCategorizedLabelsUseCase,
        contactUiMapper = contactUiMapper,
        timeRepository = timeRepository,
        defaultDispatcher = mainDispatcherRule.testDispatcher,
    )

    @Test
    fun `uiState should categorize labels into active, otherEvents, hidden, and ignored sections with assigned contacts`() = runTest {
        // Given
        val customEventKey = ContactLabels.customEventLabel("Jahrestag")
        val configs = listOf(
            LabelConfig("Family", isHiddenFromFilter = false, isIgnored = false),
            LabelConfig("Friends", isHiddenFromFilter = true, isIgnored = false),
            LabelConfig(
                ContactLabels.LABEL_NO_BIRTHDAY,
                isHiddenFromFilter = false,
                isIgnored = true,
            ),
        )
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Alice",
                labels = listOf("Family"),
                birthday = today,
                anniversary = LocalDate.of(2020, 6, 1),
                customEvents = mapOf(customEventKey to LocalDate.of(2022, 8, 10)),
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Bob",
                labels = listOf("Friends"),
                birthday = null,
            ),
        )

        whenever(contactRepository.labelConfigs).thenReturn(flowOf(configs))
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        whenever(contactRepository.otherEventsEnabled).thenReturn(flowOf(true))

        val viewModel = createViewModel()

        // When
        val state = viewModel.uiState.first { it.activeLabels.isNotEmpty() }

        // Then
        assertThat(state.labelsEnabled).isTrue()
        assertThat(state.allLabeledContactsCount).isEqualTo(2)

        assertThat(state.activeLabels.map { it.label.name }).containsExactly("Family")
        assertThat(state.activeLabels.first().assignedContacts.map { it.fullName })
            .containsExactly("Alice")

        assertThat(state.otherEventLabels.map { it.label.name }).containsExactly(
            ContactLabels.LABEL_ANNIVERSARY,
            customEventKey,
        ).inOrder()
        assertThat(state.otherEventLabels[0].assignedContacts.map { it.fullName })
            .containsExactly("Alice")
        assertThat(state.otherEventLabels[1].assignedContacts.map { it.fullName })
            .containsExactly("Alice")

        assertThat(state.hiddenLabels.map { it.label.name }).containsExactly("Friends")
        assertThat(state.hiddenLabels.first().assignedContacts.map { it.fullName })
            .containsExactly("Bob")

        assertThat(state.ignoredLabels.map { it.label.name })
            .containsExactly(ContactLabels.LABEL_NO_BIRTHDAY)
        assertThat(state.ignoredLabels.first().label.isIgnored).isTrue()
        assertThat(state.ignoredLabels.first().assignedContacts.map { it.fullName })
            .containsExactly("Bob")
    }

    @Test
    fun `uiState should reflect labelsEnabled false`() = runTest {
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(false))

        val viewModel = createViewModel()

        val state = viewModel.uiState.first { !it.labelsEnabled }
        assertThat(state.labelsEnabled).isFalse()
        assertThat(state.activeLabels).isEmpty()
        assertThat(state.otherEventLabels).isEmpty()
        assertThat(state.hiddenLabels).isEmpty()
        assertThat(state.ignoredLabels).isEmpty()
    }

    @Test
    fun `updateLabelConfig should delegate to repository`() = runTest {
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(
            LabelIntent.UpdateLabelConfig(
                name = "Test",
                hidden = true,
                ignored = false,
                isSystem = true,
                notificationsEnabled = false,
                showInWidget = true,
            )
        )

        // Then
        verify(contactRepository).updateLabelConfig(
            LabelConfig(
                name = "Test",
                isHiddenFromFilter = true,
                isIgnored = false,
                isSystem = true,
                notificationsEnabled = false,
                showInWidget = true,
            )
        )
    }

    @Test
    fun `setLabelsEnabled should delegate to repository`() = runTest {
        whenever(contactRepository.labelConfigs).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(flowOf(emptyList()))
        whenever(contactRepository.labelsEnabled).thenReturn(flowOf(true))
        val viewModel = createViewModel()

        // When
        viewModel.onIntent(LabelIntent.SetLabelsEnabled(false))

        // Then
        verify(contactRepository).updateLabelsEnabled(false)
    }
}
