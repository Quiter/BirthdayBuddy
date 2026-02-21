package com.heckmannch.birthdaybuddy.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val context = LocalContext.current
    val uiState by mainViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        hasPermission = perms[Manifest.permission.READ_CONTACTS] == true
        if (hasPermission) {
            mainViewModel.loadContacts()
        }
    }

    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) {
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val perms = mutableListOf(Manifest.permission.READ_CONTACTS)
            if (android.os.Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                availableLabels = uiState.availableLabels,
                selectedLabels = uiState.selectedLabels,
                hiddenDrawerLabels = uiState.hiddenDrawerLabels,
                onLabelToggle = { label, _ -> mainViewModel.toggleLabel(label) },
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToSettings()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                MainSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { mainViewModel.updateSearchQuery(it) },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            floatingActionButton = {
                if (hasPermission) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                type = ContactsContract.Contacts.CONTENT_TYPE
                            }
                            context.startActivity(intent)
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_contact))
                    }
                }
            },
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
        ) { padding ->
            val listState = rememberLazyListState()
            
            LaunchedEffect(listState.isScrollInProgress) {
                if (listState.isScrollInProgress) {
                    focusManager.clearFocus()
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (!hasPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.permission_required_desc),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }) {
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { mainViewModel.loadContacts() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.contacts.isEmpty() && !uiState.isLoading) {
                            // Empty States Logik
                            if (uiState.searchQuery.isNotEmpty()) {
                                // Fall: Suche ohne Treffer
                                EmptyState(
                                    icon = Icons.Default.SearchOff,
                                    title = stringResource(R.string.main_empty_search_title),
                                    description = stringResource(R.string.main_empty_search_desc, uiState.searchQuery)
                                )
                            } else {
                                // Fall: Generell keine Geburtstage gefunden
                                EmptyState(
                                    icon = Icons.Default.Cake,
                                    title = stringResource(R.string.main_empty_birthdays_title),
                                    description = stringResource(R.string.main_empty_birthdays_desc)
                                )
                            }
                        } else {
                            BirthdayList(
                                contacts = uiState.contacts, 
                                listState = listState
                            )
                        }
                    }
                }
            }
        }
    }
}
