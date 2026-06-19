package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
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
    actions: HomeActions,
    searchFocusRequester: FocusRequester,
) {
    // Die gesamte TopBar bekommt einen soliden Hintergrund, damit die Liste 
    // beim Scrollen sauber darunter verschwindet und keine "Geister-Flächen" entstehen.
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
        ) {
            SearchBar(
                query = searchQuery,
                placeholder = animatedPlaceholder,
                onQueryChange = actions.onSearchQueryChange,
                onClearQuery = actions.onClearSearch,
                onSettingsClick = actions.onNavigateToSettings,
                focusRequester = searchFocusRequester,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )

            LabelFilterBar(
                visible = isFilterBarVisible,
                labels = availableLabels,
                selectedLabel = selectedLabel,
                onLabelSelected = actions.onLabelSelected,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeTopBarPreview() {
    val actions = HomeActions(
        onSearchQueryChange = {},
        onLabelSelected = {},
        onClearSearch = {},
        onNavigateToSettings = {},
        onAddContact = {},
        onRequestPermission = {},
        onAddGiftIdea = {},
        onToggleGiftIdea = { _, _, _ -> },
        onUpdateGiftIdeaText = { _, _, _ -> },
        onDeleteGiftIdea = { _, _ -> },
        onUpdateBirthday = { _, _ -> },
        onOpenContact = { _, _ -> },
        onDial = {},
        onSendSms = {},
        onOpenMessengerApp = { _, _ -> },
        onRefresh = {}
    )

    BirthdayBuddyTheme {
        HomeTopBar(
            searchQuery = "",
            animatedPlaceholder = "Geburtstage suchen",
            availableLabels = listOf("Familie", "Freunde"),
            selectedLabel = null,
            isFilterBarVisible = true,
            actions = actions,
            searchFocusRequester = remember { FocusRequester() },
        )
    }
}
