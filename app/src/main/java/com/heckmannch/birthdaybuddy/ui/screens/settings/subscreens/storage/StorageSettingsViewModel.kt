package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Storage Settings subpage.
 *
 * Delegates all filesystem and Coil cache operations to [StorageRepository] (which executes
 * off the main thread on [@IoDispatcher][com.heckmannch.birthdaybuddy.di.IoDispatcher]) and
 * exposes an immutable [StorageUiState] along with one-off [StorageSettingsEvent]s.
 */
@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState(isLoading = true))
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    private val _events = Channel<StorageSettingsEvent>(Channel.BUFFERED)

    /**
     * Buffered one-off UI events emitted by this ViewModel.
     * Uses a buffered [Channel] so events sent during brief lifecycle pauses or rotations
     * are retained and delivered once collection resumes.
     */
    val events: Flow<StorageSettingsEvent> = _events.receiveAsFlow()

    private var refreshJob: Job? = null

    init {
        refreshStorageUsage()
    }

    /**
     * Loads or refreshes the database and cache size metrics off the main thread.
     *
     * Guards against overlapping refreshes and avoids racing with an in-flight cache clear.
     */
    fun refreshStorageUsage() {
        if (_uiState.value.isClearingCache || (refreshJob?.isActive == true)) return

        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val (databaseBytes, cacheBytes) = storageRepository.getStorageUsage()
                _uiState.update {
                    it.copy(
                        databaseBytes = databaseBytes,
                        cacheBytes = cacheBytes,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Retain last known metrics on unexpected I/O failure.
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Clears Coil and temporary application caches off the main thread, refreshes the storage
     * usage metrics, and emits [StorageSettingsEvent.CacheCleared].
     *
     * Guards against duplicate concurrent runs and cancels any stale in-flight refresh.
     */
    fun clearCache() {
        if (_uiState.value.isClearingCache) return

        refreshJob?.cancel()
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingCache = true) }
            try {
                storageRepository.clearCache()
                val updatedUsage = storageRepository.getStorageUsage()
                _uiState.update {
                    it.copy(
                        databaseBytes = updatedUsage.databaseBytes,
                        cacheBytes = updatedUsage.cacheBytes,
                        isLoading = false,
                    )
                }
                _events.send(StorageSettingsEvent.CacheCleared)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Retain last known metrics if cleanup or refresh fails unexpectedly.
            } finally {
                _uiState.update { it.copy(isClearingCache = false) }
            }
        }
    }
}
