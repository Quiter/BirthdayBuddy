package com.heckmannch.birthdaybuddy.ui.screens.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.components.*
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy.viewmodel.ContactUiModel
import com.heckmannch.birthdaybuddy.viewmodel.HomeUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Der Hauptbildschirm der App.
 * Orchestriert die Suche, Filterung, Geburtstagsliste und die Fast-Scrollbar.
 */
@Composable
fun HomeScreen(
    viewModel: BirthdayViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val homeState = rememberHomeState()
    
    val appPlaceholder = stringResource(R.string.home_placeholder_app)
    val searchPlaceholder = stringResource(R.string.home_placeholder_search)
    val onboardingNotifMsg = stringResource(R.string.onboarding_notif_enabled_msg)

    // --- Launchers ---
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.syncContacts()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        viewModel.setNotificationsEnabled(isGranted)
        viewModel.setOnboardingCompleted(true)
    }

    // --- Effekte ---
    LaunchedEffect(Unit) {
        homeState.animatedPlaceholder = appPlaceholder
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.READ_CONTACTS) 
        else viewModel.syncContacts()
        
        delay(2000)
        homeState.animatedPlaceholder = searchPlaceholder
    }

    // Scroll-Reset Trigger (Widget Events oder Filterwechsel)
    LaunchedEffect(viewModel.scrollToTopEvent) {
        viewModel.scrollToTopEvent.collectLatest {
            homeState.resetScrollRequested = true
            viewModel.setIsResettingFilter(true)
        }
    }

    LaunchedEffect(uiState.searchQuery, uiState.selectedLabel) {
        homeState.resetScrollRequested = true
        viewModel.setIsResettingFilter(true)
    }

    // Durchführung des Scroll-Resets
    LaunchedEffect(uiState.contacts) {
        if (homeState.resetScrollRequested && uiState.contacts != null) {
            homeState.performScrollReset { viewModel.setIsResettingFilter(false) }
        }
    }

    LaunchedEffect(homeState.listState.isScrollInProgress) {
        if (homeState.listState.isScrollInProgress) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // --- Onboarding ---
    if (uiState.contacts != null && !onboardingCompleted && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
        OnboardingDialog(
            onConfirm = {
                viewModel.setOnboardingCompleted(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setNotificationsEnabled(true)
                }
                scope.launch { homeState.snackbarHostState.showSnackbar(onboardingNotifMsg) }
            },
            onDismiss = {
                viewModel.setOnboardingCompleted(true)
            }
        )
    }

    // --- Callbacks ---
    val onRequestPermission = remember(context, homeState) {
        {
            val activity = context as? Activity
            val shouldShowRationale = activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS) } ?: false
            if (shouldShowRationale || !homeState.hasAttemptedContactPermission) {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                homeState.hasAttemptedContactPermission = true
            } else {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.fromParts("package", context.packageName, null) }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    HomeContent(
        uiState = uiState,
        homeState = homeState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onLabelSelected = viewModel::onLabelSelected,
        onClearSearch = {
            viewModel.onSearchQueryChange("")
            focusManager.clearFocus()
            keyboardController?.hide()
        },
        onNavigateToSettings = onNavigateToSettings,
        onAddContact = {
            val intent = Intent(Intent.ACTION_INSERT).apply { type = ContactsContract.Contacts.CONTENT_TYPE }
            context.startActivity(intent)
        },
        onRequestPermission = onRequestPermission,
        onSetSwipeHintShown = viewModel::setSwipeHintShown,
        onUpdateGiftIdeas = viewModel::updateGiftIdeas,
        onOpenContact = { id, key ->
            try {
                val lookupUri = ContactsContract.Contacts.getLookupUri(id.toLong(), key)
                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
            } catch (_: Exception) {}
        },
        onRefresh = { viewModel.syncContacts(showLoading = true) },
    )
}

/**
 * Plain State Holder für die UI-Logik des HomeScreens.
 */
@Stable
class HomeState(
    val listState: LazyListState,
    val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    var hasAttemptedContactPermission by mutableStateOf(false)
    var resetScrollRequested by mutableStateOf(false)
    var animatedPlaceholder by mutableStateOf("")
    var filterVisibilityLock by mutableStateOf<Boolean?>(null)
    var isFastScrolling by mutableStateOf(false)

    val showScrollUp by derivedStateOf { listState.firstVisibleItemIndex > 0 }

    fun isFilterBarVisible(isResetting: Boolean): Boolean {
        return if (isResetting) true else filterVisibilityLock ?: (listState.firstVisibleItemIndex == 0)
    }

    fun onSetFastScrolling(isScrolling: Boolean) {
        isFastScrolling = isScrolling
        filterVisibilityLock = if (isScrolling) (listState.firstVisibleItemIndex == 0) else null
    }

    fun scrollToTop(animate: Boolean = true) {
        scope.launch {
            if (animate) listState.animateScrollToItem(0)
            else listState.scrollToItem(0)
        }
    }

    suspend fun performScrollReset(onComplete: () -> Unit) {
        scrollToTop(animate = false)
        delay(100)
        scrollToTop(animate = false)
        onComplete()
        resetScrollRequested = false
    }
}

