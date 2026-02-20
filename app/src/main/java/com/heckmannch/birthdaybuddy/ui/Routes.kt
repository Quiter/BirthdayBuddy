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
}
