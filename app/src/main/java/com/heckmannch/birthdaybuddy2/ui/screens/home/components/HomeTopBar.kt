package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 16.dp),
    ) {
        SearchBar(
            query = searchQuery,
            placeholder = animatedPlaceholder,
            onQueryChange = onSearchQueryChange,
            onClearQuery = onClearSearch,
            onSettingsClick = onNavigateToSettings
        )

        LabelFilterBar(
            visible = isFilterBarVisible,
            labels = availableLabels,
            selectedLabel = selectedLabel,
            onLabelSelected = onLabelSelected
        )
    }
}
