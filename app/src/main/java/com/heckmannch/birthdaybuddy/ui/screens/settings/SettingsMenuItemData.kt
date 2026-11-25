package com.heckmannch.birthdaybuddy.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import com.heckmannch.birthdaybuddy.R

/**
 * Data holder for items displayed in the settings menu list.
 */
internal data class SettingsMenuItemData(
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector,
    val tab: SettingsTab,
    val section: SettingsSectionType
)

/**
 * Categorization sections for settings menu items.
 */
internal enum class SettingsSectionType {
    GENERAL,
    DATA,
    ABOUT
}

/**
 * Remembers the static list of all available settings menu items.
 */
@Composable
internal fun rememberSettingsMenuItems(): List<SettingsMenuItemData> {
    return remember {
        listOf(
            SettingsMenuItemData(
                titleRes = R.string.settings_theme_title,
                descRes = R.string.settings_theme_desc,
                icon = Icons.Default.Palette,
                tab = SettingsTab.THEME,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_notifications_title,
                descRes = R.string.settings_notifications_desc,
                icon = Icons.Default.Notifications,
                tab = SettingsTab.NOTIFICATIONS,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_calendar_title,
                descRes = R.string.settings_calendar_desc,
                icon = Icons.Default.DateRange,
                tab = SettingsTab.CALENDAR,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_labels_title,
                descRes = R.string.settings_labels_desc,
                icon = Icons.AutoMirrored.Filled.Label,
                tab = SettingsTab.LABELS,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_other_events_title,
                descRes = R.string.settings_other_events_desc,
                icon = Icons.Default.Star,
                tab = SettingsTab.OTHER_EVENTS,
                section = SettingsSectionType.GENERAL
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_sync_title,
                descRes = R.string.settings_sync_desc,
                icon = Icons.Default.Refresh,
                tab = SettingsTab.SYNC,
                section = SettingsSectionType.DATA
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_backup_title,
                descRes = R.string.settings_backup_desc,
                icon = Icons.Default.Share,
                tab = SettingsTab.BACKUP,
                section = SettingsSectionType.DATA
            ),
            SettingsMenuItemData(
                titleRes = R.string.storage_management_title,
                descRes = R.string.storage_management_desc,
                icon = Icons.Default.Storage,
                tab = SettingsTab.STORAGE,
                section = SettingsSectionType.DATA
            ),
            SettingsMenuItemData(
                titleRes = R.string.settings_about_title,
                descRes = R.string.settings_about_desc,
                icon = Icons.Default.Info,
                tab = SettingsTab.ABOUT,
                section = SettingsSectionType.ABOUT
            )
        )
    }
}
