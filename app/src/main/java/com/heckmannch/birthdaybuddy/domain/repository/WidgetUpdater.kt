package com.heckmannch.birthdaybuddy.domain.repository

/**
 * Domain interface for updating application widgets.
 */
interface WidgetUpdater {
    suspend fun updateWidget()
}
