/*
 * Copyright 2026 BirthdayBuddy / Morphe Architecture.
 */

package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusLarge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.graphics.lerp as lerpColor

private val ScrollbarEdgePadding = 4.dp
private val ScrollbarTouchWidth = 16.dp
private val ScrollbarOverlayWidth = 104.dp
private val ScrollbarTrackWidth = 4.dp
private val ScrollbarMinThumbHeight = 36.dp
private val AlphabetThumbHeight = 28.dp
private val AlphabetBubbleWidth = 58.dp
private val AlphabetBubbleHeight = 42.dp

private val ScrollbarIdleTimeout = 650.milliseconds
private const val ScrollbarSettleDuration = 180
private const val MinThumbFraction = 0.08f
private const val MinAlphabetTargets = 5

@Immutable
data class ScrollTarget(
    val listIndex: Int,
    val label: String
)

internal fun buildScrollTargets(
    emit: ((listIndex: Int, label: String) -> Unit) -> Unit
): List<ScrollTarget> {
    val seenLabels = HashSet<String>()
    return buildList {
        emit { listIndex, label ->
            val targetLabel = label.scrollLabel()
            if (seenLabels.add(targetLabel)) {
                add(ScrollTarget(listIndex = listIndex, label = targetLabel))
            }
        }
    }
}

fun <T> buildIndexedScrollTargets(
    items: List<T>,
    label: (T) -> String
): List<ScrollTarget> = buildScrollTargets { emit ->
    items.forEachIndexed { index, item -> emit(index, label(item)) }
}

private data class ScrollbarSeekTarget(
    val index: Int,
    val offset: Int = 0,
    val label: String? = null
)

private val ReportOnlyConnection = object : NestedScrollConnection {}

private class ScrollbarSeekController(private val scope: CoroutineScope) {
    private var job: Job? = null

    fun seek(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch { block() }
    }

    fun afterSeek(block: suspend () -> Unit) {
        val last = job
        job = scope.launch {
            last?.join()
            block()
        }
    }
}

@Composable
fun BoxScope.ListScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    alphabetTargets: List<ScrollTarget> = emptyList(),
    alphabetMode: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val sizeCache = remember(listState) { mutableMapOf<Int, Int>() }
    val metrics = remember(listState) {
        derivedStateOf { listState.scrollbarMetrics(sizeCache) }
    }
    val canScroll by remember(listState) {
        derivedStateOf { listState.canScrollBackward || listState.canScrollForward }
    }
    val alphabetEnabled = alphabetMode && alphabetTargets.size >= MinAlphabetTargets

    ScrollbarOverlay(
        progress = { metrics.value.progress },
        thumbFraction = { metrics.value.thumbFraction },
        canScroll = canScroll,
        isScrolling = listState.isScrollInProgress,
        alphabetEnabled = alphabetEnabled,
        contentPadding = contentPadding,
        modifier = modifier,
        resolveSeek = { fraction ->
            val alphabetTarget = if (alphabetEnabled) {
                alphabetTargets[
                    (fraction * alphabetTargets.lastIndex)
                        .roundToInt()
                        .coerceIn(alphabetTargets.indices)
                ]
            } else {
                null
            }
            if (alphabetTarget != null) {
                ScrollbarSeekTarget(index = alphabetTarget.listIndex, label = alphabetTarget.label)
            } else {
                listState.seekTarget(fraction, sizeCache, metrics.value)
            }
        },
        scrollOffset = { metrics.value.scrollOffset },
        scrollTo = { target -> listState.scrollToItem(target.index, target.offset) }
    )
}

