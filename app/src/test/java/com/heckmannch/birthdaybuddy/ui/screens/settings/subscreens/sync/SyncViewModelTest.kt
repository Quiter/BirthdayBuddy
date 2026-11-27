package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.sync

import android.content.ContentResolver
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.data.local.ContactDao
import com.heckmannch.birthdaybuddy.data.local.ContactUserDataDao
import com.heckmannch.birthdaybuddy.data.local.LabelConfigDao
import com.heckmannch.birthdaybuddy.data.mapper.ContactDbMapper
import com.heckmannch.birthdaybuddy.data.mapper.LabelConfigMapper
import com.heckmannch.birthdaybuddy.data.repository.ContactRepositoryImpl
import com.heckmannch.birthdaybuddy.data.repository.SystemContactDataSource
import com.heckmannch.birthdaybuddy.domain.model.AppSettings
import com.heckmannch.birthdaybuddy.domain.permission.PermissionChecker
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.repository.WidgetUpdater
import com.heckmannch.birthdaybuddy.util.Clock
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
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
    private val coupleRepository: CoupleRepository = mock()
    private val clock: Clock = mock()

    private lateinit var viewModel: SyncViewModel

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        whenever(clock.currentTimeMillis()).thenReturn(1000L)
        viewModel = SyncViewModel(contactRepository, coupleRepository, clock)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `syncContacts clears ignored couple pairs, calls sync, and emits Success event`() =
        runTest {
            var event: SyncEvent? = null

            val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect {
                    event = it
                }
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            verify(coupleRepository).clearIgnoredCouplePairs()
            verify(contactRepository).syncContacts()
            assertThat(event).isEqualTo(SyncEvent.Success)
            assertThat(viewModel.uiState.value.isSyncing).isFalse()

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

            val receivedEvents = mutableListOf<SyncEvent>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect {
                    receivedEvents.add(it)
                }
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            assertThat(receivedEvents).containsExactly(SyncEvent.Error(errorMsg))
            assertThat(viewModel.uiState.value.isSyncing).isFalse()

            job.cancel()
        }

    @Test
    fun `syncContacts with real ContactRepositoryImpl emits Error event when contact provider fails`() =
        runTest {
            val permissionChecker: PermissionChecker = mock()
            val contactDao: ContactDao = mock()
            val labelConfigDao: LabelConfigDao = mock()
            val contactUserDataDao: ContactUserDataDao = mock()
            val systemContactDataSource: SystemContactDataSource = mock()
            val settingsRepository: SettingsRepository = mock()
            val calendarSyncRepository: CalendarSyncRepository = mock()
            val widgetUpdater: WidgetUpdater = mock()
            val contentResolver: ContentResolver = mock()

            whenever(contactDao.getAllContacts()).thenReturn(MutableStateFlow(emptyList()))
            whenever(labelConfigDao.getAllConfigs()).thenReturn(MutableStateFlow(emptyList()))
            whenever(settingsRepository.settings).thenReturn(MutableStateFlow(AppSettings()))
            whenever(permissionChecker.hasContactsPermission()).thenReturn(true)
            whenever(systemContactDataSource.fetchContactGroups())
                .thenThrow(RuntimeException("Contacts provider unavailable"))

            val realRepository = ContactRepositoryImpl(
                permissionChecker = permissionChecker,
                contactDao = contactDao,
                labelConfigDao = labelConfigDao,
                contactUserDataDao = contactUserDataDao,
                systemContactDataSource = systemContactDataSource,
                settingsRepository = settingsRepository,
                calendarSyncRepository = calendarSyncRepository,
                widgetUpdater = widgetUpdater,
                contactDbMapper = ContactDbMapper(),
                labelConfigMapper = LabelConfigMapper(),
                contentResolver = contentResolver,
                ioDispatcher = mainDispatcherRule.testDispatcher,
                defaultDispatcher = mainDispatcherRule.testDispatcher,
            )
            val realViewModel = SyncViewModel(realRepository, coupleRepository, clock)

            val receivedEvents = mutableListOf<SyncEvent>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                realViewModel.events.collect { receivedEvents.add(it) }
            }

            realViewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            assertThat(receivedEvents).containsExactly(SyncEvent.Error("Contacts provider unavailable"))
            assertThat(realViewModel.uiState.value.isSyncing).isFalse()

            job.cancel()
        }

    @Test
    fun `syncContacts resets isSyncing and does not emit Error when CancellationException occurs`() =
        runTest {
            whenever(contactRepository.syncContacts()).thenAnswer {
                throw CancellationException("Sync cancelled")
            }

            val receivedEvents = mutableListOf<SyncEvent>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { receivedEvents.add(it) }
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            assertThat(receivedEvents).isEmpty()
            assertThat(viewModel.uiState.value.isSyncing).isFalse()

            job.cancel()
        }

    @Test
    fun `syncContacts buffers Success event when no collector is active and delivers on resume`() =
        runTest {
            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            val receivedEvents = mutableListOf<SyncEvent>()
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { receivedEvents.add(it) }
            }
            testScheduler.advanceUntilIdle()

            assertThat(receivedEvents).containsExactly(SyncEvent.Success)
            job.cancel()
        }

    @Test
    fun `syncContacts delivers buffered Error event after collector pauses and resumes without replaying consumed events`() =
        runTest {
            val firstCollectorEvents = mutableListOf<SyncEvent>()
            val firstJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { firstCollectorEvents.add(it) }
            }

            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()
            assertThat(firstCollectorEvents).containsExactly(SyncEvent.Success)

            // Simulate lifecycle pause / configuration change
            firstJob.cancel()

            whenever(contactRepository.syncContacts()).thenAnswer {
                throw IOException("Sync failed while paused")
            }
            viewModel.syncContacts()
            testScheduler.advanceUntilIdle()

            // Simulate lifecycle resume
            val resumedCollectorEvents = mutableListOf<SyncEvent>()
            val resumedJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { resumedCollectorEvents.add(it) }
            }
            testScheduler.advanceUntilIdle()

            assertThat(resumedCollectorEvents).containsExactly(SyncEvent.Error("Sync failed while paused"))
            resumedJob.cancel()
        }
}


