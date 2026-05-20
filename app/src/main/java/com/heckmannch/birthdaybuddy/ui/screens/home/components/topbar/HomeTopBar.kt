package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        // Oberer Teil (Suche) - jetzt ohne Elevation, damit es zur Liste passt
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Spacer(modifier = Modifier.statusBarsPadding())
                SearchBar(
                    query = searchQuery,
                    placeholder = animatedPlaceholder,
                    onQueryChange = actions.onSearchQueryChange,
                    onClearQuery = actions.onClearSearch,
                    onSettingsClick = actions.onNavigateToSettings,
                    focusRequester = searchFocusRequester,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }
        }

        // Unterer Teil (Filter) - nutzt den Standard-Hintergrund des Scaffolds (wie die Liste)
        LabelFilterBar(
            visible = isFilterBarVisible,
            labels = availableLabels,
            selectedLabel = selectedLabel,
            onLabelSelected = actions.onLabelSelected,
        )
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
        onWhatsApp = {},
        onSignal = {},
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
            searchFocusRequester = remember { FocusRequester() }
        )
    }
}
