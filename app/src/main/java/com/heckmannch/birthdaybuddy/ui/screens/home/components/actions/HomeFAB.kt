package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions

/**
 * Multifunktionaler Floating Action Button für den Homescreen.
 * Morpht zwischen "Kontakt hinzufügen" und "Nach oben scrollen" basierend auf dem Scroll-Zustand.
 */
@Composable
fun HomeFAB(
    showScrollUp: Boolean,
    actions: HomeActions,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Smoother Farbübergang zwischen den Zuständen
    val containerColor by animateColorAsState(
        targetValue = if (showScrollUp) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.primaryContainer,
        label = "FAB Container Color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (showScrollUp) MaterialTheme.colorScheme.onSecondaryContainer
        else MaterialTheme.colorScheme.onPrimaryContainer,
        label = "FAB Content Color"
    )

    FloatingActionButton(
        onClick = if (showScrollUp) onScrollToTop else actions.onAddContact,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        AnimatedContent(
            targetState = showScrollUp,
            transitionSpec = {
                val spec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                (fadeIn(spec) + scaleIn(spec)).togetherWith(fadeOut(spec) + scaleOut(spec))
            },
            label = "FAB Icon"
        ) { targetShowScrollUp ->
            if (targetShowScrollUp) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.home_scroll_to_top)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_add_contact)
                )
            }
        }
    }
}