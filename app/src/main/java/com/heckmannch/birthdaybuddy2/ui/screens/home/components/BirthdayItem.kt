package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import android.content.Intent
import android.provider.ContactsContract
import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayGold
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy2.ui.theme.KidColors
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy2.viewmodel.ContactUiModel
import com.heckmannch.birthdaybuddy2.viewmodel.GiftIdea
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class DragValue { Closed, Open }

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BirthdayItem(
    contact: ContactUiModel,
    viewModel: BirthdayViewModel,
    isFirstItem: Boolean,
    showHint: Boolean,
    isExpanded: Boolean,
    onExpand: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var showGiftDialog by remember { mutableStateOf(false) }

    // AnchoredDraggable Setup
    val buttonWidth = 50.dp
    val buttonSpacing = 4.dp
    val gapToContact = 4.dp
    val totalButtonsWidth = (buttonWidth * 3) + (buttonSpacing * 2)
    val openAnchorPx = with(density) { (totalButtonsWidth + gapToContact).toPx() }

    val draggableState = remember {
        AnchoredDraggableState(
            initialValue = DragValue.Closed,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            decayAnimationSpec = exponentialDecay()
        ).apply {
            updateAnchors(
                DraggableAnchors {
                    DragValue.Closed at 0f
                    DragValue.Open at -openAnchorPx
                }
            )
        }
    }

    LaunchedEffect(showHint) {
        if (showHint) {
            delay(1000)
            draggableState.animateTo(DragValue.Open)
            delay(1000)
            draggableState.animateTo(DragValue.Closed)
            viewModel.setSwipeHintShown()
        }
    }

    LaunchedEffect(isExpanded) {
        if (!isExpanded && draggableState.currentValue == DragValue.Open) {
            draggableState.animateTo(DragValue.Closed)
        }
    }

    LaunchedEffect(draggableState.targetValue) {
        if (draggableState.targetValue == DragValue.Open) {
            onExpand()
        }
    }

    if (showGiftDialog) {
        GiftIdeaDialog(
            initialIdeas = GiftIdea.fromString(contact.giftIdeas),
            onDismiss = { showGiftDialog = false },
            onConfirm = { ideas ->
                viewModel.updateGiftIdeas(contact.lookupKey, GiftIdea.toString(ideas))
                showGiftDialog = false
                scope.launch { draggableState.animateTo(DragValue.Closed) }
            }
        )
    }

    val borderStroke = remember(contact) {
        if (contact.isToday && (contact.nextAge != null)) {
            when {
                contact.nextAge <= 10 -> BorderStroke(2.dp, Brush.linearGradient(KidColors))
                contact.nextAge >= 20 && contact.nextAge % 10 == 0 -> BorderStroke(2.dp, BirthdayGold)
                else -> BorderStroke(2.dp, BirthdaySilver)
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(IntrinsicSize.Min)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .width(totalButtonsWidth),
            horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SwipeActionButton(
                icon = Icons.Default.Edit,
                color = Color(0xFFFFB300),
                onClick = { showGiftDialog = true }
            )
            SwipeActionButton(
                icon = Icons.Default.Person,
                color = MaterialTheme.colorScheme.primary,
                onClick = {
                    try {
                        val lookupUri = ContactsContract.Contacts.getLookupUri(contact.contactId.toLong(), contact.lookupKey)
                        viewModel.getApplication<android.app.Application>().startActivity(
                            Intent(Intent.ACTION_VIEW, lookupUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                        scope.launch { draggableState.animateTo(DragValue.Closed) }
                    } catch (_: Exception) {}
                }
            )
            SwipeActionButton(
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.outline,
                onClick = {
                    viewModel.updateLabelConfig(
                        name = contact.fullName,
                        isHiddenFromFilter = true,
                        isIgnored = true,
                        isSystem = false
                    )
                    scope.launch { draggableState.animateTo(DragValue.Closed) }
                }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset { 
                    val offset = if (draggableState.offset.isNaN()) 0f else draggableState.offset
                    IntOffset(offset.roundToInt(), 0) 
                }
                .anchoredDraggable(draggableState, Orientation.Horizontal),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            border = borderStroke
        ) {
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
                        if (!contact.giftIdeas.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Geschenkideen vorhanden",
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
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
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
                contentDescription = "Kontaktbild von ${contact.fullName}",
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
            text = contact.daysLeftText,
            style = MaterialTheme.typography.labelSmall,
            color = if (contact.isToday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
