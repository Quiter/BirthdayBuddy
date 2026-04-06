package com.heckmannch.birthdaybuddy.ui.settings

import androidx.lifecycle.ViewModel
import com.heckmannch.birthdaybuddy.data.preferences.FilterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AlarmsViewModel @Inject constructor(
    val filterManager: FilterManager
) : ViewModel() {
    val notificationHourFlow = filterManager.preferencesFlow.map { it.notificationHour }
    val notificationMinuteFlow = filterManager.preferencesFlow.map { it.notificationMinute }
    val notificationDaysFlow = filterManager.preferencesFlow.map { it.notificationDays }
}
