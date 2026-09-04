package com.heckmannch.birthdaybuddy.domain.repository

/**
 * Domain interface for updating and scheduling application widgets.
 */
interface WidgetUpdater {
    /**
     * Updates all installed widget instances immediately with the latest contact data.
     */
    suspend fun updateWidget()

    /**
     * Schedules the next daily widget update worker via WorkManager.
     */
    fun scheduleDailyUpdate()
}
