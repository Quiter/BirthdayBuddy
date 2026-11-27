package com.heckmannch.birthdaybuddy.ui.components.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.heckmannch.birthdaybuddy.ui.theme.CornerRadiusSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SettingsSegmentBlockGap
import com.heckmannch.birthdaybuddy.ui.theme.SettingsSegmentGap
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingTiny

/**
 * Saveable UI state for the expandable settings search bar.
 */
@Stable
class SettingsSearchState(
    initialVisible: Boolean = false,
    initialQuery: String = "",
) {
    /** Whether the search field is currently expanded in the header. */
    var visible by mutableStateOf(initialVisible)
        private set

    /** Current raw search query entered by the user. */
    var query by mutableStateOf(initialQuery)

    /** Expands the search bar. */
    fun open() {
        visible = true
    }

    /** Collapses the search bar and clears the query. */
    fun collapse() {
        visible = false
        query = ""
    }

    /** Toggles the search bar between expanded and collapsed states. */
    fun toggle() {
        if (visible) collapse() else open()
    }

    /** Clears only the query while keeping the search bar open. */
    fun clear() {
        query = ""
    }

    companion object {
        val Saver: Saver<SettingsSearchState, Any> = listSaver(
            save = { listOf(it.visible, it.query) },
            restore = { saved ->
                SettingsSearchState(
                    initialVisible = saved[0] as Boolean,
                    initialQuery = saved[1] as String,
                )
            },
        )
    }
}

/**
 * Remembers a [SettingsSearchState] across configuration changes and process recreation.
 */
@Composable
fun rememberSettingsSearchState(
    initialVisible: Boolean = false,
    initialQuery: String = "",
): SettingsSearchState = rememberSaveable(saver = SettingsSearchState.Saver) {
    SettingsSearchState(initialVisible = initialVisible, initialQuery = initialQuery)
}

/**
 * Intercepts system back presses to collapse an active [SettingsSearchState] before navigating away.
 */
@Composable
fun SettingsSearchBackHandler(state: SettingsSearchState) {
    BackHandler(enabled = state.visible) {
        state.collapse()
    }
}

/**
 * Tracks live search matches across an entire settings page (including subpage previews)
 * so empty-state messages and folded sections update reactively.
 */
@Stable
class SettingsSearch(initialQuery: String = "") {
    /** Trimmed query string used for case-insensitive matching. */
    var query by mutableStateOf(initialQuery)

    private val registrations = mutableStateMapOf<Any, Boolean>()

    /** True when the user has typed a non-blank query. */
    val isActive: Boolean
        get() = query.isNotBlank()

    /** Number of searchable items currently matching [query]. */
    val shown: Int by derivedStateOf {
        registrations.values.count { it }
    }

    /**
     * Evaluates whether any of [texts] matches the current [query].
     * Always returns true when [isActive] is false.
     */
    fun matches(vararg texts: String?): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return texts.any { candidate ->
            !candidate.isNullOrBlank() && candidate.contains(needle, ignoreCase = true)
        }
    }

    internal fun report(token: Any, matched: Boolean) {
        if (registrations[token] != matched) {
            registrations[token] = matched
        }
    }

    internal fun remove(token: Any) {
        registrations.remove(token)
    }
}

/**
 * Section-scoped match tracker provided by [SettingsSection] so a section hides its heading
 * whenever every [SearchableSetting] inside it is filtered out.
 */
@Stable
class SearchMatches {
    private val registrations = mutableStateMapOf<Any, Boolean>()

    /** True if at least one child setting inside this section matches the active search. */
    val hasAny: Boolean by derivedStateOf {
        registrations.values.any { it }
    }

    /** True if at least one [SearchableSetting] has registered inside this section. */
    val hasRegistrations: Boolean by derivedStateOf {
        registrations.isNotEmpty()
    }

    internal fun report(token: Any, matched: Boolean) {
        if (registrations[token] != matched) {
            registrations[token] = matched
        }
    }

    internal fun remove(token: Any) {
        registrations.remove(token)
    }
}

/** Active page-level [SettingsSearch] provided to settings trees. */
val LocalSettingsSearch = compositionLocalOf<SettingsSearch?> { null }

/** Section-level [SearchMatches] provided by [SettingsSection]. */
val LocalSearchMatches = compositionLocalOf<SearchMatches?> { null }

