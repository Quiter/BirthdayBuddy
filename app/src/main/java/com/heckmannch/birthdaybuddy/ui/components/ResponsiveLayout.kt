package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import com.heckmannch.birthdaybuddy.ui.theme.AlphaContainerMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.MaxWidthExpanded
import com.heckmannch.birthdaybuddy.ui.theme.MaxWidthMedium

// --- Extensions for clean breakpoint checks ---

val WindowSizeClass.isWidthCompact: Boolean
    get() = !isWidthAtLeastBreakpoint(600)

val WindowSizeClass.isWidthMedium: Boolean
    get() = isWidthAtLeastBreakpoint(600) && !isWidthAtLeastBreakpoint(840)

val WindowSizeClass.isWidthExpanded: Boolean
    get() = isWidthAtLeastBreakpoint(840)

val WindowSizeClass.isHeightCompact: Boolean
    get() = !isHeightAtLeastBreakpoint(480)

val WindowSizeClass.isHeightMedium: Boolean
    get() = isHeightAtLeastBreakpoint(480) && !isHeightAtLeastBreakpoint(900)

val WindowSizeClass.isHeightExpanded: Boolean
    get() = isHeightAtLeastBreakpoint(900)

/**
 * CompositionLocal zur Bereitstellung der aktuellen Fenstergrößenklasse.
 * Verhindert Parameter-Drilling in tieferen UI-Hierarchien.
 */
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    WindowSizeClass(360, 640)
}

/**
 * CompositionLocal zur Bereitstellung der aktuellen WindowAdaptiveInfo.
 * Verhindert redundante Berechnungen und Parameter-Drilling.
 */
val LocalWindowAdaptiveInfo = staticCompositionLocalOf<WindowAdaptiveInfo> {
    error("No WindowAdaptiveInfo provided")
}

/**
 * Zentriert den Inhalt auf breiten Bildschirmen (Tablets, Chromebooks),
 * um zu verhindern, dass die UI unschön in die Breite gezogen wird.
 * Berücksichtigt Display-Aussparungen (Notches).
 */
@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
    includeDisplayCutout: Boolean = true,
    content: @Composable () -> Unit
) {
    val maxWidth = Modifier
        .then(
            when {
                windowSizeClass.isWidthExpanded -> Modifier.widthIn(max = MaxWidthExpanded)
                windowSizeClass.isWidthMedium -> Modifier.widthIn(max = MaxWidthMedium)
                else -> Modifier
            }
        )
        .fillMaxWidth()
        .then(
            if (includeDisplayCutout) {
                val layoutDirection = LocalLayoutDirection.current
                val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
                Modifier.padding(
                    start = cutoutPadding.calculateStartPadding(layoutDirection),
                    end = cutoutPadding.calculateEndPadding(layoutDirection)
                )
            } else {
                Modifier
            }
        ) // Notch-Schutz

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = maxWidth) {
            content()
        }
    }
}

/**
 * Ein Basis-Gerüst für alle Screens, das Adaptive Design unterstützt.
 * Optimiert für Edge-to-Edge (Android 15+).
 */
@Composable
fun AppResponsiveScaffold(
    modifier: Modifier = Modifier,
    windowSizeClass: WindowSizeClass = LocalWindowSizeClass.current,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentColor: Color = contentColorFor(if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.background else containerColor),
    contentWindowInsets: WindowInsets = WindowInsets.statusBars.union(WindowInsets.navigationBars).union(WindowInsets.displayCutout),
    consumePadding: Boolean = true,
    useAdaptiveWidth: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = { paddingValues ->
            // Die Zentrierung und das Padding für Top/BottomBar werden hier gebündelt.
            val contentModifier = Modifier.fillMaxSize()

            if (useAdaptiveWidth) {
                AdaptiveContentContainer(
                    windowSizeClass = windowSizeClass,
                    modifier = contentModifier
                ) {
                    content(if (consumePadding) paddingValues else PaddingValues(0.dp))
                }
            } else {
                Box(modifier = contentModifier) {
                    content(if (consumePadding) paddingValues else PaddingValues(0.dp))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.PHONE, name = "Phone (Compact)")
@Composable
private fun ResponsiveScaffoldPhonePreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(360, 640)
        ) {
            AppResponsiveScaffold(
                windowSizeClass = WindowSizeClass(360, 640),
                topBar = {
                    TopAppBar(title = { Text("Phone Layout") })
                }
            ) {
                PreviewContent("Compact Content")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.TABLET, name = "Tablet (Medium)")
@Composable
private fun ResponsiveScaffoldTabletPreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(600, 640)
        ) {
            AppResponsiveScaffold(
                windowSizeClass = WindowSizeClass(600, 640),
                topBar = {
                    TopAppBar(title = { Text("Tablet Layout") })
                }
            ) {
                PreviewContent("Medium Content (Centered)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.DESKTOP, name = "Desktop (Expanded Centered)")
@Composable
private fun ResponsiveScaffoldDesktopPreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(840, 640)
        ) {
            AppResponsiveScaffold(
                windowSizeClass = WindowSizeClass(840, 640),
                topBar = {
                    TopAppBar(title = { Text("Desktop Layout (Centered)") })
                }
            ) {
                PreviewContent("Expanded Content (Max 840dp)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.DESKTOP, name = "Desktop (Expanded Full Width)")
@Composable
private fun ResponsiveScaffoldDesktopFullWidthPreview() {
    BirthdayBuddyTheme {
        CompositionLocalProvider(
            LocalWindowSizeClass provides WindowSizeClass(840, 640)
        ) {
            AppResponsiveScaffold(
                windowSizeClass = WindowSizeClass(840, 640),
                useAdaptiveWidth = false,
                topBar = {
                    TopAppBar(title = { Text("Desktop Layout (Full Width)") })
                }
            ) {
                PreviewContent("Expanded Content (Full Width - Bleed)")
            }
        }
    }
}

@Composable
private fun PreviewContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AlphaContainerMedium))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
