package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import android.Manifest
import android.os.Build
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
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.heckmannch.birthdaybuddy.ui.theme.AnimDelayPermission
import com.heckmannch.birthdaybuddy.ui.theme.AnimDurationMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.util.findActivity
import com.heckmannch.birthdaybuddy.util.openAppSettings
import com.heckmannch.birthdaybuddy.util.shouldShowRationale
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Represents the distinct steps/pages in the multi-step onboarding flow.
 *
 * Provides a type-safe abstraction over onboarding page indices to avoid error-prone
 * hardcoded page numbers when conditionally displaying optional pages such as [CALENDAR_GUIDE].
 */
enum class OnboardingStep {
    WELCOME,
    CONTACTS,
    NOTIFICATIONS,
    CALENDAR,
    CALENDAR_GUIDE,
    READY;

    companion object {
        /**
         * Resolves the ordered sequence of active onboarding steps based on whether the calendar guide
         * should be displayed.
         *
         * @param showCalendarGuide `true` if the user enabled calendar sync and granted calendar permissions,
         *                          inserting the [CALENDAR_GUIDE] step before [READY].
         * @return An immutable [List] of [OnboardingStep] representing the configured navigation flow.
         */
        fun getSteps(showCalendarGuide: Boolean): List<OnboardingStep> {
            return buildList {
                add(WELCOME)
                add(CONTACTS)
                add(NOTIFICATIONS)
                add(CALENDAR)
                if (showCalendarGuide) {
                    add(CALENDAR_GUIDE)
                }
                add(READY)
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinish: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = rememberCoroutineScope()

    val onPermissionGrantedSuccess: () -> Unit = {
        viewModel.onIntent(OnboardingIntent.RefreshPermissions)
        scope.launch {
            kotlinx.coroutines.delay(AnimDelayPermission.milliseconds)
            viewModel.onIntent(OnboardingIntent.SetCurrentPage(uiState.currentPage + 1))
        }
    }

    val contactLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionGrantedSuccess()
            } else {
                viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            }
        }

    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                onPermissionGrantedSuccess()
            } else {
                viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            }
        }

    val calendarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
            if (granted) {
                onPermissionGrantedSuccess()
            } else {
                viewModel.onIntent(OnboardingIntent.RefreshPermissions)
            }
        }

    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(OnboardingIntent.RefreshPermissions)
        onPauseOrDispose { }
    }

    val onRequestContactPermission: () -> Unit = {
        val shouldShowRationale =
            activity?.shouldShowRationale(Manifest.permission.READ_CONTACTS) ?: false
        if (shouldShowRationale || !uiState.hasContactPermission) {
            contactLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            context.openAppSettings()
        }
    }

    val onRequestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val shouldShowRationale =
                activity?.shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS)
                    ?: false
            if (shouldShowRationale || !uiState.hasNotificationPermission) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.openAppSettings()
            }
        } else {
            onPermissionGrantedSuccess()
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
    val steps = remember(showCalendarGuide) { OnboardingStep.getSteps(showCalendarGuide) }
    val pagerState = rememberPagerState { steps.size }

    LaunchedEffect(uiState.currentPage, pagerState.pageCount) {
        if (uiState.currentPage != pagerState.currentPage && uiState.currentPage in 0 until pagerState.pageCount) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    val currentStep = steps.getOrElse(pagerState.currentPage) { OnboardingStep.WELCOME }

    val ambientColor by animateColorAsState(
        targetValue = when (currentStep) {
            OnboardingStep.WELCOME -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaContainerSubtle)
            OnboardingStep.CONTACTS -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = AlphaContainerSubtle)
            OnboardingStep.NOTIFICATIONS -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = AlphaContainerSubtle)
            OnboardingStep.CALENDAR -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = AlphaContainerSubtle)
            OnboardingStep.CALENDAR_GUIDE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = AlphaContainerSubtle)
            OnboardingStep.READY -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = AlphaContainerSubtle)
        },
        animationSpec = tween(durationMillis = AnimDurationMedium),
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
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars)
                .union(WindowInsets.displayCutout),
            bottomBar = {
                OnboardingFooter(
                    currentPage = pagerState.currentPage,
                    pageCount = pagerState.pageCount,
                    isNextEnabled = when (currentStep) {
                        OnboardingStep.WELCOME -> true
                        OnboardingStep.CONTACTS -> !contactsEnabled || uiState.hasContactPermission
                        OnboardingStep.NOTIFICATIONS -> !notificationsEnabled || uiState.hasNotificationPermission
                        OnboardingStep.CALENDAR -> !calendarEnabled || uiState.hasCalendarPermission
                        OnboardingStep.CALENDAR_GUIDE,
                        OnboardingStep.READY -> true
                    },
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
                when (steps[page]) {
                    OnboardingStep.WELCOME -> WelcomePage()
                    OnboardingStep.CONTACTS -> ContactsPage(
                        enabled = contactsEnabled,
                        onEnabledChange = { contactsEnabled = it },
                        isGranted = uiState.hasContactPermission,
                        onGrant = onRequestContactPermission
                    )

                    OnboardingStep.NOTIFICATIONS -> NotificationsPage(
                        enabled = notificationsEnabled,
                        onEnabledChange = { notificationsEnabled = it },
                        persistent = uiState.isPersistentNotificationEnabled,
                        onPersistentChange = {
                            onIntent(
                                OnboardingIntent.SetPersistentNotifications(
                                    it
                                )
                            )
                        },
                        isGranted = uiState.hasNotificationPermission,
                        onGrant = onRequestNotificationPermission
                    )

                    OnboardingStep.CALENDAR -> CalendarPage(
                        enabled = calendarEnabled,
                        onEnabledChange = { calendarEnabled = it },
                        isGranted = uiState.hasCalendarPermission,
                        onGrant = onRequestCalendarPermission
                    )

                    OnboardingStep.CALENDAR_GUIDE -> CalendarGuidePage()

                    OnboardingStep.READY -> ReadyPage(
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
            onIntent = {},
            onRequestContactPermission = {},
            onRequestNotificationPermission = {},
            onRequestCalendarPermission = {},
            onFinish = { _, _, _ -> }
        )
    }
}
