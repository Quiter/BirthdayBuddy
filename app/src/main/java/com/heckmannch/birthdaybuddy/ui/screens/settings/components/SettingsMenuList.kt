package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.SettingsColumn
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsMenuItemData
import com.heckmannch.birthdaybuddy.ui.screens.settings.SettingsSectionType
import com.heckmannch.birthdaybuddy.ui.screens.settings.rememberSettingsMenuItems
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Scrollable list of settings menu cards grouped by section.
 */
@Composable
internal fun SettingsMenuList(
    menuItems: List<SettingsMenuItemData>,
    onItemClick: (SettingsMenuItemData) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        SettingsColumn(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
        ) {
            val generalItems = menuItems.filter { it.section == SettingsSectionType.GENERAL }
            SettingsMenuSectionCard(
                sectionTitle = stringResource(R.string.settings_theme_title),
                sectionIcon = Icons.Default.Palette,
                items = generalItems,
                onItemClick = onItemClick
            )

            val dataItems = menuItems.filter { it.section == SettingsSectionType.DATA }
            SettingsMenuSectionCard(
                sectionTitle = stringResource(R.string.settings_sync_title),
                sectionIcon = Icons.Default.Refresh,
                items = dataItems,
                onItemClick = onItemClick
            )

            val aboutItems = menuItems.filter { it.section == SettingsSectionType.ABOUT }
            SettingsMenuSectionCard(
                sectionTitle = stringResource(R.string.settings_about_title),
                sectionIcon = Icons.Default.Info,
                items = aboutItems,
                onItemClick = onItemClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsMenuListPreview() {
    BirthdayBuddyTheme {
        SettingsMenuList(
            menuItems = rememberSettingsMenuItems(),
            onItemClick = {},
            paddingValues = PaddingValues()
        )
    }
}
