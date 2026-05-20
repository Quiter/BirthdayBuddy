package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private object ScrollbarDefaults {
    val BarWidth = 150.dp
    val ThumbSize = 48.dp
    const val BUBBLE_DELAY = 500L
    const val MIN_ITEMS_THRESHOLD = 10
    val ThumbWidthDragging = 12.dp
    val ThumbWidthIdle = 6.dp
    val ThumbHeightDragging = 32.dp
    val ThumbHeightIdle = 24.dp
    const val THUMB_ALPHA_IDLE = 0.4f
    val BubbleOffsetY = 4.dp
    val BubbleCornerLarge = 24.dp
    val BubbleCornerSmall = 4.dp
    val BubbleElevation = 6.dp
    val ThumbPaddingEnd = 6.dp
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

    // Bubble visibility: Zeigt die Bubble NUR beim Ziehen der Scrollbar
    val showBubble by produceState(initialValue = false, key1 = isDragging) {
        value = if (isDragging) {
            true
        } else {
            delay(ScrollbarDefaults.BUBBLE_DELAY)
            false
        }
    }

    BoxWithConstraints(modifier = modifier.width(ScrollbarDefaults.BarWidth)) {
        val totalItems = contacts.size
        val viewHeight = maxHeight
        val thumbHeight = ScrollbarDefaults.ThumbSize
        val trackHeight = viewHeight - thumbHeight
        val trackHeightPx = with(density) { trackHeight.toPx() }

        // Zurücksetzen des Offsets bei Filter-Wechsel
        LaunchedEffect(contacts, isResettingFilter) {
            if (isResettingFilter) {
                dragOffsetPx = 0f
            }
        }

        val canScroll by remember(contacts) {
            derivedStateOf {
                // Wenn wir gerade den Filter zurücksetzen, zeigen wir die Scrollbar vorsorglich an,
                // falls die Liste potenziell lang genug ist.
                if (isResettingFilter && (totalItems > ScrollbarDefaults.MIN_ITEMS_THRESHOLD)) return@derivedStateOf true

                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (visibleItemsInfo.isEmpty()) return@derivedStateOf false

                val lastItem = visibleItemsInfo.last()
                val isLastItemVisible = (lastItem.index == (totalItems - 1))
                
                // Falls das letzte Item noch nicht mal in der Liste der sichtbaren ist -> Scrollbar anzeigen
                if (!isLastItemVisible) return@derivedStateOf true
                
                // Falls das letzte Item in der Liste ist, prüfen ob es nach unten übersteht
                val viewportEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
                (lastItem.offset + lastItem.size) > viewportEnd
            }
        }

        if (canScroll && totalItems > 0) {
            val currentLabel by remember(contacts, trackHeightPx, getLabel) {
                derivedStateOf {
                    val index = if (isDragging) {
                        val percent = if (trackHeightPx > 0) (dragOffsetPx / trackHeightPx).coerceIn(0f, 1f) else 0f
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
                        
                        // Präziser fraktionaler Index
                        val fractionalIndex = listState.firstVisibleItemIndex.toFloat() + 
                            (scrollOffset / itemSize).coerceIn(0f, 1f)
                        
                        val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                        val itemsInViewport = if (itemSize > 0) viewportHeight.toFloat() / itemSize else 1f
                        val maxIndex = (totalItems.toFloat() - itemsInViewport).coerceAtLeast(1f)
                        
                        val scrollPercent = (fractionalIndex / maxIndex).coerceIn(0f, 1f)
                        trackHeight * scrollPercent
                    }
                }
            }

            val thumbWidth by animateDpAsState(
                targetValue = if (isDragging) ScrollbarDefaults.ThumbWidthDragging else ScrollbarDefaults.ThumbWidthIdle,
                label = "Thumb Width",
            )

            val thumbAlpha by remember {
                derivedStateOf {
                    if (isDragging || listState.isScrollInProgress) 1f else ScrollbarDefaults.THUMB_ALPHA_IDLE
                }
            }
            val animatedThumbAlpha by animateFloatAsState(
                targetValue = thumbAlpha,
                label = "Thumb Alpha",
            )

            // Bubble
            AnimatedVisibility(
                visible = showBubble,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .graphicsLayer {
                        translationY = thumbOffset.toPx() - ScrollbarDefaults.BubbleOffsetY.toPx()
                    },
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
                        text = currentLabel,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            val scrollbarDesc = stringResource(R.string.home_scrollbar_desc, currentLabel)

            // Thumb
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(ScrollbarDefaults.ThumbSize)
                    .height(thumbHeight)
                    .graphicsLayer {
                        translationY = thumbOffset.toPx()
                    }
                    .semantics { 
                        contentDescription = scrollbarDesc
                    }
                    .pointerInput(totalItems, trackHeightPx) {
                        try {
                            detectVerticalDragGestures(
                                onDragStart = {
                                    isDragging = true
                                    onSetFastScrolling(true)
                                    dragOffsetPx = with(density) { thumbOffset.toPx() }
                                },
                                onDragEnd = { 
                                    isDragging = false
                                    onSetFastScrolling(false)
                                },
                                onDragCancel = { 
                                    isDragging = false
                                    onSetFastScrolling(false)
                                },
                            ) { change, dragAmount ->
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, trackHeightPx)
                                val scrollPercent = if (trackHeightPx > 0) dragOffsetPx / trackHeightPx else 0f
                                
                                val (targetIndex, targetOffset) = calculateScrollTarget(
                                    scrollPercent = scrollPercent,
                                    totalItems = totalItems,
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
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = ScrollbarDefaults.ThumbPaddingEnd)
                        .width(thumbWidth)
                        .height(if (isDragging) ScrollbarDefaults.ThumbHeightDragging else ScrollbarDefaults.ThumbHeightIdle)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = animatedThumbAlpha)),
                )
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
