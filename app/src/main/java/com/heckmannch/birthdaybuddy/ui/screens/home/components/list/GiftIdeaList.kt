package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea

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
            .padding(bottom = 8.dp)
    ) {
        giftIdeas.forEach { idea ->
            GiftIdeaItem(
                idea = idea,
                isNew = idea.id == newlyAddedId,
                onCheckedChange = { onCheckedChange(idea, it) },
                onTextChange = { onTextChange(idea, it) },
                onDelete = { onDelete(idea) },
                onDone = { onDone(idea) }
            )
        }

        TextButton(
            onClick = onAddNewIdea,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
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
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isNew) {
        if (isNew) {
            focusRequester.requestFocus()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = idea.isChecked,
            onCheckedChange = onCheckedChange
        )

        TextField(
            value = idea.text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
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
                onNext = { onDone() }
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (idea.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                color = if (idea.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
            )
        )

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = stringResource(R.string.gift_dialog_delete),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
