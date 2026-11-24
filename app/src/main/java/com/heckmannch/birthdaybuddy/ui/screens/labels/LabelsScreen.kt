package com.heckmannch.birthdaybuddy.ui.screens.labels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayItem
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Screen displaying contacts filtered or grouped by their assigned labels.
 *
 * Uses the existing BirthdayBuddy cards ([BirthdayItem]) to show contacts cleanly
 * according to selected label chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    uiState: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    bottomBarHeight: Dp = 0.dp,
) {
    val haptic = LocalHapticFeedback.current
    val allContacts = uiState.contacts ?: emptyList()
    val availableLabels = uiState.availableLabels

    var activeLabelFilter by remember { mutableStateOf<String?>(null) }
    var expandedContactId by remember { mutableStateOf<String?>(null) }

    val filteredContacts = remember(allContacts, activeLabelFilter) {
        if (activeLabelFilter == null) {
            allContacts.filter { it.labels.isNotEmpty() }
        } else {
            allContacts.filter { it.labels.contains(activeLabelFilter) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.labels_screen_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Label filter chip row
            if (availableLabels.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(
                        start = SpacingNormal,
                        end = SpacingNormal,
                        bottom = SpacingSmall,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                ) {
                    item {
                        FilterChip(
                            selected = activeLabelFilter == null,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeLabelFilter = null
                            },
                            label = { Text(stringResource(R.string.home_filter_all)) },
                        )
                    }

                    items(
                        items = availableLabels,
                        key = { it },
                    ) { label ->
                        FilterChip(
                            selected = activeLabelFilter == label,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeLabelFilter = if (activeLabelFilter == label) null else label
                            },
                            label = { Text(label.toDisplayLabel()) },
                        )
                    }
                }
            }

            // Contact list filtered by label
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            ) {
                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = SpacingNormal)
                            .padding(bottom = bottomBarHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.labels_screen_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = SpacingNormal,
                            end = SpacingNormal,
                            top = SpacingSmall,
                            bottom = bottomBarHeight + SpacingMedium,
                        ),
                        verticalArrangement = Arrangement.spacedBy(SpacingSmall),
                    ) {
                        itemsIndexed(
                            items = filteredContacts,
                            key = { _, contact -> contact.id },
                        ) { _, contact ->
                            val isExpanded = expandedContactId == contact.id
                            BirthdayItem(
                                contact = contact,
                                isExpanded = isExpanded,
                                newlyAddedIdeaId = null,
                                onExpand = {
                                    expandedContactId = if (isExpanded) null else contact.id
                                },
                                actions = actions,
                            )
                        }
                    }
                }
            }
        }
    }
}
