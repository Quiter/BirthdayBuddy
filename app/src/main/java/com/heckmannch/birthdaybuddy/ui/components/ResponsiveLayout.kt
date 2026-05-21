package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Zentriert den Inhalt auf breiten Bildschirmen (Tablets, Chromebooks),
 * um zu verhindern, dass die UI unschön in die Breite gezogen wird.
 */
@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    windowWidthSizeClass: WindowWidthSizeClass,
    content: @Composable () -> Unit
) {
    val maxWidth = when (windowWidthSizeClass) {
        WindowWidthSizeClass.Compact -> Modifier.fillMaxWidth()
        WindowWidthSizeClass.Medium -> Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
        WindowWidthSizeClass.Expanded -> Modifier
            .widthIn(max = 840.dp)
            .fillMaxWidth()
        else -> Modifier.fillMaxWidth()
    }

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
    windowWidthSizeClass: WindowWidthSizeClass,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = Color.Unspecified,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        content = { paddingValues ->
            // Die Zentrierung und das Padding für Top/BottomBar werden hier gebündelt.
            AdaptiveContentContainer(
                windowWidthSizeClass = windowWidthSizeClass,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content(PaddingValues(0.dp))
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
