package com.heckmannch.birthdaybuddy.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                            contentDescription = stringResource(R.string.drawer_menu_open),
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
                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.drawer_search_clear))
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
            .statusBarsPadding()
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
    val context = LocalContext.current
    val allLabel = stringResource(R.string.label_all)
    val favoritesLabel = stringResource(R.string.label_favorites)
    val sysAll = "My Contacts"
    val sysStarred = "Starred in Android"
    
    var labelsExpanded by rememberSaveable { mutableStateOf(true) }

    val kidColors = listOf(Color(0xFF4285F4), Color(0xFFF06292), Color(0xFFFFB300), Color(0xFF4CAF50))
    val headerBrush = Brush.linearGradient(kidColors)

    val sortedLabels = availableLabels
        .filterNot { hiddenDrawerLabels.contains(it) }
        .sortedWith(compareBy<String> {
            when (it) {
                sysAll -> 0
                sysStarred -> 1
                else -> 2
            }
        }.thenBy { it })

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp + 48.dp)
                .background(headerBrush)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 16.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.drawer_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 38.sp,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings, 
                        contentDescription = stringResource(R.string.drawer_settings), 
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp)
        ) {
            item {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_calendar)) },
                    selected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse("content://com.android.calendar/time/"))
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemPadding)
                )
                
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.drawer_contacts)) },
                    selected = false,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                        context.startActivity(intent)
                    },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemPadding)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            }

            item {
                ListItem(
                    headlineContent = { 
                        Text(
                            stringResource(R.string.drawer_labels_header).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    trailingContent = {
                        Icon(
                            imageVector = if (labelsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { labelsExpanded = !labelsExpanded }
                )
            }

            if (labelsExpanded) {
                items(sortedLabels) { label ->
                    val isChecked = selectedLabels.contains(label)
                    val (displayText, icon) = when(label) {
                        sysAll -> allLabel to if (isChecked) Icons.Default.People else Icons.Default.PeopleOutline
                        sysStarred -> favoritesLabel to if (isChecked) Icons.Default.Star else Icons.Default.StarOutline
                        else -> label to if (isChecked) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Outlined.Label
                    }

                    NavigationDrawerItem(
                        label = { 
                            Text(
                                text = displayText,
                                fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal 
                            ) 
                        },
                        selected = isChecked,
                        onClick = { onLabelToggle(label, isChecked) },
                        icon = { Icon(icon, contentDescription = null) },
                        modifier = Modifier.padding(NavigationDrawerItemPadding),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
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
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp 
        )
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

/**
 * Eine ansprechende Komponente für leere Zustände (Empty States).
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
