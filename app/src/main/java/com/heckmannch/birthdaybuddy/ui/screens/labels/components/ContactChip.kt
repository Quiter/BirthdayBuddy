package com.heckmannch.birthdaybuddy.ui.screens.labels.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.ui.model.ContactUiModel
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.ContactImage
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * Compact contact chip for assigned contacts row.
 */
@Composable
internal fun ContactChip(
    contact: ContactUiModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = SpacingSmall, vertical = SpacingExtraSmall)
        ) {
            ContactImage(
                imageUri = contact.imageUri,
                fullName = contact.fullName,
                initials = contact.initials,
                lookupKey = contact.lookupKey,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(SpacingExtraSmall))
            Text(
                text = contact.fullName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactChipPreview() {
    BirthdayBuddyTheme {
        ContactChip(contact = SampleData.contact1)
    }
}
