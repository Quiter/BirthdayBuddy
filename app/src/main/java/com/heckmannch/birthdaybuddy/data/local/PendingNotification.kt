package com.heckmannch.birthdaybuddy.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_notifications")
data class PendingNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactLookupKeys: List<String>, // Liste der LookupKeys der betroffenen Kontakte
    val daysBefore: Int,
    val year: Int,
    val isDone: Boolean = false,
    @ColumnInfo(defaultValue = "0") val dismissCount: Int = 0 // NEU: Zähler für Wisch-Versuche
)
