package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R

@Composable
fun BirthdayStatus(
    isToday: Boolean,
    nextAge: Int?,
    daysUntilNext: Long,
    modifier: Modifier = Modifier,
) {
    val ageText = nextAge?.let { stringResource(R.string.widget_turns_age, it) }
    val daysText = if (isToday) {
        stringResource(R.string.item_today)
    } else {
        pluralStringResource(R.plurals.item_days_left, daysUntilNext.toInt(), daysUntilNext.toInt())
    }

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier
    ) {
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
