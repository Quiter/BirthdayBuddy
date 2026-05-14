package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.viewmodel.GiftIdea
import kotlinx.coroutines.launch

@Composable
fun GiftIdeaDialog(
    initialIdeas: List<GiftIdea>,
    onDismiss: () -> Unit,
    onConfirm: (List<GiftIdea>) -> Unit,
) {
    // Saver für die Liste der Geschenkideen
    val giftIdeaSaver = Saver<SnapshotStateList<GiftIdea>, String>(
        save = { GiftIdea.toString(it.toList()) },
        restore = { GiftIdea.fromString(it).toMutableStateList() }
    )
    val ideas = rememberSaveable(saver = giftIdeaSaver) { 
        mutableStateListOf<GiftIdea>().apply { addAll(initialIdeas) } 
    }
    
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Tracking für das neu hinzugefügte Element
    var newlyAddedId by rememberSaveable { mutableStateOf<String?>(null) }

    val onAddNewIdea = {
        val newIdea = GiftIdea(text = "")
        val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
        if (firstCheckedIndex != -1) {
            ideas.add(firstCheckedIndex, newIdea)
        } else {
            ideas.add(newIdea)
        }
        newlyAddedId = newIdea.id
        scope.launch {
            val targetIdx = ideas.indexOfFirst { it.id == newIdea.id }
            if (targetIdx != -1) listState.animateScrollToItem(targetIdx)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.gift_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    itemsIndexed(
                        items = ideas, 
                        key = { _, item -> item.id },
                    ) { _, item ->
                        GiftIdeaItemRow(
                            item = item,
                            newlyAddedId = newlyAddedId,
                            modifier = Modifier.animateItem(),
                            onFocusRequested = { newlyAddedId = null },
                            onCheckedChange = { checked ->
                                val newItem = item.copy(isChecked = checked)
                                val currentIndex = ideas.indexOfFirst { it.id == item.id }
                                if (currentIndex != -1) {
                                    ideas.removeAt(currentIndex)
                                    if (checked) {
                                        ideas.add(newItem)
                                    } else {
                                        val firstCheckedIndex = ideas.indexOfFirst { it.isChecked }
                                        if (firstCheckedIndex != -1) ideas.add(firstCheckedIndex, newItem)
                                        else ideas.add(0, newItem)
                                    }
                                }
                            },
                            onTextChange = { newText ->
                                val idx = ideas.indexOfFirst { it.id == item.id }
                                if (idx != -1) {
                                    ideas[idx] = item.copy(text = newText)
                                }
                            },
                            onDelete = {
                                val idx = ideas.indexOfFirst { it.id == item.id }
                                if (idx != -1) ideas.removeAt(idx)
                            },
                            onDone = {
                                if (item.text.isNotBlank()) onAddNewIdea()
                                else focusManager.clearFocus()
                            }
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // "Listeneintrag hinzufügen" Bereich
                Surface(
                    onClick = { onAddNewIdea() },
                    modifier = Modifier.fillMaxWidth(),
                    color = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).padding(start = 12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.gift_dialog_add),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(ideas.filter { it.text.isNotBlank() }) }) {
                Text(stringResource(R.string.gift_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.gift_dialog_cancel))
            }
        }
    )
}

@Composable
private fun GiftIdeaItemRow(
    item: GiftIdea,
    newlyAddedId: String?,
    modifier: Modifier = Modifier,
    onFocusRequested: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    
    LaunchedEffect(newlyAddedId) {
        if (newlyAddedId == item.id) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .semantics { role = Role.Checkbox },
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
        )
        BasicTextField(
            value = item.text,
            onValueChange = { newText ->
                val capitalizedText = newText.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase() else it.toString() 
                }
                onTextChange(capitalizedText)
            },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next, // "Next" statt "Done" für besseren Flow
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
        )
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.gift_dialog_delete),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            )
        }
    }
}
