/*
 * Copyright 2026 BirthdayBuddy / Morphe Architecture.
 */

package com.heckmannch.birthdaybuddy.ui.components.layout

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Room a [PinnedHeaderPage]'s content leaves for what stands around it: [top] for the header,
 * [bottom] for the bottom bar, and [horizontal] each side.
 */
@Immutable
data class PageInsets(val top: Dp, val bottom: Dp, val horizontal: Dp) {
    fun scrollbarPadding(bottom: Dp = this.bottom): PaddingValues =
        PaddingValues(top = top + 12.dp, bottom = bottom + 12.dp)
}

/**
 * How far [this] runs on past its start.
 */
internal fun ScrollableState.hiddenAtStart(): Float = when (this) {
    is ScrollState -> value.toFloat()
    is LazyListState -> if (firstVisibleItemIndex > 0) Float.MAX_VALUE else firstVisibleItemScrollOffset.toFloat()
    else -> if (canScrollBackward) Float.MAX_VALUE else 0f
}

/**
 * A page whose [header] stays in place while [content] scrolls on under it.
 */
@Composable
fun PinnedHeaderPage(
    header: @Composable ColumnScope.() -> Unit,
    scrollState: ScrollableState,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    onHeaderHeightChange: (Dp) -> Unit = {},
    content: @Composable BoxScope.(insets: PageInsets) -> Unit
) {
    val gutter = 16.dp
    FloatingBarsLayout(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) { bottomBar() }
        },
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                HeaderBand(
                    glass = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    scrolledPast = { scrollState.hiddenAtStart() }
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(start = gutter, top = 16.dp, end = gutter, bottom = 12.dp),
                    content = header
                )
            }
        }
    ) { headerHeight, bottomBarHeight ->
        SideEffect { onHeaderHeightChange(headerHeight) }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val horizontal = 16.dp
            val insets = remember(headerHeight, bottomBarHeight, horizontal) {
                PageInsets(top = headerHeight, bottom = bottomBarHeight, horizontal = horizontal)
            }
            content(insets)
        }
    }
}

@Composable
fun BoxScope.HeaderBand(
    glass: Color,
    shape: Shape,
    scrolledPast: () -> Float
) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer { alpha = (scrolledPast() / 12f).coerceIn(0f, 1f) }
            .background(glass, shape)
    )
}

@Composable
fun FloatingBarsLayout(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    wrapContentHeight: Boolean = false,
    content: @Composable BoxScope.(topHeight: Dp, bottomHeight: Dp) -> Unit
) {
    val currentContent = rememberUpdatedState(content)
    val topSlot: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) { topBar() }
    }
    val bottomSlot: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) { bottomBar() }
    }

    SubcomposeLayout(modifier) { constraints ->
        val barConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val topPlaceables = subcompose(0, topSlot).map { it.measure(barConstraints) }
        val bottomPlaceables = subcompose(1, bottomSlot).map { it.measure(barConstraints) }
        val topHeight = topPlaceables.maxOfOrNull { it.height } ?: 0
        val bottomHeight = bottomPlaceables.maxOfOrNull { it.height } ?: 0
        val contentSlot = @Composable {
            Box(modifier = Modifier.fillMaxSize()) {
                currentContent.value.invoke(this, topHeight.toDp(), bottomHeight.toDp())
            }
        }
        val contentPlaceables = subcompose(2, contentSlot)
            .map { it.measure(if (wrapContentHeight) constraints.copy(minHeight = 0) else constraints) }
        val height = if (wrapContentHeight) {
            (contentPlaceables.maxOfOrNull { it.height } ?: 0).coerceAtLeast(topHeight + bottomHeight)
        } else {
            constraints.maxHeight
        }

        layout(constraints.maxWidth, height) {
            contentPlaceables.forEach { it.place(0, 0) }
            topPlaceables.forEach { it.place((constraints.maxWidth - it.width) / 2, 0) }
            bottomPlaceables.forEach {
                it.place((constraints.maxWidth - it.width) / 2, height - it.height)
            }
        }
    }
}
