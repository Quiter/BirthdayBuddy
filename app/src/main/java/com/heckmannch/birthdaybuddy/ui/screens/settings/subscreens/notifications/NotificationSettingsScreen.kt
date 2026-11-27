package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import com.heckmannch.birthdaybuddy.ui.components.settings.FoldingVisibility
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsHeaderActionButton
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItemCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsNotice
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSemanticTone
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSwitchItem
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.NotificationSettingsState.Companion.Saver
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.components.EditRuleDialog
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.notifications.components.NotificationRuleItem
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

private const val TAG = "NotificationSettingsScreen"

/**
 * Entry point for the Notifications settings subpage.
 */
@Composable
fun NotificationSettingsScreen(
    viewModel: NotificationViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationsEnabled = uiState.notificationsEnabled
    val persistentNotifications = uiState.persistentNotifications
    val rules = uiState.notificationRules
    val hasSystemPermission = uiState.hasSystemNotificationPermission

    var hasAttemptedPermission by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(NotificationIntent.RefreshPermissionStatus)
    }

    val state = rememberNotificationSettingsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasAttemptedPermission = true
        if (isGranted) {
            viewModel.onIntent(NotificationIntent.SetEnabled(true))
        }
        viewModel.onIntent(NotificationIntent.RefreshPermissionStatus)
    }

    NotificationSettingsContent(
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
        onAddRule = { days, hour, minute ->
            viewModel.onIntent(NotificationIntent.AddRule(days, hour, minute))
        },
        onUpdateRule = { viewModel.onIntent(NotificationIntent.UpdateRule(it)) },
        onReplaceRule = { oldRule, days, hour, minute ->
            viewModel.onIntent(NotificationIntent.ReplaceRule(oldRule, days, hour, minute))
        },
        onDeleteRule = { viewModel.onIntent(NotificationIntent.DeleteRule(it)) },
        onRequestPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val activity = context as? Activity
                val shouldShowRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        it,
                        Manifest.permission.POST_NOTIFICATIONS,
                    )
                } ?: false

                if (shouldShowRationale || !hasAttemptedPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    hasAttemptedPermission = true
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.w(TAG, "App-Details-Einstellungen konnten nicht geöffnet werden", e)
                    }
                }
            }
        },
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Plain state holder for dialog visibility in Notification settings.
 */
@Stable
class NotificationSettingsState(
    initialShowAddDialog: Boolean = false,
    initialRuleToEdit: NotificationRule? = null,
) {
    var ruleToEdit by mutableStateOf<NotificationRule?>(initialRuleToEdit)
    var showAddDialog by mutableStateOf(value = initialShowAddDialog)

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

    companion object {
        private const val KEY_SHOW_ADD_DIALOG = "show_add_dialog"
        private const val KEY_RULE_DAYS_BEFORE = "rule_days_before"
        private const val KEY_RULE_HOUR = "rule_hour"
        private const val KEY_RULE_MINUTE = "rule_minute"

        /**
         * [Saver] implementation for [NotificationSettingsState] to persist dialog state
         * across configuration changes.
         */
        val Saver: Saver<NotificationSettingsState, *> = mapSaver(
            save = { state ->
                buildMap {
                    put(KEY_SHOW_ADD_DIALOG, state.showAddDialog)
                    state.ruleToEdit?.let { rule ->
                        put(KEY_RULE_DAYS_BEFORE, rule.daysBefore)
                        put(KEY_RULE_HOUR, rule.hour)
                        put(KEY_RULE_MINUTE, rule.minute)
                    }
                }
            },
            restore = { saved ->
                val showAddDialog = saved[KEY_SHOW_ADD_DIALOG] as? Boolean ?: false
                val rule = if (saved.containsKey(KEY_RULE_DAYS_BEFORE)) {
                    NotificationRule(
                        daysBefore = saved[KEY_RULE_DAYS_BEFORE] as Int,
                        hour = saved[KEY_RULE_HOUR] as Int,
                        minute = saved[KEY_RULE_MINUTE] as Int,
                    )
                } else {
                    null
                }
                NotificationSettingsState(
                    initialShowAddDialog = showAddDialog,
                    initialRuleToEdit = rule,
                )
            },
        )
    }
}

