package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R

@Composable
fun HomeFAB(
    showScrollUp: Boolean,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
    onAddContact: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (showScrollUp) 180f else 0f,
        label = "FAB Rotation",
    )

    FloatingActionButton(
        onClick = { if (showScrollUp) onScrollToTop() else onAddContact() },
        modifier = modifier.graphicsLayer { 
            rotationZ = rotation 
        }
    ) {
        AnimatedContent(
            targetState = showScrollUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "FAB Icon Animation"
        ) { isUp ->
            Icon(
                imageVector = if (isUp) Icons.Default.KeyboardArrowDown else Icons.Default.Add,
                contentDescription = stringResource(
                    if (isUp) R.string.home_scroll_to_top else R.string.home_add_contact
                ),
            )
        }
    }
}
