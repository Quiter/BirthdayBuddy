package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsPageHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * Top header component for the Settings screen and sub-screens, backed by the
 * Morphe-style [SettingsPageHeader] while preserving test tags (`settings_back_button`,
 * `open_search_button`, `close_search_button`, `search_field`).
 */
@Composable
internal fun SettingsHeader(
    title: String,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActiveChange: (Boolean) -> Unit = {},
    onClearSearch: () -> Unit = {},
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {},
    showSearchButton: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = SpacingNormal, vertical = SpacingMedium),
        contentAlignment = Alignment.Center,
    ) {
        SettingsPageHeader(
            title = title,
            showBackButton = showBackButton,
            onNavigateBack = onNavigateBack,
            showSearchButton = showSearchButton,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            searchPlaceholder = stringResource(R.string.settings_search),
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            onClearSearch = onClearSearch,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsHeaderPreview() {
    BirthdayBuddyTheme {
        SettingsHeader(
            title = stringResource(R.string.settings_title),
            showBackButton = true,
            onNavigateBack = {},
            showSearchButton = false,
        )
    }
}
