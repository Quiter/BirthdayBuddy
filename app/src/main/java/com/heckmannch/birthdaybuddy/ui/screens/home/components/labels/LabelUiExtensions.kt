package com.heckmannch.birthdaybuddy.ui.screens.home.components.labels

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels

/**
 * Extension function to resolve the display name for a label string.
 * Special system labels are mapped to their respective localized string resources,
 * while custom user labels are returned as is.
 */
@Composable
fun String.toDisplayLabel(): String {
    return when (this) {
        ContactLabels.LABEL_NO_BIRTHDAY -> stringResource(R.string.home_filter_no_birthday)
        ContactLabels.LABEL_ANNIVERSARY -> stringResource(R.string.home_filter_anniversary)
        ContactLabels.LABEL_NAME_DAY -> stringResource(R.string.home_filter_name_day)
        else -> this
    }
}
