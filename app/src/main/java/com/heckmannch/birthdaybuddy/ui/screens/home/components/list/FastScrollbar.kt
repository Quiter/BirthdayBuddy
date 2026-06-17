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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.heckmannch.birthdaybuddy.R
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
    val BubbleElevation = 6.dp
}

@Composable
fun FastScrollbar(
    listState: LazyListState,
    contacts: List<ContactUiModel>,
    getLabel: (ContactUiModel) -> String,
    modifier: Modifier = Modifier,
    isResettingFilter: Boolean = false,
    onSetFastScrolling: (Boolean) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(value = false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    // Visibility state machine: show on scroll/drag/data updates, fade out after 1.5 seconds of idle
    var isVisible by remember { mutableStateOf(value = false) }

    LaunchedEffect(listState.isScrollInProgress, isDragging, contacts) {
        if (listState.isScrollInProgress || isDragging) {
            isVisible = true
        } else {
            // Show briefly when data changes, or keep visible while active. Otherwise, fade out after delay.
            isVisible = true
            delay(1500.milliseconds)
            isVisible = false
        }
    }

    val animatedVisibilityAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Scrollbar Visibility Alpha"
    )

    // Bubble visibility: Zeigt die Bubble NUR beim Ziehen der Scrollbar
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
        modifier = modifier
            .width(ScrollbarDefaults.BarWidth)
    ) {
        val totalItems = contacts.size
        val viewHeight = maxHeight
        val trackHeight = viewHeight - ScrollbarDefaults.ThumbHeight
        val trackHeightPx = with(density) { trackHeight.toPx() }
        val thumbHeightPx = with(density) { ScrollbarDefaults.ThumbHeight.toPx() }

        val currentTotalItems by rememberUpdatedState(totalItems)
        val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)
        val currentOnSetFastScrolling by rememberUpdatedState(onSetFastScrolling)

        // Zurücksetzen des Offsets bei Filter-Wechsel
        LaunchedEffect(contacts, isResettingFilter) {
            if (isResettingFilter) {
                dragOffsetPx = 0f
            }
        }

        val canScroll by remember(contacts, isResettingFilter) {
            derivedStateOf {
                if (isResettingFilter && (totalItems > ScrollbarDefaults.MIN_ITEMS_THRESHOLD)) return@derivedStateOf true

                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isEmpty()) return@derivedStateOf false

                val lastItem = visibleItemsInfo.last()
                val isLastItemVisible = (lastItem.index == (totalItems - 1))

                if (!isLastItemVisible) return@derivedStateOf true

                val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
                (lastItem.offset + lastItem.size) > viewportEnd
            }
        }

        if (canScroll && totalItems > 0) {
            val currentLabel by remember(contacts, trackHeightPx, getLabel) {
                derivedStateOf {
                    val index = if (isDragging) {
                        val percent =
                            if (trackHeightPx > 0) (dragOffsetPx / trackHeightPx).coerceIn(
                                0f,
                                1f
                            ) else 0f
                        (percent * (totalItems - 1)).toInt()
                    } else {
                        listState.firstVisibleItemIndex
                    }
                    val contact = contacts.getOrNull(index.coerceIn(0, totalItems - 1))
                    if (contact != null) getLabel(contact) else ""
                }
            }

            // Haptisches Feedback bei Label-Wechsel während des Drags
            LaunchedEffect(currentLabel) {
                if (isDragging) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            val thumbOffset by remember(trackHeight, isResettingFilter, contacts) {
                derivedStateOf {
                    if (isResettingFilter) return@derivedStateOf 0.dp

                    if (isDragging) {
                        with(density) { dragOffsetPx.toDp() }.coerceIn(0.dp, trackHeight)
                    } else {
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) return@derivedStateOf 0.dp

                        val firstItem = visibleItems.first()
                        val scrollOffset = listState.firstVisibleItemScrollOffset.toFloat()
                        val itemSize = firstItem.size.toFloat()

                        val fractionalIndex = listState.firstVisibleItemIndex.toFloat() +
                                (scrollOffset / itemSize).coerceIn(0f, 1f)

                        val viewportHeight =
                            layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val itemsInViewport =
                            if (itemSize > 0) viewportHeight.toFloat() / itemSize else 1f
                        val maxIndex = (totalItems.toFloat() - itemsInViewport).coerceAtLeast(1f)

                        val scrollPercent = (fractionalIndex / maxIndex).coerceIn(0f, 1f)
                        trackHeight * scrollPercent
                    }
                }
            }

            // Bubble
            ScrollbarBubble(
                visible = showBubble,
                label = currentLabel,
                thumbOffset = { thumbOffset }
            )

            val scrollbarDesc = stringResource(R.string.home_scrollbar_desc, currentLabel)

            // Interactive Scroll Track Column
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(ScrollbarDefaults.ThumbSize) // 48.dp
                    .fillMaxHeight()
                    .graphicsLayer {
                        alpha = animatedVisibilityAlpha
                    }
                    .then(
                        if (isVisible) {
                            Modifier.pointerInput(listState) {
                                try {
                                    detectVerticalDragGestures(
                                        onDragStart = { offset ->
                                            isDragging = true
                                            currentOnSetFastScrolling(true)
                                            dragOffsetPx = (offset.y - thumbHeightPx / 2f).coerceIn(
                                                0f,
                                                currentTrackHeightPx
                                            )

                                            val scrollPercent =
                                                if (currentTrackHeightPx > 0) dragOffsetPx / currentTrackHeightPx else 0f
                                            val (targetIndex, targetOffset) = calculateScrollTarget(
                                                scrollPercent = scrollPercent,
                                                totalItems = currentTotalItems,
                                                layoutInfo = listState.layoutInfo,
                                            )
                                            scope.launch {
                                                listState.scrollToItem(targetIndex, targetOffset)
                                            }
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            currentOnSetFastScrolling(false)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            currentOnSetFastScrolling(false)
                                        },
                                    ) { change, dragAmount ->
                                        dragOffsetPx =
                                            (dragOffsetPx + dragAmount).coerceIn(0f, currentTrackHeightPx)
                                        val scrollPercent =
                                            if (currentTrackHeightPx > 0) dragOffsetPx / currentTrackHeightPx else 0f

                                        val (targetIndex, targetOffset) = calculateScrollTarget(
                                            scrollPercent = scrollPercent,
                                            totalItems = currentTotalItems,
                                            layoutInfo = listState.layoutInfo,
                                        )
                                        scope.launch {
                                            listState.scrollToItem(targetIndex, targetOffset)
                                        }
                                        change.consume()
                                    }
                                } finally {
                                    isDragging = false
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
            ) {
                // Thumb Capsule overlaying the list
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                        .width(ScrollbarDefaults.ThumbWidth) // 26.dp
                        .height(ScrollbarDefaults.ThumbHeight) // 44.dp
                        .graphicsLayer {
                            translationY = thumbOffset.toPx()
                        }
                        .semantics {
                            contentDescription = scrollbarDesc
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Zeigt das Month/Letter-Label in einer eleganten Bubble an.
 * Nutzt ein Popup, um sicherzustellen, dass die Bubble vor allen anderen
 * UI-Elementen (inklusive der HomeTopBar) gezeichnet wird und nicht abgeschnitten wird.
 */
@Composable
private fun ScrollbarBubble(
    visible: Boolean,
    label: String,
    thumbOffset: () -> Dp,
    modifier: Modifier = Modifier,
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
                usePlatformDefaultWidth = false,
            )
        ) {
            Box(
                modifier = modifier
                    .width(ScrollbarDefaults.BarWidth)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val offsetY = (thumbOffset() - ScrollbarDefaults.BubbleOffsetY).toPx()
                            translationY = offsetY.coerceAtLeast(0f)
                        }
                ) {
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
                                bottomEnd = ScrollbarDefaults.BubbleCornerLarge,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = ScrollbarDefaults.BubbleElevation,
                            modifier = Modifier.padding(end = 16.dp),
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



/**
 * Berechnet den Ziel-Index und Offset basierend auf der Scroll-Position in Prozent.
 */
private fun calculateScrollTarget(
    scrollPercent: Float,
    totalItems: Int,
    layoutInfo: LazyListLayoutInfo,
): Pair<Int, Int> {
    val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    val itemsInViewport = viewportHeight.toFloat() / itemSize
    val maxIndex = (totalItems.toFloat() - itemsInViewport).coerceAtLeast(1f)

    val targetFractionalIndex = scrollPercent * maxIndex
    val targetIndex = targetFractionalIndex.toInt().coerceIn(0, totalItems - 1)
    val targetOffset = ((targetFractionalIndex - targetIndex) * itemSize).toInt()

    return targetIndex to targetOffset
}
