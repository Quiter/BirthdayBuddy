package com.heckmannch.birthdaybuddy.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.local.LabelConfig
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private val timeRepository: TimeRepository = mock()
    private val mapper = ContactMapper()
    private val today = LocalDate.of(2024, 5, 15)

    @Before
    fun setup() {
        whenever(timeRepository.currentDate).doReturn(MutableStateFlow(today))
        whenever(contactRepository.labelConfigs).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.allContacts).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.otherEventsEnabled).doReturn(MutableStateFlow(false))
        whenever(contactRepository.ignoredCouplePairs).doReturn(MutableStateFlow(emptyList()))
    }

    @Test
    fun initialState_isCorrect() = runTest {
        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
        )
        val state = viewModel.uiState.first()

        assertThat(state.searchQuery).isEmpty()
        assertThat(state.selectedLabel).isNull()
        assertThat(state.isSyncing).isFalse()
    }

    @Test
    fun onLabelSelected_updatesState() = runTest {
        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
        )
        viewModel.onLabelSelected("Freunde")

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

        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
        )

        viewModel.onLabelSelected("Freunde")
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

        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
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

        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
        )

        // Verify availableLabels contains anniversary and name_day labels
        val initialLabelsState = viewModel.uiState.first { it.availableLabels.isNotEmpty() }
        assertThat(initialLabelsState.availableLabels).contains(HomeViewModel.LABEL_ANNIVERSARY)
        assertThat(initialLabelsState.availableLabels).contains(HomeViewModel.LABEL_NAME_DAY)

        // Select Anniversary label
        viewModel.onLabelSelected(HomeViewModel.LABEL_ANNIVERSARY)

        val anniversaryState = viewModel.uiState.first { state ->
            state.selectedLabel == HomeViewModel.LABEL_ANNIVERSARY &&
                    state.contacts?.size == 1 &&
                    state.contacts.first().fullName == "Anniversary Person"
        }
        assertThat(anniversaryState.contacts?.first()?.daysUntilNext).isEqualTo(5)
    }
}