@Composable
fun rememberHomeState(
    listState: LazyListState = rememberLazyListState(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scope: CoroutineScope = rememberCoroutineScope(),
) = remember(listState, snackbarHostState, scope) { HomeState(listState, snackbarHostState, scope) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    uiState: HomeUiState,
    homeState: HomeState,
    onSearchQueryChange: (String) -> Unit,
    onLabelSelected: (String?) -> Unit,
    onClearSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddContact: () -> Unit,
    onRequestPermission: () -> Unit,
    onSetSwipeHintShown: () -> Unit,
    onUpdateGiftIdeas: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onRefresh: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    
    // Optimierung: Filter-Sichtbarkeit in derivedStateOf kapseln, damit HomeContent 
    // nicht bei jedem Scroll-Pixel re-composed.
    val isFilterBarVisible by remember(uiState.isResettingFilter, homeState) {
        derivedStateOf { homeState.isFilterBarVisible(uiState.isResettingFilter) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = homeState.snackbarHostState) },
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                animatedPlaceholder = homeState.animatedPlaceholder,
                availableLabels = uiState.availableLabels,
                selectedLabel = uiState.selectedLabel,
                isFilterBarVisible = isFilterBarVisible,
                onSearchQueryChange = onSearchQueryChange,
                onLabelSelected = onLabelSelected,
                onNavigateToSettings = onNavigateToSettings,
                onClearSearch = onClearSearch,
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !homeState.isFastScrolling,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                HomeFAB(
                    showScrollUp = homeState.showScrollUp,
                    onAddContact = onAddContact,
                    onScrollToTop = {
                        focusManager.clearFocus()
                        homeState.scrollToTop()
                    },
                    modifier = Modifier.padding(8.dp) // Bewegt den FAB zusätzlich 8dp vom Rand weg
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isSyncing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            BirthdayList(
                contacts = uiState.contacts, // Hier das ?: emptyList() entfernt
                swipeHintShown = uiState.swipeHintShown,
                listState = homeState.listState,
                onRequestPermission = onRequestPermission,
                onSetSwipeHintShown = onSetSwipeHintShown,
                onUpdateGiftIdeas = onUpdateGiftIdeas,
                onOpenContact = onOpenContact,
            )

            FastScrollbar(
                listState = homeState.listState,
                contacts = uiState.contacts ?: emptyList(),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                isResettingFilter = uiState.isResettingFilter,
                onSetFastScrolling = { homeState.onSetFastScrolling(it) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun HomePreview() {
    val sampleContacts = listOf(
        ContactUiModel(
            id = "1",
            contactId = "1",
            lookupKey = "k1",
            fullName = "Max Mustermann",
            dateText = "12. Mai",
            monthName = "Mai",
            imageUri = null,
            initials = "M",
            nextAge = 30,
            daysUntilNext = 5,
            isToday = false,
            labels = listOf("Freunde"),
            giftIdeas = emptyList()
        ),
        ContactUiModel(
            id = "2",
            contactId = "2",
            lookupKey = "k2",
            fullName = "Erika Mustermann",
            dateText = "Heute",
            monthName = "Mai",
            imageUri = null,
            initials = "E",
            nextAge = 40,
            daysUntilNext = 0,
            isToday = true,
            labels = listOf("Familie"),
            giftIdeas = emptyList()
        )
    )
    BirthdayBuddyTheme {
        HomeContent(
            uiState = HomeUiState(
                contacts = sampleContacts,
                availableLabels = listOf("Familie", "Freunde", "Arbeit")
            ),
            homeState = rememberHomeState(),
            onSearchQueryChange = {},
            onLabelSelected = {},
            onClearSearch = {},
            onNavigateToSettings = {},
            onAddContact = {},
            onRequestPermission = {},
            onSetSwipeHintShown = {},
            onUpdateGiftIdeas = { _, _ -> },
            onOpenContact = { _, _ -> },
            onRefresh = {}
        )
    }
}
