package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCard
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal object ScrollbarDefaults {
    val BarWidth = 150.dp
    val ThumbSize = 48.dp
    const val BUBBLE_DELAY = 500L
    val ThumbWidth = 26.dp
    val ThumbHeight = 44.dp
    val BubbleOffsetY = 4.dp
    val BubbleCornerLarge = 24.dp
    val BubbleCornerSmall = 4.dp
    val BubbleElevation = 8.dp
}

/**
 * Representation of a vertical scroll section (e.g. Month or Letter).
 */
data class ScrollSection(
    val label: String,
    val startIndex: Int,
    val count: Int
)

/**
 * FastScrollbar displays a custom, draggable scroll bar overlay on a list.
 *
 * ### File Structure
 * This composable is split across two files in the same package:
 * - `FastScrollbar.kt` (this file): public API, [ScrollSection] data class, all state
 *   management, geometry calculations, drag-gesture handling and thumb UI.
 * - `ScrollbarBubble.kt`: the isolated animated label bubble ([ScrollbarBubble]), extracted
 *   for readability. It is `internal` so it remains package-private.
 *
 * ### CRITICAL INSTRUCTIONS FOR AI CODING AGENTS (LLMs):
 * 1. **Core Purpose**: Enables fast vertical scrolling and visual tracking in lists using
 *    alphabetical or monthly groupings.
 * 2. **Dynamic Bubble Labeling**:
 *    - The bubble displays either the month (e.g. "January") if the list is sorted
 *      chronologically, or the first letter of the name (e.g. "A") if sorted alphabetically.
 *    - This is controlled by the caller-provided `getLabel` lambda mapper.
 * 3. **Section-Based Proportional Mapping (Drag only)**:
 *    - To prevent large sections from hogging all visual track space, the scrollbar track is
 *      partitioned equally among unique sections (represented by [ScrollSection]) ONLY when
 *      translating a thumb drag position to a list scroll position.
 *    - Each section (e.g. month or letter) occupies equal vertical height on the track
 *      regardless of the number of contacts in it.
 * 4. **Stable Linear Model for Thumb Position — List → Thumb (IMPORTANT)**:
 *    - The thumb position when the user scrolls the list manually MUST use a purely linear
 *      model based on `expectedItemHeights` prefix sums.
 *    - Formula: `scrollPercent = scrolledPx / maxScrollPx`
 *    - `scrolledPx = cumulativeHeights[firstVisibleIndex] + firstVisibleScrollOffset`
 *    - `maxScrollPx = totalHeight - viewportHeight + afterContentPadding`
 *    - The `afterContentPadding` (from [LazyListState.layoutInfo]) is CRITICAL: the
 *      LazyColumn's bottom contentPadding (`80.dp + navBarPadding`) extends the scroll range
 *      beyond what items alone contribute. Omitting it causes `maxScrollPx` to be too small
 *      (or negative for short filtered lists), making the thumb jump to the bottom.
 *    - Do NOT use section-based calculations for this forward mapping (List → Thumb).
 *      Doing so causes a "rubber ruler" effect where the thumb jumps back on manual scroll.
 *    - The section-based mapping is used ONLY for the inverse direction (Thumb Drag → List).
 * 5. **Fractional requestScrollToItem Gestures**:
 *    - Scrolling during drags is initiated with `listState.requestScrollToItem(index, offsetPx)`.
 *    - It calculates the fractional position (index and pixel offset) continuously.
 *    - Do NOT spawn asynchronous coroutine jobs (`scope.launch { listState.scrollToItem() }`)
 *      on drag events, as it creates race conditions and stuttering.
 * 6. **Visibility Rules**:
 *    - The scrollbar is only active/needed when the list contains at least 3 scrollable items
 *      off-screen (`totalItemsCount > visibleItemsCount + 2`).
 *    - It becomes visible ONLY after the user has manually scrolled the list (`hasUserScrolled`).
 *    - **Pull-To-Refresh Exclusion**: Ignore list drags when the list is at the very top
 *      (index 0, offset 0) to avoid showing the scrollbar during refresh gestures.
 *    - **Top-of-List Reset**: Reset `hasUserScrolled` to `false` (and hide the scrollbar)
 *      as soon as the list is scrolled completely back to the top (index 0, offset 0).
 *      This ensures UI elements at the very top of the LazyColumn (e.g. [LabelFilterBar]) are
 *      fully visible and not obscured by the scrollbar.
 *    - **Tab / Filter Switches**: Automatically reset `hasUserScrolled` to `false` AND
 *      `dragOffsetPx` to `0f` when `contacts` change, so programmatic list resets do not
 *      trigger visibility and the thumb always starts at the correct (top) position.
 * 7. **LabelFilterBar / Header Items (IMPORTANT)**:
 *    - The [LabelFilterBar] is rendered as the **first item** inside the [BirthdayList]
 *      [LazyColumn] (not as a separate sticky overlay). This means:
 *      a) It scrolls away when the user scrolls down, and reappears when scrolling back up.
 *      b) The `headerCount` parameter MUST be set to 1 when `availableLabels` is non-empty,
 *         plus 1 more if a couple-suggestion banner is shown. The caller (HomeContent) is
 *         responsible for computing and passing the correct value.
 *      c) The [expectedItemHeights] array allocates `headerCount` slots of 56 dp each before
 *         the contact items (80 dp each), so the linear height model stays accurate.
 *      d) Do NOT move [LabelFilterBar] back to a top-of-screen sticky overlay — that would
 *         break the [headerCount] accounting and cause the scrollbar thumb to jump.
 */
