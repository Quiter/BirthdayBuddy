package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate

@Composable
fun HomeFAB(
    showScrollUp: Boolean,
    onScrollToTop: () -> Unit,
    onAddContact: () -> Unit,
) {
    val rotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (showScrollUp) 180f else 0f,
        label = "FAB Rotation"
    )

    FloatingActionButton(
        onClick = { if (showScrollUp) onScrollToTop() else onAddContact() },
        modifier = Modifier.rotate(rotation)
    ) {
        AnimatedContent(
            targetState = showScrollUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FAB Icon Animation"
        ) { isUp ->
            Icon(
                imageVector = if (isUp) Icons.Default.KeyboardArrowDown else Icons.Default.Add,
                contentDescription = if (isUp) "Nach oben" else "Kontakt hinzufügen"
            )
        }
    }
}
