package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.layout.FloatingBarsLayout
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.components.layout.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.layout.isWidthCompact
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeNavKey
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeState
import com.heckmannch.birthdaybuddy.ui.screens.home.navigateToContactDetail
import com.heckmannch.birthdaybuddy.ui.screens.home.rememberHomeState
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.DetailDialogScale
import com.heckmannch.birthdaybuddy.ui.theme.SettingsAnimDurationMs
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNone

/**
 * Adaptive list-detail display composable for the home screen using Navigation 3.
 *
 * Coordinates the contact list pane with fast-scrolling and the contact detail pane
 * for compact and wide screens, using Morphe-style fade and subtle scale transitions
 * when opening or closing contact details.
 *
 * @param uiState Current UI state containing contact list, search query, labels, and sync status.
 * @param homeState State holder managing scroll state, search focus, and animations.
 * @param actions User interaction callbacks.
 * @param modifier Optional [Modifier] for the container layout.
 * @param contentPadding Scaffold and system bar insets for inner list scrolling.
 * @param compactListTopBar Optional top bar rendered inside the list pane on compact screens so it
 *   crossfades out cleanly when navigating to the full-screen contact detail pane.
 * @param isRefreshing Whether a pull-to-refresh contact sync is currently active.
 * @param onRefresh Optional pull-to-refresh callback applied exclusively to the contact list pane.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeListDetailDisplay(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(SpacingNone),
    compactListTopBar: (@Composable () -> Unit)? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val layoutDirection = LocalLayoutDirection.current
    val contacts = uiState.contacts

    val getScrollLabel: (ContactUiModel) -> String = remember(uiState.selectedLabel) {
        { contact ->
            if (uiState.selectedLabel == ContactLabels.LABEL_NO_BIRTHDAY) {
                contact.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: ""
            } else {
                contact.monthName
            }
        }
    }

    val windowAdaptiveInfo = LocalWindowAdaptiveInfo.current
    val windowSizeClass = LocalWindowSizeClass.current
    val directive = remember(windowAdaptiveInfo, windowSizeClass) {
        val base = calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = SpacingNone)
        if (windowSizeClass.isWidthCompact) {
            base.copy(maxHorizontalPartitions = 1)
        } else {
            base
        }
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val backStack = homeState.backStack
    val selectedContactId = homeState.selectedContactId

    LaunchedEffect(contacts, selectedContactId) {
        if ((selectedContactId != null) && (contacts?.none { it.id == selectedContactId } == true)) {
            backStack.removeLastOrNull()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(listDetailStrategy),
        transitionSpec = detailEnterTransitionSpec(),
        popTransitionSpec = detailExitTransitionSpec(),
        predictivePopTransitionSpec = { detailExitTransitionSpec().invoke(this) },
        modifier = modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<HomeNavKey.ContactList>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        BirthdayQuotePlaceholder(
                            contentPadding = contentPadding,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                ),
            ) {
                val contactListPane: @Composable (PaddingValues) -> Unit = { listPadding ->
                    val listBody: @Composable () -> Unit = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            BirthdayList(
                                contacts = contacts,
                                newlyAddedIdeaId = null, // Gift idea is added in the detail pane
                                hasContactPermission = uiState.hasContactPermission,
                                listState = homeState.listState,
                                availableLabels = uiState.availableLabels,
                                selectedLabel = uiState.selectedLabel,
                                searchQuery = uiState.searchQuery,
                                actions = actions,
                                coupleSuggestion = uiState.coupleSuggestion,
                                onContactSelected = { contact ->
                                    backStack.navigateToContactDetail(contact.id)
                                },
                                onInteraction = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                },
                                contentPadding = listPadding,
                            )

                            val currentHeaderCount = remember(
                                uiState.selectedLabel,
                                uiState.coupleSuggestion,
                            ) {
                                val currentShowCoupleSuggestion =
                                    (uiState.selectedLabel == ContactLabels.LABEL_ANNIVERSARY) &&
                                        (uiState.coupleSuggestion != null)
                                if (currentShowCoupleSuggestion) 1 else 0
                            }

                            FastScrollbar(
                                listState = homeState.listState,
                                contacts = uiState.contacts ?: emptyList(),
                                getLabel = getScrollLabel,
                                headerCount = currentHeaderCount,
                                contentPadding = listPadding,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight(),
                            ) { homeState.onSetFastScrolling(it) }
                        }
                    }

                    if (onRefresh != null) {
                        val pullToRefreshState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = onRefresh,
                            state = pullToRefreshState,
                            modifier = Modifier.fillMaxSize(),
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullToRefreshState,
                                    isRefreshing = isRefreshing,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = listPadding.calculateTopPadding()),
                                )
                            },
                        ) {
                            listBody()
                        }
                    } else {
                        listBody()
                    }
                }

                if (compactListTopBar != null) {
                    FloatingBarsLayout(
                        modifier = Modifier.fillMaxSize(),
                        topBar = compactListTopBar,
                    ) { headerHeight, _ ->
                        val listPadding = PaddingValues(
                            start = contentPadding.calculateStartPadding(layoutDirection),
                            top = headerHeight,
                            end = contentPadding.calculateEndPadding(layoutDirection),
                            bottom = contentPadding.calculateBottomPadding(),
                        )
                        contactListPane(listPadding)
                    }
                } else {
                    contactListPane(contentPadding)
                }
            }

            entry<HomeNavKey.ContactDetail>(
                metadata = ListDetailSceneStrategy.detailPane(),
            ) { key ->
                val contact = remember(contacts, key.contactId) {
                    contacts?.find { it.id == key.contactId }
                }
                contact?.let { resolvedContact ->
                    val detailPadding = if (compactListTopBar != null) {
                        PaddingValues(SpacingNone)
                    } else {
                        contentPadding
                    }
                    BirthdayDetailPane(
                        contact = resolvedContact,
                        newlyAddedIdeaId = homeState.newlyAddedIdeaId ?: uiState.newlyAddedIdeaId,
                        actions = actions,
                        onClose = {
                            backStack.removeLastOrNull()
                        },
                        contentPadding = detailPadding,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    )
}

/**
 * Morphe-style crossfade and subtle scale-in transition when opening a contact detail view.
 */
private fun detailEnterTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        val enter = fadeIn(
            animationSpec = tween(SettingsAnimDurationMs, easing = LinearOutSlowInEasing),
        ) + scaleIn(
            initialScale = DetailDialogScale,
            animationSpec = tween(SettingsAnimDurationMs, easing = FastOutSlowInEasing),
        )
        val exit = fadeOut(
            animationSpec = tween(SettingsAnimDurationMs, easing = LinearOutSlowInEasing),
        )
        enter togetherWith exit
    }

/**
 * Morphe-style crossfade and subtle scale-out transition when dismissing a contact detail view.
 */
private fun detailExitTransitionSpec(): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform =
    {
        val enter = fadeIn(
            animationSpec = tween(SettingsAnimDurationMs, easing = LinearOutSlowInEasing),
        )
        val exit = fadeOut(
            animationSpec = tween(SettingsAnimDurationMs, easing = LinearOutSlowInEasing),
        ) + scaleOut(
            targetScale = DetailDialogScale,
            animationSpec = tween(SettingsAnimDurationMs, easing = LinearOutSlowInEasing),
        )
        enter togetherWith exit
    }

@Preview(device = Devices.PHONE)
@Composable
private fun HomeListDetailDisplayPreview() {
    BirthdayBuddyTheme {
        HomeListDetailDisplay(
            uiState = SampleData.homeUiState,
            homeState = rememberHomeState(),
            actions = SampleData.homeActions,
        )
    }
}
