package com.heckmannch.birthdaybuddy2.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelFilterBar(
    visible: Boolean,
    labels: List<String>,
    selectedLabel: String?,
    onLabelSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (labels.isNotEmpty()) {
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
            modifier = modifier
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = labels,
                    key = { it }
                ) { label ->
                    FilterChip(
                        selected = selectedLabel == label,
                        onClick = { onLabelSelected(label) },
                        label = { Text(label) },
                        leadingIcon = if (selectedLabel == label) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }
    }
}
