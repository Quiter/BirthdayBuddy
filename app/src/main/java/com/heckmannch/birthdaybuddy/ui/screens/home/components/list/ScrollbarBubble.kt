package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Animated label bubble that appears beside the scrollbar thumb during drag.
 *
 * Rendered via a [Popup] (with clipping disabled) so it is never clipped by
 * the parent container and always renders above all other UI layers.
 *
 * Design decisions:
 * - [MutableTransitionState] drives enter/exit animations without depending
 *   on the composable being recomposed while invisible.
 * - The offset is provided as a lambda (`thumbOffset: () -> Dp`) to allow
 *   `graphicsLayer` to read it during the draw phase, avoiding recomposition
 *   on every drag pixel.
 * - Visibility is `internal` (not `private`) so that [FastScrollbar] can call
 *   it from a separate file within the same package.
 *
 * @param visible Whether the bubble should be shown.
 * @param label   The section label to display (e.g. month name or first letter).
 * @param thumbOffset Lambda returning the current vertical thumb offset in Dp.
 */
@Composable
internal fun ScrollbarBubble(
    visible: Boolean,
    label: String,
    thumbOffset: () -> Dp,
) {
    val transitionState = remember { MutableTransitionState(initialState = false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = androidx.compose.ui.Alignment.TopStart,
            offset = remember { IntOffset.Zero },
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .width(ScrollbarDefaults.BarWidth)
                    .fillMaxHeight()
            ) {
                Box(modifier = Modifier.graphicsLayer {
                    translationY =
                        (thumbOffset() - ScrollbarDefaults.BubbleOffsetY).toPx().coerceAtLeast(0f)
                }) {
                    AnimatedVisibility(
                        visibleState = transitionState,
                        enter = fadeIn() + slideInHorizontally { it / 2 },
                        exit = fadeOut() + slideOutHorizontally { it / 2 },
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = ScrollbarDefaults.BubbleCornerLarge,
                                bottomStart = ScrollbarDefaults.BubbleCornerLarge,
                                topEnd = ScrollbarDefaults.BubbleCornerSmall,
                                bottomEnd = ScrollbarDefaults.BubbleCornerLarge
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = ScrollbarDefaults.BubbleElevation,
                            modifier = Modifier.padding(end = SpacingNormal)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(
                                    horizontal = SpacingNormal,
                                    vertical = SpacingSmall
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                }
            }
        }
    }
}
