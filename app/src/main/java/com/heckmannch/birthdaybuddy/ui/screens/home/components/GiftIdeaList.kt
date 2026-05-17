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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea

@Composable
fun GiftIdeaList(
    giftIdeas: List<GiftIdea>,
    newlyAddedId: String?,
    onAddNewIdea: () -> Unit,
    onFocusRequested: () -> Unit,
    onCheckedChange: (GiftIdea, Boolean) -> Unit,
    onTextChange: (GiftIdea, String) -> Unit,
    onDelete: (GiftIdea) -> Unit,
    onDone: (GiftIdea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        if (giftIdeas.isNotEmpty()) {
            Text(
                text = stringResource(R.string.gift_dialog_title),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.primary
            )
            giftIdeas.forEach { idea ->
                GiftIdeaItemRow(
                    item = idea,
                    newlyAddedId = newlyAddedId,
                    onFocusRequested = onFocusRequested,
                    onCheckedChange = { onCheckedChange(idea, it) },
                    onTextChange = { onTextChange(idea, it) },
                    onDelete = { onDelete(idea) },
                    onDone = { onDone(idea) }
                )
            }
        }

        // "Eintrag hinzufügen" Schnellzugriff
        Surface(
            onClick = onAddNewIdea,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                    text = stringResource(R.string.gift_dialog_add),
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
            .padding(horizontal = 4.dp)
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
                .padding(vertical = 8.dp)
                .focusRequester(focusRequester),
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
