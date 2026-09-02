package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.LabelFilterBar
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Top bar composable for the home screen dashboard.
 *
 * Encapsulates the status bar insets handling, the search bar, optional sidebar toggle button,
 * and the contextual label filter bar when displayed at the top.
 *
 * @param searchQuery Current search text query.
 * @param placeholder Dynamic animated placeholder hint for the search bar.
 * @param availableLabels List of selectable label filters.
 * @param selectedLabel Currently active label filter, if any.
 * @param showSidebar Whether the navigation sidebar toggle icon should be rendered.
 * @param showFilterBar Whether the label filter chip row should be rendered below the search bar.
 * @param focusRequester Focus requester tied to the search text field.
 * @param actions User interaction callbacks.
 * @param onToggleSidebar Callback invoked when the user toggles the navigation sidebar.
 * @param modifier Optional [Modifier] for the top bar container.
 */
@Composable
fun HomeTopBar(
    searchQuery: String,
    placeholder: String,
    availableLabels: List<String>,
    selectedLabel: String?,
    showSidebar: Boolean,
    showFilterBar: Boolean,
    focusRequester: FocusRequester,
    actions: HomeActions,
    onToggleSidebar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier.padding(top = topPadding)
        ) {
            SearchBar(
                query = searchQuery,
                placeholder = placeholder,
                onQueryChange = actions.onSearchQueryChange,
                onClearQuery = actions.onClearSearch,
                onSettingsClick = actions.onNavigateToSettings,
                focusRequester = focusRequester,
                navigationIcon = if (showSidebar) {
                    {
                        IconButton(onClick = onToggleSidebar) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Toggle Sidebar"
                            )
                        }
                    }
                } else null,
                modifier = Modifier.padding(bottom = SpacingSmall),
            )
            if (showFilterBar && availableLabels.isNotEmpty()) {
                LabelFilterBar(
                    visible = true,
                    labels = availableLabels,
                    selectedLabel = selectedLabel,
                    onLabelSelected = actions.onLabelSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
