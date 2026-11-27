package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.otherevents

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetDetectedOtherEventsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SyncCalendarUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the "Other Events" settings subpage.
 *
 * Shares the canonical event-label discovery source ([GetDetectedOtherEventsUseCase]) with
 * Calendar settings, Home filtering, and Label categorization.
 */
@HiltViewModel
class OtherEventsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val contactRepository: ContactRepository,
    private val syncCalendarUseCase: SyncCalendarUseCase,
    getDetectedOtherEventsUseCase: GetDetectedOtherEventsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val otherEventsEnabledFlow = settingsRepository.settings
        .map { it.otherEventsEnabled }
        .distinctUntilChanged()

    private val disabledEventKeysFlow = settingsRepository.settings
        .map { it.disabledOtherEventLabels.toSet() }
        .distinctUntilChanged()

    private val detectedEventsFlow = getDetectedOtherEventsUseCase(
        contacts = contactRepository.allContacts,
        disabledEventKeys = disabledEventKeysFlow,
    )

    val uiState: StateFlow<OtherEventsUiState> = combine(
        otherEventsEnabledFlow,
        detectedEventsFlow,
    ) { otherEventsEnabled, detectedEvents ->
        OtherEventsUiState(
            otherEventsEnabled = otherEventsEnabled,
            detectedEvents = detectedEvents.map { event ->
                DetectedOtherEventUiModel(
                    eventKey = event.eventKey,
                    contactCount = event.contactCount,
                    isEnabled = event.isEnabled,
                )
            },
        )
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OtherEventsUiState(),
        )

    fun onIntent(intent: OtherEventsIntent) {
        when (intent) {
            is OtherEventsIntent.SetOtherEventsEnabled -> setOtherEventsEnabled(intent.enabled)
            is OtherEventsIntent.SetEventLabelEnabled -> setEventLabelEnabled(
                eventKey = intent.eventKey,
                enabled = intent.enabled,
            )
        }
    }

    /**
     * Toggles global "Other Events" support, refreshes contacts when enabling, and triggers
     * calendar synchronization so affected event calendars stay in sync.
     */
    private fun setOtherEventsEnabled(enabled: Boolean) = viewModelScope.launch {
        try {
            settingsRepository.updateSettings { it.copy(otherEventsEnabled = enabled) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update otherEventsEnabled setting", e)
            return@launch
        }

        if (enabled) {
            try {
                contactRepository.syncContacts()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync contacts after enabling other events", e)
            }
        }

        try {
            syncCalendarUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync calendar after toggling other events", e)
        }
    }

    /**
     * Enables or disables a single detected event category via opt-out persistence in
     * `disabledOtherEventLabels` and refreshes calendar synchronization.
     */
    private fun setEventLabelEnabled(eventKey: String, enabled: Boolean) = viewModelScope.launch {
        try {
            settingsRepository.updateSettings { current ->
                val updated = current.disabledOtherEventLabels.toMutableSet()
                if (enabled) {
                    updated.remove(eventKey)
                } else {
                    updated.add(eventKey)
                }
                current.copy(disabledOtherEventLabels = updated.sorted())
            }
            syncCalendarUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update event label setting for $eventKey", e)
        }
    }

    private companion object {
        private const val TAG = "OtherEventsViewModel"
    }
}

sealed interface OtherEventsIntent {
    data class SetOtherEventsEnabled(val enabled: Boolean) : OtherEventsIntent
    data class SetEventLabelEnabled(val eventKey: String, val enabled: Boolean) : OtherEventsIntent
}
