package com.heckmannch.birthdaybuddy.util

import javax.inject.Inject

interface Clock {
    fun currentTimeMillis(): Long
}

class SystemClock @Inject constructor() : Clock {
    override fun currentTimeMillis() = System.currentTimeMillis()
}
