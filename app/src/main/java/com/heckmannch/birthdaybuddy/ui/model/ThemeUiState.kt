package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class ThemeUiState(
    val themeMode: String = "SYSTEM",
    val themeAmoled: Boolean = false,
    val themeAccent: String = "SYSTEM"
)
