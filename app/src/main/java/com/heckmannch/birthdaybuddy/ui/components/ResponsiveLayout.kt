package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        WindowWidthSizeClass.Medium -> Modifier.widthIn(max = 600.dp).fillMaxWidth()
        WindowWidthSizeClass.Expanded -> Modifier.widthIn(max = 840.dp).fillMaxWidth()
        else -> Modifier.fillMaxWidth()
    }

    Box(
        modifier = modifier.fillMaxSize(),
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
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    // Hier könnten wir später eine NavigationRail für Tablets hinzufügen.
    // Für jetzt konzentrieren wir uns auf die Zentrierung und Insets.
    androidx.compose.material3.Scaffold(
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        content = { paddingValues ->
            AdaptiveContentContainer(
                windowWidthSizeClass = windowWidthSizeClass,
                modifier = Modifier.padding(paddingValues)
            ) {
                content(PaddingValues(0.dp)) // Padding wurde bereits im Container angewendet
            }
        }
    )
}
