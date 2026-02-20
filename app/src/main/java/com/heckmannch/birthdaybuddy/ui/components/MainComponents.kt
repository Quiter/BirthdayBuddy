package com.heckmannch.birthdaybuddy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.model.BirthdayContact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = { },
                expanded = false,
                onExpandedChange = { },
                placeholder = { 
                    Text(
                        stringResource(R.string.main_search_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ) 
                },
                leadingIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            Icons.Default.Menu, 
                            contentDescription = "Menü öffnen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Suche löschen")
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        },
        expanded = false,
        onExpandedChange = { },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = SearchBarDefaults.inputFieldShape,
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) { }
}

@Composable
fun MainDrawerContent(
    availableLabels: Set<String>,
    selectedLabels: Set<String>,
    hiddenDrawerLabels: Set<String>,
    onLabelToggle: (String, Boolean) -> Unit,
    onSettingsClick: () -> Unit
) {
    val allLabel = stringResource(R.string.label_all)
    val favoritesLabel = stringResource(R.string.label_favorites)
    
    val sortedLabels = availableLabels
        .filterNot { hiddenDrawerLabels.contains(it) }
        .sortedWith(compareBy<String> {
            when (it) {
                "My Contacts" -> 0
                "Starred in Android" -> 1
                else -> 2
            }
        }.thenBy { it })

    ModalDrawerSheet {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    "Birthday Buddy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
                )
                
                Text(
                    stringResource(R.string.drawer_labels_header),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
            }

            if (sortedLabels.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.drawer_no_labels),
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(sortedLabels) { label ->
                val isChecked = selectedLabels.contains(label)
                val (displayText, icon) = when(label) {
                    "My Contacts" -> allLabel to if (isChecked) Icons.Default.People else Icons.Outlined.People
                    "Starred in Android" -> favoritesLabel to if (isChecked) Icons.Default.Star else Icons.Outlined.Star
                    else -> label to if (isChecked) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Outlined.Label
                }

                NavigationDrawerItem(
                    label = { 
                        Text(
                            displayText, 
                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal 
                        ) 
                    },
                    selected = isChecked,
                    onClick = { onLabelToggle(label, isChecked) },
                    icon = { Icon(icon, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemPadding),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp))
                
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_settings)) },
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemPadding)
                )
            }
        }
    }
}

private val NavigationDrawerItemPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)

@Composable
fun BirthdayList(
    contacts: List<BirthdayContact>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(
            items = contacts,
            key = { "${it.id}_${it.name}" }
        ) { contact ->
            BirthdayItem(
                contact = contact,
                modifier = Modifier.animateItem()
            )
        }
    }
}
