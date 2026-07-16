package com.heckmannch.birthdaybuddy.ui.screens.home

import androidx.window.core.layout.WindowSizeClass
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import coil.imageLoader
import coil.request.ImageRequest
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.HomeUiState
import com.heckmannch.birthdaybuddy.ui.util.ContactActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

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
 * @param windowSizeClass Window sizing categories to adapt screen layouts for different device classes
 *   (e.g., compact phones, split-screen, tablets, foldables).
 * @param onNavigateToSettings Callback executed when the user initiates navigation to the app settings screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    scrollToTopEvent: SharedFlow<Unit>,
    windowSizeClass: WindowSizeClass,
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

    // Preloaded resource strings for search placeholder rotation.
    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)

    // --- SECTION 1: Android Contact Permissions Handling ---
    // Permission requests must be handled in two phases to handle Android's granular security model:
    // 1. READ_CONTACTS is requested to fetch the names and birthdates.
    // 2. WRITE_CONTACTS is requested optionally to support updates, link/unlink couples, etc.

    /**
     * Silent launcher to request [Manifest.permission.WRITE_CONTACTS].
     * Triggered if the user has already granted [Manifest.permission.READ_CONTACTS], so the app can
     * perform syncs or edits directly.
     */
    val writePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("HomeScreen", "WRITE_CONTACTS permission granted silently")
                onIntent(HomeIntent.SyncContacts())
            }
        }

    /**
     * Primary launcher triggered when contact permissions are explicitly requested by user action.
     * Requests [Manifest.permission.READ_CONTACTS] first. If successful, it checks if write permissions
     * are still needed and launches the write permission request in sequence. Otherwise, initiates sync.
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_CONTACTS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                } else {
                    onIntent(HomeIntent.SyncContacts())
                }
            }
        }

    /**
     * Startup Permission Check Side-Effect:
     * Automatically attempts to request [Manifest.permission.WRITE_CONTACTS] silently if the user
     * has already granted read access, but write access is somehow missing or revoked.
     */
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
        }
    }

    // --- SECTION 2: UI Coordination, Focus, & Micro-Animations ---

    /**
     * Placeholder Text Micro-Animation:
     * Animates the search bar's hint/placeholder. Shows the application name first, then
     * transitions to a friendly "Search..." prompt after a brief duration to guide user intent.
     */
    LaunchedEffect(Unit) {
        homeState.animatedPlaceholder = appPlaceholder
        delay(2000.milliseconds)
        homeState.animatedPlaceholder = searchPlaceholder
    }

    /**
     * External Search Focus Request Handler:
     * Listens for search focus requests triggered programmatically by the ViewModel.
     * Implements a slight delay to allow navigation transition/morphing animations to settle before
     * requesting focus on the text field and revealing the soft keyboard.
     */
    LaunchedEffect(uiState.searchFocusRequested) {
        if (uiState.searchFocusRequested) {
            delay(500.milliseconds) // Settle transition animation
            homeState.searchFocusRequester.requestFocus()
            keyboardController?.show()
            onIntent(HomeIntent.ConsumeSearchFocus)
        }
    }

    /**
     * Gift Idea Addition Focus Management:
     * Listens for the addition of a new gift idea. Consumes the event after a short delay
     * to ensure the text field focus adapts correctly to the newly inserted text input item.
     */
    LaunchedEffect(uiState.newlyAddedIdeaId) {
        if (uiState.newlyAddedIdeaId != null) {
            delay(100.milliseconds) // Settle keyboard input focus
            onIntent(HomeIntent.ConsumeNewlyAddedIdeaId)
        }
    }

    // --- SECTION 3: Performance & Coil Image Prefetching ---

    // Deduplicate state changes: Only recalculate the list of first 20 contact IDs if the list content changes.
    val firstContactIds = remember(uiState.contacts) {
        uiState.contacts?.take(20)?.map { it.id } ?: emptyList()
    }

    /**
     * Image Prefetching Optimization:
     * Preloads the profile picture avatars of the first 20 visible contacts in memory.
     * By asynchronously requesting images through the Coil imageLoader before they are laid out in the
     * viewport, we avoid frame drops and scrolling stutters. Specifying memoryCacheKey prevents
     * redundant reads/decodes when the list recomposes.
     */
    LaunchedEffect(firstContactIds) {
        val contacts = uiState.contacts
        if (!contacts.isNullOrEmpty()) {
            contacts.take(20)
                .mapNotNull { it.imageUri }
                .forEach { uri ->
                    val request = ImageRequest.Builder(context)
                        .data(uri)
                        .size(150) // Bounds-constrained bitmap scale size
                        .memoryCacheKey(uri) // Identical cache key matches the list item renderer image load
                        .build()
                    context.imageLoader.enqueue(request)
                }
        }
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
                    contactActions.requestContactPermission(
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

    // --- SECTION 6: View Layer Invocation ---
    // Passes state and consolidated actions down to the layout rendering container.
    HomeContent(
        uiState = uiState,
        homeState = homeState,
        actions = actions,
        windowSizeClass = windowSizeClass
    )
}
