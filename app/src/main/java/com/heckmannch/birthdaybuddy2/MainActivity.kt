package com.heckmannch.birthdaybuddy2

import android.os.Bundle
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddy2Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirthdayBuddy2Theme {
                val viewModel: BirthdayViewModel = viewModel()
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val listState = rememberLazyListState()
                
                val searchQuery by viewModel.searchQuery.collectAsState()
                var animatedPlaceholder by remember { mutableStateOf("BirthdayBuddy") }

                // Gmail-Style Animation: Erst Name, dann Placeholder
                LaunchedEffect(Unit) {
                    delay(2000) // 2 Sekunden BirthdayBuddy zeigen
                    animatedPlaceholder = "Kontakt suchen"
                }
                
                // FAB Logik: Zeige Pfeil oben, wenn wir nicht am Anfang der Liste sind
                val showScrollUp by remember {
                    derivedStateOf {
                        listState.firstVisibleItemIndex > 0
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.syncContacts()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp)
                                .statusBarsPadding()
                        ) {
                            DockedSearchBar(
                                modifier = Modifier.fillMaxWidth(),
                                expanded = false,
                                onExpandedChange = { },
                                inputField = {
                                    SearchBarDefaults.InputField(
                                        query = searchQuery,
                                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                                        onSearch = { },
                                        expanded = false,
                                        onExpandedChange = { },
                                        placeholder = {
                                            AnimatedContent(
                                                targetState = animatedPlaceholder,
                                                transitionSpec = {
                                                    fadeIn() togetherWith fadeOut()
                                                },
                                                label = "Placeholder Animation"
                                            ) { text ->
                                                Text(text)
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                                    Icon(Icons.Default.Close, contentDescription = "Suche löschen")
                                                }
                                            } else {
                                                IconButton(onClick = {
                                                    when (PackageManager.PERMISSION_GRANTED) {
                                                        ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.READ_CONTACTS
                                                        ) -> {
                                                            viewModel.syncContacts()
                                                        }
                                                        else -> {
                                                            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Refresh, contentDescription = "Synchronisieren")
                                                }
                                            }
                                        }
                                    )
                                }
                            ) { }
                        }
                    },
                    floatingActionButton = {
                        val rotation by animateFloatAsState(
                            targetValue = if (showScrollUp) 180f else 0f,
                            label = "FAB Rotation"
                        )

                        FloatingActionButton(
                            onClick = {
                                if (showScrollUp) {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                } else {
                                    val intent = Intent(Intent.ACTION_INSERT).apply {
                                        type = ContactsContract.Contacts.CONTENT_TYPE
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.rotate(rotation)
                        ) {
                            // Wir nutzen Crossfade oder AnimatedContent für den Icon-Wechsel
                            AnimatedContent(
                                targetState = showScrollUp,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "FAB Icon Change"
                            ) { isUp ->
                                if (isUp) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Nach oben")
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Kontakt hinzufügen")
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    BirthdayList(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding),
                        listState = listState
                    )
                }
            }
        }
    }
}

@Composable
fun BirthdayList(
    viewModel: BirthdayViewModel, 
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val contacts by viewModel.contacts.collectAsState()

    if (contacts.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Noch keine Geburtstage. Nutze den Refresh-Button oben.")
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp) // Platz für FAB
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
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
                    if (contact.nextAgeText != null) {
                        Text(
                            text = contact.nextAgeText,
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
