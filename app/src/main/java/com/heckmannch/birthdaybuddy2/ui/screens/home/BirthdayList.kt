package com.heckmannch.birthdaybuddy2.ui.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayGold
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdaySilver
import com.heckmannch.birthdaybuddy2.ui.theme.KidColors
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy2.viewmodel.ContactUiModel

@Composable
fun BirthdayList(
    viewModel: BirthdayViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    onRequestPermission: () -> Unit,
) {
    val context = LocalContext.current
    val contactsState by viewModel.contacts.collectAsState()
    
    // Nichts anzeigen, während die Daten das erste Mal geladen werden (verhindert Flackern)
    val contacts = contactsState ?: return

    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    LaunchedEffect(contacts) {
        hasPermission.value = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    if (contacts.isEmpty()) {
        Column(
            modifier = modifier
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
            
            if (!hasPermission.value) {
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
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(
                items = contacts,
                key = { it.id },
                contentType = { "birthdayItem" }
            ) { contact ->
                BirthdayItem(contact)
            }
        }
    }
}

@Composable
fun BirthdayItem(contact: ContactUiModel) {
    val context = LocalContext.current
    val borderStroke = remember(contact) {
        if (contact.isToday && (contact.nextAge != null)) {
            when {
                contact.nextAge <= 10 -> {
                    BorderStroke(
                        width = 2.dp,
                        brush = Brush.linearGradient(KidColors)
                    )
                }
                ((contact.nextAge >= 20) && ((contact.nextAge % 10) == 0)) -> {
                    BorderStroke(2.dp, BirthdayGold)
                }
                else -> {
                    BorderStroke(2.dp, BirthdaySilver)
                }
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = borderStroke
    ) {
        ListItem(
            headlineContent = {
                Text(text = contact.fullName, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                Text(
                    text = contact.dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingContent = {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            try {
                                val lookupUri = ContactsContract.Contacts.getLookupUri(
                                    contact.contactId.toLong(),
                                    contact.lookupKey
                                )
                                val intent = Intent(Intent.ACTION_VIEW, lookupUri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
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
                            Text(
                                text = contact.initials,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            },
            trailingContent = {
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
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}
