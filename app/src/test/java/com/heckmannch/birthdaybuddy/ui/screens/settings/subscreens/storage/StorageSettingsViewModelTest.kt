package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.storage

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.MainDispatcherRule
import com.heckmannch.birthdaybuddy.domain.repository.StorageRepository
import com.heckmannch.birthdaybuddy.domain.repository.StorageUsage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class StorageSettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val storageRepository: StorageRepository = mock()

    @Test
    fun `init loads storage usage and exposes formatted values`() = runTest {
        whenever(storageRepository.getStorageUsage()).thenReturn(
            StorageUsage(databaseBytes = 2048L, cacheBytes = 1024L),
        )

        val viewModel = StorageSettingsViewModel(storageRepository)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isClearingCache).isFalse()
        assertThat(state.databaseBytes).isEqualTo(2048L)
        assertThat(state.cacheBytes).isEqualTo(1024L)
        assertThat(state.totalBytes).isEqualTo(3072L)
        assertThat(state.formattedDatabaseSize).isEqualTo("2.0 KB")
        assertThat(state.formattedCacheSize).isEqualTo("1.0 KB")
        assertThat(state.formattedTotalSize).isEqualTo("3.0 KB")
    }

    @Test
    fun `refreshStorageUsage updates metrics with fresh values`() = runTest {
        whenever(storageRepository.getStorageUsage()).thenReturn(
            StorageUsage(databaseBytes = 1000L, cacheBytes = 500L),
            StorageUsage(databaseBytes = 4096L, cacheBytes = 2048L),
        )

        val viewModel = StorageSettingsViewModel(storageRepository)
        testScheduler.advanceUntilIdle()

        viewModel.refreshStorageUsage()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.databaseBytes).isEqualTo(4096L)
        assertThat(state.cacheBytes).isEqualTo(2048L)
        assertThat(state.isLoading).isFalse()
        verify(storageRepository, times(2)).getStorageUsage()
    }

    @Test
    fun `clearCache clears cache, reloads usage, and emits CacheCleared event`() = runTest {
        whenever(storageRepository.getStorageUsage()).thenReturn(
            StorageUsage(databaseBytes = 10_240L, cacheBytes = 51_200L),
            StorageUsage(databaseBytes = 10_240L, cacheBytes = 0L),
        )

        val viewModel = StorageSettingsViewModel(storageRepository)
        testScheduler.advanceUntilIdle()

        val receivedEvents = mutableListOf<StorageSettingsEvent>()
        val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { receivedEvents.add(it) }
        }

        viewModel.clearCache()
        testScheduler.advanceUntilIdle()

        inOrder(storageRepository) {
            verify(storageRepository).getStorageUsage()
            verify(storageRepository).clearCache()
            verify(storageRepository).getStorageUsage()
        }

        val state = viewModel.uiState.value
        assertThat(state.databaseBytes).isEqualTo(10_240L)
        assertThat(state.cacheBytes).isEqualTo(0L)
        assertThat(state.isClearingCache).isFalse()
        assertThat(receivedEvents).containsExactly(StorageSettingsEvent.CacheCleared)

        eventJob.cancel()
    }

    @Test
    fun `clearCache prevents parallel runs while clearing is active`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val fakeRepo = object : StorageRepository {
            var clearCalls = 0
            override suspend fun getStorageUsage(): StorageUsage =
                StorageUsage(databaseBytes = 1024L, cacheBytes = if (clearCalls > 0) 0L else 4096L)

            override suspend fun clearCache() {
                clearCalls++
                gate.await()
            }
        }

        val viewModel = StorageSettingsViewModel(fakeRepo)
        testScheduler.advanceUntilIdle()

        viewModel.clearCache()
        assertThat(viewModel.uiState.value.isClearingCache).isTrue()

        // Second invocation while still clearing must be ignored
        viewModel.clearCache()
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()

        assertThat(fakeRepo.clearCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.isClearingCache).isFalse()
        assertThat(viewModel.uiState.value.cacheBytes).isEqualTo(0L)
    }

    @Test
    fun `clearCache resets isClearingCache and does not emit event when exception occurs`() = runTest {
        whenever(storageRepository.getStorageUsage()).thenReturn(
            StorageUsage(databaseBytes = 2048L, cacheBytes = 4096L),
        )
        whenever(storageRepository.clearCache()).thenAnswer {
            throw IOException("Disk full")
        }

        val viewModel = StorageSettingsViewModel(storageRepository)
        testScheduler.advanceUntilIdle()

        val receivedEvents = mutableListOf<StorageSettingsEvent>()
        val eventJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { receivedEvents.add(it) }
        }

        viewModel.clearCache()
        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.isClearingCache).isFalse()
        assertThat(viewModel.uiState.value.databaseBytes).isEqualTo(2048L)
        assertThat(viewModel.uiState.value.cacheBytes).isEqualTo(4096L)
        assertThat(receivedEvents).isEmpty()

        eventJob.cancel()
    }

    @Test
    fun `formatStorageBytes formats negative, bytes, KB, MB, and GB values`() {
        assertThat(formatStorageBytes(-50L)).isEqualTo("0 B")
        assertThat(formatStorageBytes(0L)).isEqualTo("0 B")
        assertThat(formatStorageBytes(512L)).isEqualTo("512 B")
        assertThat(formatStorageBytes(1023L)).isEqualTo("1023 B")
        assertThat(formatStorageBytes(1024L)).isEqualTo("1.0 KB")
        assertThat(formatStorageBytes(1536L)).isEqualTo("1.5 KB")
        assertThat(formatStorageBytes(1024L * 1024L)).isEqualTo("1.0 MB")
        assertThat(formatStorageBytes(2L * 1024L * 1024L + 512L * 1024L)).isEqualTo("2.5 MB")
        assertThat(formatStorageBytes(1024L * 1024L * 1024L)).isEqualTo("1.0 GB")
    }
}
