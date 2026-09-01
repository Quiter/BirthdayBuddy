package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Repräsentiert eine ausstehende Benachrichtigung für anstehende Geburtstage/Ereignisse.
 *
 * Indizes:
 * - `isDone`: Beschleunigt Abfragen nach aktiven Benachrichtigungen (`WHERE isDone = 0`)
 *   in [PendingNotificationDao.getActiveNotifications] und [PendingNotificationDao.getActiveNotificationsImmediate].
 * - `year`, `daysBefore`: Optimiert die Duplikatsprüfung in [PendingNotificationDao.hasNotificationBeenScheduled]
 *   sowie Löschabfragen in [PendingNotificationDao.deleteOldNotifications], um Full-Table-Scans zu vermeiden.
 */
@Entity(
    tableName = "pending_notifications",
    indices = [
        Index(value = ["isDone"]),
        Index(value = ["year", "daysBefore"])
    ]
)
data class PendingNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactLookupKeys: List<String>, // Liste der LookupKeys der betroffenen Kontakte
    val daysBefore: Int,
    val year: Int,
    val isDone: Boolean = false,
    @ColumnInfo(defaultValue = "0") val dismissCount: Int = 0 // NEU: Zähler für Wisch-Versuche
)
