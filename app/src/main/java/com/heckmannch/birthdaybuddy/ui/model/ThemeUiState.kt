package com.heckmannch.birthdaybuddy.ui.model

data class ThemeUiState(
    val themeMode: String = "SYSTEM",
    val themeAmoled: Boolean = false,
    val themeAccent: String = "SYSTEM",
    val themeContrast: Double = 0.0
)
