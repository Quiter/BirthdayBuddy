package com.heckmannch.birthdaybuddy.data.local

/**
 * System-defined special label identifiers used to represent pseudo-categories
 * (contacts without a birthday, anniversaries, name days) in the label filter
 * and [LabelConfig] system.
 *
 * These values are stored as [LabelConfig] entries in the database and are
 * referenced across multiple layers (ViewModels, UI screens). Placing them
 * here in the data layer avoids cross-package ViewModel dependencies.
 */
object ContactLabels {
    const val LABEL_NO_BIRTHDAY = "special:no_birthday"
    const val LABEL_ANNIVERSARY = "special:anniversary"
    const val LABEL_NAME_DAY = "special:name_day"
}
