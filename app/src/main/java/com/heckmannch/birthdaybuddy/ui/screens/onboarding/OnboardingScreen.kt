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
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.ContactsPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.NotificationsPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.OnboardingFooter
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.ReadyPage
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.WelcomePage
import com.heckmannch.birthdaybuddy.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    windowWidthSizeClass: WindowWidthSizeClass,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 4 }

    // Lokale States für die Einstellungen während des Onboardings
    var notificationsEnabled by remember { mutableStateOf(value = true) }
    var persistentEnabled by remember { mutableStateOf(value = true) }

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

    val contactLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasContactPermission = isGranted
            if (isGranted) {
                scope.launch { pagerState.animateScrollToPage(2) }
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
            // Fallback: Einstellungen öffnen
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
                scope.launch { pagerState.animateScrollToPage(3) }
            }
        }

    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            OnboardingFooter(
                currentPage = pagerState.currentPage,
                pageCount = pagerState.pageCount,
                isNextEnabled = when (pagerState.currentPage) {
                    0 -> true
                    1 -> hasContactPermission
                    2 -> !notificationsEnabled || hasNotifPermission
                    else -> true
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
            when (page) {
                0 -> WelcomePage()
                1 -> ContactsPage(
                    isGranted = hasContactPermission,
                    onGrant = onRequestContactPermission
                ) {
                    scope.launch { pagerState.animateScrollToPage(2) }
                }

                2 -> NotificationsPage(
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
                        scope.launch { pagerState.animateScrollToPage(3) }
                    }
                }

                3 -> ReadyPage(
                    hasContactPermission = hasContactPermission,
                    notificationsEnabled = notificationsEnabled && hasNotifPermission
                ) {
                    viewModel.setPersistentNotifications(persistentEnabled)
                    viewModel.completeOnboarding(notificationsEnabled && hasNotifPermission)
                    onFinish()
                }
            }
        }
    }
}
