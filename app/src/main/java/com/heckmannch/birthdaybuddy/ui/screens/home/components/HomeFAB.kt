package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

/**
 * Multifunktionaler FAB für den Home-Screen.
 * Morphing-Animation zwischen "Kontakt hinzufügen" und "Scroll to Top".
 */
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
            transitionSpec = { 
                (fadeIn() + scaleIn()) togetherWith (fadeOut() + scaleOut()) 
            },
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

@Preview
@Composable
private fun HomeFABPreview() {
    BirthdayBuddyTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HomeFAB(showScrollUp = false, onScrollToTop = {}, onAddContact = {})
            Spacer(modifier = Modifier.height(16.dp))
            HomeFAB(showScrollUp = true, onScrollToTop = {}, onAddContact = {})
        }
    }
}
