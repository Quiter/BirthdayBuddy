package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.RadioButton
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
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.domain.model.ThemeMode
import com.heckmannch.birthdaybuddy.ui.components.ColorPickerDialog
import com.heckmannch.birthdaybuddy.ui.components.SettingsClickableRow
import com.heckmannch.birthdaybuddy.ui.components.SettingsDetailScaffold
import com.heckmannch.birthdaybuddy.ui.components.SettingsDivider
import com.heckmannch.birthdaybuddy.ui.components.SettingsSection
import com.heckmannch.birthdaybuddy.ui.components.SettingsSwitchRow
import com.heckmannch.birthdaybuddy.ui.components.withSettingsInsets
import com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme.components.ThemeAccentSelector
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayKidGreen

private const val TAG = "ThemeSettingsScreen"

@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode = uiState.themeMode
    val themeAmoled = uiState.themeAmoled
    val themeAccent = uiState.themeAccent
    val customAccentColor = uiState.customAccentColor

    ThemeSettingsScreenContent(
        themeMode = themeMode,
        themeAmoled = themeAmoled,
        themeAccent = themeAccent,
        customAccentColor = customAccentColor,
        showBackButton = showBackButton,
        onThemeModeChange = { viewModel.onIntent(ThemeIntent.SetThemeMode(it)) },
        onThemeAmoledChange = { viewModel.onIntent(ThemeIntent.SetThemeAmoled(it)) },
        onThemeAccentChange = { accent, customColor ->
            viewModel.onIntent(
                ThemeIntent.SetThemeAccent(
                    accent,
                    customColor
                )
            )
        },
        onNavigateBack = onNavigateBack
    )
}

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
    onNavigateBack: () -> Unit = {}
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = stringResource(R.string.settings_theme_title),
        showBackButton = showBackButton,
        onNavigateBack = onNavigateBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues.withSettingsInsets()
        ) {
            // --- Theme Mode ---
            item {
                SettingsSection(title = stringResource(R.string.settings_theme_mode_header)) {
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_theme_mode_system),
                        description = stringResource(R.string.settings_theme_mode_system_desc),
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                        trailingContent = {
                            RadioButton(
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { onThemeModeChange(ThemeMode.SYSTEM) }
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_theme_mode_light),
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                        trailingContent = {
                            RadioButton(
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { onThemeModeChange(ThemeMode.LIGHT) }
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsClickableRow(
                        title = stringResource(R.string.settings_theme_mode_dark),
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                        trailingContent = {
                            RadioButton(
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { onThemeModeChange(ThemeMode.DARK) }
                            )
                        }
                    )
                    SettingsDivider()
                    val isDarkThemeActive =
                        themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_theme_amoled),
                        description = if (isDarkThemeActive) {
                            stringResource(R.string.settings_theme_amoled_desc)
                        } else {
                            stringResource(R.string.settings_theme_amoled_disabled_desc)
                        },
                        checked = themeAmoled,
                        onCheckedChange = onThemeAmoledChange,
                        enabled = isDarkThemeActive
                    )
                }
            }


            // --- Accent Colors ---
            item {
                SettingsSection(title = stringResource(R.string.settings_theme_accent_header)) {
                    ThemeAccentSelector(
                        themeAccent = themeAccent,
                        customAccentColor = customAccentColor,
                        onThemeAccentChange = onThemeAccentChange,
                        onOpenColorPicker = { showColorPickerDialog = true }
                    )
                }
            }

            item {
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
                            BirthdayKidGreen  // Green
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
                        presets = presets
                    )
                }
            }
        }
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
            onNavigateBack = {}
        )
    }
}



