package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.sync

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.CoupleRepository
import com.heckmannch.birthdaybuddy.util.Clock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val coupleRepository: CoupleRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    private val _events = Channel<SyncEvent>(Channel.BUFFERED)

    /**
     * Buffered one-off UI events emitted by this ViewModel.
     * Uses a buffered [Channel] so events sent during brief lifecycle pauses or rotations
     * are retained and delivered once collection resumes.
     */
    val events: Flow<SyncEvent> = _events.receiveAsFlow()

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
                coupleRepository.clearIgnoredCouplePairs()
                contactRepository.syncContacts()

                val elapsedTime = clock.currentTimeMillis() - startTime
                if (elapsedTime < MIN_SPINNER_DURATION_MS) {
                    delay((MIN_SPINNER_DURATION_MS - elapsedTime).milliseconds)
                }

                _events.send(SyncEvent.Success)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val elapsedTime = clock.currentTimeMillis() - startTime
                if (elapsedTime < MIN_SPINNER_DURATION_MS) {
                    delay((MIN_SPINNER_DURATION_MS - elapsedTime).milliseconds)
                }
                _events.send(SyncEvent.Error(e.message))
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    private companion object {
        private const val MIN_SPINNER_DURATION_MS = 800L
    }
}

