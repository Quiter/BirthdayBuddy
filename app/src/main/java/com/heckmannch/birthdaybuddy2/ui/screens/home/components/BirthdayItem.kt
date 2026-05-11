package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy2.R
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayGold
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy2.ui.theme.KidColors
import com.heckmannch.birthdaybuddy2.viewmodel.ContactUiModel
import com.heckmannch.birthdaybuddy2.viewmodel.GiftIdea
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DragValue { Closed, Open }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BirthdayItem(
    contact: ContactUiModel,
    showHint: Boolean,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onSetSwipeHintShown: () -> Unit,
    onUpdateGiftIdeas: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val showGiftDialogState = remember { mutableStateOf(false) }

    // Maße und Anker-Berechnungen (Optimiert durch remember)
    val buttonWidth = 50.dp
    val buttonSpacing = 4.dp
    val gapToContact = 4.dp
    
    val anchors = remember(density) {
        val totalButtonsWidth = (buttonWidth * 2) + buttonSpacing
        val openAnchorPx = with(density) { (totalButtonsWidth + gapToContact).toPx() }
        DraggableAnchors {
            DragValue.Closed at 0f
            DragValue.Open at -openAnchorPx
        }
    }

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Closed,
        )
    }
    
    // Anker bei Dichte-Änderung aktualisieren
    SideEffect {
        draggableState.updateAnchors(anchors)
    }

    LaunchedEffect(showHint) {
        if (showHint) {
            delay(1000)
            draggableState.animateTo(DragValue.Open)
            delay(1000)
            draggableState.animateTo(DragValue.Closed)
            onSetSwipeHintShown()
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded && (draggableState.currentValue == DragValue.Open)) {
            draggableState.animateTo(targetValue = DragValue.Closed)
        }
    }

    LaunchedEffect(draggableState.targetValue) {
        if (draggableState.targetValue == DragValue.Open) {
            onExpand()
        }
    }

    if (showGiftDialogState.value) {
        GiftIdeaDialog(
            initialIdeas = contact.giftIdeas,
            onDismiss = { showGiftDialogState.value = false },
        ) { ideas ->
            onUpdateGiftIdeas(contact.lookupKey, GiftIdea.toString(ideas))
            showGiftDialogState.value = false
            scope.launch { draggableState.animateTo(targetValue = DragValue.Closed) }
        }
    }

    val borderStroke = remember(contact.isToday, contact.nextAge) {
        if (contact.isToday) {
            val age = contact.nextAge
            when {
                // Alle durch 10 teilbaren (10, 20, 30...) sind Gold
                (age != null && age % 10 == 0) -> BorderStroke(2.dp, BirthdayGold)
                // Kinder von 0 bis 9 sind Bunt
                (age != null) && (age in 0..9) -> BorderStroke(2.dp, Brush.linearGradient(KidColors))
                // Alle anderen (inkl. ohne Jahr) sind Silber
                else -> BorderStroke(2.dp, BirthdaySilver)
            }
        } else null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .width((buttonWidth * 2) + buttonSpacing),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SwipeActionButton(
                icon = Icons.Default.Edit,
                color = Color(0xFFFFB300),
                contentDescription = stringResource(R.string.item_action_gifts),
            ) { showGiftDialogState.value = true }
            SwipeActionButton(
                icon = Icons.Default.Person,
                color = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(R.string.item_action_contact),
            ) {
                onOpenContact(contact.contactId, contact.lookupKey)
                scope.launch { draggableState.animateTo(DragValue.Closed) }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer { 
                    translationX = if (draggableState.offset.isNaN()) 0f else draggableState.offset
                }
                .anchoredDraggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        state = draggableState,
                        positionalThreshold = { distance: Float -> distance * 0.5f },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                        )
                    )
                ),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = borderStroke
        ) {
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
                        if (contact.hasGiftIdeas) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.item_gift_ideas_hint),
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB300)
                            )
                        }
                    }
                },
                supportingContent = {
                    Text(
                        text = contact.dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingContent = {
                    ContactImage(contact = contact)
                },
                trailingContent = {
                    BirthdayStatus(contact = contact)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: ImageVector,
    color: Color,
    contentDescription: String?,
    width: androidx.compose.ui.unit.Dp = 50.dp,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.15f),
        contentColor = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun ContactImage(contact: ContactUiModel) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (contact.imageUri != null) {
            AsyncImage(
                model = contact.imageUri,
                contentDescription = stringResource(R.string.item_image_desc, contact.fullName),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(text = contact.initials, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun BirthdayStatus(contact: ContactUiModel) {
    Column(horizontalAlignment = Alignment.End) {
        contact.nextAgeText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = if (contact.isToday) stringResource(R.string.item_today) else pluralStringResource(R.plurals.item_days_left, contact.daysUntilNext.toInt(), contact.daysUntilNext.toInt()),
            style = MaterialTheme.typography.labelSmall,
            color = if (contact.isToday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
