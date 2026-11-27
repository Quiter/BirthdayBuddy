package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.ContactRepository
import com.heckmannch.birthdaybuddy.domain.repository.SettingsRepository
import com.heckmannch.birthdaybuddy.domain.usecase.GetDetectedOtherEventsUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.SetCalendarSyncEnabledUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UpdateCalendarColorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    contactRepository: ContactRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val setCalendarSyncEnabledUseCase: SetCalendarSyncEnabledUseCase,
    private val updateCalendarColorUseCase: UpdateCalendarColorUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _hasCalendarPermission =
        MutableStateFlow(calendarSyncRepository.hasCalendarPermissions())

    val uiState: StateFlow<CalendarUiState> = combine(
        settingsRepository.settings,
        contactRepository.allContacts,
        _hasCalendarPermission
    ) { settings, contacts, hasPermission ->
        val disabledSet = settings.disabledOtherEventLabels.toSet()
        val detectedEvents = GetDetectedOtherEventsUseCase.detect(contacts, disabledSet)

        val anniversaryEnabled = settings.isOtherEventEnabled(ContactLabels.LABEL_ANNIVERSARY)
        val nameDayEnabled = settings.isOtherEventEnabled(ContactLabels.LABEL_NAME_DAY)

        val customCalendars = if (settings.otherEventsEnabled) {
            detectedEvents
                .asSequence()
                .filter { it.isEnabled && ContactLabels.isDynamicCustomEventKey(it.eventKey) }
                .map { event ->
                    CustomEventCalendarColorItem(
                        eventKey = event.eventKey,
                        color = settings.calendarColorForEvent(event.eventKey),
                    )
                }
                .toList()
        } else {
            emptyList()
        }

        CalendarUiState(
            calendarSyncEnabled = settings.calendarSyncEnabled,
            otherEventsEnabled = settings.otherEventsEnabled,
            anniversaryEnabled = anniversaryEnabled,
            nameDayEnabled = nameDayEnabled,
            birthdayCalendarColor = settings.birthdayCalendarColor,
            anniversaryCalendarColor = settings.anniversaryCalendarColor,
            nameDayCalendarColor = settings.nameDayCalendarColor,
            customEventCalendars = customCalendars,
            hasCalendarPermission = hasPermission
        )
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarUiState()
        )

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.UpdateCalendarColor -> {
                updateCalendarColor(intent.type, intent.color)
            }

            is CalendarIntent.UpdateCustomEventCalendarColor -> {
                updateCustomEventCalendarColor(intent.eventKey, intent.color)
            }

            is CalendarIntent.CheckPermissionStatus -> {
                checkPermissionStatus()
            }

            is CalendarIntent.SetCalendarSyncEnabled -> {
                setCalendarSyncEnabled(intent.enabled)
            }
        }
    }

    private fun updateCalendarColor(type: CalendarSyncRepository.CalendarType, color: Int) =
        viewModelScope.launch {
            updateCalendarColorUseCase(type, color)
        }

    private fun updateCustomEventCalendarColor(eventKey: String, color: Int) =
        viewModelScope.launch {
            updateCalendarColorUseCase(eventKey, color)
        }

    private fun checkPermissionStatus() {
        _hasCalendarPermission.value = calendarSyncRepository.hasCalendarPermissions()
    }

    private fun setCalendarSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        setCalendarSyncEnabledUseCase(enabled)
    }
}

sealed interface CalendarIntent {
    data class UpdateCalendarColor(
        val type: CalendarSyncRepository.CalendarType,
        val color: Int
    ) : CalendarIntent

    data class UpdateCustomEventCalendarColor(
        val eventKey: String,
        val color: Int
    ) : CalendarIntent

    data object CheckPermissionStatus : CalendarIntent

    data class SetCalendarSyncEnabled(val enabled: Boolean) : CalendarIntent
}
