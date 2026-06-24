package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private object ScrollbarDefaults {
    val BarWidth = 150.dp
    val ThumbSize = 48.dp
    const val BUBBLE_DELAY = 500L
    const val MIN_ITEMS_THRESHOLD = 10
    val ThumbWidth = 26.dp
    val ThumbHeight = 44.dp
    val BubbleOffsetY = 4.dp
    val BubbleCornerLarge = 24.dp
    val BubbleCornerSmall = 4.dp
    val BubbleElevation = 8.dp
}

@Composable
fun FastScrollbar(
    listState: LazyListState,
    contacts: List<ContactUiModel>,
    getLabel: (ContactUiModel) -> String,
    modifier: Modifier = Modifier,
    headerCount: Int = 0,
    onSetFastScrolling: (Boolean) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(value = false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    // Sichtbarkeit: Nur bei echtem Scrollen oder Ziehen zeigen
    var isVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(listState.isScrollInProgress, isDragging) {
        if (listState.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            if (isVisible) {
                delay(1500.milliseconds)
                isVisible = false
            }
        }
    }

    val animatedVisibilityAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Scrollbar Alpha"
    )

    // Bubble Sichtbarkeit: NUR beim Ziehen, bleibt kurz nach dem Loslassen sichtbar
    var showBubble by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging) {
        if (isDragging) {
            showBubble = true
        } else {
            delay(ScrollbarDefaults.BUBBLE_DELAY.milliseconds)
            showBubble = false
        }
    }

    BoxWithConstraints(
        modifier = modifier.width(ScrollbarDefaults.BarWidth)
    ) {
        val totalItems = contacts.size
        val viewHeight = maxHeight
        val trackHeight = viewHeight - ScrollbarDefaults.ThumbHeight
        val trackHeightPx = with(density) { trackHeight.toPx() }
        val thumbHeightPx = with(density) { ScrollbarDefaults.ThumbHeight.toPx() }

        val currentTotalItems by rememberUpdatedState(totalItems)
        val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)
        val currentOnSetFastScrolling by rememberUpdatedState(onSetFastScrolling)
        val currentHeaderCount by rememberUpdatedState(headerCount)

        // Best Practice: canScroll entscheidet nur, ob die Scrollbar ÜBERHAUPT existiert.
        // Einmal angezeigt, bleibt sie da, bis die Sichtbarkeit (alpha) sie ausblendet.
        val isNeeded = remember(totalItems) {
            totalItems > ScrollbarDefaults.MIN_ITEMS_THRESHOLD
        }

        if (isNeeded && totalItems > 0) {
            val thumbOffset by remember(trackHeight, headerCount) {
                derivedStateOf {
                    if (isDragging) {
                        with(density) { dragOffsetPx.toDp() }.coerceIn(0.dp, trackHeight)
                    } else {
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@derivedStateOf 0.dp

                        val firstItem = visibleItems.first()
                        val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                        val itemSize = firstItem.size.toFloat().coerceAtLeast(1f)

                        val contactIndex = (listState.firstVisibleItemIndex - headerCount).coerceAtLeast(0)
                        val fractionalIndex =
                            contactIndex.toFloat() + (scrollOffset / itemSize)
                        val maxScrollIndex =
                            (totalItems.toFloat() - (layoutInfo.viewportEndOffset / itemSize)).coerceAtLeast(
                                1f
                            )

                        val scrollPercent = (fractionalIndex / maxScrollIndex).coerceIn(0f, 1f)
                        trackHeight * scrollPercent
                    }
                }
            }

            val currentLabel by remember(contacts, trackHeight) {
                derivedStateOf {
                    val percent =
                        if (trackHeight > 0.dp) (thumbOffset / trackHeight).coerceIn(0f, 1f) else 0f
                    val index = (percent * (totalItems - 1)).toInt().coerceIn(0, totalItems - 1)
                    contacts.getOrNull(index)?.let(getLabel) ?: ""
                }
            }

            LaunchedEffect(currentLabel) {
                if (isDragging) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            // Bubble in einem Popup, damit sie IMMER über dem FAB liegt (Z-Index fix)
            ScrollbarBubble(
                visible = showBubble,
                label = currentLabel,
                thumbOffset = { thumbOffset })

            // Interaktive Spur
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(ScrollbarDefaults.ThumbSize)
                    .fillMaxHeight()
                    .navigationBarsPadding() // Sicherstellen, dass wir nicht in die Systemgesten unten kommen
                    .graphicsLayer { alpha = animatedVisibilityAlpha }
                    .pointerInput(listState) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                currentOnSetFastScrolling(true)
                                dragOffsetPx = (offset.y - thumbHeightPx / 2f).coerceIn(
                                    0f,
                                    currentTrackHeightPx
                                )
                                val scrollPercent = dragOffsetPx / currentTrackHeightPx
                                val (targetIndex, targetOffset) = calculateScrollTarget(
                                    scrollPercent,
                                    currentTotalItems
                                )
                                scope.launch { listState.scrollToItem(currentHeaderCount + targetIndex, targetOffset) }
                            },
                            onDragEnd = { isDragging = false; currentOnSetFastScrolling(false) },
                            onDragCancel = { isDragging = false; currentOnSetFastScrolling(false) },
                        ) { change, dragAmount ->
                            dragOffsetPx =
                                (dragOffsetPx + dragAmount).coerceIn(0f, currentTrackHeightPx)
                            val scrollPercent = dragOffsetPx / currentTrackHeightPx
                            val (targetIndex, targetOffset) = calculateScrollTarget(
                                scrollPercent,
                                currentTotalItems
                            )
                            scope.launch { listState.scrollToItem(currentHeaderCount + targetIndex, targetOffset) }
                            change.consume()
                        }
                    }
            ) {
                // Der visuelle Thumb (Kapsel)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp)
                        .width(ScrollbarDefaults.ThumbWidth)
                        .height(ScrollbarDefaults.ThumbHeight)
                        .graphicsLayer { translationY = thumbOffset.toPx() }
                        .semantics { contentDescription = "Scrollbar" },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    tonalElevation = 4.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScrollbarBubble(
    visible: Boolean,
    label: String,
    thumbOffset: () -> Dp,
) {
    val transitionState = remember { MutableTransitionState(initialState = false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = Alignment.TopStart,
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
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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

private fun calculateScrollTarget(
    scrollPercent: Float,
    totalItems: Int,
): Pair<Int, Int> {
    val targetFractionalIndex = scrollPercent * (totalItems - 1)
    val targetIndex = targetFractionalIndex.toInt().coerceIn(0, totalItems - 1)
    return targetIndex to 0
}
