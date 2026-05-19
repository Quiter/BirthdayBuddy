package com.heckmannch.birthdaybuddy.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Sucht rekursiv nach einer Activity im Context.
 * Hilfreich, da Contexts in Compose oft durch Wrapper (z.B. Hilt) umschlossen sind.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
