package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.layout.PinnedHeaderPage
import com.heckmannch.birthdaybuddy.ui.components.settings.ListSegment
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsEmptyState
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsPageHeader
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSearchBackHandler
import com.heckmannch.birthdaybuddy.ui.components.settings.rememberSettingsSearchState
import com.heckmannch.birthdaybuddy.ui.components.settings.settingsPageBackground
import com.heckmannch.birthdaybuddy.ui.components.settings.verticalScrollFade
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SettingsContentMaxWidth
import com.heckmannch.birthdaybuddy.ui.theme.SettingsSegmentGap
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Metadata for an open-source library used by BirthdayBuddy.
 */
@Immutable
data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: String,
    val description: String,
)

private val OpenSourceLibraries = listOf(
    OpenSourceLibrary("Jetpack Compose", "Google", "Apache 2.0", "Modern toolkit for building native Android UI."),
    OpenSourceLibrary("Material 3", "Google", "Apache 2.0", "Material Design 3 components and theming for Compose."),
    OpenSourceLibrary("Jetpack Navigation 3", "Google", "Apache 2.0", "Type-safe navigation library for Jetpack Compose."),
    OpenSourceLibrary("Jetpack Glance", "Google", "Apache 2.0", "Compose-style framework for Android app widgets."),
    OpenSourceLibrary("Hilt", "Google", "Apache 2.0", "Dependency injection library for Android."),
    OpenSourceLibrary("Room", "Google", "Apache 2.0", "SQLite object mapping library."),
    OpenSourceLibrary("Coroutines & KotlinX Serialization", "JetBrains", "Apache 2.0", "Asynchronous programming and JSON serialization."),
    OpenSourceLibrary("Haze", "Chris Banes", "Apache 2.0", "Glassmorphism and progressive blur effects for Compose."),
    OpenSourceLibrary("MaterialKolor", "Jordy", "MIT", "Dynamic Material You HCT color scheme generation."),
)

/**
 * Morphe-style searchable open-source licenses screen with segmented cards and live search.
 */
@Composable
fun LicensesScreen(
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val searchState = rememberSettingsSearchState()
    val listState = rememberLazyListState()
    SettingsSearchBackHandler(searchState)

    val filteredLibraries = remember(searchState.query) {
        val needle = searchState.query.trim()
        if (needle.isEmpty()) {
            OpenSourceLibraries
        } else {
            OpenSourceLibraries.filter { lib ->
                lib.name.contains(needle, ignoreCase = true) ||
                    lib.author.contains(needle, ignoreCase = true) ||
                    lib.license.contains(needle, ignoreCase = true) ||
                    lib.description.contains(needle, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsPageBackground()),
    ) {
        PinnedHeaderPage(
            scrollState = listState,
            header = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    SettingsPageHeader(
                        title = stringResource(R.string.opensource_licenses),
                        showBackButton = showBackButton,
                        onNavigateBack = onNavigateBack,
                        showSearchButton = true,
                        isSearchActive = searchState.visible,
                        searchQuery = searchState.query,
                        searchPlaceholder = stringResource(R.string.settings_licenses_search),
                        onSearchQueryChange = { searchState.query = it },
                        onSearchActiveChange = { active ->
                            if (active) searchState.open() else searchState.collapse()
                        },
                        onClearSearch = { searchState.clear() },
                    )
                }
            },
        ) { insets ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = SettingsContentMaxWidth)
                        .fillMaxSize()
                        .verticalScrollFade(
                            fadeTop = {
                                listState.firstVisibleItemIndex > 0 ||
                                    listState.firstVisibleItemScrollOffset > 0
                            }
                        )
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(
                        start = insets.horizontal,
                        end = insets.horizontal,
                        top = insets.top + SpacingSmall,
                        bottom = SpacingLarge,
                    ),
                    verticalArrangement = Arrangement.spacedBy(SettingsSegmentGap),
                ) {
                    if (filteredLibraries.isEmpty()) {
                        item(key = "empty") {
                            SettingsEmptyState(
                                title = stringResource(R.string.settings_search_no_results),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredLibraries,
                            key = { _, lib -> lib.name },
                        ) { index, lib ->
                            SettingsItem(
                                onClick = null,
                                title = lib.name,
                                subtitle = "${lib.author} • ${lib.license}\n${lib.description}",
                                icon = Icons.Rounded.Description,
                                segment = ListSegment.of(index, filteredLibraries.size),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LicensesScreenPreview() {
    BirthdayBuddyTheme {
        LicensesScreen(onNavigateBack = {})
    }
}
