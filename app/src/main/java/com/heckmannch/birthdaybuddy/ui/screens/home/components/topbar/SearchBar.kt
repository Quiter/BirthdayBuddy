package com.heckmannch.birthdaybuddy.ui.screens.home.components.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarFocusedElevation
import com.heckmannch.birthdaybuddy.ui.theme.SearchBarHeight
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNone
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

/**
 * A custom search bar composable that supports animated placeholder transitions,
 * dynamic styling based on focus state, query clearing, settings navigation, and optional navigation icons.
 *
 * Internally preserves [TextFieldValue] state to decouple rapid local keyboard typing from asynchronous
 * state hoisted query flows, preventing cursor jumping and out-of-order text insertion.
 *
 * @param query The current text entered into the search field.
 * @param placeholder The animated placeholder text displayed when the search query is empty.
 * @param onQueryChange Callback invoked when the search query changes.
 * @param onClearQuery Callback invoked when the user clicks the clear query icon button.
 * @param onSettingsClick Callback invoked when the user clicks the settings icon button.
 * @param focusRequester The [FocusRequester] used to request focus on the underlying text field.
 * @param modifier The modifier to be applied to the search bar layout.
 * @param navigationIcon Optional composable to display as the leading icon in place of the default search icon.
 */
@Composable
fun SearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSettingsClick: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = query,
                selection = TextRange(query.length)
            )
        )
    }

    // Tracks user-emitted query changes so that delayed asynchronous echoes from upstream StateFlows
    // do not overwrite the user's active typing or reset cursor selection.
    val pendingQueries = remember { mutableListOf<String>() }

    // Synchronize external query changes (e.g. programmatically cleared search, filter resets)
    LaunchedEffect(query) {
        val pendingIndex = pendingQueries.indexOf(query)
        if (pendingIndex != -1) {
            // query is an echo of an in-flight user keystroke; consume up to this query
            for (i in 0..pendingIndex) {
                pendingQueries.removeAt(0)
            }
        } else if (query != textFieldValue.text) {
            // query was modified externally (not by typing inside this search bar)
            pendingQueries.clear()
            textFieldValue = textFieldValue.copy(
                text = query,
                selection = TextRange(query.length)
            )
        }
    }

    val containerColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.surface
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaEmphasisLow),
        label = "SearchBarContainerColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = AlphaEmphasisLow)
        else Color.Transparent,
        label = "SearchBarBorderColor"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) SearchBarBorderWidth else SpacingNone,
        label = "SearchBarBorderWidth"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingNormal)
            .height(SearchBarHeight),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isFocused) SearchBarFocusedElevation else SpacingNone
    ) {
        TextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != query) {
                    pendingQueries.add(newValue.text)
                    onQueryChange(newValue.text)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("search_field")
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = {
                AnimatedContent(
                    targetState = placeholder,
                    transitionSpec = {
                        (slideInVertically { height -> height } + fadeIn(animationSpec = tween(300))) togetherWith
                                (slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(300)))
                    },
                    label = "SearchBarPlaceholderTransition"
                ) { targetPlaceholder ->
                    Text(
                        text = targetPlaceholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium)
                    )
                }
            },
            leadingIcon = {
                if (navigationIcon != null) {
                    navigationIcon()
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingIcon = {
                Row(
                    modifier = Modifier.padding(end = SpacingExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = textFieldValue.text.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(
                            onClick = {
                                textFieldValue = TextFieldValue("")
                                pendingQueries.clear()
                                onClearQuery()
                            },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.home_search_clear)
                            )
                        }
                    }
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.home_settings)
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    BirthdayBuddyTheme {
        SearchBar(
            query = "",
            placeholder = "Search contact",
            onQueryChange = {},
            onClearQuery = {},
            onSettingsClick = {},
            focusRequester = remember { FocusRequester() }
        )
    }
}