@Composable
fun FastScrollbar(
    listState: LazyListState,
    contacts: List<ContactUiModel>,
    getLabel: (ContactUiModel) -> String,
    modifier: Modifier = Modifier,
    headerCount: Int = 0,
    onSetFastScrolling: (Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val totalItems = contacts.size

    // Dynamic visibility threshold: list must be longer than the viewport + 2 items
    val isNeeded by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleCount = layoutInfo.visibleItemsInfo.size
            if (visibleCount == 0) false
            else layoutInfo.totalItemsCount > visibleCount + 2
        }
    }

    if (!isNeeded || totalItems <= 0) return

    // Group contacts into scrollable sections based on label (Month or Letter)
    val sections = remember(contacts, getLabel, headerCount) {
        val list = mutableListOf<ScrollSection>()
        if (contacts.isEmpty()) return@remember list

        var currentLabel = getLabel(contacts.first())
        var startIndex = headerCount
        var count = 1

        for (i in 1 until contacts.size) {
            val label = getLabel(contacts[i])
            if (label == currentLabel) {
                count++
            } else {
                list.add(ScrollSection(currentLabel, startIndex, count))
                currentLabel = label
                startIndex = headerCount + i
                count = 1
            }
        }
        list.add(ScrollSection(currentLabel, startIndex, count))
        list
    }

    // Stable expected height model: header ~56dp, contact ~80dp
    val expectedItemHeights = remember(contacts.size, headerCount) {
        val heights = FloatArray(contacts.size + headerCount)
        val dp = density.density
        if (headerCount > 0) {
            repeat(headerCount) { i -> heights[i] = 56f * dp }
        }
        for (i in headerCount until heights.size) heights[i] = 80f * dp
        heights
    }

    // Prefix-sum array for O(1) cumulative height lookup.
    // cumulativeHeights[i] = total height of items 0..i-1
    val cumulativeHeights = remember(expectedItemHeights) {
        FloatArray(expectedItemHeights.size + 1).also { cum ->
            cum[0] = 0f
            for (i in expectedItemHeights.indices) {
                cum[i + 1] = cum[i] + expectedItemHeights[i]
            }
        }
    }

    val totalHeight = remember(cumulativeHeights) { cumulativeHeights.last() }

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    // Visibility: Show only after the user has manually scrolled the list (ignoring pull-to-refresh)
    var hasUserScrolled by remember { mutableStateOf(false) }
    val isListDragged by listState.interactionSource.collectIsDraggedAsState()

    val firstVisibleIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstVisibleOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

    LaunchedEffect(isListDragged, firstVisibleIndex.value, firstVisibleOffset.value) {
        if (isListDragged && (firstVisibleIndex.value > 0 || firstVisibleOffset.value > 0)) {
            hasUserScrolled = true
        }
        // Hide the scrollbar (and reset the user-scrolled flag) as soon as the list
        // is back at the very top. This makes the LabelFilterBar and other top items
        // fully visible again, and restores the pull-to-refresh exclusion guard.
        if (!listState.isScrollInProgress && firstVisibleIndex.value == 0 && firstVisibleOffset.value == 0) {
            hasUserScrolled = false
        }
    }

    // Reset visibility state on new search, tab switch, or list updates.
    // Also reset dragOffsetPx so the thumb always starts at the top after a programmatic
    // list reset — without this, the thumb would remain at the old position the first time
    // the user drags again (because onDragStart re-anchors to the touch point, not the thumb).
    LaunchedEffect(contacts) {
        hasUserScrolled = false
        dragOffsetPx = 0f
    }

    // NOTE: hasUserScrolled = true for thumb drags is set directly in onDragStart
    // (no LaunchedEffect needed; the gesture callback is synchronous).

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, isDragging, hasUserScrolled) {
        if (hasUserScrolled && (listState.isScrollInProgress || isDragging)) {
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

    // Bubble visibility logic
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
        val viewHeight = maxHeight
        val trackHeight = viewHeight - ScrollbarDefaults.ThumbHeight
        val trackHeightPx = with(density) { trackHeight.toPx() }
        val thumbHeightPx = with(density) { ScrollbarDefaults.ThumbHeight.toPx() }

        // ─────────────────────────────────────────────────────────────────────
        // FORWARD MAPPING: List position → Thumb position (LINEAR)
        //
        // scrollPercent = scrolledPx / maxScrollPx
        //   scrolledPx  = cumulativeHeights[firstIndex] + firstVisibleScrollOffset
        //   maxScrollPx = totalHeight - viewportHeight + afterContentPadding
        //
        // afterContentPadding (= LazyColumn bottom contentPadding in px) is CRITICAL:
        // it extends the scrollable range beyond the item heights. Without it,
        // maxScrollPx is too small (or negative for short filtered lists), causing
        // scrollPercent to be coerced to 1.0 and the thumb to jump to the bottom.
        //
        // This is purely based on the stable expectedItemHeights model and is
        // therefore independent of visible items or section boundaries. No jumping.
        // ─────────────────────────────────────────────────────────────────────
        val scrollPercent by remember(totalHeight, cumulativeHeights) {
            derivedStateOf {
                val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                if (viewportHeight <= 0f) return@derivedStateOf 0f

                val afterPad = listState.layoutInfo.afterContentPadding.toFloat()
                val maxScrollPx = totalHeight - viewportHeight + afterPad
                if (maxScrollPx <= 0f) return@derivedStateOf 0f

                val firstIndex = listState.firstVisibleItemIndex
                    .coerceIn(0, (expectedItemHeights.size - 1).coerceAtLeast(0))
                val firstOffset = listState.firstVisibleItemScrollOffset.toFloat()

                val scrolledPx = cumulativeHeights[firstIndex] + firstOffset
                (scrolledPx / maxScrollPx).coerceIn(0f, 1f)
            }
        }

        // Thumb offset: use dragOffsetPx during drag, derive from scrollPercent otherwise
        val thumbOffset by remember(trackHeight) {
            derivedStateOf {
                if (isDragging) {
                    with(density) { dragOffsetPx.toDp() }.coerceIn(0.dp, trackHeight)
                } else {
                    trackHeight * scrollPercent
                }
            }
        }

        // Keep dragOffsetPx in sync while the user scrolls manually,
        // so that a subsequent drag always starts from the correct thumb position.
        LaunchedEffect(thumbOffset, isDragging) {
            if (!isDragging) {
                dragOffsetPx = with(density) { thumbOffset.toPx() }
            }
        }

        // ─────────────────────────────────────────────────────────────────────
        // INVERSE MAPPING: Thumb drag position → List position (SECTION-BASED)
        //
        // The track is divided into equal slices — one per section — so that
        // every section (month / letter) is reachable with the same drag distance
        // regardless of how many contacts it contains.
        // ─────────────────────────────────────────────────────────────────────
        fun updateScrollPosition(y: Float) {
            if (sections.isEmpty()) return
            val percent = (y / trackHeightPx).coerceIn(0f, 1f)

            // Special case: thumb at the very top → scroll to index 0 so that all
            // header items (e.g. LabelFilterBar at index 0) become visible again.
            // Without this, the section-based mapping would resolve to
            // section.startIndex = headerCount (≥ 1) and skip the header entirely.
            if (percent == 0f) {
                listState.requestScrollToItem(0, 0)
                return
            }

            val sectionFloat = percent * sections.size.toFloat()
            val sectionIndex = sectionFloat.toInt().coerceIn(0, sections.size - 1)
            val sectionProgress = (sectionFloat - sectionIndex).coerceIn(0f, 1f)
            val section = sections[sectionIndex]

            val targetFloatIndex =
                section.startIndex.toFloat() + sectionProgress * section.count.toFloat()
            val targetIndex = targetFloatIndex.toInt()
                .coerceIn(0, (totalItems + headerCount - 1).coerceAtLeast(0))
            val fractional = (targetFloatIndex - targetIndex).coerceIn(0f, 1f)
            val itemSizePx = expectedItemHeights.getOrNull(targetIndex) ?: (80f * density.density)
            val offsetPx = (fractional * itemSizePx).toInt()

            listState.requestScrollToItem(targetIndex, offsetPx)
        }

        // Active section label for the bubble — derived from thumb position via section mapping
        val currentLabel by remember(sections) {
            derivedStateOf {
                if (sections.isEmpty()) return@derivedStateOf ""
                val percent = if (trackHeight > 0.dp) {
                    (thumbOffset / trackHeight).coerceIn(0f, 1f)
                } else 0f
                val sectionIndex = (percent * sections.size).toInt()
                    .coerceIn(0, sections.size - 1)
                sections.getOrNull(sectionIndex)?.label ?: ""
            }
        }

        // Haptic feedback when crossing a section boundary during drag
        var lastLabelForHaptic by remember { mutableStateOf("") }
        LaunchedEffect(currentLabel) {
            if (isDragging && currentLabel.isNotEmpty() && currentLabel != lastLabelForHaptic) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                lastLabelForHaptic = currentLabel
            }
        }

        ScrollbarBubble(
            visible = showBubble && currentLabel.isNotEmpty(),
            label = currentLabel,
            thumbOffset = { thumbOffset }
        )

        // Interactive touch track
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(ScrollbarDefaults.ThumbSize)
                .fillMaxHeight()
                .navigationBarsPadding()
                .graphicsLayer { alpha = animatedVisibilityAlpha }
                .pointerInput(trackHeightPx, sections, headerCount) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            hasUserScrolled = true
                            onSetFastScrolling(true)
                            dragOffsetPx =
                                (offset.y - thumbHeightPx / 2f).coerceIn(0f, trackHeightPx)
                            updateScrollPosition(dragOffsetPx)
                        },
                        onDragEnd = { isDragging = false; onSetFastScrolling(false) },
                        onDragCancel = { isDragging = false; onSetFastScrolling(false) },
                    ) { change, dragAmount ->
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, trackHeightPx)
                        updateScrollPosition(dragOffsetPx)
                        change.consume()
                    }
                }
        ) {
            // Visual scrollbar thumb
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp)
                    .width(ScrollbarDefaults.ThumbWidth)
                    .height(ScrollbarDefaults.ThumbHeight)
                    .graphicsLayer { translationY = thumbOffset.toPx() }
                    .semantics { contentDescription = "Scrollbar" },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = AlphaOnboardingCard),
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

