package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import androidx.window.core.layout.WindowSizeClass
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.model.OnboardingUiState
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.CalendarGuidePage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.CalendarPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.ContactsPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.NotificationsPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.OnboardingFooter
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.ReadyPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.WelcomePage
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerSubtle
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.util.PermissionHelper
import com.heckmannch.birthdaybuddy.util.findActivity
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    windowSizeClass: WindowSizeClass,
    onFinish: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val contactLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            if (isGranted) {
                scope.launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    viewModel.onIntent(OnboardingIntent.SetCurrentPage(uiState.currentPage + 1))
                }
            }
        }

    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            if (isGranted) {
                scope.launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    viewModel.onIntent(OnboardingIntent.SetCurrentPage(uiState.currentPage + 1))
                }
            }
        }

    val calendarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
            viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            if (granted) {
                scope.launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    viewModel.onIntent(OnboardingIntent.SetCurrentPage(uiState.currentPage + 1))
                }
            }
        }

    val permissionHelper = remember(activity) { activity?.let { PermissionHelper(it) } }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onRequestContactPermission: () -> Unit = {
        val shouldShowRationale = permissionHelper?.shouldShowRationale(Manifest.permission.READ_CONTACTS) ?: false
        if (shouldShowRationale || !uiState.hasContactPermission) {
            contactLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    val onRequestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shouldShowRationale = permissionHelper?.shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS) ?: false
            if (shouldShowRationale || !uiState.hasNotificationPermission) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                }
            }
        } else {
            viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            scope.launch {
                kotlinx.coroutines.delay(300.milliseconds)
                viewModel.onIntent(OnboardingIntent.SetCurrentPage(uiState.currentPage + 1))
            }
        }
    }

    val onRequestCalendarPermission: () -> Unit = {
        calendarLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    OnboardingContent(
        uiState = uiState,
        windowSizeClass = windowSizeClass,
        onIntent = { viewModel.onIntent(it) },
        onRequestContactPermission = onRequestContactPermission,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onRequestCalendarPermission = onRequestCalendarPermission,
        onFinish = { _, notificationsEnabled, calendarEnabled ->
            viewModel.onIntent(
                OnboardingIntent.CompleteOnboarding(
                    notificationsEnabled = notificationsEnabled && uiState.hasNotificationPermission,
                    calendarSyncEnabled = calendarEnabled && uiState.hasCalendarPermission
                )
            )
            onFinish()
        }
    )
}

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    windowSizeClass: WindowSizeClass,
    onIntent: (OnboardingIntent) -> Unit,
    onRequestContactPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onFinish: (contactsEnabled: Boolean, notificationsEnabled: Boolean, calendarEnabled: Boolean) -> Unit,
) {
    var contactsEnabled by remember { mutableStateOf(value = true) }
    var notificationsEnabled by remember { mutableStateOf(value = true) }
    var calendarEnabled by remember { mutableStateOf(value = true) }

    val showCalendarGuide = calendarEnabled && uiState.hasCalendarPermission
    val pagerState = rememberPagerState { if (showCalendarGuide) 6 else 5 }

    LaunchedEffect(uiState.currentPage) {
        if (uiState.currentPage != pagerState.currentPage && uiState.currentPage in 0 until pagerState.pageCount) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    val actualPage =
        if (!showCalendarGuide && pagerState.currentPage >= 4) pagerState.currentPage + 1 else pagerState.currentPage

    val ambientColor by animateColorAsState(
        targetValue = when (actualPage) {
            0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaContainerSubtle)
            1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = AlphaContainerSubtle)
            2 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = AlphaContainerSubtle)
            3 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaContainerSubtle)
            4 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = AlphaContainerSubtle)
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = AlphaContainerSubtle)
        },
        animationSpec = tween(durationMillis = 500),
        label = "ambient_color"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ambientColor,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        AppResponsiveScaffold(
            windowSizeClass = windowSizeClass,
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars)
                .union(WindowInsets.displayCutout),
            bottomBar = {
                OnboardingFooter(
                    currentPage = pagerState.currentPage,
                    pageCount = pagerState.pageCount,
                    isNextEnabled = when (pagerState.currentPage) {
                        0 -> true
                        1 -> !contactsEnabled || uiState.hasContactPermission
                        2 -> !notificationsEnabled || uiState.hasNotificationPermission
                        3 -> !calendarEnabled || uiState.hasCalendarPermission
                        else -> true
                    },
                    windowSizeClass = windowSizeClass,
                    onBack = {
                        onIntent(OnboardingIntent.SetCurrentPage(pagerState.currentPage - 1))
                    },
                    onNext = {
                        onIntent(OnboardingIntent.SetCurrentPage(pagerState.currentPage + 1))
                    }
                )
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                userScrollEnabled = false
            ) { page ->
                val actualPage = if (!showCalendarGuide && page >= 4) page + 1 else page
                when (actualPage) {
                    0 -> WelcomePage(windowSizeClass = windowSizeClass)
                    1 -> ContactsPage(
                        windowSizeClass = windowSizeClass,
                        enabled = contactsEnabled,
                        onEnabledChange = { contactsEnabled = it },
                        isGranted = uiState.hasContactPermission,
                        onGrant = onRequestContactPermission
                    )

                    2 -> NotificationsPage(
                        windowSizeClass = windowSizeClass,
                        enabled = notificationsEnabled,
                        onEnabledChange = { notificationsEnabled = it },
                        persistent = uiState.isPersistentNotificationEnabled,
                        onPersistentChange = { onIntent(OnboardingIntent.SetPersistentNotifications(it)) },
                        isGranted = uiState.hasNotificationPermission,
                        onGrant = onRequestNotificationPermission
                    )

                    3 -> CalendarPage(
                        windowSizeClass = windowSizeClass,
                        enabled = calendarEnabled,
                        onEnabledChange = { calendarEnabled = it },
                        isGranted = uiState.hasCalendarPermission,
                        onGrant = onRequestCalendarPermission
                    )

                    4 -> CalendarGuidePage(windowSizeClass = windowSizeClass)

                    5 -> ReadyPage(
                        windowSizeClass = windowSizeClass,
                        hasContactPermission = contactsEnabled && uiState.hasContactPermission,
                        notificationsEnabled = notificationsEnabled && uiState.hasNotificationPermission,
                        calendarSyncEnabled = calendarEnabled && uiState.hasCalendarPermission
                    ) {
                        onFinish(
                            contactsEnabled,
                            notificationsEnabled,
                            calendarEnabled
                        )
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    BirthdayBuddyTheme {
        OnboardingContent(
            uiState = OnboardingUiState(),
            windowSizeClass = WindowSizeClass(360, 640),
            onIntent = {},
            onRequestContactPermission = {},
            onRequestNotificationPermission = {},
            onRequestCalendarPermission = {},
            onFinish = { _, _, _ -> }
        )
    }
}
