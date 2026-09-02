package com.heckmannch.birthdaybuddy.ui.screens.settings.sync

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * UI State for the Sync Settings screen.
 *
 * @property isSyncing Indicates whether a contact synchronization operation is currently ongoing.
 */
@Immutable
data class SyncUiState(
    val isSyncing: Boolean = false,
)

/**
 * One-off UI events emitted by [SyncViewModel].
 */
sealed interface SyncEvent {
    data object Success : SyncEvent
    data class Error(val message: String? = null) : SyncEvent
}

/**
 * ViewModel for the Sync Settings screen.
 * Handles manual contact synchronization operations independently from HomeViewModel.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SyncEvent>(replay = 0)
    val events: SharedFlow<SyncEvent> = _events.asSharedFlow()

    /**
     * Backward-compatible event flow signaling completed synchronization.
     */
    val syncCompletedEvent: SharedFlow<Unit>
        get() = _legacySyncCompletedEvent

    private val _legacySyncCompletedEvent = MutableSharedFlow<Unit>(replay = 0)

    /**
     * Synchronizes contacts from the device system repository.
     * Guards against duplicate concurrent runs and ensures proper error handling.
     */
    fun syncContacts() {
        if (_uiState.value.isSyncing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val startTime = clock.currentTimeMillis()
            try {
                contactRepository.clearIgnoredCouplePairs()
                contactRepository.syncContacts()

                val elapsedTime = clock.currentTimeMillis() - startTime
                if (elapsedTime < MIN_SPINNER_DURATION_MS) {
                    delay((MIN_SPINNER_DURATION_MS - elapsedTime).milliseconds)
                }

                _legacySyncCompletedEvent.emit(Unit)
                _events.emit(SyncEvent.Success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val elapsedTime = clock.currentTimeMillis() - startTime
                if (elapsedTime < MIN_SPINNER_DURATION_MS) {
                    delay((MIN_SPINNER_DURATION_MS - elapsedTime).milliseconds)
                }
                _events.emit(SyncEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    private companion object {
        private const val MIN_SPINNER_DURATION_MS = 800L
    }
}
