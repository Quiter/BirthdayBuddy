package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Composable
fun GiftIdeaList(
    giftIdeas: List<GiftIdea>,
    newlyAddedId: String?,
    onAddNewIdea: () -> Unit,
    onCheckedChange: (GiftIdea, Boolean) -> Unit,
    onTextChange: (GiftIdea, String) -> Unit,
    onDelete: (GiftIdea) -> Unit,
    onDone: (GiftIdea) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SpacingSmall),
    ) {
        giftIdeas.forEach { idea ->
            key(idea.id) {
                GiftIdeaItem(
                    idea = idea,
                    isNew = idea.id == newlyAddedId,
                    onCheckedChange = { onCheckedChange(idea, it) },
                    onTextChange = { onTextChange(idea, it) },
                    onDelete = { onDelete(idea) },
                    onDone = { currentText -> onDone(idea.copy(text = currentText)) },
                )
            }
        }

        TextButton(
            onClick = onAddNewIdea,
            modifier = Modifier
                .padding(start = SpacingSmall)
                .testTag("add_gift_idea_button")
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(IconSizeSmall)
            )
            Spacer(modifier = Modifier.width(SpacingSmall))
            Text(stringResource(R.string.gift_dialog_add))
        }
    }
}

@Composable
private fun GiftIdeaItem(
    idea: GiftIdea,
    isNew: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDone: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    var isFocused by remember(idea.id) { mutableStateOf(false) }
    var isDeleted by remember(idea.id) { mutableStateOf(false) }

    // Lokaler State für das Textfeld, um Cursor-Sprünge und Überschreiben bei DB-Sync zu vermeiden.
    var localText by remember(idea.id) { mutableStateOf(idea.text) }
    var lastSavedText by remember(idea.id) { mutableStateOf(idea.text) }

    // Speichert in-flight DB-Writes, damit asynchrone Echos aus Room nicht den lokalen State überschreiben.
    val pendingTexts = remember(idea.id) { mutableListOf<String>() }

    fun commitTextChange(text: String) {
        if (!isDeleted && (text != lastSavedText)) {
            lastSavedText = text
            pendingTexts.add(text)
            onTextChange(text)
        }
    }

    // Synchronisiere extern geänderten Text nur, wenn das Feld nicht fokussiert ist
    // und es sich nicht um ein Echo eines eigenen in-flight DB-Writes handelt.
    LaunchedEffect(idea.text) {
        val pendingIndex = pendingTexts.indexOf(idea.text)
        if (pendingIndex != -1) {
            repeat(pendingIndex + 1) {
                pendingTexts.removeAt(0)
            }
        } else if (!isFocused && (idea.text != localText)) {
            pendingTexts.clear()
            localText = idea.text
            lastSavedText = idea.text
        }
    }

    // Debouncing für Texteingaben (300ms Verzögerung)
    LaunchedEffect(localText) {
        if (!isDeleted && (localText != lastSavedText)) {
            delay(300.milliseconds)
            commitTextChange(localText)
        }
    }

    LaunchedEffect(isNew) {
        if (isNew) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = idea.isChecked,
            onCheckedChange = onCheckedChange
        )

        TextField(
            value = localText,
            onValueChange = {
                localText = it
            },
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focusState ->
                    val wasFocused = isFocused
                    isFocused = focusState.isFocused
                    if (wasFocused && !focusState.isFocused) {
                        commitTextChange(localText)
                        if (!isDeleted && (localText == lastSavedText) && (idea.text != localText)) {
                            localText = idea.text
                            lastSavedText = idea.text
                        }
                    }
                }
                .focusRequester(focusRequester)
                .testTag("gift_text_field"),
            placeholder = { Text(stringResource(R.string.gift_idea_placeholder)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    commitTextChange(localText)
                    onDone(localText)
                }
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (idea.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                color = if (idea.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium) else MaterialTheme.colorScheme.onSurface
            )
        )

        IconButton(
            onClick = {
                isDeleted = true
                onDelete()
            }
        ) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = stringResource(R.string.gift_dialog_delete),
                tint = MaterialTheme.colorScheme.error.copy(alpha = AlphaEmphasisMedium),
                modifier = Modifier.size(IconSizeMedium)
            )
        }
    }
}

@Preview
@Composable
private fun GiftIdeaListPreview() {
    BirthdayBuddyTheme {
        GiftIdeaList(
            giftIdeas = listOf(SampleData.giftIdea1, SampleData.giftIdea2),
            newlyAddedId = null,
            onAddNewIdea = {},
            onCheckedChange = { _, _ -> },
            onTextChange = { _, _ -> },
            onDelete = {},
            onDone = {},
        )
    }
}
