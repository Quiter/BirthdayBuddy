package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.BuildConfig
import com.heckmannch.birthdaybuddy.data.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.data.repository.ContactRepository
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
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

    private val settings = notificationRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val calendarSyncEnabled: StateFlow<Boolean> = settings
        .filterNotNull()
        .map { it.calendarSyncEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val otherEventsEnabled: StateFlow<Boolean> = settings
        .filterNotNull()
        .map { it.otherEventsEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val birthdayCalendarColor: StateFlow<Int> = settings
        .filterNotNull()
        .map { it.birthdayCalendarColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFFE91E63.toInt())

    val anniversaryCalendarColor: StateFlow<Int> = settings
        .filterNotNull()
        .map { it.anniversaryCalendarColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFF9C27B0.toInt())

    val nameDayCalendarColor: StateFlow<Int> = settings
        .filterNotNull()
        .map { it.nameDayCalendarColor }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0xFFFF9800.toInt())

    fun updateCalendarColor(type: CalendarSyncRepository.CalendarType, color: Int) = viewModelScope.launch {
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
