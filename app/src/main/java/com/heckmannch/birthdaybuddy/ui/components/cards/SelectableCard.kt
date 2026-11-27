package com.heckmannch.birthdaybuddy.ui.components.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.state.ToggleableState
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisDisabled
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisHigh
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisLow
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardSubtitle
import com.heckmannch.birthdaybuddy.ui.theme.AlphaGlassCardUnselected
import com.heckmannch.birthdaybuddy.ui.theme.AlphaSurfaceContainerHigh
import com.heckmannch.birthdaybuddy.ui.theme.GlassCardSelectionMarkSize
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth

val SelectionMarkSize = GlassCardSelectionMarkSize

/**
 * Wraps card content with a selection mode alpha dimming animation.
 */
@Composable
fun SelectableCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    content: @Composable () -> Unit
) {
    val cardAlpha by animateFloatAsState(
        targetValue = if (isSelectionMode && !isSelected) AlphaGlassCardUnselected else AlphaEmphasisHigh,
        animationSpec = tween(200),
        label = "card_alpha"
    )

    Box(modifier = modifier.graphicsLayer { alpha = cardAlpha }) {
        content()
    }
}

/**
 * Selection indicator mark placed at the trailing end of a card's row.
 */
@Composable
fun RowScope.SelectionMark(
    selected: Boolean?,
    modifier: Modifier = Modifier,
    ink: Color? = null,
    idle: (@Composable () -> Unit)? = null
) {
    val shown = remember { mutableStateOf(false) }
    if (selected != null) shown.value = selected
    val mark = @Composable { SelectionCheckIndicator(ToggleableState(shown.value), ink = ink) }

    if (idle == null) {
        AnimatedVisibility(
            visible = selected != null,
            modifier = modifier,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)
        ) {
            mark()
        }
        return
    }

    SwappedSlot(showsMark = selected != null, mark = mark, idle = idle, modifier = modifier)
}

@Composable
private fun SwappedSlot(
    showsMark: Boolean,
    mark: @Composable () -> Unit,
    idle: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.sizeIn(minWidth = SelectionMarkSize, minHeight = SelectionMarkSize),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = !showsMark,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)
        ) {
            idle()
        }
        AnimatedVisibility(
            visible = showsMark,
            enter = fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)
        ) {
            mark()
        }
    }
}

/**
 * Circle check indicator matching the frosted glass card style.
 */
@Composable
fun SelectionCheckIndicator(
    state: ToggleableState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color? = LocalAccent.current,
    ink: Color? = null
) {
    val colors = MaterialTheme.colorScheme
    val container = ink ?: accentColor?.copy(alpha = AlphaGlassCardSubtitle) ?: colors.primaryContainer
    val content = ink?.contrastingContent() ?: colors.onPrimaryContainer
    val outline = ink?.copy(alpha = AlphaSurfaceContainerHigh) ?: colors.outlineVariant

    Crossfade(
        targetState = state,
        animationSpec = tween(200),
        modifier = modifier,
        label = "selection_check"
    ) { shown ->
        when (shown) {
            ToggleableState.On -> {
                Box(
                    modifier = Modifier
                        .size(SelectionMarkSize)
                        .clip(CircleShape)
                        .background(if (enabled) container else container.copy(alpha = AlphaEmphasisDisabled)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (enabled) content else content.copy(alpha = AlphaEmphasisDisabled),
                        modifier = Modifier.size(IconSizeSmall)
                    )
                }
            }
            ToggleableState.Off -> {
                Box(
                    modifier = Modifier
                        .size(SelectionMarkSize)
                        .clip(CircleShape)
                        .border(SelectedBorderWidth, outline, CircleShape)
                ) {}
            }
            ToggleableState.Indeterminate -> {
                Box(
                    modifier = Modifier
                        .size(SelectionMarkSize)
                        .clip(CircleShape)
                        .background(container.copy(alpha = AlphaEmphasisLow)),
                    contentAlignment = Alignment.Center
                ) {}
            }
        }
    }
}
