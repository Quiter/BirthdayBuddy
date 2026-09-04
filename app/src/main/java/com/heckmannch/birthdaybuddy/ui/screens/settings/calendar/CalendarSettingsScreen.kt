package com.heckmannch.birthdaybuddy.ui.screens.settings.calendar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.ui.components.ColorPickerDialog
import com.heckmannch.birthdaybuddy.ui.components.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsCard
import com.heckmannch.birthdaybuddy.ui.components.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.SettingsDivider
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.SettingsSwitchRow
import com.heckmannch.birthdaybuddy.ui.components.StepItem
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMedium
import com.heckmannch.birthdaybuddy.ui.theme.CalendarPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun CalendarSettingsScreen(
    viewModel: CalendarViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calendarSyncEnabled = uiState.calendarSyncEnabled
    val otherEventsEnabled = uiState.otherEventsEnabled
    val birthdayColor = uiState.birthdayCalendarColor
    val anniversaryColor = uiState.anniversaryCalendarColor
    val nameDayColor = uiState.nameDayCalendarColor
    val hasPermission = uiState.hasCalendarPermission

    var activeColorPickerType by remember {
        mutableStateOf<CalendarSyncRepository.CalendarType?>(
            null
        )
    }
    var activeColorPickerInitialColor by remember { mutableStateOf(Color.Unspecified) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            viewModel.onIntent(CalendarIntent.SetCalendarSyncEnabled(true))
        } else {
            viewModel.onIntent(CalendarIntent.SetCalendarSyncEnabled(false))
        }
        viewModel.onIntent(CalendarIntent.CheckPermissionStatus)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(CalendarIntent.CheckPermissionStatus)
    }

    val onToggleChange: (Boolean) -> Unit = { enabled ->
        if (enabled) {
            if (hasPermission) {
                viewModel.onIntent(CalendarIntent.SetCalendarSyncEnabled(true))
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )
                )
            }
        } else {
            viewModel.onIntent(CalendarIntent.SetCalendarSyncEnabled(false))
        }
    }

    CalendarSettingsScreenContent(
        calendarSyncEnabled = calendarSyncEnabled && hasPermission,
        hasPermission = hasPermission,
        otherEventsEnabled = otherEventsEnabled,
        birthdayColor = birthdayColor,
        anniversaryColor = anniversaryColor,
        nameDayColor = nameDayColor,
        showBackButton = showBackButton,
        onToggleChange = onToggleChange,
        onColorRowClick = { type, color ->
            activeColorPickerType = type
            activeColorPickerInitialColor = color
        },
        onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
        },
        onNavigateBack = onNavigateBack
    )

    activeColorPickerType?.let { type ->
        val presets = remember { CalendarPresetColors }
        ColorPickerDialog(
            initialColor = activeColorPickerInitialColor,
            title = stringResource(R.string.calendar_color_picker_title),
            onDismissRequest = { activeColorPickerType = null },
            onColorSelected = { selectedColor ->
                viewModel.onIntent(CalendarIntent.UpdateCalendarColor(type, selectedColor.toArgb()))
                activeColorPickerType = null
            },
            presets = presets
        )
    }
}

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun CalendarSettingsScreenContent(
    calendarSyncEnabled: Boolean,
    hasPermission: Boolean,
    otherEventsEnabled: Boolean,
    birthdayColor: Int,
    anniversaryColor: Int,
    nameDayColor: Int,
    showBackButton: Boolean = true,
    onToggleChange: (Boolean) -> Unit = {},
    onColorRowClick: (CalendarSyncRepository.CalendarType, Color) -> Unit = { _, _ -> },
    onRequestPermission: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    SettingsDetailScaffold(
        title = stringResource(R.string.settings_calendar_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues.withSettingsInsets(),
            verticalArrangement = Arrangement.spacedBy(SpacingNormal)
        ) {
            item {
                InfoCard(
                    title = stringResource(R.string.calendar_settings_header),
                    description = stringResource(R.string.calendar_settings_desc)
                )
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = stringResource(R.string.calendar_settings_enable),
                        checked = calendarSyncEnabled,
                        onCheckedChange = onToggleChange,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null
                            )
                        }
                    )
                }
            }

            if (calendarSyncEnabled) {
                item {
                    SettingsSection(title = stringResource(R.string.calendar_colors_section_title)) {
                        // NOTE FOR FUTURE LLMs/DEVELOPERS:
                        // We intentionally use SettingsClickableRow instead of StepItem here.
                        // While both show leading items (color circles/icons), color selections are independent,
                        // non-sequential settings actions. StepItem is reserved for progressive setup steps
                        // (as seen in SetupStepsCard below). Using SettingsClickableRow maintains structural semantic clarity.
                        SettingsClickableRow(
                            title = stringResource(R.string.calendar_color_birthdays),
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(IconSizeLarge)
                                        .background(
                                            Color(birthdayColor),
                                            shape = CircleShape
                                        )
                                )
                            },
                            onClick = {
                                onColorRowClick(
                                    CalendarSyncRepository.CalendarType.BIRTHDAY,
                                    Color(birthdayColor)
                                )
                            }
                        )
                        if (otherEventsEnabled) {
                            SettingsDivider()
                            SettingsClickableRow(
                                title = stringResource(R.string.calendar_color_anniversaries),
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(IconSizeLarge)
                                            .background(
                                                Color(anniversaryColor),
                                                shape = CircleShape
                                            )
                                    )
                                },
                                onClick = {
                                    onColorRowClick(
                                        CalendarSyncRepository.CalendarType.ANNIVERSARY,
                                        Color(anniversaryColor)
                                    )
                                }
                            )
                            SettingsDivider()
                            SettingsClickableRow(
                                title = stringResource(R.string.calendar_color_namedays),
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(IconSizeLarge)
                                            .background(
                                                Color(nameDayColor),
                                                shape = CircleShape
                                            )
                                    )
                                },
                                onClick = {
                                    onColorRowClick(
                                        CalendarSyncRepository.CalendarType.NAMEDAY,
                                        Color(nameDayColor)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {
                SetupStepsCard(
                    hasPermission = hasPermission,
                    calendarSyncEnabled = calendarSyncEnabled,
                    onRequestPermission = onRequestPermission
                )
            }
        }
    }
}


