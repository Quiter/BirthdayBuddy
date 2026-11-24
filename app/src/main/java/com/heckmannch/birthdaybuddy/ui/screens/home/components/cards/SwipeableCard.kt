package com.heckmannch.birthdaybuddy.ui.screens.home.components.cards

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import kotlinx.coroutines.launch

/**
 * Data describing one side of a card's swipe action - icon, label, and colors.
 */
data class SwipeActionConfig(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

/**
 * Represents a triggerable swipe action associated with a card side.
 */
class SwipeAction(val config: SwipeActionConfig, val onTrigger: () -> Unit)

private val SwipeActionThreshold = 90.dp

@Composable
private fun BoxScope.SwipeReveal(config: SwipeActionConfig, progress: Float, atEnd: Boolean) {
    val tint = config.containerColor.copy(alpha = 0.85f * progress)
    val clear = config.containerColor.copy(alpha = 0f)
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                startToEndGradient(
                    startColor = if (atEnd) clear else tint,
                    endColor = if (atEnd) tint else clear,
                    rtl = isRtl()
                )
            ),
        contentAlignment = if (atEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = SpacingLarge)
                .graphicsLayer { alpha = progress },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingExtraSmall)
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.contentColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = config.label,
                style = MaterialTheme.typography.labelSmall,
                color = config.contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Container component that enables horizontal swipe gesture handling for cards.
 * Uncovers custom start/end actions when dragged beyond [SwipeActionThreshold].
 * Directly modeled after Morphe Manager's SwipeableCardContainer.
 */
@Composable
fun SwipeableCardContainer(
    modifier: Modifier = Modifier,
    start: SwipeAction? = null,
    end: SwipeAction? = null,
    enabled: Boolean = true,
    offsetX: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) },
    accessibilityActions: List<CustomAccessibilityAction> = emptyList(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val actionThresholdPx = with(LocalDensity.current) { SwipeActionThreshold.toPx() }
    val currentStart by rememberUpdatedState(start)
    val currentEnd by rememberUpdatedState(end)

    LaunchedEffect(enabled) {
        if (!enabled) offsetX.animateTo(0f, tween(200))
    }

    val rtl = isRtl()
    val directionSign = if (rtl) -1f else 1f

    val startProgress by remember { derivedStateOf { (-offsetX.value / actionThresholdPx).coerceIn(0f, 1f) } }
    val endProgress by remember { derivedStateOf { (offsetX.value / actionThresholdPx).coerceIn(0f, 1f) } }

    val swipeActions = if (enabled) listOfNotNull(start, end) else emptyList()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                customActions = accessibilityActions + swipeActions.map { action ->
                    CustomAccessibilityAction(action.config.label) { action.onTrigger(); true }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(CornerRadiusLarge))
        ) {
            start?.takeIf { startProgress > 0.01f }?.let { SwipeReveal(it.config, startProgress, atEnd = true) }
            end?.takeIf { endProgress > 0.01f }?.let { SwipeReveal(it.config, endProgress, atEnd = false) }
        }

        Box(
            modifier = Modifier
                .graphicsLayer { translationX = offsetX.value * directionSign }
                .then(
                    if (enabled) Modifier.pointerInput(rtl) {
                        val settle = spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                        val limit = actionThresholdPx * 1.5f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                val (action, haptic) = when {
                                    offsetX.value < -actionThresholdPx ->
                                        currentStart to HapticFeedbackConstants.LONG_PRESS
                                    offsetX.value > actionThresholdPx ->
                                        currentEnd to HapticFeedbackConstants.VIRTUAL_KEY
                                    else -> null to null
                                }
                                scope.launch {
                                    if (action != null && haptic != null) {
                                        view.performHapticFeedback(haptic)
                                        offsetX.animateTo(0f, tween(200))
                                        action.onTrigger()
                                    } else {
                                        offsetX.animateTo(0f, settle)
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, settle) }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val clamped = (offsetX.value + dragAmount * directionSign).coerceIn(
                                        if (currentStart != null) -limit else 0f,
                                        if (currentEnd != null) limit else 0f
                                    )
                                    offsetX.snapTo(clamped)
                                }
                            }
                        )
                    } else Modifier
                )
        ) {
            content()
        }
    }
}
