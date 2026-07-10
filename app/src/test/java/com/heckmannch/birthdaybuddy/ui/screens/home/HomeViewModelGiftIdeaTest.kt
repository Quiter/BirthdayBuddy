package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.TimeRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetAvailableLabelsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetContactsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.GetCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.IgnoreCoupleSuggestionUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.LinkAsCoupleUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UnlinkCoupleUseCase
import android.content.Context
import com.heckmannch.birthdaybuddy.ui.mapper.ContactUiMapper
import com.heckmannch.birthdaybuddy.ui.mapper.CoupleSuggestionUiMapper
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private val getContactsUseCase: GetContactsUseCase = mockk(relaxed = true)
    private val getAvailableLabelsUseCase: GetAvailableLabelsUseCase = mockk(relaxed = true)
    private val getCoupleSuggestionUseCase: GetCoupleSuggestionUseCase = mockk(relaxed = true)
    private val linkAsCoupleUseCase: LinkAsCoupleUseCase = mockk(relaxed = true)
    private val unlinkCoupleUseCase: UnlinkCoupleUseCase = mockk(relaxed = true)
    private val ignoreCoupleSuggestionUseCase: IgnoreCoupleSuggestionUseCase = mockk(relaxed = true)
    private val timeRepository: TimeRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)
    private val clock = TestClock()

    private val today = LocalDate.of(2024, 5, 15)

    @Before
    fun setup() {
        // Stub standard flows accessed on HomeViewModel initialization
        every { timeRepository.currentDate } returns MutableStateFlow(today)
        every { contactRepository.labelConfigs } returns MutableStateFlow(emptyList())
        every { contactRepository.allContacts } returns MutableStateFlow(emptyList())
        every { contactRepository.potentialCouples } returns MutableStateFlow(emptyList())
        every { contactRepository.otherEventsEnabled } returns MutableStateFlow(false)
        every { contactRepository.ignoredCouplePairs } returns MutableStateFlow(emptyList())
        every { contactRepository.labelsEnabled } returns MutableStateFlow(true)

        // Stub use case operator functions returning flows to prevent flow combine hangs
        every { getContactsUseCase(any(), any(), any(), any(), any()) } returns MutableStateFlow(emptyList())
        every { getAvailableLabelsUseCase(any(), any(), any(), any()) } returns MutableStateFlow(emptyList())
        every { getCoupleSuggestionUseCase(any()) } returns MutableStateFlow(null)

        viewModel = HomeViewModel(
            contactRepository = contactRepository,
            getContactsUseCase = getContactsUseCase,
            contactUiMapper = ContactUiMapper(),
            coupleSuggestionUiMapper = CoupleSuggestionUiMapper(),
            getAvailableLabelsUseCase = getAvailableLabelsUseCase,
            getCoupleSuggestionUseCase = getCoupleSuggestionUseCase,
            linkAsCoupleUseCase = linkAsCoupleUseCase,
            unlinkCoupleUseCase = unlinkCoupleUseCase,
            ignoreCoupleSuggestionUseCase = ignoreCoupleSuggestionUseCase,
            timeRepository = timeRepository,
            context = context,
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
    }

    @Test
    fun addGiftIdea_delegatesToRepository_andSetsNewlyAddedIdeaId() = runTest {
        val lookupKey = "test_lookup_key"
        val giftIdeaSlot = slot<GiftIdea>()

        // Dispatch AddGiftIdea intent
        viewModel.onIntent(HomeIntent.AddGiftIdea(lookupKey))

        // Verify delegation to contactRepository with a new empty GiftIdea
        coVerify {
            contactRepository.addGiftIdea(eq(lookupKey), capture(giftIdeaSlot))
        }

        val capturedIdea = giftIdeaSlot.captured
        assertThat(capturedIdea.text).isEmpty()

        // Verify that UI state updates newlyAddedIdeaId to match the new gift idea's ID
        val state = viewModel.uiState.first { it.newlyAddedIdeaId != null }
        assertThat(state.newlyAddedIdeaId).isEqualTo(capturedIdea.id)
    }

    @Test
    fun toggleGiftIdea_delegatesToRepository() = runTest {
        val lookupKey = "test_lookup_key"
        val giftIdea = GiftIdea(id = "idea_123", text = "Original Text", isChecked = false)
        val isChecked = true

        // Dispatch ToggleGiftIdea intent
        viewModel.onIntent(HomeIntent.ToggleGiftIdea(lookupKey, giftIdea, isChecked))

        // Verify delegation to contactRepository
        coVerify {
            contactRepository.toggleGiftIdea(eq(lookupKey), eq(giftIdea), eq(isChecked))
        }
    }

    @Test
    fun deleteGiftIdea_delegatesToRepository() = runTest {
        val lookupKey = "test_lookup_key"
        val ideaId = "idea_123"

        // Dispatch DeleteGiftIdea intent
        viewModel.onIntent(HomeIntent.DeleteGiftIdea(lookupKey, ideaId))

        // Verify delegation to contactRepository
        coVerify {
            contactRepository.deleteGiftIdea(eq(lookupKey), eq(ideaId))
        }
    }

    @Test
    fun updateGiftIdeaText_delegatesToRepository() = runTest {
        val lookupKey = "test_lookup_key"
        val ideaId = "idea_123"
        val newText = "Updated Gift Idea Text"

        // Dispatch UpdateGiftIdeaText intent
        viewModel.onIntent(HomeIntent.UpdateGiftIdeaText(lookupKey, ideaId, newText))

        // Verify delegation to contactRepository
        coVerify {
            contactRepository.updateGiftIdeaText(eq(lookupKey), eq(ideaId), eq(newText))
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
    fun consumeNewlyAddedIdeaId_clearsIdInUiState() = runTest {
        val lookupKey = "test_lookup_key"
        
        // 1. Populate newlyAddedIdeaId using AddGiftIdea
        viewModel.onIntent(HomeIntent.AddGiftIdea(lookupKey))
        val stateWithId = viewModel.uiState.first { it.newlyAddedIdeaId != null }
        assertThat(stateWithId.newlyAddedIdeaId).isNotNull()

        // 2. Dispatch ConsumeNewlyAddedIdeaId intent
        viewModel.onIntent(HomeIntent.ConsumeNewlyAddedIdeaId)

        // 3. Verify it resets back to null in the UI state
        val stateWithoutId = viewModel.uiState.first { it.newlyAddedIdeaId == null }
        assertThat(stateWithoutId.newlyAddedIdeaId).isNull()
    }
}
