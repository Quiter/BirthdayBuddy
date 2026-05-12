package com.heckmannch.birthdaybuddy2.ui.screens.settings.labels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.heckmannch.birthdaybuddy2.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel
import com.heckmannch.birthdaybuddy2.viewmodel.LabelManagementModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSettingsScreen(
    viewModel: BirthdayViewModel,
    onNavigateBack: () -> Unit,
) {
    val labels by viewModel.labelManagementList.collectAsStateWithLifecycle()

    LabelSettingsContent(
        labels = labels,
        onNavigateBack = onNavigateBack,
    ) { name, hidden, ignored, isSystem ->
        viewModel.updateLabelConfig(name, hidden, ignored, isSystem)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelSettingsContent(
    labels: List<LabelManagementModel>,
    onNavigateBack: () -> Unit,
    onConfigChanged: (String, Boolean, Boolean, Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Labels verwalten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück",
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (labels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Keine Labels gefunden", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    InfoCard()
                }

                items(
                    items = labels,
                    key = { it.name } // Optimierung 1: Stabiler Key für Performance
                ) { label ->
                    LabelConfigCard(
                        label = label,
                        onConfigChanged = onConfigChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Filter verbergen: Label erscheint nicht in der Leiste zum Filtern der Kontakte.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Ignorieren: Kontakte werden komplett ausgeblendet, sind aber in der Suche auffindbar.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelConfigCard(
    label: LabelManagementModel,
    onConfigChanged: (String, Boolean, Boolean, Boolean) -> Unit
) {
    // Optimierung 2: Callbacks memoizen, um unnötige Recompositions zu vermeiden
    val onHideToggle = remember(label) { 
        { onConfigChanged(label.name, !label.isHiddenFromFilter, label.isIgnored, label.isSystem) } 
    }
    val onIgnoreToggle = remember(label) { 
        { onConfigChanged(label.name, label.isHiddenFromFilter, !label.isIgnored, label.isSystem) } 
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (label.isSystem) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                    ) {
                        Text(
                            text = "SYSTEM",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = label.isHiddenFromFilter,
                    onClick = onHideToggle,
                    label = { Text("Verbergen") },
                    leadingIcon = if (label.isHiddenFromFilter) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                FilterChip(
                    selected = label.isIgnored,
                    onClick = onIgnoreToggle,
                    label = { Text("Ignorieren") },
                    leadingIcon = if (label.isIgnored) {
                        { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelSettingsPreview() {
    BirthdayBuddyTheme {
        LabelSettingsContent(
            labels = listOf(
                LabelManagementModel("Familie", isHiddenFromFilter = false, isIgnored = false, isSystem = true),
                LabelManagementModel("Arbeit", isHiddenFromFilter = true, isIgnored = false, isSystem = false),
                LabelManagementModel("Ex-Kollegen", isHiddenFromFilter = false, isIgnored = true, isSystem = false)
            ),
            onNavigateBack = {},
            onConfigChanged = { _, _, _, _ -> }
        )
    }
}
