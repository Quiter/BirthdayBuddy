package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.BorderWidthThin
import com.heckmannch.birthdaybuddy.ui.theme.SelectedBorderWidth
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Card representing "All Labels" selection.
 */
@Composable
internal fun AllLabelsCard(
    isActive: Boolean,
    contactsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val containerColor = if (isActive) colors.secondaryContainer.copy(alpha = 0.55f) else colors.surfaceContainerLow
    val borderColor = if (isActive) colors.primary else colors.outlineVariant.copy(alpha = 0.5f)

    OutlinedCard(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(width = if (isActive) SelectedBorderWidth else BorderWidthThin, color = borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingNormal)
        ) {
            SelectionCheckIndicator(
                selected = isActive,
                modifier = Modifier.padding(end = SpacingMedium)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.labels_all_card),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(SpacingExtraSmall))
                Text(
                    text = stringResource(R.string.labels_all_card_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Surface(
                color = colors.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = if (contactsCount == 1) {
                        stringResource(R.string.labels_count_contacts_one)
                    } else {
                        stringResource(R.string.labels_count_contacts_many, contactsCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall),
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AllLabelsCardActivePreview() {
    BirthdayBuddyTheme {
        AllLabelsCard(
            isActive = true,
            contactsCount = 5,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AllLabelsCardInactivePreview() {
    BirthdayBuddyTheme {
        AllLabelsCard(
            isActive = false,
            contactsCount = 12,
            onClick = {}
        )
    }
}
