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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Plain State Holder für die UI-Logik des HomeScreens.
 * Kapselt Scroll-Logik, Snackbar-Status und Fokus-Management.
 */
@Stable
class HomeState(
    val listState: LazyListState,
    val snackbarHostState: SnackbarHostState,
    val searchFocusRequester: FocusRequester,
    private val scope: CoroutineScope,
) {
    var hasAttemptedContactPermission by mutableStateOf(false)
    var resetScrollRequested by mutableStateOf(false)
    var animatedPlaceholder by mutableStateOf("")
    var filterVisibilityLock by mutableStateOf<Boolean?>(null)
    var isFastScrolling by mutableStateOf(false)

    val showScrollUp by derivedStateOf { listState.firstVisibleItemIndex > 0 }

    /**
     * Entscheidet, ob die Filterleiste sichtbar sein soll.
     * Sie ist nur sichtbar, wenn wir ganz oben in der Liste sind.
     */
    fun isFilterBarVisible(isResetting: Boolean): Boolean {
        return if (isResetting) true
        else filterVisibilityLock
            ?: (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0)
    }

    /**
     * Steuert den Sperr-Status der Filterleiste während des schnellen Scrollens.
     */
    fun onSetFastScrolling(isScrolling: Boolean) {
        isFastScrolling = isScrolling
        filterVisibilityLock = if (isScrolling) (listState.firstVisibleItemIndex == 0) else null
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
        scrollToTop(animate = false)
        delay(100.milliseconds)
        scrollToTop(animate = false)
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
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember(listState, snackbarHostState, searchFocusRequester, scope) {
    HomeState(listState, snackbarHostState, searchFocusRequester, scope)
}
