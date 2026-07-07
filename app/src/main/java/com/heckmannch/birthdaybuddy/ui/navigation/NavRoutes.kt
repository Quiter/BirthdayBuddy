package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation-Routen für den NavDisplay (Navigation 3).
 * Jede Route ist ein typsicheres, serialisierbares NavKey-Objekt.
 */
@Serializable
object Home : NavKey

@Serializable
object Onboarding : NavKey

@Serializable
object Settings : NavKey

@Serializable
object LabelSettings : NavKey

@Serializable
object NotificationSettings : NavKey

@Serializable
object OtherEventsSettings : NavKey

@Serializable
object CalendarSettings : NavKey

@Serializable
object BackupSettings : NavKey

@Serializable
object ThemeSettings : NavKey

@Serializable
object SyncSettings : NavKey

@Serializable
object About : NavKey

@Serializable
object PrivacyPolicy : NavKey
