package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppTopHeader
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Top bar composable for the home screen dashboard.
 *
 * Built on top of [AppTopHeader], encapsulating status bar insets handling, an expandable
 * search bar, title presentation, settings action, and optional navigation sidebar toggle.
 *
 * @param searchQuery Current search text query.
 * @param isSearchActive Whether search mode is active.
 * @param placeholder Dynamic animated placeholder hint for the search bar.
 * @param showSidebar Whether the navigation sidebar toggle icon should be rendered.
 * @param focusRequester Focus requester tied to the search text field.
 * @param actions User interaction callbacks.
 * @param onToggleSidebar Callback invoked when the user toggles the navigation sidebar.
 * @param onToggleSearch Callback invoked when search mode is activated or deactivated.
 * @param modifier Optional [Modifier] for the top bar container.
 */
@Composable
fun HomeTopBar(
    searchQuery: String,
    isSearchActive: Boolean,
    placeholder: String,
    showSidebar: Boolean,
    focusRequester: FocusRequester,
    actions: HomeActions,
    onToggleSidebar: () -> Unit,
    onToggleSearch: (Boolean) -> Unit = actions.onToggleSearch,
    modifier: Modifier = Modifier,
) {
    AppTopHeader(
        title = stringResource(R.string.app_name),
        isSearchActive = isSearchActive,
        searchQuery = searchQuery,
        onSearchQueryChange = actions.onSearchQueryChange,
        onSearchActiveChange = onToggleSearch,
        searchPlaceholder = placeholder,
        onClearSearch = actions.onClearSearch,
        focusRequester = focusRequester,
        navigationIcon = if (showSidebar) {
            {
                IconButton(onClick = onToggleSidebar) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.cd_toggle_sidebar),
                    )
                }
            }
        } else null,
        actions = {
            IconButton(
                onClick = actions.onNavigateToSettings,
                modifier = Modifier.testTag("settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.home_settings),
                )
            }
        },
        modifier = modifier.testTag("home_top_bar"),
    )
}

/**
 * Overloaded [HomeTopBar] for backwards compatibility when [isSearchActive] is omitted.
 */
@Composable
fun HomeTopBar(
    searchQuery: String,
    placeholder: String,
    showSidebar: Boolean,
    focusRequester: FocusRequester,
    actions: HomeActions,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeTopBar(
        searchQuery = searchQuery,
        isSearchActive = searchQuery.isNotEmpty(),
        placeholder = placeholder,
        showSidebar = showSidebar,
        focusRequester = focusRequester,
        actions = actions,
        onToggleSidebar = onToggleSidebar,
        onToggleSearch = actions.onToggleSearch,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun HomeTopBarPreview() {
    BirthdayBuddyTheme {
        HomeTopBar(
            searchQuery = "",
            isSearchActive = false,
            placeholder = "Geburtstage durchsuchen...",
            showSidebar = false,
            focusRequester = remember { FocusRequester() },
            actions = SampleData.homeActions,
            onToggleSidebar = {},
        )
    }
}
