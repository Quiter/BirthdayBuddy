package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val currentThumbHeight by animateDpAsState(
        targetValue = if (isDragging) ScrollbarDefaults.ThumbHeightDragging else ScrollbarDefaults.ThumbHeightIdle,
        label = "Thumb Height",
    )

    BoxWithConstraints(modifier = modifier.width(ScrollbarDefaults.BarWidth)) {
        val totalItems = contacts.size
        val viewHeight = maxHeight
        val trackHeight = viewHeight - currentThumbHeight
        val trackHeightPx = with(density) { trackHeight.toPx() }

        // Um Re-Kompositionen des pointerInput Blocks zu verhindern, nutzen wir rememberUpdatedState
        val currentTotalItems by rememberUpdatedState(totalItems)
        val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)
        val currentDensity by rememberUpdatedState(density)
        val currentOnSetFastScrolling by rememberUpdatedState(onSetFastScrolling)
        val currentThumbHeightState by rememberUpdatedState(currentThumbHeight)

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

                        // Präziser fraktionaler Index
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
            ScrollbarBubble(
                visible = showBubble,
                label = currentLabel,
                thumbOffset = thumbOffset
            )

            val scrollbarDesc = stringResource(R.string.home_scrollbar_desc, currentLabel)

            // Interactive Scroll Track Column
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(ScrollbarDefaults.ThumbSize) // 48.dp
                    .fillMaxHeight()
                    .pointerInput(listState) {
                        try {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    currentOnSetFastScrolling(true)
                                    val thumbHeightPx = with(currentDensity) { currentThumbHeightState.toPx() }
                                    dragOffsetPx = (offset.y - thumbHeightPx / 2f).coerceIn(0f, currentTrackHeightPx)
                                    
                                    val scrollPercent = if (currentTrackHeightPx > 0) dragOffsetPx / currentTrackHeightPx else 0f
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
                                dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, currentTrackHeightPx)
                                val scrollPercent = if (currentTrackHeightPx > 0) dragOffsetPx / currentTrackHeightPx else 0f
                                
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
            ) {
                // Visuals for track line and thumb
                ScrollbarTrackAndThumb(
                    isDragging = isDragging,
                    isScrollInProgress = listState.isScrollInProgress,
                    currentThumbHeight = currentThumbHeight,
                    thumbOffset = thumbOffset,
                    thumbWidth = thumbWidth,
                    animatedThumbAlpha = animatedThumbAlpha,
                    scrollbarDesc = scrollbarDesc,
                    modifier = Modifier.fillMaxSize()
                )
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
    thumbOffset: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val transitionState = remember { MutableTransitionState(initialState = false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Popup(
            alignment = Alignment.TopStart,
            offset = remember(thumbOffset, density) {
                with(density) {
                    IntOffset(
                        x = 0,
                        y = (thumbOffset - ScrollbarDefaults.BubbleOffsetY).roundToPx()
                    )
                }
            },
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = false, // Verhindert automatische Nudge-/Positionskorrekturen des OS am Rand
                usePlatformDefaultWidth = false,
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
                modifier = modifier,
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

/**
 * Zeichnet den dezenten Scroll-Track und den interaktiven Thumb-Indikator.
 */
@Composable
private fun ScrollbarTrackAndThumb(
    isDragging: Boolean,
    isScrollInProgress: Boolean,
    currentThumbHeight: Dp,
    thumbOffset: Dp,
    thumbWidth: Dp,
    animatedThumbAlpha: Float,
    scrollbarDesc: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // 1. Subtle, elegant track line (centered at 12.dp from the right edge)
        val trackAlpha by remember {
            derivedStateOf {
                if (isDragging || isScrollInProgress) 0.15f else 0.04f
            }
        }
        val animatedTrackAlpha by animateFloatAsState(
            targetValue = trackAlpha,
            label = "Track Alpha",
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp) // Centered at 12.dp from the right (10.dp padding + 4.dp/2 = 12.dp)
                .width(4.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = animatedTrackAlpha))
        )

        // 2. Thumb Box (centered at 12.dp from the right edge)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(ScrollbarDefaults.ThumbSize)
                .height(currentThumbHeight)
                .graphicsLayer {
                    translationY = thumbOffset.toPx()
                }
                .semantics {
                    contentDescription = scrollbarDesc
                },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp - thumbWidth / 2) // Dynamically centers the thumb on the 12.dp vertical line
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = animatedThumbAlpha)),
            )
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
