package com.heckmannch.birthdaybuddy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.data.repository.NotificationRepository
import com.heckmannch.birthdaybuddy.ui.model.ThemeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val uiState: StateFlow<ThemeUiState> = notificationRepository.settings
        .map { settings ->
            ThemeUiState(
                themeMode = settings.themeMode,
                themeAmoled = settings.themeAmoled,
                themeAccent = settings.themeAccent
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeUiState()
        )

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
