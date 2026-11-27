package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardLayout
import com.heckmannch.birthdaybuddy.ui.components.cards.GlassCardTag
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.defaultCardGradient
import com.heckmannch.birthdaybuddy.ui.theme.labelActiveCardGradient

/**
 * Card representing "All Labels" selection using the central [GlassCardLayout].
 */
@Composable
internal fun AllLabelsCard(
    isActive: Boolean,
    contactsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradientColors = if (isActive) labelActiveCardGradient() else defaultCardGradient()
    val countText = if (contactsCount == 1) {
        stringResource(R.string.labels_count_contacts_one)
    } else {
        stringResource(R.string.labels_count_contacts_many, contactsCount)
    }

    GlassCardLayout(
        modifier = modifier,
        gradientColors = gradientColors,
        onClick = onClick,
        trailing = {
            GlassCardTag(text = countText)
        }
    ) {
        SelectionCheckIndicator(
            selected = isActive,
            ink = style.titleColor
        )

        Column(
            modifier = with(row) { Modifier.weight(1f) },
            verticalArrangement = Arrangement.spacedBy(style.textSpacing)
        ) {
            Text(
                text = stringResource(R.string.labels_all_card),
                style = style.titleStyle,
                fontWeight = FontWeight.Bold,
                color = style.titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.labels_all_card_desc),
                style = style.subtitleStyle,
                color = style.subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
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
