package com.heckmannch.birthdaybuddy2.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy2.viewmodel.BirthdayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSettingsScreen(
    viewModel: BirthdayViewModel,
    onNavigateBack: () -> Unit,
) {
    val labels by viewModel.labelManagementList.collectAsState()

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
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Keine Labels gefunden", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                item {
                    Text(
                        text = "Hier kannst du festlegen, wie Labels in der App behandelt werden.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                items(labels) { label ->
                    LabelConfigItem(
                        name = label.name,
                        isHiddenFromFilter = label.isHiddenFromFilter,
                        isIgnored = label.isIgnored,
                    ) { hidden, ignored ->
                        viewModel.updateLabelConfig(label.name, hidden, ignored)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun LabelConfigItem(
    name: String,
    isHiddenFromFilter: Boolean,
    isIgnored: Boolean,
    onConfigChanged: (Boolean, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("In Filterleiste verbergen", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Das Label wird nicht mehr oben auf dem Homescreen angezeigt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isHiddenFromFilter,
                onCheckedChange = { onConfigChanged(it, isIgnored) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Kontakte ignorieren", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Kontakte mit diesem Label werden komplett aus der Liste und dem Widget ausgeblendet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isIgnored,
                onCheckedChange = { onConfigChanged(isHiddenFromFilter, it) }
            )
        }
    }
}
