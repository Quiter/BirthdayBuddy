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
    }

    @Test
    fun initialState_isCorrect() = runTest {
        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
            widgetUpdater = mock(),
            imagePrefetcher = mock(),
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
            widgetUpdater = mock(),
            imagePrefetcher = mock(),
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
            widgetUpdater = mock(),
            imagePrefetcher = mock(),
        )

        viewModel.onLabelSelected("Freunde")
        val state = viewModel.uiState.first { it.selectedLabel == "Freunde" && it.contacts != null }

        assertThat(state.contacts).hasSize(1)
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
            widgetUpdater = mock(),
            imagePrefetcher = mock(),
        )
        val state = viewModel.uiState.first { (it.contacts != null) }

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Visible")
    }
}
