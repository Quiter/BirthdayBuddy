package com.heckmannch.birthdaybuddy.viewmodel

import com.heckmannch.birthdaybuddy.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
