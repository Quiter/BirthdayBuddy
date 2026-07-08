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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
            calendarSyncRepository.debugPrintAllCalendars()
        }
    }

    val uiState: StateFlow<CalendarUiState> = notificationRepository.settings
        .map { settings ->
            CalendarUiState(
                calendarSyncEnabled = settings.calendarSyncEnabled,
                otherEventsEnabled = settings.otherEventsEnabled,
                birthdayCalendarColor = settings.birthdayCalendarColor,
                anniversaryCalendarColor = settings.anniversaryCalendarColor,
                nameDayCalendarColor = settings.nameDayCalendarColor
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CalendarUiState()
        )

    fun updateCalendarColor(type: CalendarSyncRepository.CalendarType, color: Int) =
        viewModelScope.launch {
            updateCalendarColorUseCase(type, color)
        }

    fun hasCalendarPermissions(): Boolean {
        return calendarSyncRepository.hasCalendarPermissions()
    }

    fun setCalendarSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        setCalendarSyncEnabledUseCase(enabled)
    }
}
