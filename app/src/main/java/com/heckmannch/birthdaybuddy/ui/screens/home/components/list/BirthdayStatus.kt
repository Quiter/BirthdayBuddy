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
import androidx.compose.ui.semantics.semantics
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

/**
 * Displays the birthday status of a person in a contact list item.
 *
 * Shows the upcoming age (if available) and the remaining time until the next birthday:
 * - If [daysUntilNext] is `null` (no birthday date configured), displays an add button triggering [onEditBirthday].
 * - If [isToday] is `true`, displays "Today" alongside a cake icon.
 * - Otherwise, displays the remaining days until the next birthday.
 *
 * @param isToday `true` if the person's birthday is today, `false` otherwise.
 * @param nextAge The upcoming age the person will turn on their next birthday, or `null` if the birth year is unknown.
 * @param daysUntilNext The number of days remaining until the next birthday, or `null` if no birthday date is set.
 * @param modifier The [Modifier] to be applied to the layout.
 * @param onEditBirthday Callback invoked when the user taps the add/edit birthday button.
 */
@Composable
fun BirthdayStatus(
    isToday: Boolean,
    nextAge: Int?,
    daysUntilNext: Long?,
    modifier: Modifier = Modifier,
    onEditBirthday: () -> Unit,
) {
    if (daysUntilNext == null) {
        IconButton(
            onClick = onEditBirthday,
            modifier = modifier.padding(end = SpacingMedium)
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
            modifier = modifier
                .padding(end = SpacingSmall)
                .semantics(mergeDescendants = true) {}
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
        Column(
            modifier = Modifier.padding(SpacingNormal),
            horizontalAlignment = Alignment.End
        ) {
            // Missing Date / Action button
            BirthdayStatus(
                isToday = false,
                nextAge = null,
                daysUntilNext = null,
                onEditBirthday = {}
            )
            Spacer(modifier = Modifier.padding(bottom = SpacingNormal))

            // Birthday Today with age
            BirthdayStatus(
                isToday = true,
                nextAge = 25,
                daysUntilNext = 0,
                onEditBirthday = {}
            )
            Spacer(modifier = Modifier.padding(bottom = SpacingNormal))

            // Birthday Today without age
            BirthdayStatus(
                isToday = true,
                nextAge = null,
                daysUntilNext = 0,
                onEditBirthday = {}
            )
            Spacer(modifier = Modifier.padding(bottom = SpacingNormal))

            // Upcoming Birthday
            BirthdayStatus(
                isToday = false,
                nextAge = 30,
                daysUntilNext = 5,
                onEditBirthday = {}
            )
        }
    }
}

