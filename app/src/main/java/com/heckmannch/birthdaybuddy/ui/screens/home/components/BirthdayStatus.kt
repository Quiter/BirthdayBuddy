package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R

@Composable
fun BirthdayStatus(
    isToday: Boolean,
    nextAge: Int?,
    daysUntilNext: Long,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null,
) {
    val isMissingBirthday = daysUntilNext == Long.MAX_VALUE
    val ageText = nextAge?.let { stringResource(R.string.widget_turns_age, it) }
    val daysText = when {
        isToday -> stringResource(R.string.item_today)
        isMissingBirthday -> ""
        else -> pluralStringResource(R.plurals.item_days_left, daysUntilNext.toInt(), daysUntilNext.toInt())
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
        if (isMissingBirthday && onAddClick != null) {
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_add_contact),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            if (ageText != null) {
                Text(
                    text = ageText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = daysText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isToday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
