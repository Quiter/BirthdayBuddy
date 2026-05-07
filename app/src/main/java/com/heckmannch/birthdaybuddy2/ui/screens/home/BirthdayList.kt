package com.heckmannch.birthdaybuddy2.ui.screens.home

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
import com.heckmannch.birthdaybuddy2.ui.screens.home.components.BirthdayItem
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel

@Composable
fun BirthdayList(
    viewModel: BirthdayViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    onRequestPermission: () -> Unit,
) {
    val context = LocalContext.current
    val contactsState by viewModel.contacts.collectAsState()
    val contacts = contactsState ?: return

    val hasPermission by remember(contacts) {
        derivedStateOf {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (contacts.isEmpty()) {
        EmptyListState(hasPermission = hasPermission, onRequestPermission = onRequestPermission)
    } else {
        val swipeHintShown by viewModel.swipeHintShown.collectAsState()
        var expandedContactId by remember { mutableStateOf<String?>(null) }
        
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            itemsIndexed(
                items = contacts,
                key = { _, it -> it.id },
                contentType = { _, _ -> "birthdayItem" }
            ) { index, contact ->
                BirthdayItem(
                    contact = contact,
                    viewModel = viewModel,
                    isFirstItem = index == 0,
                    showHint = !swipeHintShown && index == 0,
                    isExpanded = expandedContactId == contact.id,
                    onExpand = { expandedContactId = contact.id }
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
