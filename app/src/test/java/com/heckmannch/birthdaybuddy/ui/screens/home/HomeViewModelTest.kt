package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.ContactLabels
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetAvailableLabelsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetContactsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.IgnoreCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.LinkAsCoupleUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UnlinkCoupleUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel

    @After
    fun tearDown() = runTest {
        if (::viewModel.isInitialized) {
            val job = viewModel.viewModelScope.coroutineContext[Job]
            job?.cancel()
            job?.join()
        }
    }


    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private val timeRepository: TimeRepository = mock()
    private val getContactsUseCase = GetContactsUseCase(ContactMapper())
    private val getAvailableLabelsUseCase = GetAvailableLabelsUseCase()
    private val getCoupleSuggestionUseCase = GetCoupleSuggestionUseCase(contactRepository)
    private val linkAsCoupleUseCase = LinkAsCoupleUseCase(contactRepository)
    private val unlinkCoupleUseCase = UnlinkCoupleUseCase(contactRepository)
    private val ignoreCoupleSuggestionUseCase = IgnoreCoupleSuggestionUseCase(contactRepository)
    private val today = LocalDate.of(2024, 5, 15)

    @Before
    fun setup() {
        whenever(timeRepository.currentDate).doReturn(MutableStateFlow(today))
        whenever(contactRepository.labelConfigs).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.allContacts).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.potentialCouples).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.otherEventsEnabled).doReturn(MutableStateFlow(false))
        whenever(contactRepository.ignoredCouplePairs).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.labelsEnabled).doReturn(MutableStateFlow(true))
    }

    @Test
    fun initialState_isCorrect() = runTest {
        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )
        val state = viewModel.uiState.first()

        assertThat(state.searchQuery).isEmpty()
        assertThat(state.selectedLabel).isNull()
        assertThat(state.isSyncing).isFalse()
    }

    @Test
    fun onLabelSelected_updatesState() = runTest {
        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )
        viewModel.onIntent(HomeIntent.LabelSelected("Freunde"))

        val state = viewModel.uiState.first { it.selectedLabel == "Freunde" }
        assertThat(state.selectedLabel).isEqualTo("Freunde")
    }

    @Test
    fun labelFiltering_worksCorrectly() = runTest {
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
            )
        )
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )

        viewModel.onIntent(HomeIntent.LabelSelected("Freunde"))
        val state = viewModel.uiState.first { state ->
            state.selectedLabel == "Freunde" && state.contacts?.size == 1
        }

        assertThat(state.contacts?.first()?.fullName).isEqualTo("Friend")
    }

    @Test
    fun ignoredLabels_areExcludedFromList() = runTest {
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
            )
        )
        val labelConfigs = listOf(
            LabelConfig(name = "Ignored", isIgnored = true)
        )
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))
        whenever(contactRepository.labelConfigs).thenReturn(MutableStateFlow(labelConfigs))

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )
        val state = viewModel.uiState.first { (it.contacts != null) }

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Visible")
    }

    @Test
    fun otherEventsFiltering_worksCorrectly() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "k1",
                fullName = "Anniversary Person",
                birthday = today,
                anniversary = today.plusDays(5),
                nameDay = null
            ),
            Contact(
                contactId = "2",
                lookupKey = "k2",
                fullName = "Name Day Person",
                birthday = today,
                anniversary = null,
                nameDay = today.plusDays(10)
            )
        )
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))
        whenever(contactRepository.otherEventsEnabled).thenReturn(MutableStateFlow(true))

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )

        // Verify availableLabels contains anniversary and name_day labels
        val initialLabelsState = viewModel.uiState.first { it.availableLabels.isNotEmpty() }
        assertThat(initialLabelsState.availableLabels).contains(ContactLabels.LABEL_ANNIVERSARY)
        assertThat(initialLabelsState.availableLabels).contains(ContactLabels.LABEL_NAME_DAY)

        // Select Anniversary label
        viewModel.onIntent(HomeIntent.LabelSelected(ContactLabels.LABEL_ANNIVERSARY))

        val anniversaryState = viewModel.uiState.first { state ->
            state.selectedLabel == ContactLabels.LABEL_ANNIVERSARY &&
                    state.contacts?.size == 1 &&
                    state.contacts.first().fullName == "Anniversary Person"
        }
        assertThat(anniversaryState.contacts?.first()?.daysUntilNext).isEqualTo(5)
    }

    @Test
    fun whenLabelsDisabled_thenAvailableLabelsIsEmptyAndContactsHaveNoLabels() = runTest {
        val labelConfigs = listOf(
            LabelConfig("Family", isHiddenFromFilter = false, isIgnored = true)
        )
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "1",
                fullName = "Test Contact",
                birthday = today.plusDays(5),
                labels = listOf("Family")
            )
        )
        whenever(contactRepository.labelConfigs).thenReturn(MutableStateFlow(labelConfigs))
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))
        whenever(contactRepository.labelsEnabled).thenReturn(MutableStateFlow(false))

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )

        // Wait for UI State to propagate contacts
        val state = viewModel.uiState.first { it.contacts != null }

        // 1. Available labels should be empty because label management is disabled
        assertThat(state.availableLabels).isEmpty()

        // 2. Mapped contacts should not contain the ignored label in UI, and should not be filtered out (even if marked ignored)
        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.labels).isEmpty()
    }

    @Test
    fun whenLabelsDisabled_thenMainListCombinesBirthdaysAnniversariesAndNameDays() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "1",
                fullName = "Birthday Person",
                birthday = today.plusDays(5),
                anniversary = null,
                nameDay = null
            ),
            Contact(
                contactId = "2",
                lookupKey = "2",
                fullName = "Anniversary Person",
                birthday = null,
                anniversary = today.plusDays(10),
                nameDay = null
            ),
            Contact(
                contactId = "3",
                lookupKey = "3",
                fullName = "NameDay Person",
                birthday = null,
                anniversary = null,
                nameDay = today.plusDays(15)
            )
        )
        whenever(contactRepository.labelConfigs).thenReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))
        whenever(contactRepository.labelsEnabled).thenReturn(MutableStateFlow(false))
        whenever(contactRepository.otherEventsEnabled).thenReturn(MutableStateFlow(true))

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )

        // Wait for UI State to propagate contacts
        val state = viewModel.uiState.first { it.contacts != null && it.contacts.size == 3 }

        // Main list should contain 3 events, sorted by daysUntilNext
        val uiContacts = state.contacts!!
        assertThat(uiContacts).hasSize(3)
        assertThat(uiContacts[0].fullName).isEqualTo("Birthday Person")
        assertThat(uiContacts[1].fullName).isEqualTo("Anniversary Person")
        assertThat(uiContacts[2].fullName).isEqualTo("NameDay Person")
    }

    @Test
    fun whenLabelsDisabledAndOtherEventsDisabled_thenMainListOnlyContainsBirthdays() = runTest {
        val contactsList = listOf(
            Contact(
                contactId = "1",
                lookupKey = "1",
                fullName = "Birthday Person",
                birthday = today.plusDays(5),
                anniversary = null,
                nameDay = null
            ),
            Contact(
                contactId = "2",
                lookupKey = "2",
                fullName = "Anniversary Person",
                birthday = null,
                anniversary = today.plusDays(10),
                nameDay = null
            ),
            Contact(
                contactId = "3",
                lookupKey = "3",
                fullName = "NameDay Person",
                birthday = null,
                anniversary = null,
                nameDay = today.plusDays(15)
            )
        )
        whenever(contactRepository.labelConfigs).thenReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contactsList))
        whenever(contactRepository.labelsEnabled).thenReturn(MutableStateFlow(false))
        whenever(contactRepository.otherEventsEnabled).thenReturn(MutableStateFlow(false))

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
        )

        // Wait for UI State to propagate contacts
        val state = viewModel.uiState.first { it.contacts != null && it.contacts.size == 1 }

        // Main list should contain only 1 event
        val contacts = state.contacts!!
        assertThat(contacts).hasSize(1)
        assertThat(contacts[0].fullName).isEqualTo("Birthday Person")
    }
}
