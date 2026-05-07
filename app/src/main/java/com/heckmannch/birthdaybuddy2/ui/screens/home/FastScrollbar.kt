package com.heckmannch.birthdaybuddy2.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy2.viewmodel.ContactUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FastScrollbar(
    viewModel: BirthdayViewModel,
    listState: LazyListState,
    contacts: List<ContactUiModel>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    // Synchronisierung des Drag-Status mit dem ViewModel
    LaunchedEffect(isDragging) {
        viewModel.setFastScrolling(isDragging)
    }
    
    // Bubble visibility with delay for smoother transition
    var showBubble by remember { mutableStateOf(false) }
    LaunchedEffect(isDragging, listState.isScrollInProgress) {
        showBubble = if (isDragging || listState.isScrollInProgress) {
            true
        } else {
            delay(1000)
            false
        }
    }

    BoxWithConstraints(modifier = modifier.width(150.dp)) {
        val totalItems = contacts.size
        val viewHeight = maxHeight
        val thumbHeight = 48.dp
        val trackHeight = viewHeight - thumbHeight
        val trackHeightPx = with(density) { trackHeight.toPx() }
        
        val currentTotalItems by rememberUpdatedState(totalItems)
        val currentTrackHeightPx by rememberUpdatedState(trackHeightPx)

        val canScroll by remember(totalItems) {
            derivedStateOf {
                val visibleItems = listState.layoutInfo.visibleItemsInfo.size
                totalItems > visibleItems
            }
        }

        val currentMonth by remember(contacts, isDragging, dragOffsetPx) {
            derivedStateOf {
                val index = if (isDragging) {
                    val percent = (dragOffsetPx / trackHeightPx).coerceIn(0f, 1f)
                    (percent * (totalItems - 1)).toInt()
                } else {
                    listState.firstVisibleItemIndex
                }
                contacts.getOrNull(index)?.monthName ?: ""
            }
        }

        if (canScroll) {
            val thumbOffset by remember(totalItems, isDragging, dragOffsetPx) {
                derivedStateOf {
                    if (isDragging) {
                        with(density) { dragOffsetPx.toDp() }.coerceIn(0.dp, trackHeight)
                    } else {
                        val layoutInfo = listState.layoutInfo
                        val visibleItemsCount = layoutInfo.visibleItemsInfo.size
                        if (visibleItemsCount == 0) return@derivedStateOf 0.dp
                        
                        val maxScrollIndex = (totalItems - visibleItemsCount).coerceAtLeast(1)
                        val scrollPercent = (listState.firstVisibleItemIndex.toFloat() / maxScrollIndex).coerceIn(0f, 1f)
                        trackHeight * scrollPercent
                    }
                }
            }

            val thumbWidth by animateDpAsState(
                targetValue = if (isDragging) 12.dp else 6.dp,
                label = "Thumb Width",
            )

            val thumbAlpha by animateFloatAsState(
                targetValue = if (isDragging || listState.isScrollInProgress) 1f else 0.4f,
                label = "Thumb Alpha",
            )

            // Bubble
            AnimatedVisibility(
                visible = showBubble,
                enter = fadeIn() + slideInHorizontally { it / 2 },
                exit = fadeOut() + slideOutHorizontally { it / 2 },
                modifier = Modifier
                    .offset { IntOffset(0, (thumbOffset.toPx() - 4.dp.toPx()).toInt()) }
                    .align(Alignment.TopStart),
            ) {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 4.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 6.dp,
                    modifier = Modifier.padding(end = 16.dp),
                ) {
                    Text(
                        text = currentMonth,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }

            // Thumb
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, thumbOffset.toPx().toInt()) }
                    .align(Alignment.TopEnd)
                    .width(48.dp)
                    .height(thumbHeight)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { 
                                isDragging = true 
                                dragOffsetPx = with(density) { thumbOffset.toPx() }
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                        ) { change, dragAmount ->
                            dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(0f, currentTrackHeightPx)
                            val newScrollPercent = if (currentTrackHeightPx > 0) dragOffsetPx / currentTrackHeightPx else 0f
                            val targetIndex = (newScrollPercent * (currentTotalItems - 1)).toInt()
                                .coerceIn(0, currentTotalItems - 1)
                            scope.launch { listState.scrollToItem(targetIndex) }
                            change.consume()
                        }
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .width(thumbWidth)
                        .height(if (isDragging) 32.dp else 24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = thumbAlpha)),
                )
            }
        }
    }
}
