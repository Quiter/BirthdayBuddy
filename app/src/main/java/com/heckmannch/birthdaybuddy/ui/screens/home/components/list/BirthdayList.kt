package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.data.local.Contact
import com.heckmannch.birthdaybuddy.ui.components.LottieIllustration
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.HomeActions
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Verwaltet die Liste der Geburtstage.
 * Kümmert sich um den Empty-State und die exklusive Expansion von Items.
 */
@Composable
fun BirthdayList(
    contacts: List<ContactUiModel>?,
    newlyAddedIdeaId: String?,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    selectedLabel: String? = null,
    searchQuery: String = "",
    actions: HomeActions,
    coupleSuggestion: Pair<Contact, Contact>? = null,
    selectedContactId: String? = null,
    onContactSelected: ((ContactUiModel) -> Unit)? = null,
    onInteraction: () -> Unit = {},
) {
    val context = LocalContext.current

    val hasPermission = remember(context) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    // WICHTIG: Wenn contacts null ist, zeigen wir einen Shimmer-Loader
    if (contacts == null) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            userScrollEnabled = false,
        ) {
            items(10) {
                BirthdayItemSkeleton()
            }
        }
        return
    }

    var expandedContactId by rememberSaveable { mutableStateOf<String?>(null) }

    val isListDragged by listState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isListDragged) {
        if (isListDragged) {
            expandedContactId = null
        }
    }

    var previousLabel by remember { mutableStateOf(selectedLabel) }
    var previousQuery by remember { mutableStateOf(searchQuery) }
    var skipPlacementAnimation by remember { mutableStateOf(false) }

    // Schließen und Animationen anpassen, wenn sich Filter oder Suche ändern
    LaunchedEffect(selectedLabel, searchQuery) {
        expandedContactId = null
        if (selectedLabel != previousLabel || searchQuery != previousQuery) {
            skipPlacementAnimation = true
            previousLabel = selectedLabel
            previousQuery = searchQuery
            delay(150.milliseconds)
            skipPlacementAnimation = false
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        if (selectedLabel == HomeViewModel.LABEL_ANNIVERSARY && coupleSuggestion != null) {
            item(key = "couple_suggestion") {
                CoupleSuggestionBanner(
                    suggestion = coupleSuggestion,
                    onLink = actions.onLinkAsCouple,
                    onIgnore = actions.onIgnoreCoupleSuggestion
                )
            }
        }
        if (contacts.isEmpty()) {
            item(key = "empty_state") {
                EmptyListState(
                    hasPermission = hasPermission,
                    onRequestPermission = actions.onRequestPermission,
                    modifier = Modifier.fillParentMaxSize()
                )
            }
        } else {
            itemsIndexed(
                items = contacts,
                key = { _, it -> it.id },
                contentType = { _, _ -> "birthdayItem" },
            ) { _, contact ->
                val isExpanded = onContactSelected == null && expandedContactId == contact.id
                val isSelected = onContactSelected != null && selectedContactId == contact.id

                BirthdayItem(
                    contact = contact,
                    isExpanded = isExpanded,
                    isSelected = isSelected,
                    newlyAddedIdeaId = newlyAddedIdeaId,
                    onExpand = {
                        onInteraction()
                        if (onContactSelected != null) {
                            onContactSelected(contact)
                        } else {
                            expandedContactId = if (isExpanded) null else contact.id
                        }
                    },
                    actions = actions,
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(durationMillis = 200),
                        fadeOutSpec = tween(durationMillis = 150),
                        placementSpec = if (skipPlacementAnimation) null else spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium,
                            visibilityThreshold = IntOffset.VisibilityThreshold
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun BirthdayItemSkeleton() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .height(72.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        MaterialTheme.shapes.medium
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun EmptyListState(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!hasPermission) {
            LottieIllustration(
                resId = R.raw.anim_contacts,
                modifier = Modifier.size(160.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (!hasPermission) {
            Text(
                text = stringResource(R.string.empty_permission_desc),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text(stringResource(R.string.empty_permission_btn))
            }
        } else {
            Text(
                text = stringResource(R.string.empty_no_birthdays),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirthdayListPreview() {
    val actions = HomeActions(
        onSearchQueryChange = {},
        onLabelSelected = {},
        onClearSearch = {},
        onNavigateToSettings = {},
        onAddContact = {},
        onRequestPermission = {},
        onAddGiftIdea = {},
        onToggleGiftIdea = { _, _, _ -> },
        onUpdateGiftIdeaText = { _, _, _ -> },
        onDeleteGiftIdea = { _, _ -> },
        onUpdateBirthday = { _, _ -> },
        onOpenContact = { _, _ -> },
        onDial = {},
        onSendSms = {},
        onOpenMessengerApp = { _, _ -> },
        onRefresh = {}
    )

    BirthdayBuddyTheme {
        BirthdayList(
            contacts = SampleData.sampleContacts,
            newlyAddedIdeaId = null,
            listState = rememberLazyListState(),
            actions = actions,
        )
    }
}

@Composable
fun CoupleSuggestionBanner(
    suggestion: Pair<Contact, Contact>,
    onLink: (String, String) -> Unit,
    onIgnore: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp, top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactImage(
                imageUri = suggestion.first.imageUri,
                fullName = suggestion.first.fullName,
                initials = suggestion.first.fullName.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .joinToString("").take(2),
                secondImageUri = suggestion.second.imageUri,
                secondInitials = suggestion.second.fullName.split(" ")
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .joinToString("").take(2),
                secondFullName = suggestion.second.fullName
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.couple_suggestion_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.couple_suggestion_msg,
                        suggestion.first.fullName,
                        suggestion.second.fullName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onLink(
                                suggestion.first.lookupKey,
                                suggestion.second.lookupKey
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.couple_suggestion_yes))
                    }
                    OutlinedButton(
                        onClick = {
                            onIgnore(
                                suggestion.first.lookupKey,
                                suggestion.second.lookupKey
                            )
                        },
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.couple_suggestion_no),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
