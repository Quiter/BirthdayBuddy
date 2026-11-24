package com.heckmannch.birthdaybuddy.ui.screens.home.components.header

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R

/**
 * State holder for the search field state in the home screen header.
 *
 * Inspired by Morphe Manager's `SearchFieldState`. Manages search visibility,
 * active search query string, and filtering state.
 *
 * @param initialQuery Initial query string populated when search is initialized.
 */
@Stable
class SearchFieldState internal constructor(initialQuery: String = "") {
    var visible by mutableStateOf(initialQuery.isNotBlank())
        private set

    var query by mutableStateOf(initialQuery)

    /**
     * Whether the search query is currently active and filtering contacts.
     */
    val isFiltering: Boolean get() = query.isNotBlank()

    /**
     * Toggles search mode between expanded and collapsed.
     */
    fun toggle() {
        if (visible) collapse() else open()
    }

    /**
     * Opens search mode.
     */
    fun open() {
        visible = true
    }

    /**
     * Collapses search mode and clears the current query string.
     */
    fun collapse() {
        query = ""
        visible = false
    }

    companion object {
        /**
         * State saver for preserving search state across configuration changes and process death.
         */
        val Saver = listSaver<SearchFieldState, Any>(
            save = { listOf(it.visible, it.query) },
            restore = { (visible, query) ->
                SearchFieldState().apply {
                    this.visible = visible as Boolean
                    this.query = query as String
                }
            }
        )
    }
}

/**
 * Remembers a [SearchFieldState] across recompositions.
 *
 * @param searchable Whether search functionality is enabled.
 * @param initialQuery Initial search query.
 */
@Composable
fun rememberSearchFieldState(
    searchable: Boolean = true,
    initialQuery: String = ""
): SearchFieldState {
    val state = rememberSaveable(saver = SearchFieldState.Saver) { SearchFieldState(initialQuery) }

    LaunchedEffect(searchable) {
        if (!searchable) state.collapse()
    }

    return state
}

/**
 * Routes system back gesture to collapse [SearchFieldState] when search mode is active.
 *
 * @param state Active [SearchFieldState].
 */
@Composable
fun SearchFieldBackHandler(state: SearchFieldState) {
    BackHandler(enabled = state.visible) {
        state.collapse()
    }
}

/**
 * Title action icon button to toggle search mode on and off.
 *
 * @param state Managed [SearchFieldState].
 * @param label Content description for accessibility.
 * @param enabled Whether search toggle is clickable.
 * @param modifier Optional [Modifier].
 */
@Composable
fun SearchToggleAction(
    state: SearchFieldState,
    label: String = stringResource(R.string.cd_open_search),
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = { state.toggle() },
        enabled = enabled,
        modifier = modifier.testTag(if (state.visible) "close_search_button" else "open_search_button"),
    ) {
        Icon(
            imageVector = if (state.visible) Icons.Default.Close else Icons.Default.Search,
            contentDescription = if (state.visible) stringResource(R.string.cd_close_search) else label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
