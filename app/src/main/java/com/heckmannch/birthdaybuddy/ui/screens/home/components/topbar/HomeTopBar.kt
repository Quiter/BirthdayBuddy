package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.header.HomeHeader
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Top bar composable for the home screen dashboard.
 *
 * Delegates presentation to [HomeHeader] located in `ui/screens/home/components/header`,
 * providing integrated search, app branding title/logo, settings navigation, and contact addition actions.
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
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    HomeHeader(
        searchQuery = searchQuery,
        isSearchActive = isSearchActive,
        placeholder = placeholder,
        showSidebar = showSidebar,
        focusRequester = focusRequester,
        actions = actions,
        onToggleSidebar = onToggleSidebar,
        onToggleSearch = onToggleSearch,
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
