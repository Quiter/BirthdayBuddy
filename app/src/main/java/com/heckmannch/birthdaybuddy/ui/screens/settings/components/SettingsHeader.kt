package com.heckmannch.birthdaybuddy.ui.screens.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppTopHeader

/**
 * Top header component for the main settings menu.
 * Displays the screen title, optional back navigation button, and search functionality.
 */
@Composable
internal fun SettingsHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchActiveChange: (Boolean) -> Unit,
    onClearSearch: () -> Unit,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit = {}
) {
    AppTopHeader(
        title = stringResource(R.string.settings_title),
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onSearchActiveChange = onSearchActiveChange,
        searchPlaceholder = stringResource(R.string.settings_search),
        onClearSearch = onClearSearch,
        navigationIcon = if (showBackButton) {
            {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.notifications_back),
                    )
                }
            }
        } else null
    )
}
