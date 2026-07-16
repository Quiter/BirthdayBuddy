package com.heckmannch.birthdaybuddy.ui.model

import androidx.compose.runtime.Immutable
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode

@Immutable
data class ThemeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeAmoled: Boolean = false,
    val themeAccent: ThemeAccent = ThemeAccent.SYSTEM,
    val customAccentColor: String? = null
)
