package com.heckmannch.birthdaybuddy.viewmodel

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.database.AppSettings
import com.heckmannch.birthdaybuddy.database.Contact
import com.heckmannch.birthdaybuddy.repository.ContactRepository
import com.heckmannch.birthdaybuddy.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.repository.TimeRepository
import androidx.work.WorkManager
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.NotificationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BirthdayViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockedWorker: MockedStatic<NotificationWorker>
    private lateinit var mockedWorkManager: MockedStatic<WorkManager>
    
    private val contactRepository: ContactRepository = mock {
        on { allContacts } doReturn flowOf(emptyList())
        on { labelConfigs } doReturn flowOf(emptyList())
    }
    
    private val notificationRepository: NotificationRepository = mock {
        on { settings } doReturn flowOf(AppSettings())
        on { allRules } doReturn flowOf(emptyList())
    }
    
    private val timeRepository: TimeRepository = mock {
        on { currentDate } doReturn flowOf(LocalDate.of(2024, 5, 15))
    }
    
    private val context: Context = mock()
    private val mapper = ContactMapper()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockedWorker = mockStatic(NotificationWorker::class.java)
        mockedWorkManager = mockStatic(WorkManager::class.java)
        
        val workManager: WorkManager = mock()
        mockedWorkManager.`when`<WorkManager> { WorkManager.getInstance(any()) }.thenReturn(workManager)
    }
    
    @After
    fun tearDown() {
        mockedWorker.close()
        mockedWorkManager.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `search logic finds contacts regardless of name order`() = runTest {
        val contacts = listOf(
            Contact(fullName = "Max Mustermann", birthday = LocalDate.of(1990, 1, 1), lookupKey = "1", contactId = "1"),
            Contact(fullName = "Erika Muster", birthday = LocalDate.of(1995, 1, 1), lookupKey = "2", contactId = "2")
        )
        
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        
        val viewModel = BirthdayViewModel(context, contactRepository, notificationRepository, mapper, timeRepository)
        
        // Search for "Mustermann Max"
        viewModel.onSearchQueryChange("Mustermann Max")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertThat(state.contacts?.size).isEqualTo(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }

    @Test
    fun `search logic ignores leading and trailing spaces`() = runTest {
        val contacts = listOf(
            Contact(fullName = "Max Mustermann", birthday = LocalDate.of(1990, 1, 1), lookupKey = "1", contactId = "1")
        )
        
        whenever(contactRepository.allContacts).thenReturn(flowOf(contacts))
        
        val viewModel = BirthdayViewModel(context, contactRepository, notificationRepository, mapper, timeRepository)
        
        // Search with spaces
        viewModel.onSearchQueryChange("  Max  ")
        advanceUntilIdle()
        
        val state = viewModel.uiState.first()
        assertThat(state.contacts?.size).isEqualTo(1)
        assertThat(state.contacts?.first()?.fullName).isEqualTo("Max Mustermann")
    }
}

// Helper to use whenever with mockito-kotlin style if not imported correctly
private fun <T> whenever(methodCall: T) = org.mockito.kotlin.whenever(methodCall)
