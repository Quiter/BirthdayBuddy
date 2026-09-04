package com.heckmannch.birthdaybuddy.ui.screens.settings.theme

import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.heckmannch.birthdaybuddy.ui.theme.AccentPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayKidGreen
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

private val LIGHT_ICON_ACCENTS = setOf(
    ThemeAccent.SYSTEM,
    ThemeAccent.BLUE,
    ThemeAccent.GREEN,
    ThemeAccent.PURPLE,
    ThemeAccent.RED,
    ThemeAccent.PINK
)

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SpacingNormal, vertical = SpacingNormal)
                    ) {
                        val chunkedColors = remember(customAccentColor, themeAccent) {
                            val colors = mutableListOf<AccentColorOption>()

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                colors.add(
                                    AccentColorOption(
                                        ThemeAccent.SYSTEM,
                                        Color.Transparent,
                                        isSystem = true
                                    )
                                )
                            }

                            colors.addAll(
                                listOf(
                                    AccentColorOption(ThemeAccent.PURPLE, AccentPresetColors[5]),
                                    AccentColorOption(ThemeAccent.BLUE, AccentPresetColors[0]),
                                    AccentColorOption(ThemeAccent.GREEN, AccentPresetColors[1]),
                                    AccentColorOption(ThemeAccent.RED, AccentPresetColors[2]),
                                    AccentColorOption(ThemeAccent.ORANGE, AccentPresetColors[3]),
                                    AccentColorOption(ThemeAccent.PINK, AccentPresetColors[4])
                                )
                            )

                            val isCustomAccent = themeAccent == ThemeAccent.CUSTOM
                            val customColor = if (isCustomAccent && customAccentColor != null) {
                                try {
                                    Color(customAccentColor.toColorInt())
                                } catch (_: Exception) {
                                    Color(0xFFE91E63)
                                }
                            } else {
                                Color(0xFFE91E63)
                            }
                            colors.add(
                                AccentColorOption(
                                    ThemeAccent.CUSTOM,
                                    customColor,
                                    isCustom = true
                                )
                            )

                            colors.chunked(4)
                        }

                        // Akzentfarben in Zeilen von je 4 Elementen rendern
                        chunkedColors.forEach { rowColors ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = SpacingSmall),
                                horizontalArrangement = Arrangement.spacedBy(SpacingNormal)
                            ) {
                                rowColors.forEach { option ->
                                    val isSelected = themeAccent == option.id
                                    ColorItem(
                                        option = option,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (option.isCustom) {
                                                showColorPickerDialog = true
                                            } else {
                                                onThemeAccentChange(option.id, null)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Filler, falls die Zeile nicht voll ist (z.B. am Ende des Grids)
                                repeat(4 - rowColors.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                if (showColorPickerDialog) {
                    val parsedInitialColor = remember(customAccentColor) {
                        if (customAccentColor != null && customAccentColor.startsWith("#")) {
                            try {
                                Color(customAccentColor.toColorInt())
                            } catch (_: Exception) {
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
    MaterialTheme {
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

data class AccentColorOption(
    val id: ThemeAccent,
    val color: Color,
    val isSystem: Boolean = false,
    val isCustom: Boolean = false
)

@Composable
private fun ColorItem(
    option: AccentColorOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1.0f, label = "color_scale")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(IconSizeExtraLarge)
                .clip(CircleShape)
                .then(
                    if (option.isSystem) {
                        // Rainbow-Gradient für Systemfarben (Material You)
                        Modifier.background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFE91E63),
                                    Color(0xFF9C27B0),
                                    Color(0xFF2196F3),
                                    BirthdayKidGreen,
                                    Color(0xFFFFEB3B),
                                    Color(0xFFFF9800),
                                    Color(0xFFE91E63)
                                )
                            )
                        )
                    } else {
                        Modifier.background(option.color)
                    }
                )
                .border(
                    width = if (isSelected) SelectedBorderWidthThick else BorderWidthThin,
                    color = if (isSelected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                )
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (option.isSystem) Color.White else if (option.isCustom) {
                        if (option.color.luminance() > 0.5f) Color.Black else Color.White
                    } else if (option.id in LIGHT_ICON_ACCENTS) {
                        Color.White
                    } else {
                        Color.Black
                    },
                    modifier = Modifier.size(IconSizeNormal)
                )
            } else if (option.isSystem) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(IconSizeSmall)
                )
            } else if (option.isCustom) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = if (option.color.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(IconSizeSmall)
                )
            }
        }

        Spacer(modifier = Modifier.height(SpacingExtraSmall))

        val label = when (option.id) {
            ThemeAccent.SYSTEM -> stringResource(R.string.theme_accent_system)
            ThemeAccent.PURPLE -> stringResource(R.string.theme_accent_purple)
            ThemeAccent.BLUE -> stringResource(R.string.theme_accent_blue)
            ThemeAccent.GREEN -> stringResource(R.string.theme_accent_green)
            ThemeAccent.RED -> stringResource(R.string.theme_accent_red)
            ThemeAccent.ORANGE -> stringResource(R.string.theme_accent_orange)
            ThemeAccent.PINK -> stringResource(R.string.theme_accent_pink)
            ThemeAccent.CUSTOM -> stringResource(R.string.theme_accent_custom)
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


