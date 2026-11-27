package com.heckmannch.birthdaybuddy.ui.screens.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import com.heckmannch.birthdaybuddy.domain.repository.GiftIdeaRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetAvailableLabelsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetContactsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.IgnoreCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.LinkAsCoupleUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UnlinkCoupleUseCase
import com.heckmannch.birthdaybuddy.ui.mapper.ContactUiMapper
import com.heckmannch.birthdaybuddy.ui.mapper.CoupleSuggestionUiMapper
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelGiftIdeaTest {

    private lateinit var viewModel: HomeViewModel

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock dependencies with mockk(relaxed = true)
    private val contactRepository: ContactRepository = mockk(relaxed = true)
    private val giftIdeaRepository: GiftIdeaRepository = mockk(relaxed = true)
    private val coupleRepository: CoupleRepository = mockk(relaxed = true)
    private val getContactsUseCase: GetContactsUseCase = mockk(relaxed = true)
    private val getAvailableLabelsUseCase: GetAvailableLabelsUseCase = mockk(relaxed = true)
    private val getCoupleSuggestionUseCase: GetCoupleSuggestionUseCase = mockk(relaxed = true)
    private val linkAsCoupleUseCase: LinkAsCoupleUseCase = mockk(relaxed = true)
    private val unlinkCoupleUseCase: UnlinkCoupleUseCase = mockk(relaxed = true)
    private val ignoreCoupleSuggestionUseCase: IgnoreCoupleSuggestionUseCase = mockk(relaxed = true)
    private val timeRepository: TimeRepository = mockk(relaxed = true)
    private val permissionChecker: PermissionChecker = mockk(relaxed = true)
    private val clock = TestClock()

    private val today = LocalDate.of(2024, 5, 15)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        // Stub standard flows accessed on HomeViewModel initialization
        every { timeRepository.currentDate } returns MutableStateFlow(today)
        every { contactRepository.labelConfigs } returns MutableStateFlow(emptyList())
        every { contactRepository.allContacts } returns MutableStateFlow(emptyList())
        every { contactRepository.otherEventsEnabled } returns MutableStateFlow(false)
        every { contactRepository.labelsEnabled } returns MutableStateFlow(true)
        every { contactRepository.contactChanges } returns emptyFlow()

        // Stub use case operator functions returning flows to prevent flow combine hangs
        every { getContactsUseCase(any(), any(), any(), any()) } returns MutableStateFlow(
            emptyList()
        )
        every { getAvailableLabelsUseCase(any(), any(), any(), any()) } returns MutableStateFlow(
            emptyList()
        )
        every { getCoupleSuggestionUseCase(any()) } returns MutableStateFlow(null)
        every { permissionChecker.hasContactsPermission() } returns true

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            giftIdeaRepository = giftIdeaRepository,
            coupleRepository = coupleRepository,
            getContactsUseCase = getContactsUseCase,
            contactUiMapper = ContactUiMapper(),
            coupleSuggestionUiMapper = CoupleSuggestionUiMapper(),
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
            permissionChecker = permissionChecker,
            clock = clock,
        )
    }

    private class TestClock(var time: Long = 0L) : Clock {
        override fun currentTimeMillis(): Long = time
    }

    @After
    fun tearDown() = runTest {
        // Correctly cancel viewModelScope to prevent background flow leaks
        if (::viewModel.isInitialized) {
            val job = viewModel.viewModelScope.coroutineContext[Job]
            job?.cancel()
            job?.join()
        }
        unmockkStatic(Log::class)
    }

    @Test
    fun addGiftIdea_delegatesToRepository_andEmitsNewlyAddedIdeaEvent() = runTest {
        val lookupKey = "test_lookup_key"
        val giftIdeaSlot = slot<GiftIdea>()

        val events = mutableListOf<HomeUiEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        // Dispatch AddGiftIdea intent
        viewModel.onIntent(HomeIntent.AddGiftIdea(lookupKey))

        // Verify delegation to giftIdeaRepository with a new empty GiftIdea
        coVerify {
            giftIdeaRepository.addGiftIdea(eq(lookupKey), capture(giftIdeaSlot))
        }

        val capturedIdea = giftIdeaSlot.captured
        assertThat(capturedIdea.text).isEmpty()

        // Verify that eventFlow emits FocusNewlyAddedIdea to match the new gift idea's ID
        val event = events.filterIsInstance<HomeUiEvent.FocusNewlyAddedIdea>().firstOrNull()
        assertThat(event).isNotNull()
        assertThat(event?.ideaId).isEqualTo(capturedIdea.id)

        collectJob.cancel()
    }

    @Test
    fun toggleGiftIdea_delegatesToRepository() = runTest {
        val lookupKey = "test_lookup_key"
        val giftIdea = GiftIdea(id = "idea_123", text = "Original Text", isChecked = false)
        val isChecked = true

        // Dispatch ToggleGiftIdea intent
        viewModel.onIntent(HomeIntent.ToggleGiftIdea(lookupKey, giftIdea, isChecked))

        // Verify delegation to giftIdeaRepository
        coVerify {
            giftIdeaRepository.toggleGiftIdea(eq(lookupKey), eq(giftIdea), eq(isChecked))
        }
    }

    @Test
    fun deleteGiftIdea_delegatesToRepository() = runTest {
        val lookupKey = "test_lookup_key"
        val ideaId = "idea_123"

        // Dispatch DeleteGiftIdea intent
        viewModel.onIntent(HomeIntent.DeleteGiftIdea(lookupKey, ideaId))

        // Verify delegation to giftIdeaRepository
        coVerify {
            giftIdeaRepository.deleteGiftIdea(eq(lookupKey), eq(ideaId))
        }
    }

    @Test
    fun updateGiftIdeaText_delegatesToRepository() = runTest {
        val lookupKey = "test_lookup_key"
        val ideaId = "idea_123"
        val newText = "Updated Gift Idea Text"

        // Dispatch UpdateGiftIdeaText intent
        viewModel.onIntent(HomeIntent.UpdateGiftIdeaText(lookupKey, ideaId, newText))

        // Verify delegation to giftIdeaRepository
        coVerify {
            giftIdeaRepository.updateGiftIdeaText(eq(lookupKey), eq(ideaId), eq(newText))
        }
    }

    @Test
    fun updateBirthday_delegatesToRepository() = runTest {
        val contactId = "contact_abc"
        val birthday = LocalDate.of(1990, 8, 25)

        // Dispatch UpdateBirthday intent
        viewModel.onIntent(HomeIntent.UpdateBirthday(contactId, birthday))

        // Verify delegation to contactRepository
        coVerify {
            contactRepository.updateContactBirthday(eq(contactId), eq(birthday))
        }
    }

    @Test
    fun triggerSearchFocus_emitsRequestSearchFocusEvent() = runTest {
        val events = mutableListOf<HomeUiEvent>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        // Dispatch TriggerSearchFocus intent
        viewModel.onIntent(HomeIntent.TriggerSearchFocus)
        val event = events.filterIsInstance<HomeUiEvent.RequestSearchFocus>().firstOrNull()
        assertThat(event).isNotNull()

        collectJob.cancel()
    }

    @Test
    fun giftIdeaAndCoupleMutations_doNotCrash_whenRepositoryThrowsDbException() = runTest {
        val dbError = RuntimeException("Database write failed")
        val idea = GiftIdea(id = "idea_1", text = "Book", isChecked = false)
        coEvery { giftIdeaRepository.addGiftIdea(any(), any()) } throws dbError
        coEvery { giftIdeaRepository.toggleGiftIdea(any(), any(), any()) } throws dbError
        coEvery { giftIdeaRepository.deleteGiftIdea(any(), any()) } throws dbError
        coEvery { giftIdeaRepository.updateGiftIdeaText(any(), any(), any()) } throws dbError
        coEvery { contactRepository.updateContactBirthday(any(), any()) } throws dbError
        coEvery { linkAsCoupleUseCase(any(), any()) } throws dbError
        coEvery { unlinkCoupleUseCase(any()) } throws dbError
        coEvery { ignoreCoupleSuggestionUseCase(any(), any()) } throws dbError
        coEvery { contactRepository.getAllContactsImmediate() } throws dbError

        viewModel.onIntent(HomeIntent.AddGiftIdea("k1"))
        viewModel.onIntent(HomeIntent.ToggleGiftIdea("k1", idea, true))
        viewModel.onIntent(HomeIntent.DeleteGiftIdea("k1", "idea_1"))
        viewModel.onIntent(HomeIntent.UpdateGiftIdeaText("k1", "idea_1", "New text"))
        viewModel.onIntent(HomeIntent.UpdateBirthday("c1", LocalDate.of(1990, 8, 25)))
        viewModel.onIntent(HomeIntent.LinkAsCouple("k1", "k2"))
        viewModel.onIntent(HomeIntent.UnlinkCouple("k1"))
        viewModel.onIntent(HomeIntent.IgnoreCoupleSuggestion("k1", "k2"))
        viewModel.onIntent(HomeIntent.OpenBirthdayPicker("k1", 1990, 8, 25))
        advanceUntilIdle()

        verify(exactly = 9) { Log.e("HomeViewModel", any(), dbError) }
        assertThat(viewModel.viewModelScope.coroutineContext[Job]?.isActive).isTrue()
    }

    @Test
    fun syncContacts_logsErrorAndResetsIsSyncing_whenRepositoryThrows() = runTest {
        val syncError = RuntimeException("Contacts provider failed")
        coEvery { contactRepository.syncContacts() } throws syncError

        viewModel.onIntent(HomeIntent.SyncContacts(showLoading = true))
        advanceUntilIdle()

        verify(atLeast = 1) { Log.e("HomeViewModel", "Failed to sync contacts", syncError) }
        assertThat(viewModel.uiState.value.isSyncing).isFalse()
    }

    @Test
    fun launchSafe_doesNotLogCancellationException() = runTest {
        coEvery { giftIdeaRepository.deleteGiftIdea(any(), any()) } throws CancellationException("Cancelled")

        viewModel.onIntent(HomeIntent.DeleteGiftIdea("k1", "idea_1"))
        advanceUntilIdle()

        verify(exactly = 0) { Log.e(any(), any(), any()) }
        assertThat(viewModel.viewModelScope.coroutineContext[Job]?.isActive).isTrue()
    }
}
