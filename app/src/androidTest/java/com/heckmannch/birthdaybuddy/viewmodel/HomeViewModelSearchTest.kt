package com.heckmannch.birthdaybuddy.viewmodel

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
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
class HomeViewModelSearchTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private val timeRepository: TimeRepository = mock()
    private val mapper = ContactMapper()

    @Before
    fun setup() {
        // Basiskonfiguration für die Mocks
        whenever(contactRepository.allContacts).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.labelConfigs).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.otherEventsEnabled).doReturn(MutableStateFlow(false))
        whenever(timeRepository.currentDate).doReturn(MutableStateFlow(LocalDate.of(2024, 5, 15)))
    }

    @Test
    fun searchLogic_findsContactsRegardlessOfNameOrder() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "1",
                fullName = "Max Mustermann",
                birthday = LocalDate.of(1990, 1, 1)
            ),
            Contact(
                contactId = "2",
                lookupKey = "2",
                fullName = "Erika Muster",
                birthday = LocalDate.of(1995, 1, 1)
            )
        )
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))

        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
        )

        // Suche nach "Mustermann Max"
        viewModel.onSearchQueryChange("Mustermann Max")

        // Warten bis der State die Suche reflektiert und Ergebnisse liefert
        val state = viewModel.uiState
            .filter { (it.searchQuery == "Mustermann Max") && (it.contacts != null) }
            .first()

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun searchLogic_ignoresLeadingAndTrailingSpaces() = runTest {
        val contacts = listOf(
            Contact(
                contactId = "1",
                lookupKey = "1",
                fullName = "Max Mustermann",
                birthday = LocalDate.of(1990, 1, 1)
            )
        )
        whenever(contactRepository.allContacts).thenReturn(MutableStateFlow(contacts))

        val viewModel = HomeViewModel(
            contactRepository = contactRepository,
            mapper = mapper,
            timeRepository = timeRepository,
        )

        viewModel.onSearchQueryChange("  Max  ")

        val state = viewModel.uiState
            .filter { (it.searchQuery == "  Max  ") && (it.contacts != null) }
            .first()

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }
}
