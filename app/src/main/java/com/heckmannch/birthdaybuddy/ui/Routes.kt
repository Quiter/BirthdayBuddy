package com.heckmannch.birthdaybuddy.ui

import kotlinx.serialization.Serializable

/**
 * Alle Routen der App als typsichere Objekte.
 */
sealed interface Route {
    @Serializable object Main : Route
    @Serializable object Settings : Route
    @Serializable object Alarms : Route
    @Serializable object LabelManager : Route
    
    // Alte Routen (bleiben vorerst für Kompatibilität)
    @Serializable object MainScreenExcludeLabels : Route
    @Serializable object MainScreenIncludeLabels : Route
    @Serializable object WidgetIncludeLabels : Route
    @Serializable object WidgetExcludeLabels : Route
    @Serializable object NotificationIncludeLabels : Route
    @Serializable object NotificationExcludeLabels : Route
}
