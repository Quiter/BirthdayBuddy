package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.repository.CalendarSyncRepository
import com.heckmannch.birthdaybuddy.ui.components.cards.InfoCard
import com.heckmannch.birthdaybuddy.ui.components.colorpicker.ColorPickerDialog
import com.heckmannch.birthdaybuddy.ui.components.layout.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSwitchRow
import com.heckmannch.birthdaybuddy.ui.screens.settings.components.SettingsMenuSectionCard
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar.components.SetupStepsCard
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.CalendarPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

@Composable
fun CalendarSettingsScreen(
    viewModel: CalendarViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
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
                SettingsMenuSectionCard(
                    sectionTitle = stringResource(R.string.settings_calendar_title),
                    sectionIcon = Icons.Default.DateRange
                ) {
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
                    SettingsMenuSectionCard(
                        sectionTitle = stringResource(R.string.calendar_colors_section_title),
                        sectionIcon = Icons.Default.DateRange
                    ) {
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

@Preview(showBackground = true)
@Composable
private fun CalendarSettingsPreview() {
    BirthdayBuddyTheme {
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



