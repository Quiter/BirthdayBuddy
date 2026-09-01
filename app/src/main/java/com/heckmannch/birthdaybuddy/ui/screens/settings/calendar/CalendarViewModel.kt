package com.heckmannch.birthdaybuddy.ui.screens.settings.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.BuildConfig
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.domain.usecase.SetCalendarSyncEnabledUseCase
import com.heckmannch.birthdaybuddy.domain.usecase.UpdateCalendarColorUseCase
import com.heckmannch.birthdaybuddy.ui.model.CalendarUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    notificationRepository: NotificationRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val setCalendarSyncEnabledUseCase: SetCalendarSyncEnabledUseCase,
    private val updateCalendarColorUseCase: UpdateCalendarColorUseCase,
) : ViewModel() {

    init {
        if (BuildConfig.DEBUG) {
            viewModelScope.launch {
                calendarSyncRepository.debugPrintAllCalendars()
            }
        }
    }

    private val _hasCalendarPermission =
        MutableStateFlow(calendarSyncRepository.hasCalendarPermissions())

    val uiState: StateFlow<CalendarUiState> = combine(
        notificationRepository.settings,
        _hasCalendarPermission
    ) { settings, hasPermission ->
        CalendarUiState(
            calendarSyncEnabled = settings.calendarSyncEnabled,
            otherEventsEnabled = settings.otherEventsEnabled,
            birthdayCalendarColor = settings.birthdayCalendarColor,
            anniversaryCalendarColor = settings.anniversaryCalendarColor,
            nameDayCalendarColor = settings.nameDayCalendarColor,
            hasCalendarPermission = hasPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun onIntent(intent: CalendarIntent) {
        when (intent) {
            is CalendarIntent.UpdateCalendarColor -> {
                updateCalendarColor(intent.type, intent.color)
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

    data object CheckPermissionStatus : CalendarIntent

    data class SetCalendarSyncEnabled(val enabled: Boolean) : CalendarIntent
}
