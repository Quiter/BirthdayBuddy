package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.bars.AppTopHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Top header component for settings screen and sub-screens, inheriting design elements from [AppTopHeader].
 *
 * Specific Settings screen customizations:
 * 1. Screen title.
 * 2. Back navigation button for sub-screens.
 * 3. Search action button disabled by default (`showSearchButton = false`).
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
    val isSubscreen = LocalIsSubscreen.current || showBackButton
    CompositionLocalProvider(LocalIsSubscreen provides isSubscreen) {
        AppTopHeader(
            title = title,
            modifier = modifier,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSearchActiveChange = onSearchActiveChange,
            searchPlaceholder = stringResource(R.string.settings_search),
            onClearSearch = onClearSearch,
            showSearchButton = showSearchButton,
            navigationIcon = if (showBackButton) {
                {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                }
            } else null
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
