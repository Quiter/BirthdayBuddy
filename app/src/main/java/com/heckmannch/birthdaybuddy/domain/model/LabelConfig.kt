package com.heckmannch.birthdaybuddy.domain.model

/**
 * Pure domain model representing label configuration.
 * Decoupled from any database-specific attributes.
 */
data class LabelConfig(
    val name: String,
    val isHiddenFromFilter: Boolean = false,
    val isIgnored: Boolean = false,
    val isSystem: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val showInWidget: Boolean = true
)
