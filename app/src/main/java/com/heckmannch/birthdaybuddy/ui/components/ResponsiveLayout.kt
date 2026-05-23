package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * CompositionLocal zur Bereitstellung der aktuellen Fensterbreitenklasse.
 * Verhindert Parameter-Drilling in tieferen UI-Hierarchien.
 */
val LocalWindowWidthSizeClass = compositionLocalOf<WindowWidthSizeClass> {
    error("Keine WindowWidthSizeClass bereitgestellt. Bitte umschließe deine App mit einer CompositionLocalProvider.")
}

/**
 * Zentriert den Inhalt auf breiten Bildschirmen (Tablets, Chromebooks),
 * um zu verhindern, dass die UI unschön in die Breite gezogen wird.
 */
@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    windowWidthSizeClass: WindowWidthSizeClass = LocalWindowWidthSizeClass.current,
    content: @Composable () -> Unit
) {
    val maxWidth = Modifier
        .then(
            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Medium -> Modifier.widthIn(max = 600.dp)
                WindowWidthSizeClass.Expanded -> Modifier.widthIn(max = 840.dp)
                else -> Modifier
            }
        )
        .fillMaxWidth()

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
 */
@Composable
fun AppResponsiveScaffold(
    modifier: Modifier = Modifier,
    windowWidthSizeClass: WindowWidthSizeClass = LocalWindowWidthSizeClass.current,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    contentColor: Color = contentColorFor(if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.background else containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    consumePadding: Boolean = true,
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
            AdaptiveContentContainer(
                windowWidthSizeClass = windowWidthSizeClass,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (consumePadding) Modifier.padding(paddingValues) else Modifier
                    )
            ) {
                content(if (consumePadding) PaddingValues(0.dp) else paddingValues)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.PHONE, name = "Phone (Compact)")
@Composable
private fun ResponsiveScaffoldPhonePreview() {
    BirthdayBuddyTheme {
        AppResponsiveScaffold(
            windowWidthSizeClass = WindowWidthSizeClass.Compact,
            topBar = {
                TopAppBar(title = { Text("Phone Layout") })
            }
        ) {
            PreviewContent("Compact Content")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.TABLET, name = "Tablet (Medium)")
@Composable
private fun ResponsiveScaffoldTabletPreview() {
    BirthdayBuddyTheme {
        AppResponsiveScaffold(
            windowWidthSizeClass = WindowWidthSizeClass.Medium,
            topBar = {
                TopAppBar(title = { Text("Tablet Layout") })
            }
        ) {
            PreviewContent("Medium Content (Centered)")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = Devices.DESKTOP, name = "Desktop (Expanded)")
@Composable
private fun ResponsiveScaffoldDesktopPreview() {
    BirthdayBuddyTheme {
        AppResponsiveScaffold(
            windowWidthSizeClass = WindowWidthSizeClass.Expanded,
            topBar = {
                TopAppBar(title = { Text("Desktop Layout") })
            }
        ) {
            PreviewContent("Expanded Content (Max 840dp)")
        }
    }
}

@Composable
private fun PreviewContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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

