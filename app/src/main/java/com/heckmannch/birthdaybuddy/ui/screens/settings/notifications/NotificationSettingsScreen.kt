package com.heckmannch.birthdaybuddy.ui.screens.settings.notifications

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.AppWidthSizeClass
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsSectionHeader
import com.heckmannch.birthdaybuddy.ui.components.SettingsSwitchRow
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.EditRuleDialog
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.NotificationRuleItem
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerHigh
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.ContactImageSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun NotificationSettingsScreen(
    windowWidthSizeClass: AppWidthSizeClass,
    viewModel: NotificationViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationsEnabled = uiState.notificationsEnabled
    val persistentNotifications = uiState.persistentNotifications
    val rules = uiState.notificationRules

    var hasAttemptedPermission by remember { mutableStateOf(false) }

    var hasSystemPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasSystemPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    val state = rememberNotificationSettingsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAttemptedPermission = true
        hasSystemPermission = isGranted
        if (isGranted) {
            viewModel.onIntent(NotificationIntent.SetEnabled(true))
        }
    }

    NotificationSettingsContent(
        windowWidthSizeClass = windowWidthSizeClass,
        notificationsEnabled = notificationsEnabled,
        persistentNotifications = persistentNotifications,
        rules = rules,
        hasSystemPermission = hasSystemPermission,
        state = state,
        showBackButton = showBackButton,
        onToggleNotifications = { enabled ->
            if (enabled) {
                if (hasSystemPermission) {
                    viewModel.onIntent(NotificationIntent.SetEnabled(true))
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onIntent(NotificationIntent.SetEnabled(true))
                    }
                }
            } else {
                viewModel.onIntent(NotificationIntent.SetEnabled(false))
            }
        },
        onTogglePersistent = { viewModel.onIntent(NotificationIntent.SetPersistent(it)) },
        onAddRule = { days, hour, minute -> viewModel.onIntent(NotificationIntent.AddRule(days, hour, minute)) },
        onUpdateRule = { viewModel.onIntent(NotificationIntent.UpdateRule(it)) },
        onDeleteRule = { viewModel.onIntent(NotificationIntent.DeleteRule(it)) },
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = context as? Activity
                val shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                } ?: false

                if (shouldShowRationale || !hasAttemptedPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    hasAttemptedPermission = true
                } else {
                    // Fallback: Systemeinstellungen öffnen, wenn der Dialog nicht mehr erscheint
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            }
        },
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Plain State Holder für die UI-Logik der Benachrichtigungs-Einstellungen.
 * Kapselt die Sichtbarkeit der Dialoge.
 */
@Stable
class NotificationSettingsState {
    var ruleToEdit by mutableStateOf<NotificationRule?>(null)
    var showAddDialog by mutableStateOf(value = false)

    fun openAddDialog() {
        showAddDialog = true
    }

    fun closeAddDialog() {
        showAddDialog = false
    }

    fun openEditDialog(rule: NotificationRule) {
        ruleToEdit = rule
    }

    fun closeEditDialog() {
        ruleToEdit = null
    }
}

@Composable
fun rememberNotificationSettingsState(): NotificationSettingsState {
    return remember { NotificationSettingsState() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsContent(
    windowWidthSizeClass: AppWidthSizeClass,
    notificationsEnabled: Boolean,
    persistentNotifications: Boolean,
    rules: List<NotificationRule>,
    hasSystemPermission: Boolean,
    state: NotificationSettingsState,
    showBackButton: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit,
    onTogglePersistent: (Boolean) -> Unit,
    onAddRule: (Int, Int, Int) -> Unit,
    onUpdateRule: (NotificationRule) -> Unit,
    onDeleteRule: (NotificationRule) -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.notifications_back),
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (notificationsEnabled && hasSystemPermission) {
                FloatingActionButton(onClick = { state.openAddDialog() }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.notifications_add_rule)
                    )
                }
            }
        }
    ) { paddingValues ->
        if (!hasSystemPermission) {
            PermissionRequestState(
                onRequestPermission = onRequestPermission,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            val layoutDirection = LocalLayoutDirection.current
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 340.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = paddingValues.calculateStartPadding(layoutDirection) + SpacingNormal,
                    top = paddingValues.calculateTopPadding() + SpacingNormal,
                    end = paddingValues.calculateEndPadding(layoutDirection) + SpacingNormal,
                    bottom = paddingValues.calculateBottomPadding() + SpacingNormal
                ),
                horizontalArrangement = Arrangement.spacedBy(SpacingNormal),
                verticalArrangement = Arrangement.spacedBy(SpacingNormal)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SettingsCard {
                        SettingsSwitchRow(
                            title = stringResource(R.string.notifications_header),
                            description = stringResource(R.string.notifications_desc),
                            checked = notificationsEnabled,
                            onCheckedChange = onToggleNotifications
                        )

                        if (notificationsEnabled) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaEmphasisLow),
                                modifier = Modifier.padding(horizontal = SpacingNormal)
                            )
                            SettingsSwitchRow(
                                title = stringResource(R.string.notifications_persistent_header),
                                description = stringResource(R.string.notifications_persistent_desc),
                                checked = persistentNotifications,
                                onCheckedChange = onTogglePersistent
                            )
                        }
                    }
                }

                if (notificationsEnabled) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        InfoCard(
                            title = stringResource(R.string.notifications_persistent_info_title),
                            description = stringResource(R.string.notifications_persistent_info_desc),
                            modifier = Modifier
                        )
                    }

                    if (rules.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(SpacingExtraLarge),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.notifications_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SettingsSectionHeader(title = stringResource(R.string.notifications_planned_header))
                                SettingsCard {
                                    rules.forEachIndexed { index, rule ->
                                        NotificationRuleItem(
                                            rule = rule,
                                            onEditRule = { state.openEditDialog(it) },
                                            onDeleteRule = onDeleteRule,
                                        )
                                        if (index < rules.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                    alpha = AlphaEmphasisLow
                                                ),
                                                modifier = Modifier.padding(horizontal = SpacingNormal)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showAddDialog) {
        EditRuleDialog(
            onDismiss = { state.closeAddDialog() },
            onConfirm = { days, hour, minute ->
                onAddRule(days, hour, minute)
                state.closeAddDialog()
            },
        )
    }

    state.ruleToEdit?.let { rule ->
        EditRuleDialog(
            rule = rule,
            onDismiss = { state.closeEditDialog() },
            onConfirm = { days, hour, minute ->
                onUpdateRule(rule.copy(daysBefore = days, hour = hour, minute = minute))
                state.closeEditDialog()
            },
        )
    }
}

@Composable
private fun PermissionRequestState(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SpacingExtraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(ContactImageSizeNormal),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = AlphaSurfaceContainerHigh),
        )
        Spacer(modifier = Modifier.height(SpacingNormal))
        Text(
            text = stringResource(R.string.notifications_permission_missing_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(SpacingLarge))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.notifications_permission_btn))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsPreview() {
    BirthdayBuddyTheme {
        NotificationSettingsContent(
            windowWidthSizeClass = AppWidthSizeClass.COMPACT,
            notificationsEnabled = true,
            persistentNotifications = true,
            rules = listOf(
                NotificationRule(1, 0, 9, 0),
                NotificationRule(2, 1, 18, 0),
            ),
            hasSystemPermission = true,
            state = rememberNotificationSettingsState(),
            onToggleNotifications = {},
            onTogglePersistent = {},
            onAddRule = { _, _, _ -> },
            onUpdateRule = { _ -> },
            onDeleteRule = { _ -> },
            onRequestPermission = {},
            onNavigateBack = {},
        )
    }
}
