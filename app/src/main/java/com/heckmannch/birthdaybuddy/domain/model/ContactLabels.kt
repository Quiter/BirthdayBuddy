package com.heckmannch.birthdaybuddy.domain.model

/**
 * Domain-defined special label identifiers used to represent pseudo-categories
 * (contacts without a birthday, anniversaries, name days) in the label filter
 * and [LabelConfig] system.
 *
 * Placed in the domain layer to decouple UI and UseCases from data layer details.
 */
object ContactLabels {
    const val LABEL_NO_BIRTHDAY = "special:no_birthday"
    const val LABEL_ANNIVERSARY = "special:anniversary"
    const val LABEL_NAME_DAY = "special:name_day"
}
