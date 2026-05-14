package com.heckmannch.birthdaybuddy.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelFilterBar(
    visible: Boolean,
    labels: List<String>,
    selectedLabel: String?,
    onLabelSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current

    if (labels.isNotEmpty()) {
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = modifier,
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedLabel == null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLabelSelected(null)
                        },
                        label = { Text(stringResource(R.string.home_filter_all)) }
                    )
                }

                items(
                    items = labels,
                    key = { it }
                ) { label ->
                    FilterChip(
                        selected = selectedLabel == label,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLabelSelected(label)
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LabelFilterBarPreview() {
    BirthdayBuddyTheme {
        LabelFilterBar(
            visible = true,
            labels = listOf("Familie", "Freunde", "Arbeit"),
            selectedLabel = "Freunde",
            onLabelSelected = {}
        )
    }
}
