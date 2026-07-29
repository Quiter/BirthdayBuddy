package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.navigation3.runtime.NavKey
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
data object Settings : NavKey

@Serializable
data object LabelSettings : NavKey

@Serializable
data object NotificationSettings : NavKey

@Serializable
data object OtherEventsSettings : NavKey

@Serializable
data object CalendarSettings : NavKey

@Serializable
data object BackupSettings : NavKey

@Serializable
data object ThemeSettings : NavKey

@Serializable
data object SyncSettings : NavKey

@Serializable
data object About : NavKey

@Serializable
data object PrivacyPolicy : NavKey
