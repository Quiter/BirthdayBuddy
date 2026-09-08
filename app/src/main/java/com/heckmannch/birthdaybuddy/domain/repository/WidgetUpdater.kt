package com.heckmannch.birthdaybuddy.domain.repository

/**
 * Domain interface for updating and scheduling application widgets.
 *
 * Widget-Update-Strategie (Single-Path):
 * - Sofortige Updates via [updateWidget] aktualisieren Glance-Widgets synchron/direkt nach Datenänderungen oder Boot.
 * - Tägliches Scheduling via [scheduleDailyUpdate] registriert einen exakten Alarm um Mitternacht im AlarmManager.
 * - WorkManager übernimmt keine 24h-Verzögerungen, sondern wird erst beim Auslösen des Alarms für Hintergrundarbeit
 *   und automatische Wiederholungsversuche (Retries) herangezogen.
 */
interface WidgetUpdater {
    /**
     * Updates all installed widget instances immediately with the latest contact data.
     */
    suspend fun updateWidget()

    /**
     * Schedules the next daily widget update alarm at midnight (00:01 AM) via AlarmManager.
     */
    fun scheduleDailyUpdate()
}
