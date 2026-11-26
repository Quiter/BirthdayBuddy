/*
 * Copyright 2026 BirthdayBuddy / Morphe Architecture.
 */

package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.components.ListScrollbar
import com.heckmannch.birthdaybuddy.ui.components.PinnedHeaderPage
import com.heckmannch.birthdaybuddy.ui.components.ScrollToTopButton
import com.heckmannch.birthdaybuddy.ui.components.buildIndexedScrollTargets
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.screens.home.components.header.HomeHeader
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayItem

/**
 * Morphe-inspired list layout and sections for BirthdayBuddy homescreen.
 * Combines the header and birthday cards in a performant LazyColumn with pinned header,
 * smooth alphabet scrollbar, and Scroll-to-Top button.
 */
@Composable
fun HomeSectionsLayout(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    showSidebar: Boolean = false,
    onToggleSidebar: () -> Unit = {}
) {
    val listState = homeState.listState
    val contacts = uiState.contacts ?: emptyList()

    val alphabetTargets = remember(contacts) {
        buildIndexedScrollTargets(contacts) { contact ->
            contact.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }
    }

    var expandedContactId by remember { mutableStateOf<String?>(null) }

    PinnedHeaderPage(
        header = {
            HomeHeader(
                searchQuery = uiState.searchQuery,
                isSearchActive = uiState.isSearchActive,
                placeholder = homeState.animatedPlaceholder,
                showSidebar = showSidebar,
                focusRequester = homeState.searchFocusRequester,
                actions = actions,
                onToggleSidebar = onToggleSidebar,
                onToggleSearch = actions.onToggleSearch,
            )
        },
        scrollState = listState,
        modifier = modifier
    ) { insets ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = insets.top + 12.dp,
                    bottom = insets.bottom + 16.dp,
                    start = insets.horizontal,
                    end = insets.horizontal
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = contacts,
                    key = { _, contact -> contact.id }
                ) { _, contact ->
                    val isExpanded = expandedContactId == contact.id
                    BirthdayItem(
                        contact = contact,
                        isExpanded = isExpanded,
                        newlyAddedIdeaId = null,
                        onExpand = {
                            expandedContactId = if (isExpanded) null else contact.id
                        },
                        actions = actions
                    )
                }
            }

            ListScrollbar(
                listState = listState,
                alphabetTargets = alphabetTargets,
                alphabetMode = true
            )

            ScrollToTopButton(
                listState = listState,
                extraBottomPadding = insets.bottom
            )
        }
    }
}
