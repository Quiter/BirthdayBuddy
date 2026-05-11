package com.heckmannch.birthdaybuddy2.ui.screens.settings.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy2.database.NotificationRule
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddy2Theme
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel

@Composable
fun NotificationSettingsScreen(
    viewModel: BirthdayViewModel,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val rules by viewModel.notificationRules.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(enabled = true)
        }
    }

    NotificationSettingsContent(
        notificationsEnabled = notificationsEnabled,
        rules = rules,
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
        onAddRule = { days, hour, minute ->
            viewModel.addNotificationRule(days, hour, minute)
        },
        onUpdateRule = { rule ->
            viewModel.updateNotificationRule(rule)
        },
        onDeleteRule = { rule ->
            viewModel.deleteNotificationRule(rule)
        },
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsContent(
    notificationsEnabled: Boolean,
    rules: List<NotificationRule>,
    onToggleNotifications: (Boolean) -> Unit,
    onAddRule: (Int, Int, Int) -> Unit,
    onUpdateRule: (NotificationRule) -> Unit,
    onDeleteRule: (NotificationRule) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val ruleToEditState = remember { mutableStateOf<NotificationRule?>(value = null) }
    val showAddDialogState = remember { mutableStateOf(value = false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Benachrichtigungen") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (notificationsEnabled) {
                FloatingActionButton(onClick = { showAddDialogState.value = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Regel hinzufügen")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Geburtstags-Erinnerungen") },
                    supportingContent = { Text("Erhalte personalisierte Benachrichtigungen") },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onToggleNotifications
                        )
                    }
                )
            }

            if (notificationsEnabled) {
                if (rules.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Keine Erinnerungen geplant",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Geplante Erinnerungen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(rules, key = { it.id }) { rule ->
                        NotificationRuleItem(
                            rule = rule,
                            onEditRule = { ruleToEditState.value = it },
                            onDeleteRule = onDeleteRule,
                        )
                    }
                }
            }
        }
    }

    if (showAddDialogState.value) {
        EditRuleDialog(
            onDismiss = { showAddDialogState.value = false },
            onConfirm = { days, hour, minute ->
                onAddRule(days, hour, minute)
                showAddDialogState.value = false
            },
        )
    }

    ruleToEditState.value?.let { rule ->
        EditRuleDialog(
            rule = rule,
            onDismiss = { ruleToEditState.value = null },
            onConfirm = { days, hour, minute ->
                onUpdateRule(rule.copy(daysBefore = days, hour = hour, minute = minute))
                ruleToEditState.value = null
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsPreview() {
    BirthdayBuddy2Theme {
        NotificationSettingsContent(
            notificationsEnabled = true,
            rules = listOf(
                NotificationRule(1, 0, 9, 0),
                NotificationRule(2, 1, 18, 0),
            ),
            onToggleNotifications = {},
            onAddRule = { _, _, _ -> },
            onUpdateRule = { _ -> },
            onDeleteRule = { _ -> },
            onNavigateBack = {},
        )
    }
}
