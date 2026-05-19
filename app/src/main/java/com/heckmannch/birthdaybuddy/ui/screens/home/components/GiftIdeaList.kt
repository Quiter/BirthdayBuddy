package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

@Composable
fun GiftIdeaList(
    giftIdeas: List<GiftIdea>,
    newlyAddedId: String?,
    onAddNewIdea: () -> Unit,
    onCheckedChange: (GiftIdea, Boolean) -> Unit,
    onTextChange: (GiftIdea, String) -> Unit,
    onDelete: (GiftIdea) -> Unit,
    onDone: (GiftIdea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        if (giftIdeas.isNotEmpty()) {
            giftIdeas.forEach { idea ->
                key(idea.id) {
                    GiftIdeaItemRow(
                        item = idea,
                        newlyAddedId = newlyAddedId,
                        onCheckedChange = { onCheckedChange(idea, it) },
                        onTextChange = { onTextChange(idea, it) },
                        onDelete = { onDelete(idea) },
                    ) { onDone(idea) }
                }
            }
        }

        // "Eintrag hinzufügen" Schnellzugriff
        Surface(
            onClick = onAddNewIdea,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .testTag("add_gift_idea_button"),
            color = Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (giftIdeas.isEmpty()) stringResource(R.string.gift_idea_placeholder) else stringResource(R.string.gift_dialog_add),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun GiftIdeaItemRow(
    item: GiftIdea,
    newlyAddedId: String?,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    
    // Lokaler State für flüssiges Tippen ohne DB-Latenz-Probleme
    var localText by remember(item.id) { mutableStateOf(item.text) }
    
    // Falls sich der Text von außen ändert (z.B. durch Sync), aktualisieren wir den lokalen State,
    // aber nur wenn wir nicht gerade selbst fokussiert sind (um Cursor-Sprünge zu vermeiden).
    var isFocused by remember { mutableStateOf(value = false) }
    LaunchedEffect(item.text) {
        if (!isFocused && (localText != item.text)) {
            localText = item.text
        }
    }

    LaunchedEffect(newlyAddedId) {
        if (newlyAddedId == item.id) {
            focusRequester.requestFocus()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .semantics { role = Role.Checkbox },
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
        )
        BasicTextField(
            value = localText,
            onValueChange = { newText ->
                val capitalizedText = newText.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                localText = capitalizedText
                onTextChange(capitalizedText)
            },
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .testTag("gift_text_field")
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (item.isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
        )

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GiftIdeaListPreview() {
    BirthdayBuddyTheme {
        GiftIdeaList(
            giftIdeas = SampleData.contact3.giftIdeas,
            newlyAddedId = null,
            onAddNewIdea = {},
            onCheckedChange = { _, _ -> },
            onTextChange = { _, _ -> },
            onDelete = {},
            onDone = {}
        )
    }
}
