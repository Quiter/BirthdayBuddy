package com.heckmannch.birthdaybuddy.data.local

import com.heckmannch.birthdaybuddy.domain.model.ContactLabels as DomainContactLabels

/**
 * System-defined special label identifiers used to represent pseudo-categories
 * (contacts without a birthday, anniversaries, name days) in the label filter
 * and LabelConfig system.
 *
 * @deprecated Use [com.heckmannch.birthdaybuddy.domain.model.ContactLabels] instead.
 */
@Deprecated(
    message = "Use com.heckmannch.birthdaybuddy.domain.model.ContactLabels instead",
    replaceWith = ReplaceWith("ContactLabels", "com.heckmannch.birthdaybuddy.domain.model.ContactLabels")
)
@Suppress("unused")
object ContactLabels {
    const val LABEL_NO_BIRTHDAY = DomainContactLabels.LABEL_NO_BIRTHDAY
    const val LABEL_ANNIVERSARY = DomainContactLabels.LABEL_ANNIVERSARY
    const val LABEL_NAME_DAY = DomainContactLabels.LABEL_NAME_DAY
}
