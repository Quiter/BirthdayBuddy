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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.ui.screens.onboarding.components.*
import com.heckmannch.birthdaybuddy.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: SettingsViewModel,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    // Lokale States für die Einstellungen während des Onboardings
    var notificationsEnabled by remember { mutableStateOf(true) }
    var persistentEnabled by remember { mutableStateOf(true) }

    var hasContactPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val contactLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasContactPermission = isGranted
        if (isGranted) {
            scope.launch { pagerState.animateScrollToPage(2) }
        }
    }

    val onRequestContactPermission = {
        val activity = context as? Activity
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS)
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
            } catch (_: Exception) {}
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasNotifPermission = isGranted
        if (isGranted) {
            scope.launch { pagerState.animateScrollToPage(3) }
        }
    }

    Scaffold(
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
                },
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> ContactsPage(
                        isGranted = hasContactPermission,
                        onGrant = onRequestContactPermission,
                        onSkip = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        }
                    )
                    2 -> NotificationsPage(
                        enabled = notificationsEnabled,
                        onEnabledChange = { notificationsEnabled = it },
                        persistent = persistentEnabled,
                        onPersistentChange = { persistentEnabled = it },
                        isGranted = hasNotifPermission,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                hasNotifPermission = true
                                scope.launch { pagerState.animateScrollToPage(3) }
                            }
                        }
                    )
                    3 -> ReadyPage(
                        hasContactPermission = hasContactPermission,
                        notificationsEnabled = notificationsEnabled && hasNotifPermission,
                        onStart = {
                            viewModel.setPersistentNotifications(persistentEnabled)
                            viewModel.completeOnboarding(notificationsEnabled && hasNotifPermission)
                            onFinish()
                        }
                    )
                }
            }
        }
    }
}
