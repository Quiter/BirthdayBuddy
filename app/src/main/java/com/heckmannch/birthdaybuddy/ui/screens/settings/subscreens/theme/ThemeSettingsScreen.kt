package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.ui.components.colorpicker.ColorPickerDialog
import com.heckmannch.birthdaybuddy.ui.components.settings.FoldingVisibility
import com.heckmannch.birthdaybuddy.ui.components.settings.ModernIconOptionCard
import com.heckmannch.birthdaybuddy.ui.components.settings.OptionGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SectionCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsFooter
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsGroup
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsItemCard
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSubpageScaffold
import com.heckmannch.birthdaybuddy.ui.components.settings.SettingsSwitchItem
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme.components.ThemeAccentSelector
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayKidGreen

private const val TAG = "ThemeSettingsScreen"

/**
 * Entry point for the Appearance / Theme settings subpage.
 */
@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ThemeSettingsScreenContent(
        themeMode = uiState.themeMode,
        themeAmoled = uiState.themeAmoled,
        themeAccent = uiState.themeAccent,
        customAccentColor = uiState.customAccentColor,
        showBackButton = showBackButton,
        onThemeModeChange = { viewModel.onIntent(ThemeIntent.SetThemeMode(it)) },
        onThemeAmoledChange = { viewModel.onIntent(ThemeIntent.SetThemeAmoled(it)) },
        onThemeAccentChange = { accent, customColor ->
            viewModel.onIntent(ThemeIntent.SetThemeAccent(accent, customColor))
        },
        onNavigateBack = onNavigateBack,
    )
}

/**
 * Stateless content for Theme settings featuring Morphe-style expressive pill selectors,
 * conditional AMOLED Pure Black toggle, and Material You accent swatches.
 */
@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
@Composable
internal fun ThemeSettingsScreenContent(
    themeMode: ThemeMode,
    themeAmoled: Boolean,
    themeAccent: ThemeAccent = ThemeAccent.PURPLE,
    customAccentColor: String? = null,
    showBackButton: Boolean = true,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onThemeAmoledChange: (Boolean) -> Unit = {},
    onThemeAccentChange: (ThemeAccent, String?) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {},
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }
    val isDarkThemeActive =
        themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())

    SettingsSubpageScaffold(
        title = stringResource(R.string.settings_theme_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack,
    ) {
        // Theme mode pill group
        SettingsSection(title = stringResource(R.string.settings_theme_mode_header)) {
            SectionCard {
                OptionGroup {
                    ModernIconOptionCard(
                        title = stringResource(R.string.settings_theme_mode_system),
                        icon = Icons.Rounded.BrightnessAuto,
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                        index = 0,
                        count = 3,
                    )
                    ModernIconOptionCard(
                        title = stringResource(R.string.settings_theme_mode_light),
                        icon = Icons.Rounded.LightMode,
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                        index = 1,
                        count = 3,
                    )
                    ModernIconOptionCard(
                        title = stringResource(R.string.settings_theme_mode_dark),
                        icon = Icons.Rounded.DarkMode,
                        selected = themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                        index = 2,
                        count = 3,
                    )
                }
            }
            SettingsFooter(text = stringResource(R.string.settings_theme_mode_system_desc))
        }

        // Accent color & AMOLED black segmented group
        SettingsSection(title = stringResource(R.string.settings_theme_accent_header)) {
            SettingsGroup {
                FoldingVisibility(visible = themeMode != ThemeMode.LIGHT) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_theme_amoled),
                        subtitle = if (isDarkThemeActive) {
                            stringResource(R.string.settings_theme_amoled_desc)
                        } else {
                            stringResource(R.string.settings_theme_amoled_disabled_desc)
                        },
                        icon = Icons.Rounded.DarkMode,
                        checked = themeAmoled,
                        onCheckedChange = onThemeAmoledChange,
                        enabled = isDarkThemeActive,
                    )
                }

                SettingsItemCard(onClick = null) {
                    ThemeAccentSelector(
                        themeAccent = themeAccent,
                        customAccentColor = customAccentColor,
                        onThemeAccentChange = onThemeAccentChange,
                        onOpenColorPicker = { showColorPickerDialog = true },
                    )
                }
            }
        }
    }

    if (showColorPickerDialog) {
        val parsedInitialColor = remember(customAccentColor) {
            if (customAccentColor != null && customAccentColor.startsWith("#")) {
                try {
                    Color(customAccentColor.toColorInt())
                } catch (e: Exception) {
                    Log.w(TAG, "Fehler beim Parsen des Farb-Picker Initialwerts: $customAccentColor", e)
                    Color(0xFFE91E63)
                }
            } else {
                Color(0xFFE91E63)
            }
        }
        val presets = remember {
            listOf(
                Color(0xFF008080), // Teal
                Color(0xFF00BCD4), // Cyan
                Color(0xFF3F51B5), // Indigo
                Color(0xFFFFC107), // Amber
                Color(0xFF795548), // Brown
                Color(0xFFFF5722), // Deep Orange
                Color(0xFF607D8B), // Blue Grey
                BirthdayKidGreen,  // Green
            )
        }
        ColorPickerDialog(
            initialColor = parsedInitialColor,
            title = stringResource(R.string.theme_accent_custom_dialog_title),
            onDismissRequest = { showColorPickerDialog = false },
            onColorSelected = { color ->
                val hexString = String.format("#%06X", 0xFFFFFF and color.toArgb())
                onThemeAccentChange(ThemeAccent.CUSTOM, hexString)
                showColorPickerDialog = false
            },
            presets = presets,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeSettingsPreview() {
    BirthdayBuddyTheme {
        ThemeSettingsScreenContent(
            themeMode = ThemeMode.SYSTEM,
            themeAmoled = false,
            themeAccent = ThemeAccent.PURPLE,
            customAccentColor = null,
            showBackButton = true,
            onThemeModeChange = {},
            onThemeAmoledChange = {},
            onThemeAccentChange = { _, _ -> },
            onNavigateBack = {},
        )
    }
}
