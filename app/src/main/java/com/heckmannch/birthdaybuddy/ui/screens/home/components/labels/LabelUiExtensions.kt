package com.heckmannch.birthdaybuddy.ui.screens.home.components.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels

/**
 * Extension function to resolve the display name for a label string.
 * Special system/event pseudo-labels are mapped to their respective localized string resources,
 * prefixed custom date-field event labels have their prefix stripped, and regular user labels
 * are returned as is.
 */
@Composable
fun String.toDisplayLabel(): String {
    return when {
        this == ContactLabels.LABEL_NO_BIRTHDAY -> stringResource(R.string.home_filter_no_birthday)
        this == ContactLabels.LABEL_ANNIVERSARY -> stringResource(R.string.home_filter_anniversary)
        this == ContactLabels.LABEL_NAME_DAY -> stringResource(R.string.home_filter_name_day)
        this == ContactLabels.LABEL_OTHER_EVENT -> stringResource(R.string.home_filter_other_event)
        ContactLabels.isCustomEventLabel(this) -> ContactLabels.extractCustomEventName(this) ?: this
        else -> this
    }
}
