package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppTopHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Reusable top header component for settings screens and sub-screens.
 * Displays the screen title, optional back navigation button, and search functionality.
 */
@Composable
internal fun SettingsHeader(
    title: String,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActiveChange: (Boolean) -> Unit = {},
    onClearSearch: () -> Unit = {},
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {},
    showSearchButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    AppTopHeader(
        title = title,
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSearchActiveChange = onSearchActiveChange,
        searchPlaceholder = stringResource(R.string.settings_search),
        onClearSearch = onClearSearch,
        showSearchButton = showSearchButton,
        modifier = modifier,
        navigationIcon = if (showBackButton) {
            {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
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

@Preview(showBackground = true)
@Composable
private fun SettingsHeaderPreview() {
    BirthdayBuddyTheme {
        SettingsHeader(
            title = stringResource(R.string.settings_title),
            isSearchActive = false,
            searchQuery = "",
            onSearchQueryChange = {},
            onSearchActiveChange = {},
            onClearSearch = {},
            showBackButton = true,
            onNavigateBack = {},
            showSearchButton = true
        )
    }
}

