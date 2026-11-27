package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.theme.components

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.graphics.toColorInt
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ThemeAccent
import com.heckmannch.birthdaybuddy.ui.components.settings.CornerRadii
import com.heckmannch.birthdaybuddy.ui.components.settings.rememberPressShape
import com.heckmannch.birthdaybuddy.ui.components.settings.shapedClickable
import com.heckmannch.birthdaybuddy.ui.theme.AccentPresetColors
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSettingsSwatchUnselected
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusExtraLarge
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.SettingsSwatchSize
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal

private const val TAG = "ThemeAccentSelector"
private const val LightSwatchLuminanceThreshold = 0.5f

private val LightIconAccents = setOf(
    ThemeAccent.SYSTEM,
    ThemeAccent.BLUE,
    ThemeAccent.GREEN,
    ThemeAccent.PURPLE,
    ThemeAccent.RED,
    ThemeAccent.PINK,
)

@Immutable
internal data class AccentColorOption(
    val id: ThemeAccent,
    val color: Color,
    val isSystem: Boolean = false,
    val isCustom: Boolean = false,
)

/**
 * Morphe-style expressive accent swatch grid featuring rounded-square-to-pill morphing on selection
 * and tactile corner tightening on press.
 */
@Composable
internal fun ThemeAccentSelector(
    themeAccent: ThemeAccent,
    customAccentColor: String?,
    onThemeAccentChange: (ThemeAccent, String?) -> Unit,
    onOpenColorPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = remember(customAccentColor, themeAccent) {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(AccentColorOption(ThemeAccent.SYSTEM, Color.Transparent, isSystem = true))
            }
            add(AccentColorOption(ThemeAccent.PURPLE, AccentPresetColors[5]))
            add(AccentColorOption(ThemeAccent.BLUE, AccentPresetColors[0]))
            add(AccentColorOption(ThemeAccent.GREEN, AccentPresetColors[1]))
            add(AccentColorOption(ThemeAccent.RED, AccentPresetColors[2]))
            add(AccentColorOption(ThemeAccent.ORANGE, AccentPresetColors[3]))
            add(AccentColorOption(ThemeAccent.PINK, AccentPresetColors[4]))

            val customColor = if (themeAccent == ThemeAccent.CUSTOM && customAccentColor != null) {
                try {
                    Color(customAccentColor.toColorInt())
                } catch (e: Exception) {
                    Log.w(TAG, "Fehler beim Parsen der Akzentfarbe: $customAccentColor", e)
                    Color(0xFFE91E63)
                }
            } else {
                Color(0xFFE91E63)
            }
            add(AccentColorOption(ThemeAccent.CUSTOM, customColor, isCustom = true))
        }
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SpacingMedium, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(SpacingNormal),
    ) {
        options.forEach { option ->
            val isSelected = themeAccent == option.id
            AccentSwatchTile(
                option = option,
                isSelected = isSelected,
                onClick = {
                    if (option.isCustom) {
                        onOpenColorPicker()
                    } else {
                        onThemeAccentChange(option.id, null)
                    }
                },
            )
        }
    }
}

@Composable
private fun AccentSwatchTile(
    option: AccentColorOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val baseCorners = CornerRadii.uniform(
        if (isSelected) CornerRadiusExtraLarge else CornerRadiusLarge
    )
    val shape = rememberPressShape(base = baseCorners, interactionSource = interactionSource)

    val swatchFillModifier = if (option.isSystem) {
        Modifier.background(
            brush = Brush.sweepGradient(
                listOf(
                    Color(0xFFE91E63),
                    Color(0xFF9C27B0),
                    Color(0xFF2196F3),
                    Color(0xFF4CAF50),
                    Color(0xFFFFEB3B),
                    Color(0xFFFF9800),
                    Color(0xFFE91E63),
                )
            ),
            shape = shape,
        )
    } else {
        Modifier.background(color = option.color, shape = shape)
    }

    val iconTint = when {
        option.isSystem -> Color.White
        option.isCustom -> if (option.color.luminance() > LightSwatchLuminanceThreshold) Color.Black else Color.White
        option.id in LightIconAccents -> Color.White
        else -> Color.Black
    }

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

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall),
        modifier = Modifier
            .width(SettingsSwatchSize + SpacingNormal)
            .semantics(mergeDescendants = true) { selected = isSelected },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SettingsSwatchSize)
                .then(swatchFillModifier)
                .border(
                    width = if (isSelected) SelectedBorderWidth else BorderWidthThin,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = AlphaSettingsSwatchUnselected)
                    },
                    shape = shape,
                )
                .shapedClickable(
                    shape = shape,
                    interactionSource = interactionSource,
                    role = Role.RadioButton,
                    onClick = onClick,
                ),
        ) {
            when {
                isSelected -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(IconSizeNormal),
                )
                option.isSystem -> Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(IconSizeMedium),
                )
                option.isCustom -> Icon(
                    imageVector = Icons.Rounded.Colorize,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(IconSizeMedium),
                )
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
