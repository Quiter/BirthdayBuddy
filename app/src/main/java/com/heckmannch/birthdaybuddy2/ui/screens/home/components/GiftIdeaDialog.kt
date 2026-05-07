package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy2.viewmodel.GiftIdea

@Composable
fun GiftIdeaDialog(
    initialIdeas: List<GiftIdea>,
    onDismiss: () -> Unit,
    onConfirm: (List<GiftIdea>) -> Unit
) {
    var ideas by remember { mutableStateOf(initialIdeas) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Geschenkideen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(ideas, key = { _, item -> item.id }) { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Sortieren",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    ideas = ideas.toMutableList().apply {
                                        this[index] = item.copy(isChecked = checked)
                                    }
                                }
                            )
                            BasicTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    ideas = ideas.toMutableList().apply {
                                        this[index] = item.copy(text = newText)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                singleLine = true
                            )
                            
                            IconButton(
                                onClick = {
                                    ideas = ideas.toMutableList().apply { removeAt(index) }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Löschen",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                
                // "Listeneintrag hinzufügen" Bereich
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ideas = ideas + GiftIdea(text = "") }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).padding(start = 28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Listeneintrag",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ideas.filter { it.text.isNotBlank() }) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
