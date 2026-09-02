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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

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
    fun `syncContacts clears ignored couple pairs, calls sync, and emits syncCompletedEvent and Success event`() =
        runTest {
            var legacyEventEmitted = false
            var event: SyncEvent? = null

            val legacyJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.syncCompletedEvent.collect {
                    legacyEventEmitted = true
                }
            }
            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect {
                    event = it
                }
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            verify(contactRepository).clearIgnoredCouplePairs()
            verify(contactRepository).syncContacts()
            assertThat(legacyEventEmitted).isTrue()
            assertThat(event).isEqualTo(SyncEvent.Success)
            assertThat(viewModel.uiState.value.isSyncing).isFalse()

            legacyJob.cancel()
            eventJob.cancel()
        }

    @Test
    fun `syncContacts prevents parallel syncs when isSyncing is true`() =
        runTest {
            whenever(contactRepository.syncContacts()).thenAnswer {
                assertThat(viewModel.uiState.value.isSyncing).isTrue()
                // Attempt second sync while the first is running
                viewModel.syncContacts()
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            verify(contactRepository, times(1)).syncContacts()
            assertThat(viewModel.uiState.value.isSyncing).isFalse()
        }

    @Test
    fun `syncContacts emits Error event and resets isSyncing when exception occurs`() =
        runTest {
            val errorMsg = "Network timeout"
            whenever(contactRepository.syncContacts()).thenAnswer {
                throw IOException(errorMsg)
            }

            var receivedEvent: SyncEvent? = null
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect {
                    receivedEvent = it
                }
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            assertThat(receivedEvent).isEqualTo(SyncEvent.Error(errorMsg))
            assertThat(viewModel.uiState.value.isSyncing).isFalse()

            job.cancel()
        }
}

