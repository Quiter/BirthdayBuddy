package com.heckmannch.birthdaybuddy.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.FloatingBarsLayout
import com.heckmannch.birthdaybuddy.ui.components.FloatingPillBar
import com.heckmannch.birthdaybuddy.ui.components.FloatingPillItem
import com.heckmannch.birthdaybuddy.ui.components.LocalBackdropHaze
import com.heckmannch.birthdaybuddy.ui.components.LocalWindowSizeClass
import com.heckmannch.birthdaybuddy.ui.components.isHeightCompact
import com.heckmannch.birthdaybuddy.ui.components.isWidthCompact
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * The main tabs BirthdayBuddy pages between:
 * - LABELS: Left page (index 0)
 * - HOME: Middle page (index 1, default start page)
 * - SETTINGS: Right page (index 2)
 */
enum class MainPage(@param:StringRes val titleRes: Int, val icon: ImageVector) {
    LABELS(R.string.nav_tab_labels, Icons.AutoMirrored.Filled.Label),
    HOME(R.string.nav_tab_home, Icons.Default.Home),
    SETTINGS(R.string.nav_tab_settings, Icons.Default.Settings),
}

/**
 * Turns the pager to [page], sliding at most one page directly to keep transitions smooth.
 */
suspend fun PagerState.turnTo(page: MainPage) {
    val distance = page.ordinal - currentPage
    if (abs(distance) > 1) scrollToPage(page.ordinal - distance.sign)
    animateScrollToPage(page.ordinal)
}

/**
 * Main pager scaffold providing horizontal swipe gestures and the floating glass pill bar.
 * Architecture matches Morphe Manager:
 * - Left: Labels
 * - Center: Home (start)
 * - Right: Settings
 */
@Composable
fun MainPagerScaffold(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    navigationCovered: () -> Boolean = { false },
    swipeEnabled: Boolean = true,
    page: @Composable (page: MainPage, bottomBarHeight: Dp) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val current = { MainPage.entries[pagerState.currentPage] }
    val select: (MainPage) -> Unit = { target ->
        scope.launch { pagerState.turnTo(target) }
    }

    // Back gesture on side pages returns to HOME
    BackHandler(enabled = current() != MainPage.HOME) {
        select(MainPage.HOME)
    }

    val items = remember {
        MainPage.entries.map { target ->
            MainNavItem(
                page = target,
                icon = target.icon,
                titleRes = target.titleRes,
            )
        }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val isLandscape = !windowSizeClass.isWidthCompact && windowSizeClass.isHeightCompact

    val pager: @Composable (Modifier, Dp) -> Unit = { pagerModifier, bottomBarHeight ->
        Box(modifier = pagerModifier) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = swipeEnabled && !navigationCovered(),
                key = { index -> MainPage.entries[index] },
            ) { index ->
                Box(modifier = Modifier.fillMaxSize()) {
                    page(MainPage.entries[index], bottomBarHeight)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                MainNavigationSidebar(
                    items = items,
                    current = current,
                    onSelect = select,
                    modifier = Modifier.statusBarsPadding(),
                )
                VerticalDivider(modifier = Modifier.statusBarsPadding().padding(vertical = 20.dp))
                pager(Modifier.weight(1f).fillMaxHeight(), 0.dp)
            }
        } else {
            val barHaze = rememberHazeState()
            FloatingBarsLayout(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    AnimatedVisibility(
                        visible = !navigationCovered(),
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        val currentPage = current()
                        CompositionLocalProvider(LocalBackdropHaze provides barHaze) {
                            FloatingPillBar(
                                items = items.map { item ->
                                    FloatingPillItem(
                                        icon = item.icon,
                                        label = stringResource(item.titleRes),
                                        onClick = { select(item.page) },
                                        selected = item.page == currentPage,
                                    )
                                },
                            )
                        }
                    }
                },
            ) { _, bottomBarHeight ->
                pager(Modifier.fillMaxSize().hazeSource(barHaze), bottomBarHeight)
            }
        }
    }
}

private class MainNavItem(
    val page: MainPage,
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
)

@Composable
private fun MainNavigationSidebar(
    items: List<MainNavItem>,
    current: () -> MainPage,
    onSelect: (MainPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(180.dp)
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        val currentPage = current()
        items.forEach { item ->
            val isSelected = item.page == currentPage
            androidx.compose.material3.NavigationRailItem(
                selected = isSelected,
                onClick = { onSelect(item.page) },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.titleRes),
                    )
                },
                label = { androidx.compose.material3.Text(stringResource(item.titleRes)) },
            )
        }
    }
}
