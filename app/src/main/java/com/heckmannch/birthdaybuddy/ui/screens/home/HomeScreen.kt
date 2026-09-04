package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import coil3.imageLoader
import coil3.request.ImageRequest
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayDatePickerDialog
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.getAvatarCacheKey
import com.heckmannch.birthdaybuddy.ui.util.ContactActions
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

/**
 * [HomeScreen] is the orchestrator and state-coordinating controller for the main dashboard.
 *
 * ### Architectural & Design Patterns
 * - **MVI / UDF (Uni-Directional Data Flow)**: This screen acts as the controller layer. It translates
 *   user interactions into discrete events/commands ([HomeIntent]) propagated to the ViewModel, and
 *   listens to the read-only [HomeUiState] representing the latest UI state.
 * - **Separation of Concerns**: This Composable intercepts platform-specific details:
 *   1. System permissions (e.g., [Manifest.permission.READ_CONTACTS] and [Manifest.permission.WRITE_CONTACTS]).
 *   2. Device/UI feedback elements (e.g., keyboard management, system focus).
 *   3. Context-dependent prefetching (e.g., pre-loading image resources into the Coil cache).
 *   By doing this, the layout renderer [HomeContent] remains a "dumb", stateless UI view. This
 *   enables reliable @Preview rendering and simplifies JVM/Compose instrumented testing because
 *   there are no hidden platform dependencies or Hilt DI container requirements.
 *
 * @param uiState The read-only state representation of the Home screen, providing lists of contacts,
 *   filtering, status information, and animation triggers.
 * @param onIntent Callback lambda used to dispatch user actions ([HomeIntent]) to the underlying ViewModel.
 * @param scrollToTopEvent A [SharedFlow] used to trigger scroll-to-top actions from global events
 *   (e.g., clicking on navigation items, status bar taps).
 * @param onNavigateToSettings Callback executed when the user initiates navigation to the app settings screen.
 */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    scrollToTopEvent: SharedFlow<Unit>,
    onNavigateToSettings: () -> Unit,
) {
    // Android platform CompositionLocals for focus, keyboard, and package/system services context.
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // homeState stores local UI visual states: search focus states, snackbar queues, list scroll state, etc.
    val homeState = rememberHomeState()

    // contactActions acts as a wrapper around Android Intent mechanisms (calling, messaging, contacts app).
    val contactActions = remember(context) { ContactActions(context) }

    // Ensures lambda captures the latest callback reference without triggering recompositions.
    val currentOnNavigateToSettings by rememberUpdatedState(onNavigateToSettings)

    // Preloaded resource string for search placeholder.
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)

    // --- SECTION 1: Android Contact Permissions Handling ---
    // Permission requests handle Android's granular security model:
    // 1. READ_CONTACTS is requested to fetch the names and birthdates.
    // 2. WRITE_CONTACTS is requested optionally to support updates, link/unlink couples, etc.

    /**
     * Unified launcher to request contact permissions ([Manifest.permission.READ_CONTACTS] and
     * [Manifest.permission.WRITE_CONTACTS]).
     * Initiates contact synchronization once read permission is granted.
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readGranted = permissions[Manifest.permission.READ_CONTACTS] == true
            if (readGranted) {
                onIntent(HomeIntent.SyncContacts())
            }
        }

    // --- SECTION 2: UI Coordination, Focus, & Micro-Animations ---

    /**
     * Placeholder Text Micro-Animation:
     * Sets the search bar's hint/placeholder. Shows the friendly "Search..." prompt
     * directly without artificial delay to ensure immediate UI readiness.
     */
    LaunchedEffect(searchPlaceholder) {
        homeState.animatedPlaceholder = searchPlaceholder
    }

    /**
     * External Search Focus Request Handler:
     * Listens for search focus requests triggered programmatically by the ViewModel.
     * Robustly requests focus on the text field and reveals the soft keyboard before
     * consuming the focus event.
     */
    LaunchedEffect(uiState.searchFocusRequested) {
        if (uiState.searchFocusRequested) {
            runCatching {
                homeState.searchFocusRequester.requestFocus()
            }
            keyboardController?.show()
            onIntent(HomeIntent.ConsumeSearchFocus)
        }
    }

    /**
     * Gift Idea Addition Focus Management:
     * Listens for the addition of a new gift idea. Consumes the event directly
     * to reset the state without blocking or artificial delays.
     */
    LaunchedEffect(uiState.newlyAddedIdeaId) {
        if (uiState.newlyAddedIdeaId != null) {
            onIntent(HomeIntent.ConsumeNewlyAddedIdeaId)
        }
    }

    // --- SECTION 3: Performance & Coil Image Prefetching ---

    /**
     * Image Prefetching Optimization:
     * Preloads profile picture avatars of contacts in memory using Coil's imageLoader.enqueue.
     * By asynchronously requesting images before they are laid out in the viewport, we avoid
     * frame drops and scrolling stutters, especially during FastScrollbar operations.
     * Specifying explicit memoryCacheKey and diskCacheKey matching ContactImage guarantees instant
     * retrieval from memory without redundant reads/decodes or disk/provider queries.
     */
    LaunchedEffect(uiState.contacts) {
        prefetchAvatarImages(context, uiState.contacts)
    }

    // --- SECTION 4: Scroll & Soft-Keyboard Interaction Coordination ---

    /**
     * Global Scroll Reset Listener:
     * Collects scroll-to-top requests dispatched from outside (e.g., clicking on home button or top status bar).
     */
    LaunchedEffect(scrollToTopEvent) {
        scrollToTopEvent.collectLatest {
            homeState.resetScrollRequested = true
        }
    }

    /**
     * Robust List Scroll Reset Handler:
     * Triggers the actual scroll reset animation inside the list state. Initiated once contacts are loaded,
     * resetting filters/state and resetting the filter resetting flags on complete.
     */
    LaunchedEffect(uiState.contacts, homeState.resetScrollRequested) {
        if (homeState.resetScrollRequested && (uiState.contacts != null)) {
            homeState.performScrollReset {
                onIntent(HomeIntent.SetIsResettingFilter(isResetting = false))
            }
        }
    }

    /**
     * Soft Keyboard Auto-Dismissal:
     * Detects when the user begins dragging/scrolling the birthday list and automatically hides the soft
     * keyboard while clearing text focus, maximizing readable viewport real estate.
     */
    val isListDragged by homeState.listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isListDragged) {
        if (isListDragged) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // --- SECTION 5: Intent / Callback Action Bindings ---
    // Bundles all local action callbacks into a single [HomeActions] instance.
    // This reduces parameter count and resolves boilerplate prop-drilling in lower layout hierarchy sub-composables.
    val actions =
        remember(onIntent, contactActions, permissionLauncher, homeState) {
            HomeActions(
                // Search bar input queries
                onSearchQueryChange = { query -> onIntent(HomeIntent.SearchQueryChanged(query)) },

                // Label-based filtering selection
                onLabelSelected = { label -> onIntent(HomeIntent.LabelSelected(label)) },

                // Clearing query: clears query text, focus, and hides keyboard
                onClearSearch = {
                    onIntent(HomeIntent.SearchQueryChanged(""))
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },

                // Navigation to app settings
                onNavigateToSettings = { currentOnNavigateToSettings() },

                // Adding a new system contact via implicit intent
                onAddContact = contactActions::addContact,

                // Contact read/write permission requests flow coordination
                onRequestPermission = {
                    contactActions.requestContactPermissions(
                        launcher = permissionLauncher,
                        hasAttemptedBefore = homeState.hasAttemptedContactPermission,
                    ) { homeState.hasAttemptedContactPermission = true }
                },

                // Gift idea lifecycle commands
                onAddGiftIdea = { lookupKey -> onIntent(HomeIntent.AddGiftIdea(lookupKey)) },
                onToggleGiftIdea = { lookupKey, idea, isChecked ->
                    onIntent(HomeIntent.ToggleGiftIdea(lookupKey, idea, isChecked))
                },
                onUpdateGiftIdeaText = { lookupKey, ideaId, newText ->
                    onIntent(HomeIntent.UpdateGiftIdeaText(lookupKey, ideaId, newText))
                },
                onDeleteGiftIdea = { lookupKey, ideaId ->
                    onIntent(HomeIntent.DeleteGiftIdea(lookupKey, ideaId))
                },

                // Birthday edit command
                onUpdateBirthday = { contactId, birthday ->
                    onIntent(HomeIntent.UpdateBirthday(contactId, birthday))
                },
                onOpenBirthdayPicker = { key, year, month, day ->
                    onIntent(HomeIntent.OpenBirthdayPicker(key, year, month, day))
                },

                // Launching external dialers, messenger, and device contacts editor
                onOpenContact = contactActions::openContact,
                onDial = contactActions::dialNumber,
                onSendSms = contactActions::sendSms,
                onOpenMessengerApp = contactActions::openMessengerApp,

                // User-pulled swipe refresh action
                onRefresh = { onIntent(HomeIntent.SyncContacts(showLoading = true)) },

                // Couple suggestion / relationship actions
                onUnlinkCouple = { lookupKey -> onIntent(HomeIntent.UnlinkCouple(lookupKey)) },
                onLinkAsCouple = { key1, key2 -> onIntent(HomeIntent.LinkAsCouple(key1, key2)) },
                onIgnoreCoupleSuggestion = { key1, key2 ->
                    onIntent(HomeIntent.IgnoreCoupleSuggestion(key1, key2))
                },
            )
        }

    // --- SECTION 6: Global Dialog Overlays ---
    if (uiState.pendingBirthdayEdit != null) {
        val edit = uiState.pendingBirthdayEdit
        BirthdayDatePickerDialog(
            initialDate = edit.initialDate,
            onDismissRequest = {
                onIntent(HomeIntent.DismissBirthdayPicker)
            },
            onDateSelected = { date ->
                onIntent(HomeIntent.UpdateBirthday(edit.contactId, date))
            }
        )
    }

    // --- SECTION 7: View Layer Invocation ---
    // Passes state and consolidated actions down to the layout rendering container.
    HomeContent(
        uiState = uiState,
        homeState = homeState,
        actions = actions,
    )
}

/**
 * Asynchronously enqueues avatar prefetch requests into Coil's [ImageLoader].
 *
 * Coil's [ImageLoader.enqueue] is non-blocking and automatically delegates image decoding and
 * caching to its own configured coroutine dispatchers, avoiding main-thread stutter during fast scrolling.
 */
private fun prefetchAvatarImages(
    context: Context,
    contacts: List<ContactUiModel>?,
    limit: Int = 40,
) {
    if (contacts.isNullOrEmpty()) return
    // Limit prefetching to the top most relevant entries to prevent I/O pool starvation and memory cache bloat
    contacts.take(limit).forEach { contact ->
        contact.imageUri?.let { uri ->
            val cacheKey = getAvatarCacheKey(uri, contact.lookupKey)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(coil3.size.Size(256, 256))
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .build()
            context.imageLoader.enqueue(request)
        }
        contact.secondImageUri?.let { secondUri ->
            val secondCacheKey = getAvatarCacheKey(secondUri, null)
            val request = ImageRequest.Builder(context)
                .data(secondUri)
                .size(coil3.size.Size(256, 256))
                .memoryCacheKey(secondCacheKey)
                .diskCacheKey(secondCacheKey)
                .build()
            context.imageLoader.enqueue(request)
        }
    }
}

