package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy2.viewmodel.ContactUiModel

@Composable
fun BirthdayList(
    contacts: List<ContactUiModel>,
    swipeHintShown: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    onRequestPermission: () -> Unit,
    onSetSwipeHintShown: () -> Unit,
    onUpdateGiftIdeas: (String, String) -> Unit,
    onOpenContact: (String, String) -> Unit,
    onIgnoreContact: (String) -> Unit,
) {
    val context = LocalContext.current
    val hasPermission by remember {
        derivedStateOf {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (contacts.isEmpty()) {
        EmptyListState(hasPermission = hasPermission, onRequestPermission = onRequestPermission)
    } else {
        var expandedContactId by remember { mutableStateOf<String?>(null) }
        
        val onExpand = remember {
            { id: String -> expandedContactId = id }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            itemsIndexed(
                items = contacts,
                key = { _, it -> it.id },
                contentType = { _, _ -> "birthdayItem" },
            ) { index, contact ->
                val isExpanded = expandedContactId == contact.id
                val isFirstItem = index == 0
                
                BirthdayItem(
                    contact = contact,
                    showHint = !swipeHintShown && isFirstItem,
                    isExpanded = isExpanded,
                    onExpand = { onExpand(contact.id) },
                    onSetSwipeHintShown = onSetSwipeHintShown,
                    onUpdateGiftIdeas = onUpdateGiftIdeas,
                    onOpenContact = onOpenContact,
                    onIgnoreContact = onIgnoreContact
                )
            }
        }
    }
}

@Composable
private fun EmptyListState(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!hasPermission) {
            Text(
                text = "Um deine Geburtstage zu sehen, benötigt BirthdayBuddy Zugriff auf deine Kontakte.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequestPermission) {
                Text("Berechtigung erteilen")
            }
        } else {
            Text(
                text = "Keine Geburtstage gefunden. Synchronisiere deine Kontakte in den Einstellungen oder füge neue hinzu.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
