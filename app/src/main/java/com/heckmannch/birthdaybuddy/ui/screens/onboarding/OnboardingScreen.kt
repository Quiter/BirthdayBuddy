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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.annotation.RawRes
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.viewmodel.SettingsViewModel
import com.airbnb.lottie.compose.*
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
            // Footer mit Dots und Navigation
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

@Composable
private fun WelcomePage() {
    OnboardingPageContent(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc),
        icon = painterResource(R.drawable.ic_app_logo),
        // lottieRes = R.raw.anim_welcome, // Sobald die Datei existiert, hier einkommentieren
        tint = Color.Unspecified
    )
}

@Composable
private fun ContactsPage(isGranted: Boolean, onGrant: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*
        LottieIllustration(
            resId = R.raw.anim_contacts,
            modifier = Modifier.size(200.dp)
        )
        */
        // Fallback Icon solange keine Lottie Animation da ist
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Contacts,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_contacts_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_contacts_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (!isGranted) {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.onboarding_contacts_btn))
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.onboarding_contacts_skip),
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            Text(
                text = stringResource(R.string.onboarding_contacts_granted),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NotificationsPage(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    persistent: Boolean,
    onPersistentChange: (Boolean) -> Unit,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*
        LottieIllustration(
            resId = R.raw.anim_notifications,
            modifier = Modifier.size(200.dp)
        )
        */
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_notif_page_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboarding_notif_page_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Einstellungen
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.onboarding_notif_enable)) },
                    trailingContent = { Switch(checked = enabled, onCheckedChange = onEnabledChange) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (enabled) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.onboarding_notif_persistent)) },
                        trailingContent = { Switch(checked = persistent, onCheckedChange = onPersistentChange) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (enabled && !isGranted) {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.onboarding_notif_btn))
            }
        }
    }
}

@Composable
private fun ReadyPage(
    hasContactPermission: Boolean,
    notificationsEnabled: Boolean,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        /*
        LottieIllustration(
            resId = R.raw.anim_ready,
            modifier = Modifier.size(200.dp)
        )
        */
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_ready_desc),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Zusammenfassung
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_summary_header),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (hasContactPermission) stringResource(R.string.onboarding_summary_contacts_enabled)
                           else stringResource(R.string.onboarding_summary_contacts_disabled),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (notificationsEnabled) stringResource(R.string.onboarding_summary_notif_enabled)
                           else stringResource(R.string.onboarding_summary_notif_disabled),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (hasContactPermission) stringResource(R.string.onboarding_ready_sync_info)
                   else stringResource(R.string.onboarding_ready_no_sync_info),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.onboarding_ready_btn))
        }
    }
}

@Composable
private fun OnboardingPageContent(
    title: String,
    description: String,
    icon: Painter? = null,
    @RawRes lottieRes: Int? = null,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (lottieRes != null) {
            LottieIllustration(
                resId = lottieRes,
                modifier = Modifier.size(200.dp)
            )
        } else if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = tint
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LottieIllustration(
    @RawRes resId: Int,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition,
        iterations = LottieConstants.IterateForever
    )
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

@Composable
private fun OnboardingFooter(
    currentPage: Int,
    pageCount: Int,
    isNextEnabled: Boolean,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Spacer links um Dots zu zentrieren
        Spacer(modifier = Modifier.width(80.dp))

        // Dots
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            repeat(pageCount) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (index == currentPage) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        // Next Button
        if (currentPage < (pageCount - 1)) {
            TextButton(
                onClick = onNext,
                enabled = isNextEnabled,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_next),
                    color = if (isNextEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(80.dp))
        }
    }
}
