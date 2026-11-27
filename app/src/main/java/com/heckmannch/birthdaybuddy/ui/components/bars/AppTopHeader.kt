package com.heckmannch.birthdaybuddy.ui.components.bars

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Central top header component defining design standards for all app headers.
 *
 * Displays a title, optional navigation icon, optional action icons, and an integrated
 * search action button that expands into a search bar when activated.
 *
 * Adheres strictly to BirthdayBuddy padding and design guidelines:
 * Maintains a fixed vertical container height without additive top/bottom padding jumps
 * during [AnimatedContent] search transitions.
 *
 * @param title Screen title text.
 * @param modifier Optional [Modifier] for the top surface container.
 * @param isSearchActive Whether search bar mode is currently active.
 * @param searchQuery Current search text query.
 * @param onSearchQueryChange Callback when search text changes.
 * @param onSearchActiveChange Callback invoked when search mode is toggled (opened or closed).
 * @param searchPlaceholder Hint text inside the search bar.
 * @param onClearSearch Callback when search query is cleared.
 * @param focusRequester Focus requester for search input field.
 * @param showSearchButton Whether search action icon is displayed in header mode.
 * @param navigationIcon Optional composable to display leading icon (e.g., drawer toggle or back button).
 * @param actions Slot for rendering additional action icon buttons in the top header.
 */
@Composable
fun AppTopHeader(
    title: String,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActiveChange: (Boolean) -> Unit = {},
    searchPlaceholder: String = stringResource(R.string.home_placeholder_search),
    onClearSearch: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    showSearchButton: Boolean = true,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) = AppTopHeader(
    titleContent = {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    },
    modifier = modifier,
    isSearchActive = isSearchActive,
    searchQuery = searchQuery,
    onSearchQueryChange = onSearchQueryChange,
    onSearchActiveChange = onSearchActiveChange,
    searchPlaceholder = searchPlaceholder,
    onClearSearch = onClearSearch,
    focusRequester = focusRequester,
    showSearchButton = showSearchButton,
    navigationIcon = navigationIcon,
    actions = actions,
)

/**
 * Overloaded [AppTopHeader] accepting a custom [titleContent] composable.
 */
@Composable
fun AppTopHeader(
    titleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSearchActiveChange: (Boolean) -> Unit = {},
    searchPlaceholder: String = stringResource(R.string.home_placeholder_search),
    onClearSearch: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    showSearchButton: Boolean = true,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    if (isSearchActive) {
        BackHandler {
            onSearchActiveChange(false)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_top_header"),
    ) {
        Box(
            modifier = Modifier
                .padding(top = topPadding, bottom = SpacingSmall)
                .fillMaxWidth()
                .height(SearchBarHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "TopHeaderSearchTransition",
                modifier = Modifier.fillMaxWidth(),
            ) { active ->
                if (active) {
                    val focusManager = LocalFocusManager.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("search_field"),
                            placeholder = { Text(searchPlaceholder) },
                            singleLine = true,
                            shape = CircleShape,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            onSearchQueryChange("")
                                            onClearSearch()
                                        },
                                        modifier = Modifier.testTag("clear_search_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.width(SpacingSmall))

                        IconButton(
                            onClick = { onSearchActiveChange(false) },
                            modifier = Modifier.testTag("close_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_close_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingNormal),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (navigationIcon != null) {
                            navigationIcon()
                            Spacer(modifier = Modifier.width(SpacingSmall))
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            titleContent()
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            actions()
                            if (showSearchButton) {
                                IconButton(
                                    onClick = { onSearchActiveChange(true) },
                                    modifier = Modifier.testTag("open_search_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.cd_open_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTopHeaderNormalPreview() {
    BirthdayBuddyTheme {
        AppTopHeader(
            title = "BirthdayBuddy",
            isSearchActive = false,
            searchQuery = "",
            onSearchQueryChange = {},
            onSearchActiveChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppTopHeaderSearchActivePreview() {
    BirthdayBuddyTheme {
        AppTopHeader(
            title = "BirthdayBuddy",
            isSearchActive = true,
            searchQuery = "Max",
            onSearchQueryChange = {},
            onSearchActiveChange = {},
        )
    }
}
