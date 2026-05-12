package com.heckmannch.birthdaybuddy.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "label_configs")
data class LabelConfig(
    @PrimaryKey
    val name: String,
    val isHiddenFromFilter: Boolean = false,
    val isIgnored: Boolean = false,
    val isSystem: Boolean = false
)
