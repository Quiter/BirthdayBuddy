package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.viewmodel.HomeViewModel

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
                contentPadding = PaddingValues(
                    start = SpacingNormal,
                    end = SpacingNormal,
                    top = 0.dp,
                    bottom = SpacingSmall
                ),
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
            ) {
                item {
                    FilterChip(
                        selected = selectedLabel == null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLabelSelected(null)
                        },
                        label = { Text(stringResource(R.string.home_filter_all)) },
                    )
                }

                items(
                    items = labels,
                    key = { it }
                ) { label ->
                    val displayLabel = when (label) {
                        HomeViewModel.LABEL_NO_BIRTHDAY -> stringResource(R.string.home_filter_no_birthday)
                        HomeViewModel.LABEL_ANNIVERSARY -> stringResource(R.string.home_filter_anniversary)
                        HomeViewModel.LABEL_NAME_DAY -> stringResource(R.string.home_filter_name_day)
                        else -> label
                    }

                    FilterChip(
                        selected = selectedLabel == label,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLabelSelected(label)
                        },
                        label = { Text(displayLabel) }
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
