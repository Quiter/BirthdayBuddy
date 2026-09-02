package com.heckmannch.birthdaybuddy.data.repository

import android.content.ContentProviderOperation

/**
 * Interface für den direkten Zugriff auf den System-Kalender.
 * Kapselt alle Android-spezifischen ContentResolver- und CalendarContract-Aufrufe.
 */
interface SystemCalendarDataSource {
    /**
     * Prüft, ob Lese- und Schreibberechtigungen für den Kalender vorhanden sind.
     */
    fun hasCalendarPermissions(): Boolean

    /**
     * Sucht einen Kalender anhand seines Namens.
     */
    suspend fun findCalendarIdByName(calendarName: String): Long?

    /**
     * Erstellt einen lokalen Kalender vom Typ ACCOUNT_TYPE_LOCAL.
     */
    suspend fun createLocalCalendar(calendarName: String, displayName: String, color: Int): Long?

    /**
     * Hilfsmethode, um einen Kalender anhand seines Namens zu finden oder neu anzulegen.
     */
    suspend fun getOrCreateCalendar(calendarName: String, displayName: String, color: Int): Long?

    /**
     * Löscht einen Kalender anhand seiner ID.
     */
    suspend fun deleteCalendarById(
        calendarId: Long,
        accountName: String,
        accountType: String
    ): Boolean

    /**
     * Aktualisiert die Farbe eines Kalenders.
     */
    suspend fun updateCalendarColor(calendarId: Long, newColor: Int): Boolean

    /**
     * Liefert alle im System vorhandenen Kalender.
     */
    suspend fun queryAllCalendars(): List<SystemCalendarInfo>

    /**
     * Löscht alle Termine eines Kalenders.
     */
    suspend fun clearCalendarEvents(calendarId: Long): Boolean

    /**
     * Wendet Batch-Operationen (z. B. Massen-Termin-Inserts) atomar an.
     */
    suspend fun applyBatch(operations: List<ContentProviderOperation>): Boolean

    companion object {
        const val ACCOUNT_NAME = "BirthdayBuddy"
        const val OWNER_ACCOUNT = "birthdaybuddy@local"
        const val LEGACY_CALENDAR_NAME = "BirthdayBuddyCalendar"
    }
}
