package com.heckmannch.birthdaybuddy.ui.screens.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.CalendarGuidePage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.CalendarPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.ContactsPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.NotificationsPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.OnboardingFooter
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.ReadyPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.WelcomePage
import com.heckmannch.birthdaybuddy.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var contactsEnabled by remember { mutableStateOf(value = true) }
    var notificationsEnabled by remember { mutableStateOf(value = true) }
    var persistentEnabled by remember { mutableStateOf(value = true) }
    var calendarEnabled by remember { mutableStateOf(value = true) }

    var hasContactPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasCalendarPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_CALENDAR
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val showCalendarGuide = calendarEnabled && hasCalendarPermission
    val pagerState = rememberPagerState { if (showCalendarGuide) 6 else 5 }

    val contactLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasContactPermission = isGranted
            if (isGranted) {
                scope.launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }

    val onRequestContactPermission = {
        val activity = context as? Activity
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(
                it,
                Manifest.permission.READ_CONTACTS
            )
        } ?: false

        if (shouldShowRationale || !hasContactPermission) {
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

    val notifLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasNotifPermission = isGranted
            if (isGranted) {
                scope.launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }

    val calendarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                    permissions[Manifest.permission.WRITE_CALENDAR] == true
            hasCalendarPermission = granted
            if (granted) {
                scope.launch {
                    kotlinx.coroutines.delay(300.milliseconds)
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        }

    val onRequestCalendarPermission = {
        calendarLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            )
        )
    }

    val ambientColor by animateColorAsState(
        targetValue = when (pagerState.currentPage) {
            0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
            2 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
            3 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            4 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
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
            windowWidthSizeClass = windowWidthSizeClass,
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                OnboardingFooter(
                    currentPage = pagerState.currentPage,
                    pageCount = pagerState.pageCount,
                    isNextEnabled = when (pagerState.currentPage) {
                        0 -> true
                        1 -> !contactsEnabled || hasContactPermission
                        2 -> !notificationsEnabled || hasNotifPermission
                        3 -> !calendarEnabled || hasCalendarPermission
                        else -> true
                    },
                    windowWidthSizeClass = windowWidthSizeClass,
                    onBack = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    onNext = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                )
            }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                val actualPage = if (!showCalendarGuide && page >= 4) page + 1 else page
                when (actualPage) {
                    0 -> WelcomePage(windowWidthSizeClass = windowWidthSizeClass)
                    1 -> ContactsPage(
                        windowWidthSizeClass = windowWidthSizeClass,
                        enabled = contactsEnabled,
                        onEnabledChange = { contactsEnabled = it },
                        isGranted = hasContactPermission,
                        onGrant = onRequestContactPermission
                    )

                    2 -> NotificationsPage(
                        windowWidthSizeClass = windowWidthSizeClass,
                        enabled = notificationsEnabled,
                        onEnabledChange = { notificationsEnabled = it },
                        persistent = persistentEnabled,
                        onPersistentChange = { persistentEnabled = it },
                        isGranted = hasNotifPermission
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            hasNotifPermission = true
                            scope.launch {
                                kotlinx.coroutines.delay(300.milliseconds)
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }

                    3 -> CalendarPage(
                        windowWidthSizeClass = windowWidthSizeClass,
                        enabled = calendarEnabled,
                        onEnabledChange = { calendarEnabled = it },
                        isGranted = hasCalendarPermission,
                        onGrant = onRequestCalendarPermission
                    )

                    4 -> CalendarGuidePage(windowWidthSizeClass = windowWidthSizeClass)

                    5 -> ReadyPage(
                        windowWidthSizeClass = windowWidthSizeClass,
                        hasContactPermission = contactsEnabled && hasContactPermission,
                        notificationsEnabled = notificationsEnabled && hasNotifPermission,
                        calendarSyncEnabled = calendarEnabled && hasCalendarPermission
                    ) {
                        viewModel.setPersistentNotifications(persistentEnabled)
                        viewModel.completeOnboarding(
                            notificationsEnabled = notificationsEnabled && hasNotifPermission,
                            calendarSyncEnabled = calendarEnabled && hasCalendarPermission
                        )
                        onFinish()
                    }
                }
            }
        }
    }
}
