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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.database.NotificationRule
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.EditRuleDialog
import com.heckmannch.birthdaybuddy.ui.screens.settings.notifications.components.NotificationRuleItem
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.BirthdayViewModel

@Composable
fun NotificationSettingsScreen(
    viewModel: BirthdayViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val persistentNotifications by viewModel.persistentNotifications.collectAsStateWithLifecycle()
    val rules by viewModel.notificationRules.collectAsStateWithLifecycle()
    
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasSystemPermission = isGranted
        if (isGranted) {
            viewModel.setNotificationsEnabled(enabled = true)
        }
    }

    NotificationSettingsContent(
        notificationsEnabled = notificationsEnabled,
        persistentNotifications = persistentNotifications,
        rules = rules ?: emptyList(),
        hasSystemPermission = hasSystemPermission,
        state = rememberNotificationSettingsState(),
        onToggleNotifications = { enabled ->
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.setNotificationsEnabled(enabled = true)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    viewModel.setNotificationsEnabled(enabled = true)
                }
            } else {
                viewModel.setNotificationsEnabled(enabled = false)
            }
        },
        onTogglePersistent = { viewModel.setPersistentNotifications(it) },
        onAddRule = { days, hour, minute ->
            viewModel.addNotificationRule(days, hour, minute)
        },
        onUpdateRule = { rule ->
            viewModel.updateNotificationRule(rule)
        },
        onDeleteRule = { rule ->
            viewModel.deleteNotificationRule(rule)
        },
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = context as? Activity
                val shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
                } ?: false

                if (shouldShowRationale || !hasAttemptedPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    hasAttemptedPermission = true
                } else {
                    // Fallback: Systemeinstellungen öffnen, wenn der Dialog nicht mehr erscheint
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
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

    fun openAddDialog() { showAddDialog = true }
    fun closeAddDialog() { showAddDialog = false }
    
    fun openEditDialog(rule: NotificationRule) { ruleToEdit = rule }
    fun closeEditDialog() { ruleToEdit = null }
}

@Composable
fun rememberNotificationSettingsState(): NotificationSettingsState {
    return remember { NotificationSettingsState() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsContent(
    notificationsEnabled: Boolean,
    persistentNotifications: Boolean,
    rules: List<NotificationRule>,
    hasSystemPermission: Boolean,
    state: NotificationSettingsState,
    onToggleNotifications: (Boolean) -> Unit,
    onTogglePersistent: (Boolean) -> Unit,
    onAddRule: (Int, Int, Int) -> Unit,
    onUpdateRule: (NotificationRule) -> Unit,
    onDeleteRule: (NotificationRule) -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notifications_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.notifications_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (notificationsEnabled && hasSystemPermission) {
                FloatingActionButton(onClick = { state.openAddDialog() }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.notifications_add_rule))
                }
            }
        }
    ) { innerPadding ->
        if (!hasSystemPermission) {
            PermissionRequestState(
                modifier = Modifier.padding(innerPadding),
                onRequestPermission = onRequestPermission
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.notifications_header)) },
                        supportingContent = { Text(stringResource(R.string.notifications_desc)) },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = onToggleNotifications
                            )
                        }
                    )
                }

                if (notificationsEnabled) {
                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.notifications_persistent_header)) },
                            supportingContent = { Text(stringResource(R.string.notifications_persistent_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = persistentNotifications,
                                    onCheckedChange = onTogglePersistent
                                )
                            }
                        )
                    }

                    if (rules.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
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
                        item {
                            Text(
                                stringResource(R.string.notifications_planned_header),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(rules, key = { it.id }) { rule ->
                            NotificationRuleItem(
                                rule = rule,
                                onEditRule = { state.openEditDialog(it) },
                                onDeleteRule = onDeleteRule,
                            )
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
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.notifications_permission_missing_desc),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(24.dp))
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