/**
 * Remembers a [NotificationSettingsState] across configuration changes.
 */
@Composable
fun rememberNotificationSettingsState(): NotificationSettingsState =
    rememberSaveable(saver = Saver) { NotificationSettingsState() }

/**
 * Stateless UI content for Notification settings in Morphe expressive style.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun NotificationSettingsContent(
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
    onReplaceRule: (NotificationRule, Int, Int, Int) -> Unit,
    onDeleteRule: (NotificationRule) -> Unit,
    onRequestPermission: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    SettingsSubpageScaffold(
        title = stringResource(R.string.notifications_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) {
        if (!hasSystemPermission) {
            SettingsNotice(
                title = stringResource(R.string.notifications_title),
                message = stringResource(R.string.notifications_permission_missing_desc),
                icon = Icons.Rounded.NotificationsOff,
                tone = SettingsSemanticTone.Warning,
                action = {
                    Button(onClick = onRequestPermission) {
                        Text(stringResource(R.string.notifications_permission_btn))
                    }
                },
            )
        } else {
            SettingsSection(title = stringResource(R.string.notifications_header)) {
                SettingsGroup {
                    SettingsSwitchItem(
                        title = stringResource(R.string.notifications_header),
                        subtitle = stringResource(R.string.notifications_desc),
                        icon = Icons.Rounded.Notifications,
                        checked = notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                    )

                    FoldingVisibility(visible = notificationsEnabled) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.notifications_persistent_header),
                            subtitle = stringResource(R.string.notifications_persistent_desc),
                            icon = Icons.Rounded.NotificationsActive,
                            checked = persistentNotifications,
                            onCheckedChange = onTogglePersistent,
                        )
                    }
                }
            }

            FoldingVisibility(visible = notificationsEnabled) {
                SettingsNotice(
                    title = stringResource(R.string.notifications_persistent_info_title),
                    message = stringResource(R.string.notifications_persistent_info_desc),
                    icon = Icons.Rounded.Info,
                    tone = SettingsSemanticTone.Neutral,
                )
            }

            FoldingVisibility(visible = notificationsEnabled) {
                SettingsSection(
                    title = stringResource(R.string.notifications_planned_header),
                    actionContent = {
                        SettingsHeaderActionButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.notifications_add_rule),
                            onClick = { state.openAddDialog() },
                            active = true,
                        )
                    },
                ) {
                    SettingsGroup {
                        if (rules.isEmpty()) {
                            SettingsItemCard(onClick = null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = SpacingNormal),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.notifications_empty),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        } else {
                            rules.forEach { rule ->
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
    }

    if (state.showAddDialog) {
        EditRuleDialog(
            existingDaysBefore = rules.map { it.daysBefore }.toSet(),
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
            existingDaysBefore = rules.filter { it.daysBefore != rule.daysBefore }.map { it.daysBefore }.toSet(),
            onDismiss = { state.closeEditDialog() },
            onConfirm = { days, hour, minute ->
                if (days != rule.daysBefore) {
                    onReplaceRule(rule, days, hour, minute)
                } else {
                    onUpdateRule(rule.copy(hour = hour, minute = minute))
                }
                state.closeEditDialog()
            },
        )
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
                NotificationRule(daysBefore = 0, hour = 9, minute = 0),
                NotificationRule(daysBefore = 1, hour = 18, minute = 0),
            ),
            hasSystemPermission = true,
            state = rememberNotificationSettingsState(),
            onToggleNotifications = {},
            onTogglePersistent = {},
            onAddRule = { _, _, _ -> },
            onUpdateRule = { _ -> },
            onReplaceRule = { _, _, _, _ -> },
            onDeleteRule = { _ -> },
            onRequestPermission = {},
            onNavigateBack = {},
        )
    }
}
