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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.model.GiftIdea
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

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
            .padding(bottom = SpacingSmall)
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
    onDone: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // Lokaler State für das Textfeld, um Cursor-Sprünge bei DB-Sync zu vermeiden.
    // Wir synchronisieren den lokalen State nur, wenn sich die ID ändert oder 
    // der externe Text signifikant abweicht (z.B. durch Cloud-Sync).
    var localText by remember(idea.id) { mutableStateOf(idea.text) }

    LaunchedEffect(idea.text) {
        if (localText != idea.text) {
            localText = idea.text
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
                onTextChange(it)
            },
            modifier = Modifier
                .weight(1f)
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
                onNext = { onDone() }
            ),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textDecoration = if (idea.isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                color = if (idea.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium) else MaterialTheme.colorScheme.onSurface
            )
        )

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.DeleteOutline,
                contentDescription = stringResource(R.string.gift_dialog_delete),
                tint = MaterialTheme.colorScheme.error.copy(alpha = AlphaEmphasisMedium),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