/**
 * Creates and remembers a [SettingsSearch] synchronized with [query].
 */
@Composable
fun rememberSettingsSearch(query: String): SettingsSearch {
    val search = remember { SettingsSearch(query) }
    search.query = query
    return search
}

/**
 * Wraps a settings row so it automatically participates in [LocalSettingsSearch] and
 * [LocalSearchMatches], folding smoothly out of view when filtered out.
 */
@Composable
fun SearchableSetting(
    vararg searchableTexts: String?,
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    val pageSearch = LocalSettingsSearch.current
    val sectionMatches = LocalSearchMatches.current
    val token = remember { Any() }

    val matched = visible && (pageSearch == null || pageSearch.matches(*searchableTexts))

    SideEffect {
        pageSearch?.report(token, matched)
        sectionMatches?.report(token, matched)
    }
    DisposableEffect(pageSearch, sectionMatches, token) {
        onDispose {
            pageSearch?.remove(token)
            sectionMatches?.remove(token)
        }
    }

    if (pageSearch == null) {
        if (visible) content()
    } else {
        FoldingVisibility(visible = matched) {
            content()
        }
    }
}

/**
 * Expressive section heading with optional icon, description, trailing action, or clickable
 * breadcrumb affordance (used for subpage search results).
 */
@Composable
fun SectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    onTitleClick: (() -> Unit)? = null,
    actionContent: @Composable (() -> Unit)? = null,
) {
    val headerModifier = modifier
        .fillMaxWidth()
        .then(
            if (onTitleClick != null) {
                Modifier
                    .clip(RoundedCornerShape(CornerRadiusSmall))
                    .clickable(onClick = onTitleClick)
                    .padding(horizontal = SpacingExtraSmall, vertical = SpacingExtraSmall)
            } else {
                Modifier.padding(horizontal = SpacingExtraSmall)
            }
        )
        .semantics { heading() }

    Row(
        modifier = headerModifier,
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ThemedIcon(
                icon = icon,
                size = IconSizeMedium,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SpacingTiny),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (onTitleClick != null && actionContent == null) {
            ForwardChevronIcon(size = IconSizeSmall)
        } else {
            actionContent?.invoke()
        }
    }
}

/**
 * Titled section container that automatically folds away when an active [LocalSettingsSearch]
 * filters out all [SearchableSetting] children within it.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    onTitleClick: (() -> Unit)? = null,
    actionContent: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val pageSearch = LocalSettingsSearch.current
    val sectionMatches = remember { SearchMatches() }
    val showHeading = pageSearch == null ||
        !pageSearch.isActive ||
        !sectionMatches.hasRegistrations ||
        sectionMatches.hasAny

    CompositionLocalProvider(LocalSearchMatches provides sectionMatches) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingSmall),
        ) {
            FoldingVisibility(visible = showHeading) {
                SectionHeading(
                    title = title,
                    icon = icon,
                    description = description,
                    onTitleClick = onTitleClick,
                    actionContent = actionContent,
                )
            }
            content()
        }
    }
}

/**
 * Vertical layout for settings blocks that collapses spacing over 0-height folded children,
 * preventing awkward double gaps during search filtering or conditional visibility transitions.
 */
@Composable
fun SettingsColumn(
    modifier: Modifier = Modifier,
    spacing: Dp = SettingsSegmentBlockGap,
    tightSpacing: Dp = SettingsSegmentGap,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val placeables = measurables.map { measurable ->
            val parentData = measurable.parentData as? SettingsBlockParentData
            measurable.measure(childConstraints) to (parentData?.endsBlock == true)
        }
        val visibleItems = placeables.filter { (placeable, _) -> placeable.height > 0 }
        val blockGapPx = spacing.roundToPx()
        val tightGapPx = tightSpacing.roundToPx()

        var totalHeight = 0
        val yPositions = IntArray(visibleItems.size)
        visibleItems.forEachIndexed { idx, (placeable, endsBlock) ->
            yPositions[idx] = totalHeight
            totalHeight += placeable.height
            if (idx < visibleItems.lastIndex) {
                totalHeight += if (endsBlock) blockGapPx else blockGapPx.coerceAtLeast(tightGapPx)
            }
        }

        layout(constraints.maxWidth, totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            visibleItems.forEachIndexed { idx, (placeable, _) ->
                placeable.placeRelative(0, yPositions[idx])
            }
        }
    }
}
