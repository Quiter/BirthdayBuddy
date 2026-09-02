package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeNavKey
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeState
import com.heckmannch.birthdaybuddy.ui.screens.home.navigateToContactDetail

/**
 * Adaptive list-detail display composable for the home screen using Navigation 3.
 *
 * Coordinates the contact list pane with fast-scrolling and the contact detail pane
 * for wide screens, automatically handling backstack navigation and selection states.
 *
 * @param uiState Current UI state containing contact list, search query, labels, and sync status.
 * @param homeState State holder managing scroll state, search focus, and animations.
 * @param actions User interaction callbacks.
 * @param showFilterBarInTopBar Whether the label filter bar is displayed in the top bar (disabling it inside the list).
 * @param modifier Optional [Modifier] for the container layout.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeListDetailDisplay(
    uiState: HomeUiState,
    homeState: HomeState,
    actions: HomeActions,
    showFilterBarInTopBar: Boolean,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
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
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
            .copy(horizontalPartitionSpacerSize = 0.dp)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)

    val backStack = rememberNavBackStack(HomeNavKey.ContactList)
    val selectedContactId = (backStack.lastOrNull() as? HomeNavKey.ContactDetail)?.contactId

    LaunchedEffect(contacts, selectedContactId) {
        if (selectedContactId != null && contacts?.none { it.id == selectedContactId } == true) {
            backStack.removeLastOrNull()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        sceneStrategies = listOf(listDetailStrategy),
        modifier = modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<HomeNavKey.ContactList>(
                metadata = ListDetailSceneStrategy.listPane(
                    detailPlaceholder = {
                        BirthdayQuotePlaceholder(
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    BirthdayList(
                        contacts = contacts,
                        newlyAddedIdeaId = null, // Gift idea is added in the right detail pane
                        hasContactPermission = uiState.hasContactPermission,
                        listState = homeState.listState,
                        availableLabels = uiState.availableLabels,
                        selectedLabel = uiState.selectedLabel,
                        searchQuery = uiState.searchQuery,
                        actions = actions,
                        coupleSuggestion = uiState.coupleSuggestion,
                        selectedContactId = selectedContactId,
                        onContactSelected = { contact ->
                            backStack.navigateToContactDetail(contact.id)
                        },
                        onInteraction = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        showLabelFilter = !showFilterBarInTopBar
                    )

                    val currentHeaderCount = remember(
                        uiState.availableLabels,
                        showFilterBarInTopBar,
                        uiState.selectedLabel,
                        uiState.coupleSuggestion
                    ) {
                        val currentShowLabelFilter =
                            uiState.availableLabels.isNotEmpty() && !showFilterBarInTopBar
                        val currentShowCoupleSuggestion =
                            uiState.selectedLabel == ContactLabels.LABEL_ANNIVERSARY && uiState.coupleSuggestion != null
                        (if (currentShowLabelFilter) 1 else 0) + (if (currentShowCoupleSuggestion) 1 else 0)
                    }

                    FastScrollbar(
                        listState = homeState.listState,
                        contacts = uiState.contacts ?: emptyList(),
                        getLabel = getScrollLabel,
                        headerCount = currentHeaderCount,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        onSetFastScrolling = { homeState.onSetFastScrolling(it) },
                    )
                }
            }

            entry<HomeNavKey.ContactDetail>(
                metadata = ListDetailSceneStrategy.detailPane()
            ) { key ->
                val contact = remember(contacts, key.contactId) {
                    contacts?.find { it.id == key.contactId }
                }
                if (contact != null) {
                    BirthdayDetailPane(
                        contact = contact,
                        newlyAddedIdeaId = uiState.newlyAddedIdeaId,
                        actions = actions,
                        onClose = {
                            backStack.removeLastOrNull()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    )
}
