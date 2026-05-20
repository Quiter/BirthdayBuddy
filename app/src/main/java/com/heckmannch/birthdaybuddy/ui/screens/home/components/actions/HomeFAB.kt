package com.heckmannch.birthdaybuddy.ui.screens.home.components.actions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

@Composable
fun HomeFAB(
    showScrollUp: Boolean,
    onAddContact: () -> Unit,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = if (showScrollUp) onScrollToTop else onAddContact,
        modifier = modifier,
        containerColor = if (showScrollUp) MaterialTheme.colorScheme.secondaryContainer 
                        else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (showScrollUp) MaterialTheme.colorScheme.onSecondaryContainer 
                      else MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        AnimatedContent(
            targetState = showScrollUp,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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

@Preview
@Composable
fun HomeFABPreview() {
    BirthdayBuddyTheme {
        HomeFAB(
            showScrollUp = false,
            onAddContact = {},
            onScrollToTop = {}
        )
    }
}
