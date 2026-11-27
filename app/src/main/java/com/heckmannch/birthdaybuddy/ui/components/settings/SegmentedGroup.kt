package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.heckmannch.birthdaybuddy.ui.theme.SettingsSegmentGap

/**
 * Position descriptor for a segmented row inside a vertical list (e.g. `LazyColumn`).
 */
@Immutable
data class ListSegment(
    val index: Int,
    val count: Int,
) {
    /**
     * Resolves the connected card [CornerRadii] for this segment position.
     */
    fun toCornerRadii(): CornerRadii = ConnectedShapes.Card.inColumn(index, count)

    companion object {
        /**
         * Creates a [ListSegment] for the item at [index] in a list of size [count].
         */
        fun of(index: Int, count: Int): ListSegment = ListSegment(index = index, count = count)
    }
}

/**
 * Tracks active rows inside a [SettingsGroup] so each card knows its live segment position
 * and immediately adapts its outer/inner corners when sibling rows appear or fold away.
 */
@Stable
internal class SegmentRegistry {
    private val activeOrder = mutableStateListOf<Any>()
    private val allOrder = mutableListOf<Any>()

    fun register(token: Any, leaving: Boolean) {
        if (token !in allOrder) {
            allOrder.add(token)
        }
        if (leaving) {
            activeOrder.remove(token)
        } else if (token !in activeOrder) {
            val targetPos = allOrder.indexOf(token)
            val insertAt = activeOrder.indexOfFirst { allOrder.indexOf(it) > targetPos }
            if (insertAt == -1) activeOrder.add(token) else activeOrder.add(insertAt, token)
        }
    }

    fun unregister(token: Any) {
        activeOrder.remove(token)
        allOrder.remove(token)
    }

    fun cornersFor(token: Any): CornerRadii {
        val idx = activeOrder.indexOf(token)
        if (idx == -1) return CornerRadii.uniform(ConnectedShapes.Card.inner)
        return ConnectedShapes.Card.inColumn(idx, activeOrder.size)
    }
}

internal val LocalSegmentRegistry = compositionLocalOf<SegmentRegistry?> { null }

/**
 * Set to true by [FoldingVisibility] while its child is animating out so neighboring
 * rows inside a [SettingsGroup] immediately round their newly exposed edges.
 */
internal val LocalRowLeaving = compositionLocalOf { false }

/**
 * Vertical segmented container for related settings cards.
 * Children built with [SurfaceCard] or [SettingsItemCard] automatically connect with
 * [SettingsSegmentGap] spacing and expressive rounded outer corners.
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val registry = remember { SegmentRegistry() }
    CompositionLocalProvider(LocalSegmentRegistry provides registry) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SettingsSegmentGap),
            content = content,
        )
    }
}

/**
 * Resolves the reactive [CornerRadii] for a card inside a [SettingsGroup],
 * or falls back to [segment] / standalone outer card corners when outside a group.
 */
@Composable
internal fun rememberSegmentCorners(segment: ListSegment? = null): CornerRadii {
    if (segment != null) return segment.toCornerRadii()
    val registry = LocalSegmentRegistry.current ?: return CornerRadii.uniform(ConnectedShapes.Card.outer)
    val leaving = LocalRowLeaving.current
    val token = remember { Any() }

    SideEffect {
        registry.register(token, leaving)
    }
    DisposableEffect(registry, token) {
        onDispose { registry.unregister(token) }
    }
    return registry.cornersFor(token)
}

/**
 * Smooth vertical fold/unfold transition tailored for conditional rows inside [SettingsGroup]
 * and [SettingsColumn]. Signals [LocalRowLeaving] immediately on exit so adjacent cards
 * reshape their corners in sync with the fold animation.
 */
@Composable
fun FoldingVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val sizeSpec = remember {
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }
    val alphaSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = expandVertically(animationSpec = sizeSpec) + fadeIn(animationSpec = alphaSpec),
        exit = shrinkVertically(animationSpec = sizeSpec) + fadeOut(animationSpec = alphaSpec),
    ) {
        CompositionLocalProvider(LocalRowLeaving provides !visible) {
            content()
        }
    }
}
