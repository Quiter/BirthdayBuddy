package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsTab
import kotlinx.serialization.Serializable

/**
 * Navigation-Routen für den NavDisplay (Navigation 3).
 * Jede Route ist ein typsicheres, serialisierbares NavKey-Objekt.
 */
@Serializable
data object Home : NavKey

@Serializable
data object Onboarding : NavKey

@Serializable
data class Settings(val initialTab: SettingsTab? = null) : NavKey
