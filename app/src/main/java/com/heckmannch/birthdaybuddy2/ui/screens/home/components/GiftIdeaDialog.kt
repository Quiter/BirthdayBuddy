package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy2.viewmodel.GiftIdea

@Composable
fun GiftIdeaDialog(
    initialIdeas: List<GiftIdea>,
    onDismiss: () -> Unit,
    onConfirm: (List<GiftIdea>) -> Unit,
) {
    // Verwendung von mutableStateListOf für performantere Listen-Updates
    val ideas = remember { mutableStateListOf<GiftIdea>().apply { addAll(initialIdeas) } }
    val focusManager = LocalFocusManager.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Geschenkideen") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(
                        items = ideas, 
                        key = { _, item -> item.id }
                    ) { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Sortieren",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    ideas[index] = item.copy(isChecked = checked)
                                }
                            )
                            BasicTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    ideas[index] = item.copy(text = newText)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                            )
                            
                            IconButton(
                                onClick = { ideas.removeAt(index) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Löschen",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                
                // "Listeneintrag hinzufügen" Bereich
                Surface(
                    onClick = { 
                        ideas.add(GiftIdea(text = ""))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.ui.graphics.Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).padding(start = 28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Listeneintrag",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
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
