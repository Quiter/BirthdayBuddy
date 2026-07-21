package com.heckmannch.birthdaybuddy.ui.screens.settings.sync

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val contactRepository: ContactRepository = mock()
    private val clock: Clock = mock()

    private lateinit var viewModel: SyncViewModel

    @Before
    fun setup() {
        whenever(clock.currentTimeMillis()).thenReturn(1000L)
        viewModel = SyncViewModel(contactRepository, clock)
    }

    @Test
    fun `syncContacts clears ignored couple pairs, calls sync, and emits syncCompletedEvent`() = runTest {
        var eventEmitted = false
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.syncCompletedEvent.collect {
                eventEmitted = true
            }
        }

        viewModel.syncContacts()
        testScheduler.advanceUntilIdle()

        verify(contactRepository).clearIgnoredCouplePairs()
        verify(contactRepository).syncContacts()
        assertThat(eventEmitted).isTrue()

        job.cancel()
    }
}
