package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.AlphaEmphasisMedium
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.IconSizeNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

@Composable
fun BirthdayStatus(
    isToday: Boolean,
    nextAge: Int?,
    daysUntilNext: Long?,
    onEditBirthday: () -> Unit,
) {
    if (daysUntilNext == null) {
        IconButton(
            onClick = onEditBirthday,
            modifier = Modifier.padding(end = SpacingMedium)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.item_action_edit_birthday),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSizeNormal)
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = SpacingSmall)
        ) {
            if (nextAge != null) {
                Text(
                    text = stringResource(R.string.widget_turns_age, nextAge),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isToday) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cake,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium),
                        modifier = Modifier.size(IconSizeExtraSmall)
                    )
                    Spacer(modifier = Modifier.width(SpacingExtraSmall))
                    Text(
                        text = stringResource(R.string.item_today),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Text(
                    text = pluralStringResource(
                        R.plurals.item_days_left,
                        daysUntilNext.toInt(),
                        daysUntilNext.toInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = AlphaEmphasisMedium)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BirthdayStatusPreview() {
    BirthdayBuddyTheme {
        Row(modifier = Modifier.padding(SpacingNormal)) {
            BirthdayStatus(isToday = true, nextAge = 25, daysUntilNext = 0, onEditBirthday = {})
            Spacer(modifier = Modifier.width(SpacingNormal))
            BirthdayStatus(isToday = false, nextAge = 30, daysUntilNext = 5, onEditBirthday = {})
        }
    }
}
