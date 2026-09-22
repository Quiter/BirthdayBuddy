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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions

/**
 * Visual display states of [HomeFAB].
 */
enum class HomeFabMode {
    ADD,
    SCROLL_TO_TOP,
    EDIT,
}

/**
 * Multifunktionaler Floating Action Button für den Homescreen.
 * Morpht zwischen "Kontakt hinzufügen", "Nach oben scrollen" und "Kontakt bearbeiten" basierend auf dem Scroll- und Detail-Zustand.
 *
 * @param showScrollUp Zeigt an, ob die Liste gescrollt ist und der FAB nach oben scrollen soll.
 * @param actions Gebündelte [HomeActions]-Callbacks für Aktionen.
 * @param onScrollToTop Callback zum Zurückscrollen der Liste nach ganz oben.
 * @param modifier Der auf den [FloatingActionButton] anzuwendende [Modifier].
 * @param selectedContact Der aktuell im Detail-Paneel geöffnete Kontakt (sofern vorhanden).
 */
@Composable
fun HomeFAB(
    showScrollUp: Boolean,
    actions: HomeActions,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
    selectedContact: ContactUiModel? = null,
) {
    val currentMode = when {
        selectedContact != null -> HomeFabMode.EDIT
        showScrollUp -> HomeFabMode.SCROLL_TO_TOP
        else -> HomeFabMode.ADD
    }

    // Smoother Farbübergang zwischen den Zuständen
    val containerColor by animateColorAsState(
        targetValue = if (currentMode == HomeFabMode.SCROLL_TO_TOP) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        label = "FAB Container Color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (currentMode == HomeFabMode.SCROLL_TO_TOP) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "FAB Content Color"
    )

    FloatingActionButton(
        onClick = when (currentMode) {
            HomeFabMode.EDIT -> {
                {
                    if (selectedContact != null) {
                        actions.onEditContact(selectedContact.contactId, selectedContact.lookupKey)
                    }
                }
            }
            HomeFabMode.SCROLL_TO_TOP -> onScrollToTop
            HomeFabMode.ADD -> actions.onAddContact
        },
        modifier = modifier.testTag("home_fab"),
        containerColor = containerColor,
        contentColor = contentColor,
    ) {
        AnimatedContent(
            targetState = currentMode,
            transitionSpec = {
                val spec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
                (fadeIn(spec) + scaleIn(spec)).togetherWith(fadeOut(spec) + scaleOut(spec))
            },
            label = "FAB Icon"
        ) { mode ->
            when (mode) {
                HomeFabMode.EDIT -> {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.home_edit_contact)
                    )
                }
                HomeFabMode.SCROLL_TO_TOP -> {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.home_scroll_to_top)
                    )
                }
                HomeFabMode.ADD -> {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.home_add_contact)
                    )
                }
            }
        }
    }
}