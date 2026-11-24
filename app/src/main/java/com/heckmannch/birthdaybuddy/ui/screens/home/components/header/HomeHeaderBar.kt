package com.heckmannch.birthdaybuddy.ui.screens.home.components.header

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
import androidx.compose.ui.text.input.ImeAction
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Responsive top header bar composable modeled after Morphe Manager's `HeaderBar`.
 *
 * Provides a unified top line for navigation affordances, page title/logo, action buttons,
 * and an integrated expandable search field.
 *
 * @param title Content slot for page title or brand logo wordmark.
 * @param modifier Optional [Modifier] for the top surface container.
 * @param navigation Optional navigation icon composable (e.g., sidebar drawer button).
 * @param actions Optional action icon buttons rendered on the right side of the header.
 * @param search State holder managing search query and visibility.
 * @param searchLabel Hint placeholder text for the search field.
 * @param searchEnabled Whether search functionality is enabled.
 * @param focusRequester Focus requester tied to the search text input.
 * @param onClearSearch Callback when search query is cleared.
 */
@Composable
fun HomeHeaderBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigation: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    search: SearchFieldState? = null,
    searchLabel: String = stringResource(R.string.home_placeholder_search),
    searchEnabled: Boolean = true,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onClearSearch: () -> Unit = {},
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isSearchOpen = search?.visible == true

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_top_bar"),
    ) {
        Box(
            modifier = Modifier
                .padding(top = topPadding, bottom = SpacingSmall)
                .fillMaxWidth()
                .height(SearchBarHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            AnimatedContent(
                targetState = isSearchOpen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "HomeHeaderBarSearchTransition",
                modifier = Modifier.fillMaxWidth(),
            ) { searchActive ->
                if (searchActive && search != null) {
                    // Expanded Search Bar Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val focusManager = LocalFocusManager.current

                        TextField(
                            value = search.query,
                            onValueChange = { search.query = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("search_field"),
                            placeholder = { Text(searchLabel) },
                            singleLine = true,
                            shape = CircleShape,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                if (search.query.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            search.query = ""
                                            onClearSearch()
                                        },
                                        modifier = Modifier.testTag("clear_search_button"),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            textStyle = MaterialTheme.typography.bodyLarge,
                        )

                        Spacer(modifier = Modifier.width(SpacingSmall))

                        SearchToggleAction(
                            state = search,
                            enabled = searchEnabled,
                        )
                    }
                } else {
                    // Standard Title & Action Bar Mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingNormal),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (navigation != null) {
                            navigation()
                            Spacer(modifier = Modifier.width(SpacingSmall))
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            title()
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (actions != null) {
                                actions()
                            }
                            if (search != null) {
                                SearchToggleAction(
                                    state = search,
                                    enabled = searchEnabled,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
