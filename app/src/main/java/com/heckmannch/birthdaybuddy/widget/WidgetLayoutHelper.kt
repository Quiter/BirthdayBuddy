package com.heckmannch.birthdaybuddy.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.WidgetItemMinHeight

/**
 * Result data class holding the computed layout metrics for the widget item list.
 *
 * @param T The contact or item type.
 * @property displayContacts The subset of contacts that fit into the available widget height.
 * @property dynamicBlockHeight The distributed height allocated to each contact item block.
 */
data class WidgetLayoutResult<T>(
    val displayContacts: List<T>,
    val dynamicBlockHeight: Dp,
)

/**
 * Lightweight, stateless utility to calculate widget layout dimensions and determine
 * the subset of contacts to display based on available height and spacing constraints.
 */
object WidgetLayoutHelper {

    const val MAX_ITEMS_DEFAULT: Int = 10
    const val MIN_ITEMS_DEFAULT: Int = 1

    /**
     * Computes the available height for content after subtracting vertical padding.
     *
     * @param totalHeight The total available height of the widget container in Dp.
     * @param verticalPadding Total vertical padding (top + bottom) applied to the container.
     * @return The remaining height clamped to at least 0.dp.
     */
    fun calculateAvailableHeight(
        totalHeight: Dp,
        verticalPadding: Dp = SpacingExtraSmall * 2,
    ): Dp {
        return (totalHeight - verticalPadding).value.coerceAtLeast(0f).dp
    }

    /**
     * Calculates the maximum capacity of items that can fit into the available height,
     * constrained between [MIN_ITEMS_DEFAULT] and [maxItems].
     *
     * @param availableHeight The height available for item blocks.
     * @param minItemHeight The minimum height required per item block.
     * @param maxItems The maximum number of items allowed to display.
     * @return The item capacity (number of items that can fit).
     */
    fun calculateCapacity(
        availableHeight: Dp,
        minItemHeight: Dp = WidgetItemMinHeight,
        maxItems: Int = MAX_ITEMS_DEFAULT,
    ): Int {
        val minHeightVal = minItemHeight.value
        return if (minHeightVal > 0f) {
            (availableHeight.value / minHeightVal).toInt().coerceIn(MIN_ITEMS_DEFAULT, maxItems)
        } else {
            maxItems
        }
    }

    /**
     * Calculates the subset of contacts to display and the dynamic block height for each item,
     * evenly distributing the available height among the displayed contacts.
     *
     * @param totalHeight The total widget height in Dp.
     * @param contacts The list of contacts to take from.
     * @param verticalPadding Total vertical padding (top + bottom) applied to the outer container.
     * @param minItemHeight The minimum height allowed per contact item block.
     * @param maxItems The maximum number of items allowed to display.
     * @return A [WidgetLayoutResult] containing the contacts subset and dynamic block height.
     */
    fun <T> calculateLayout(
        totalHeight: Dp,
        contacts: List<T>,
        verticalPadding: Dp = SpacingExtraSmall * 2,
        minItemHeight: Dp = WidgetItemMinHeight,
        maxItems: Int = MAX_ITEMS_DEFAULT,
    ): WidgetLayoutResult<T> {
        val availableHeight = calculateAvailableHeight(totalHeight, verticalPadding)
        val capacity = calculateCapacity(availableHeight, minItemHeight, maxItems)

        val displayContacts = contacts.take(capacity)
        val count = displayContacts.size

        val dynamicBlockHeight = if (count > 0) {
            (availableHeight.value / count).dp
        } else {
            minItemHeight
        }

        return WidgetLayoutResult(
            displayContacts = displayContacts,
            dynamicBlockHeight = dynamicBlockHeight,
        )
    }

    /**
     * Overload for convenience when top and bottom spacings are provided separately.
     *
     * @param totalHeight The total widget height in Dp.
     * @param contacts The list of contacts to take from.
     * @param topPadding Top padding applied to the outer container.
     * @param bottomPadding Bottom padding applied to the outer container.
     * @param minItemHeight The minimum height allowed per contact item block.
     * @param maxItems The maximum number of items allowed to display.
     * @return A [WidgetLayoutResult] containing the contacts subset and dynamic block height.
     */
    fun <T> calculateLayout(
        totalHeight: Dp,
        contacts: List<T>,
        topPadding: Dp,
        bottomPadding: Dp,
        minItemHeight: Dp = WidgetItemMinHeight,
        maxItems: Int = MAX_ITEMS_DEFAULT,
    ): WidgetLayoutResult<T> = calculateLayout(
        totalHeight = totalHeight,
        contacts = contacts,
        verticalPadding = topPadding + bottomPadding,
        minItemHeight = minItemHeight,
        maxItems = maxItems,
    )
}
