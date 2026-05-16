package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.AppSettings
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.data.repository.TimeRepository
import com.heckmannch.birthdaybuddy.data.mapper.ContactMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelSearchTest {

    private val contactRepository: ContactRepository = mock()
    private val notificationRepository: NotificationRepository = mock()
    private val timeRepository: TimeRepository = mock()
    private lateinit var context: Context
    private val mapper = ContactMapper()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Basiskonfiguration für die Mocks
        whenever(contactRepository.allContacts).doReturn(flowOf(emptyList()))
        whenever(contactRepository.labelConfigs).doReturn(flowOf(emptyList()))
        whenever(notificationRepository.settings).doReturn(flowOf(AppSettings()))
        whenever(notificationRepository.allRules).doReturn(flowOf(emptyList()))
        whenever(timeRepository.currentDate).doReturn(flowOf(LocalDate.of(2024, 5, 15)))
    }

    @Test
    fun searchLogic_findsContactsRegardlessOfNameOrder() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "1", fullName = "Max Mustermann", birthday = LocalDate.of(1990, 1, 1)),
            Contact(contactId = "2", lookupKey = "2", fullName = "Erika Muster", birthday = LocalDate.of(1995, 1, 1))
        )
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))

        val viewModel = HomeViewModel(context, contactRepository, notificationRepository, mapper, timeRepository)
        
        // Suche nach "Mustermann Max"
        viewModel.onSearchQueryChange("Mustermann Max")

        // Warten bis der State die Suche reflektiert und Ergebnisse liefert
        val state = viewModel.uiState
            .filter { it.searchQuery == "Mustermann Max" && it.contacts != null }
            .first()

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun searchLogic_ignoresLeadingAndTrailingSpaces() = runTest {
        val contacts = listOf(
            Contact(contactId = "1", lookupKey = "1", fullName = "Max Mustermann", birthday = LocalDate.of(1990, 1, 1))
        )
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))

        val viewModel = HomeViewModel(context, contactRepository, notificationRepository, mapper, timeRepository)
        
        viewModel.onSearchQueryChange("  Max  ")

        val state = viewModel.uiState
            .filter { it.searchQuery == "  Max  " && it.contacts != null }
            .first()

        assertThat(state.contacts).hasSize(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }
}