@Composable
private fun BoxScope.ScrollbarOverlay(
    progress: () -> Float,
    thumbFraction: () -> Float,
    canScroll: Boolean,
    isScrolling: Boolean,
    alphabetEnabled: Boolean,
    contentPadding: PaddingValues,
    resolveSeek: (fraction: Float) -> ScrollbarSeekTarget,
    scrollOffset: () -> Float,
    scrollTo: suspend (target: ScrollbarSeekTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val currentResolveSeek by rememberUpdatedState(resolveSeek)
    val currentScrollOffset by rememberUpdatedState(scrollOffset)
    val currentScrollTo by rememberUpdatedState(scrollTo)
    val scope = rememberCoroutineScope()
    val seekController = remember(scope) { ScrollbarSeekController(scope) }
    val nestedScroll = remember { NestedScrollDispatcher() }
    val colors = MaterialTheme.colorScheme
    val trackColor = colors.outlineVariant.copy(alpha = 0.34f)
    val activeTrackColor = colors.primary.copy(alpha = 0.24f)
    val idleThumbColor = if (alphabetEnabled) {
        colors.primary.copy(alpha = 0.7f)
    } else {
        colors.onSurfaceVariant.copy(alpha = 0.55f)
    }
    val draggedThumbColor = colors.primary.copy(alpha = 0.9f)
    var dragging by remember { mutableStateOf(false) }
    var activeLabel by remember { mutableStateOf<String?>(null) }
    var indicatorVisible by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var releasedFrom by remember { mutableStateOf<Float?>(null) }
    val settle = remember { Animatable(0f) }

    LaunchedEffect(releasedFrom) {
        if (releasedFrom == null) return@LaunchedEffect
        settle.snapTo(0f)
        settle.animateTo(1f, tween(ScrollbarSettleDuration))
        releasedFrom = null
    }

    val indicatorProgress = {
        val from = releasedFrom
        when {
            dragging -> dragFraction
            from != null -> lerp(from, progress(), settle.value)
            else -> progress()
        }
    }

    LaunchedEffect(canScroll, isScrolling, dragging) {
        if (!canScroll) {
            indicatorVisible = false
            return@LaunchedEffect
        }
        if (isScrolling || dragging) {
            indicatorVisible = true
        } else {
            delay(ScrollbarIdleTimeout)
            indicatorVisible = false
        }
    }

    val visibilityAlpha by animateFloatAsState(
        targetValue = if (canScroll && indicatorVisible) 1f else 0f,
        animationSpec = tween(160),
        label = "list_scrollbar_alpha"
    )

    if (!canScroll) return

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(contentPadding)
            .testTag("fast_scrollbar")
            .nestedScroll(ReportOnlyConnection, nestedScroll)
            .width(ScrollbarOverlayWidth)
            .padding(
                start = ScrollbarEdgePadding,
                end = ScrollbarEdgePadding
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(ScrollbarTouchWidth)
                .clearAndSetSemantics { }
                .drawBehind {
                    val alpha = visibilityAlpha
                    if (alpha <= 0f) return@drawBehind

                    val trackWidth = ScrollbarTrackWidth.toPx()
                    val left = if (rtl) 0f else size.width - trackWidth
                    val corner = CornerRadius(trackWidth / 2f)
                    drawRoundRect(
                        color = if (alphabetEnabled) activeTrackColor else trackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(trackWidth, size.height),
                        cornerRadius = corner,
                        alpha = alpha
                    )

                    val thumbHeight = if (alphabetEnabled) {
                        AlphabetThumbHeight.toPx()
                    } else {
                        (size.height * thumbFraction())
                            .coerceIn(ScrollbarMinThumbHeight.toPx().coerceAtMost(size.height), size.height)
                    }
                    val released = releasedFrom
                    drawRoundRect(
                        color = when {
                            dragging -> draggedThumbColor
                            released != null -> lerpColor(draggedThumbColor, idleThumbColor, settle.value)
                            else -> idleThumbColor
                        },
                        topLeft = Offset(left, ((size.height - thumbHeight) * indicatorProgress()).coerceAtLeast(0f)),
                        size = Size(trackWidth, thumbHeight),
                        cornerRadius = corner,
                        alpha = alpha
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragStarted = false
                        val trackHeight = size.height.toFloat().coerceAtLeast(1f)
                        var seekedTarget: ScrollbarSeekTarget? = null
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val fraction = (change.position.y / trackHeight).coerceIn(0f, 1f)
                            if (!dragStarted) {
                                if (abs(change.position.y - down.position.y) < viewConfiguration.touchSlop) continue
                                dragStarted = true
                                dragFraction = fraction
                                dragging = true
                            }
                            dragFraction = fraction
                            val target = currentResolveSeek(fraction)
                            activeLabel = target.label
                            if (target != seekedTarget) {
                                seekedTarget = target
                                seekController.seek {
                                    val before = currentScrollOffset()
                                    currentScrollTo(target)
                                    nestedScroll.dispatchPostScroll(
                                        consumed = Offset(0f, before - currentScrollOffset()),
                                        available = Offset.Zero,
                                        source = NestedScrollSource.UserInput
                                    )
                                }
                            }
                            change.consume()
                        }
                        if (dragStarted) {
                            releasedFrom = dragFraction
                            dragging = false
                            activeLabel = null
                            seekController.afterSeek {
                                nestedScroll.dispatchPostFling(consumed = Velocity.Zero, available = Velocity.Zero)
                            }
                        }
                    }
                }
        )

        val label = activeLabel
        if (dragging && label != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(AlphabetBubbleWidth)
                    .height(AlphabetBubbleHeight),
                shape = RoundedCornerShape(CornerRadiusLarge),
                color = colors.primary,
                contentColor = colors.onPrimary,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private data class ScrollbarMetrics(
    val progress: Float,
    val thumbFraction: Float,
    val scrollOffset: Float = 0f,
    val averageItemSize: Float = 0f,
    val maxScrollOffset: Float = 1f
)

private fun LazyListState.scrollbarMetrics(sizeCache: MutableMap<Int, Int>): ScrollbarMetrics {
    val info = layoutInfo
    val visibleItems = info.visibleItemsInfo
    val totalItems = info.totalItemsCount
    if (visibleItems.isEmpty() || totalItems <= 0) {
        return ScrollbarMetrics(progress = 0f, thumbFraction = 1f)
    }
    if (sizeCache.keys.any { it >= totalItems }) {
        sizeCache.keys.retainAll { it < totalItems }
    }
    visibleItems.forEach { sizeCache[it.index] = it.size }

    val viewportSize = (info.viewportEndOffset - info.viewportStartOffset).coerceAtLeast(1)
    val averageItemSize = sizeCache.values.sum().toFloat() / sizeCache.size
    var scrollOffset = firstVisibleItemScrollOffset.toFloat()
    for (index in 0 until firstVisibleItemIndex) {
        scrollOffset += sizeCache[index]?.toFloat() ?: averageItemSize
    }
    var estimatedItemsSize = 0f
    for (index in 0 until totalItems) {
        estimatedItemsSize += sizeCache[index]?.toFloat() ?: averageItemSize
    }
    val contentSize = (estimatedItemsSize + info.beforeContentPadding + info.afterContentPadding)
        .coerceAtLeast(viewportSize.toFloat())
    val maxScrollOffset = (contentSize - viewportSize).coerceAtLeast(1f)

    return ScrollbarMetrics(
        progress = (scrollOffset / maxScrollOffset).coerceIn(0f, 1f),
        thumbFraction = (viewportSize / contentSize).coerceIn(MinThumbFraction, 1f),
        scrollOffset = scrollOffset,
        averageItemSize = averageItemSize,
        maxScrollOffset = maxScrollOffset
    )
}

private fun LazyListState.seekTarget(
    fraction: Float,
    sizeCache: Map<Int, Int>,
    metrics: ScrollbarMetrics
): ScrollbarSeekTarget {
    val totalItems = layoutInfo.totalItemsCount
    if (totalItems <= 0) return ScrollbarSeekTarget(index = 0)

    val lastIndex = totalItems - 1
    if (fraction >= 1f) {
        val lastItemSize = sizeCache[lastIndex]?.toFloat() ?: metrics.averageItemSize
        return ScrollbarSeekTarget(
            index = lastIndex,
            offset = (lastItemSize + layoutInfo.afterContentPadding).roundToInt().coerceAtLeast(0)
        )
    }

    var remaining = fraction * metrics.maxScrollOffset
    for (index in 0 until lastIndex) {
        val itemSize = sizeCache[index]?.toFloat() ?: metrics.averageItemSize
        if (itemSize <= 0f || remaining < itemSize) {
            return ScrollbarSeekTarget(index = index, offset = remaining.roundToInt().coerceAtLeast(0))
        }
        remaining -= itemSize
    }
    return ScrollbarSeekTarget(index = lastIndex, offset = remaining.roundToInt().coerceAtLeast(0))
}

private fun String.scrollLabel(): String {
    val first = trim().firstOrNull { !it.isWhitespace() } ?: return "#"
    return if (first.isLetter()) first.uppercase(Locale.ROOT) else "#"
}

// Scroll To Top Button
@Composable
fun BoxScope.ScrollToTopButton(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    firstVisibleItemThreshold: Int = 3,
    extraBottomPadding: Dp = 0.dp
) {
    val awayFromTop by remember(listState, firstVisibleItemThreshold) {
        derivedStateOf { listState.firstVisibleItemIndex >= firstVisibleItemThreshold }
    }
    val scope = rememberCoroutineScope()
    val onClick: () -> Unit = {
        scope.launch {
            if (listState.firstVisibleItemIndex == 0) {
                listState.animateScrollBy(-listState.firstVisibleItemScrollOffset.toFloat())
            } else {
                listState.scrollToItem(0)
            }
        }
    }

    AnimatedVisibility(
        visible = awayFromTop,
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp + extraBottomPadding)
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(R.string.home_scroll_to_top)
            )
        }
    }
}