// NOTE FOR FUTURE LLMs/DEVELOPERS:
// SetupStepsCard displays the progressive, sequential steps required to set up calendar synchronization.
// It is the primary use case for StepItem, utilizing step numbers, lock states, completion checkmarks,
// and contextual action buttons.
@Composable
private fun SetupStepsCard(
    hasPermission: Boolean,
    calendarSyncEnabled: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaContainerMedium)
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(SpacingNormal)
        ) {
            Text(
                text = stringResource(R.string.calendar_guide_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(SpacingNormal))

            // Step 1
            StepItem(
                stepNumber = 1,
                title = stringResource(R.string.calendar_guide_step1_title),
                description = stringResource(R.string.calendar_guide_step1_desc),
                isCompleted = hasPermission,
                actionButton = if (!hasPermission) {
                    {
                        Button(
                            onClick = onRequestPermission
                        ) {
                            Text(stringResource(R.string.calendar_settings_permission_btn))
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(SpacingNormal))

            // Step 2
            StepItem(
                stepNumber = 2,
                title = stringResource(R.string.calendar_guide_step2_title),
                description = stringResource(R.string.calendar_guide_step2_desc),
                isCompleted = calendarSyncEnabled && hasPermission,
                isLocked = !hasPermission
            )

            Spacer(modifier = Modifier.height(SpacingNormal))

            // Step 3
            StepItem(
                stepNumber = 3,
                title = stringResource(R.string.calendar_guide_step3_title),
                description = stringResource(R.string.calendar_guide_step3_desc),
                isCompleted = false,
                isLocked = !(calendarSyncEnabled && hasPermission),
                icon = Icons.Default.DateRange
            )

            if (calendarSyncEnabled && hasPermission) {
                Spacer(modifier = Modifier.height(SpacingNormal))
                Button(
                    onClick = { openDefaultCalendarApp(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeMedium)
                        )
                        Spacer(modifier = Modifier.width(SpacingSmall))
                        Text(stringResource(R.string.onboarding_calendar_guide_btn))
                    }
                }
            }
        }
    }
}

private fun openDefaultCalendarApp(context: Context) {
    try {
        val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        android.content.ContentUris.appendId(builder, System.currentTimeMillis())
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = builder.build()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            Log.e("CalendarSettingsScreen", "Could not open calendar app", e2)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CalendarSettingsPreview() {
    MaterialTheme {
        CalendarSettingsScreenContent(
            calendarSyncEnabled = true,
            hasPermission = true,
            otherEventsEnabled = true,
            birthdayColor = 0xFFE91E63.toInt(),
            anniversaryColor = 0xFF9C27B0.toInt(),
            nameDayColor = 0xFF2196F3.toInt(),
            showBackButton = true,
            onToggleChange = {},
            onColorRowClick = { _, _ -> },
            onRequestPermission = {},
            onNavigateBack = {}
        )
    }
}


