package com.heckmannch.birthdaybuddy.ui.screens.settings.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.heckmannch.birthdaybuddy.domain.repository.NotificationRepository
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

    fun onIntent(intent: ThemeIntent) {
        when (intent) {
            is ThemeIntent.SetThemeMode -> setThemeMode(intent.mode)
            is ThemeIntent.SetThemeAmoled -> setThemeAmoled(intent.enabled)
            is ThemeIntent.SetThemeAccent -> setThemeAccent(intent.accent)
        }
    }

    private fun setThemeMode(mode: String) = viewModelScope.launch {
        notificationRepository.updateSettings(themeMode = mode)
    }

    private fun setThemeAmoled(enabled: Boolean) = viewModelScope.launch {
        notificationRepository.updateSettings(themeAmoled = enabled)
    }

    private fun setThemeAccent(accent: String) = viewModelScope.launch {
        notificationRepository.updateSettings(themeAccent = accent)
    }
}

sealed interface ThemeIntent {
    data class SetThemeMode(val mode: String) : ThemeIntent
    data class SetThemeAmoled(val enabled: Boolean) : ThemeIntent
    data class SetThemeAccent(val accent: String) : ThemeIntent
}
