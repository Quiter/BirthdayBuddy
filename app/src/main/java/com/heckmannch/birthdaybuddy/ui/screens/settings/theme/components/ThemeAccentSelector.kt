package com.heckmannch.birthdaybuddy.ui.screens.settings.theme.components

import android.os.Build
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.ui.theme.AccentPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidthThick
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

private const val TAG = "ThemeAccentSelector"

private val LIGHT_ICON_ACCENTS = setOf(
    ThemeAccent.SYSTEM,
    ThemeAccent.BLUE,
    ThemeAccent.GREEN,
    ThemeAccent.PURPLE,
    ThemeAccent.RED,
    ThemeAccent.PINK
)

internal data class AccentColorOption(
    val id: ThemeAccent,
    val color: Color,
    val isSystem: Boolean = false,
    val isCustom: Boolean = false
)

/**
 * Reusable grid component for selecting theme accent colors.
 */
@Composable
internal fun ThemeAccentSelector(
    themeAccent: ThemeAccent,
    customAccentColor: String?,
    onThemeAccentChange: (ThemeAccent, String?) -> Unit,
    onOpenColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
                } catch (e: Exception) {
                    Log.w(TAG, "Fehler beim Parsen der Akzentfarbe: $customAccentColor", e)
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
                                onOpenColorPicker()
                            } else {
                                onThemeAccentChange(option.id, null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(4 - rowColors.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

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
