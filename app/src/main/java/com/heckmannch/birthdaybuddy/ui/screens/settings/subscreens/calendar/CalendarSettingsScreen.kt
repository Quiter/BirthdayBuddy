package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.MaterialTheme
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
import com.heckmannch.birthdaybuddy.ui.components.colorpicker.ColorPickerDialog
import com.heckmannch.birthdaybuddy.ui.components.settings.FoldingVisibility
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItem
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSwitchItem
import com.heckmannch.birthdaybuddy.ui.screens.home.components.labels.toDisplayLabel
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.calendar.components.SetupStepsCard
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsOutlineMuted
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CalendarPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal

private sealed interface ColorPickerTarget {
    data class Standard(val type: CalendarSyncRepository.CalendarType) : ColorPickerTarget
    data class CustomEvent(val eventKey: String) : ColorPickerTarget
}

/**
 * Entry point for the Calendar Synchronization settings subpage.
 */
@Composable
fun CalendarSettingsScreen(
    viewModel: CalendarViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calendarSyncEnabled = uiState.calendarSyncEnabled
    val otherEventsEnabled = uiState.otherEventsEnabled
    val anniversaryEnabled = uiState.anniversaryEnabled
    val nameDayEnabled = uiState.nameDayEnabled
    val birthdayColor = uiState.birthdayCalendarColor
    val anniversaryColor = uiState.anniversaryCalendarColor
    val nameDayColor = uiState.nameDayCalendarColor
    val customEventCalendars = uiState.customEventCalendars
    val hasPermission = uiState.hasCalendarPermission

    var activeColorPickerTarget by remember {
        mutableStateOf<ColorPickerTarget?>(null)
    }
    var activeColorPickerInitialColor by remember { mutableStateOf(Color.Unspecified) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true
        viewModel.onIntent(CalendarIntent.SetCalendarSyncEnabled(granted))
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
                        Manifest.permission.WRITE_CALENDAR,
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
        anniversaryEnabled = anniversaryEnabled,
        nameDayEnabled = nameDayEnabled,
        birthdayColor = birthdayColor,
        anniversaryColor = anniversaryColor,
        nameDayColor = nameDayColor,
        customEventCalendars = customEventCalendars,
        showBackButton = showBackButton,
        onToggleChange = onToggleChange,
        onColorRowClick = { type, color ->
            activeColorPickerTarget = ColorPickerTarget.Standard(type)
            activeColorPickerInitialColor = color
        },
        onCustomEventColorRowClick = { eventKey, color ->
            activeColorPickerTarget = ColorPickerTarget.CustomEvent(eventKey)
            activeColorPickerInitialColor = color
        },
        onRequestPermission = {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR,
                )
            )
        },
        onNavigateBack = onNavigateBack,
    )

    activeColorPickerTarget?.let { target ->
        val presets = remember { CalendarPresetColors }
        ColorPickerDialog(
            initialColor = activeColorPickerInitialColor,
            title = stringResource(R.string.calendar_color_picker_title),
            onDismissRequest = { activeColorPickerTarget = null },
            onColorSelected = { selectedColor ->
                when (target) {
                    is ColorPickerTarget.Standard -> {
                        viewModel.onIntent(
                            CalendarIntent.UpdateCalendarColor(target.type, selectedColor.toArgb())
                        )
                    }

                    is ColorPickerTarget.CustomEvent -> {
                        viewModel.onIntent(
                            CalendarIntent.UpdateCustomEventCalendarColor(
                                target.eventKey,
                                selectedColor.toArgb()
                            )
                        )
                    }
                }
                activeColorPickerTarget = null
            },
            presets = presets,
        )
    }
}

/**
 * Stateless UI content for Calendar Synchronization settings in Morphe expressive style.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun CalendarSettingsScreenContent(
    calendarSyncEnabled: Boolean,
    hasPermission: Boolean,
    otherEventsEnabled: Boolean,
    birthdayColor: Int,
    anniversaryColor: Int,
    nameDayColor: Int,
    anniversaryEnabled: Boolean = otherEventsEnabled,
    nameDayEnabled: Boolean = otherEventsEnabled,
    customEventCalendars: List<CustomEventCalendarColorItem> = emptyList(),
    showBackButton: Boolean = true,
    onToggleChange: (Boolean) -> Unit = {},
    onColorRowClick: (CalendarSyncRepository.CalendarType, Color) -> Unit = { _, _ -> },
    onCustomEventColorRowClick: (String, Color) -> Unit = { _, _ -> },
    onRequestPermission: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_calendar_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) {
        SettingsSection(title = stringResource(R.string.calendar_settings_header)) {
            SettingsGroup {
                SettingsSwitchItem(
                    title = stringResource(R.string.calendar_settings_enable),
                    icon = Icons.Rounded.CalendarMonth,
                    checked = calendarSyncEnabled,
                    onCheckedChange = onToggleChange,
                )
            }
            SettingsFooter(text = stringResource(R.string.calendar_settings_desc))
        }

        FoldingVisibility(visible = calendarSyncEnabled) {
            SettingsSection(title = stringResource(R.string.calendar_colors_section_title)) {
                SettingsGroup {
                    SettingsItem(
                        title = stringResource(R.string.calendar_color_birthdays),
                        leadingContent = { CalendarColorDot(color = Color(birthdayColor)) },
                        onClick = {
                            onColorRowClick(
                                CalendarSyncRepository.CalendarType.BIRTHDAY,
                                Color(birthdayColor),
                            )
                        },
                    )
                    FoldingVisibility(visible = otherEventsEnabled && anniversaryEnabled) {
                        SettingsItem(
                            title = stringResource(R.string.calendar_color_anniversaries),
                            leadingContent = { CalendarColorDot(color = Color(anniversaryColor)) },
                            onClick = {
                                onColorRowClick(
                                    CalendarSyncRepository.CalendarType.ANNIVERSARY,
                                    Color(anniversaryColor),
                                )
                            },
                        )
                    }
                    FoldingVisibility(visible = otherEventsEnabled && nameDayEnabled) {
                        SettingsItem(
                            title = stringResource(R.string.calendar_color_namedays),
                            leadingContent = { CalendarColorDot(color = Color(nameDayColor)) },
                            onClick = {
                                onColorRowClick(
                                    CalendarSyncRepository.CalendarType.NAMEDAY,
                                    Color(nameDayColor),
                                )
                            },
                        )
                    }
                    customEventCalendars.forEach { customItem ->
                        val displayName = customItem.eventKey.toDisplayLabel()
                        SettingsItem(
                            title = stringResource(R.string.calendar_color_custom_event, displayName),
                            leadingContent = { CalendarColorDot(color = Color(customItem.color)) },
                            onClick = {
                                onCustomEventColorRowClick(
                                    customItem.eventKey,
                                    Color(customItem.color),
                                )
                            },
                        )
                    }
                }
            }
        }

        SettingsSection(title = stringResource(R.string.calendar_guide_title)) {
            SetupStepsCard(
                hasPermission = hasPermission,
                calendarSyncEnabled = calendarSyncEnabled,
                onRequestPermission = onRequestPermission,
            )
        }
    }
}

@Composable
private fun CalendarColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(IconSizeNormal)
            .background(color = color, shape = CircleShape)
            .border(
                width = BorderWidthThin,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaSettingsOutlineMuted),
                shape = CircleShape,
            ),
    )
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
            onNavigateBack = {},
        )
    }
}
