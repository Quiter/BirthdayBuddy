package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class ThemeViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val settings = notificationRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<String> = settings
        .filterNotNull()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val themeAmoled: StateFlow<Boolean> = settings
        .filterNotNull()
        .map { it.themeAmoled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeAccent: StateFlow<String> = settings
        .filterNotNull()
        .map { it.themeAccent }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SYSTEM")

    fun setThemeMode(mode: String) = viewModelScope.launch {
        notificationRepository.updateSettings(themeMode = mode)
    }

    fun setThemeAmoled(enabled: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(themeAmoled = enabled)
    }

    fun setThemeAccent(accent: String) = viewModelScope.launch {
        notificationRepository.updateSettings(themeAccent = accent)
    }
}
