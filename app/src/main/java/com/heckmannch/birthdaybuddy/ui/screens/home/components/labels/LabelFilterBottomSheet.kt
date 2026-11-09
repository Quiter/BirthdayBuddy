package com.heckmannch.birthdaybuddy.ui.screens.home.components.labels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.ui.theme.SpacingMedium
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall

/**
 * A Modal Bottom Sheet for selecting contact label filters on compact screen sizes.
 *
 * @param availableLabels List of available string label categories.
 * @param selectedLabel Currently selected label filter, or null for all contacts.
 * @param onLabelSelected Callback invoked when a label filter is selected.
 * @param onDismissRequest Callback invoked when the bottom sheet is dismissed.
 * @param sheetState State for controlling the bottom sheet layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelFilterBottomSheet(
    availableLabels: List<String>,
    selectedLabel: String?,
    onLabelSelected: (String?) -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingNormal)
        ) {
            Text(
                text = stringResource(R.string.labels_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = SpacingNormal, vertical = SpacingSmall)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                // "All" contacts filter option
                item {
                    LabelBottomSheetItem(
                        title = stringResource(R.string.home_filter_all),
                        icon = Icons.AutoMirrored.Filled.List,
                        isSelected = selectedLabel == null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLabelSelected(null)
                            onDismissRequest()
                        }
                    )
                }

                items(
                    items = availableLabels,
                    key = { it }
                ) { label ->
                    val displayLabel = label.toDisplayLabel()
                    val iconVector = when (label) {
                        ContactLabels.LABEL_NO_BIRTHDAY -> Icons.Default.DateRange
                        ContactLabels.LABEL_ANNIVERSARY -> Icons.Default.Favorite
                        ContactLabels.LABEL_NAME_DAY -> Icons.Default.Face
                        else -> Icons.AutoMirrored.Filled.Label
                    }

                    LabelBottomSheetItem(
                        title = displayLabel,
                        icon = iconVector,
                        isSelected = selectedLabel == label,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLabelSelected(label)
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelBottomSheetItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = SpacingNormal, vertical = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Spacer(modifier = Modifier.width(SpacingMedium))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
