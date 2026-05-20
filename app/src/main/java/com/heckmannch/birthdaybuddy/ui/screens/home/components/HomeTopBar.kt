package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Die TopBar des Home-Screens.
 * Orchestriert die Suchleiste und die Filter-Chips.
 */
@Composable
fun HomeTopBar(
    searchQuery: String,
    animatedPlaceholder: String,
    availableLabels: List<String>,
    selectedLabel: String?,
    isFilterBarVisible: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onLabelSelected: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearSearch: () -> Unit,
    searchFocusRequester: FocusRequester,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 8.dp)
                .animateContentSize(),
        ) {
            SearchBar(
                query = searchQuery,
                placeholder = animatedPlaceholder,
                onQueryChange = onSearchQueryChange,
                onClearQuery = onClearSearch,
                onSettingsClick = onNavigateToSettings,
                focusRequester = searchFocusRequester,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            LabelFilterBar(
                visible = isFilterBarVisible,
                labels = availableLabels,
                selectedLabel = selectedLabel,
                onLabelSelected = onLabelSelected,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    BirthdayBuddyTheme {
        HomeTopBar(
            searchQuery = "",
            animatedPlaceholder = "Geburtstage suchen",
            availableLabels = listOf("Familie", "Freunde"),
            selectedLabel = null,
            isFilterBarVisible = true,
            onSearchQueryChange = {},
            onLabelSelected = {},
            onNavigateToSettings = {},
            onClearSearch = {},
            searchFocusRequester = remember { FocusRequester() }
        )
    }
}
