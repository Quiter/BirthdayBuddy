package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Plain State Holder für die UI-Logik des HomeScreens.
 * Kapselt Scroll-Logik, Snackbar-Status, Navigations-Backstack und Fokus-Management.
 */
@Stable
class HomeState(
    val listState: LazyListState,
    val snackbarHostState: SnackbarHostState,
    val searchFocusRequester: FocusRequester,
    val backStack: NavBackStack<NavKey>,
    private val scope: CoroutineScope,
) {

    var hasAttemptedContactPermission by mutableStateOf(false)
    var resetScrollRequested by mutableStateOf(false)
    var animatedPlaceholder by mutableStateOf("")
    var isFastScrolling by mutableStateOf(false)
    var newlyAddedIdeaId by mutableStateOf<String?>(null)

    val showScrollUp by derivedStateOf { listState.firstVisibleItemIndex > 0 }

    val selectedContactId: String?
        get() = (backStack.lastOrNull() as? HomeNavKey.ContactDetail)?.contactId

    /**
     * Steuert den Sperr-Status während des schnellen Scrollens.
     */
    fun onSetFastScrolling(isScrolling: Boolean) {
        isFastScrolling = isScrolling
    }

    /**
     * Scrollt die Liste zum Anfang.
     */
    fun scrollToTop(animate: Boolean = true) {
        scope.launch {
            if (animate) listState.animateScrollToItem(0)
            else listState.scrollToItem(0)
        }
    }

    /**
     * Führt einen robusten Scroll-Reset durch (nützlich bei Filteränderungen).
     */
    suspend fun performScrollReset(onComplete: () -> Unit) {
        listState.scrollToItem(0)
        onComplete()
        resetScrollRequested = false
    }
}

/**
 * Erzeugt und merkt sich einen [HomeState].
 */
@Composable
fun rememberHomeState(
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
    backStack: NavBackStack<NavKey> = rememberNavBackStack(HomeNavKey.ContactList),
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember(listState, snackbarHostState, searchFocusRequester, backStack, scope) {
    HomeState(listState, snackbarHostState, searchFocusRequester, backStack, scope)
}
