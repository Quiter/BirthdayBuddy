package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.BuildConfig
import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
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
    private val notificationRepository: NotificationRepository,
    private val calendarSyncRepository: CalendarSyncRepository,
    private val contactRepository: ContactRepository,
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
            calendarSyncRepository.updateCalendarColor(type, color)
        }

    fun hasCalendarPermissions(): Boolean {
        return calendarSyncRepository.hasCalendarPermissions()
    }

    fun setCalendarSyncEnabled(enabled: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(calendarSyncEnabled = enabled)
        if (enabled) {
            // Trigger calendar sync by fetching all contacts and syncing them
            val contacts = contactRepository.getAllContactsImmediate()
            calendarSyncRepository.syncBirthdays(contacts)
        } else {
            // Delete calendar entirely
            calendarSyncRepository.deleteCalendar()
        }
    }
}
