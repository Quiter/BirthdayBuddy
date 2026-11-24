package com.heckmannch.birthdaybuddy.ui.screens.home.components.cards

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
import androidx.compose.ui.unit.dp

val SelectionMarkSize = 28.dp

/**
 * Wraps card content with a selection mode alpha dimming animation.
 * Directly modeled after Morphe Manager's SelectableCard.
 */
@Composable
fun SelectableCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    content: @Composable () -> Unit
) {
    val cardAlpha by animateFloatAsState(
        targetValue = if (isSelectionMode && !isSelected) 0.55f else 1f,
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
 * Circle check indicator matching Morphe Manager's checkbox style.
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
    val container = ink ?: accentColor?.copy(alpha = 0.85f) ?: colors.primaryContainer
    val content = ink?.contrastingContent() ?: colors.onPrimaryContainer
    val outline = ink?.copy(alpha = 0.6f) ?: colors.outlineVariant

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
                        .background(if (enabled) container else container.copy(alpha = 0.38f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = if (enabled) content else content.copy(alpha = 0.38f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ToggleableState.Off -> {
                Box(
                    modifier = Modifier
                        .size(SelectionMarkSize)
                        .clip(CircleShape)
                        .border(1.5.dp, outline, CircleShape)
                ) {}
            }
            ToggleableState.Indeterminate -> {
                Box(
                    modifier = Modifier
                        .size(SelectionMarkSize)
                        .clip(CircleShape)
                        .background(container.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {}
            }
        }
    }
}
