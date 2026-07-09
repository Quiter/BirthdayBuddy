package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetAvailableLabelsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetContactsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.IgnoreCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.LinkAsCoupleUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UnlinkCoupleUseCase
import com.heckmannch.birthdaybuddy.util.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
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
class HomeViewModelSearchTest {

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
    private val context: android.content.Context = mock()
    private val clock = TestClock()

    private class TestClock(var time: Long = 0L) : Clock {
        override fun currentTimeMillis(): Long = time
    }

    @Before
    fun setup() {
        // Basiskonfiguration für die Mocks
        whenever(contactRepository.allContacts).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.potentialCouples).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.labelConfigs).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.otherEventsEnabled).doReturn(MutableStateFlow(false))
        whenever(contactRepository.ignoredCouplePairs).doReturn(MutableStateFlow(emptyList()))
        whenever(contactRepository.labelsEnabled).doReturn(MutableStateFlow(true))
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

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
            context = context,
            clock = clock,
        )

        // Suche nach "Mustermann Max"
        viewModel.onIntent(HomeIntent.SearchQueryChanged("Mustermann Max"))

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

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
            context = context,
            clock = clock,
        )

        viewModel.onIntent(HomeIntent.SearchQueryChanged("  Max  "))

        val state = viewModel.uiState
            .filter { (it.searchQuery == "  Max  ") && (it.contacts != null) }
            .first()

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }
}
