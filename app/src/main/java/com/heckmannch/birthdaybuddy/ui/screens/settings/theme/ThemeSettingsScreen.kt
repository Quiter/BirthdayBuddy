package com.heckmannch.birthdaybuddy.ui.screens.settings.theme

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppResponsiveScaffold
import com.heckmannch.birthdaybuddy.ui.components.ColorPickerDialog
import com.heckmannch.birthdaybuddy.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    windowWidthSizeClass: WindowWidthSizeClass,
    viewModel: ThemeViewModel,
    showBackButton: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode = uiState.themeMode
    val themeAmoled = uiState.themeAmoled
    val themeAccent = uiState.themeAccent

    var showColorPickerDialog by remember { mutableStateOf(false) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    AppResponsiveScaffold(
        windowWidthSizeClass = windowWidthSizeClass,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_theme_title)) },
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
                scrollBehavior = scrollBehavior
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- Theme Mode ---
            item {
                Text(
                    text = stringResource(R.string.settings_theme_mode_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme_mode_system)) },
                    supportingContent = { Text(stringResource(R.string.settings_theme_mode_system_desc)) },
                    trailingContent = {
                        RadioButton(
                            selected = themeMode == "SYSTEM",
                            onClick = { viewModel.setThemeMode("SYSTEM") }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setThemeMode("SYSTEM") }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme_mode_light)) },
                    trailingContent = {
                        RadioButton(
                            selected = themeMode == "LIGHT",
                            onClick = { viewModel.setThemeMode("LIGHT") }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setThemeMode("LIGHT") }
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme_mode_dark)) },
                    trailingContent = {
                        RadioButton(
                            selected = themeMode == "DARK",
                            onClick = { viewModel.setThemeMode("DARK") }
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setThemeMode("DARK") }
                )
            }

            // --- AMOLED option ---
            item {
                val isDarkThemeActive =
                    themeMode == "DARK" || (themeMode == "SYSTEM" && androidx.compose.foundation.isSystemInDarkTheme())
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme_amoled)) },
                    supportingContent = {
                        Text(
                            text = if (isDarkThemeActive) {
                                stringResource(R.string.settings_theme_amoled_desc)
                            } else {
                                stringResource(R.string.settings_theme_amoled_disabled_desc)
                            }
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = themeAmoled,
                            onCheckedChange = { viewModel.setThemeAmoled(it) },
                            enabled = isDarkThemeActive,
                            thumbContent = {
                                Icon(
                                    imageVector = if (themeAmoled) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    modifier = Modifier.clickable(enabled = isDarkThemeActive) {
                        viewModel.setThemeAmoled(!themeAmoled)
                    }
                )
            }

            // --- Accent Colors ---
            item {
                Text(
                    text = stringResource(R.string.settings_theme_accent_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val colors = mutableListOf<AccentColorOption>()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        colors.add(AccentColorOption("SYSTEM", Color.Transparent, isSystem = true))
                    }

                    colors.addAll(
                        listOf(
                            AccentColorOption("PURPLE", Color(0xFF6750A4)),
                            AccentColorOption("BLUE", Color(0xFF005FAF)),
                            AccentColorOption("GREEN", Color(0xFF388E3C)),
                            AccentColorOption("RED", Color(0xFFBA1A1A)),
                            AccentColorOption("ORANGE", Color(0xFFF57C00)),
                            AccentColorOption("PINK", Color(0xFFC2185B))
                        )
                    )

                    val isCustomAccent = themeAccent.startsWith("#")
                    val customColor = if (isCustomAccent) {
                        try {
                            Color(themeAccent.toColorInt())
                        } catch (_: Exception) {
                            Color(0xFFE91E63)
                        }
                    } else {
                        Color(0xFFE91E63)
                    }
                    colors.add(AccentColorOption("CUSTOM", customColor, isCustom = true))

                    // Akzentfarben in Zeilen von je 4 Elementen rendern
                    colors.chunked(4).forEach { rowColors ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowColors.forEach { option ->
                                val isSelected =
                                    if (option.isCustom) isCustomAccent else themeAccent == option.id
                                ColorItem(
                                    option = option,
                                    isSelected = isSelected,
                                    onClick = {
                                        if (option.isCustom) {
                                            showColorPickerDialog = true
                                        } else {
                                            viewModel.setThemeAccent(option.id)
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

            item {
                if (showColorPickerDialog) {
                    val parsedInitialColor = remember(themeAccent) {
                        if (themeAccent.startsWith("#")) {
                            try {
                                Color(themeAccent.toColorInt())
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
                            Color(0xFF4CAF50)  // Green
                        )
                    }
                    ColorPickerDialog(
                        initialColor = parsedInitialColor,
                        title = stringResource(R.string.theme_accent_custom_dialog_title),
                        onDismissRequest = { showColorPickerDialog = false },
                        onColorSelected = { color ->
                            val hexString = String.format("#%06X", 0xFFFFFF and color.toArgb())
                            viewModel.setThemeAccent(hexString)
                            showColorPickerDialog = false
                        },
                        presets = presets
                    )
                }
            }
        }
    }
}

data class AccentColorOption(
    val id: String,
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
                .size(48.dp)
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
                                    Color(0xFF4CAF50),
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
                    width = if (isSelected) 3.dp else 1.dp,
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
                    } else if (option.id in listOf(
                            "SYSTEM",
                            "BLUE",
                            "GREEN",
                            "PURPLE",
                            "RED",
                            "PINK"
                        )
                    ) {
                        Color.White
                    } else {
                        Color.Black
                    },
                    modifier = Modifier.size(24.dp)
                )
            } else if (option.isSystem) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else if (option.isCustom) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = if (option.color.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val label = when (option.id) {
            "SYSTEM" -> stringResource(R.string.theme_accent_system)
            "PURPLE" -> stringResource(R.string.theme_accent_purple)
            "BLUE" -> stringResource(R.string.theme_accent_blue)
            "GREEN" -> stringResource(R.string.theme_accent_green)
            "RED" -> stringResource(R.string.theme_accent_red)
            "ORANGE" -> stringResource(R.string.theme_accent_orange)
            "PINK" -> stringResource(R.string.theme_accent_pink)
            "CUSTOM" -> stringResource(R.string.theme_accent_custom)
            else -> option.id
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


