package com.heckmannch.birthdaybuddy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This class generates the Baseline Profile for BirthdayBuddy.
 * Run this test locally using a rooted emulator or a Google APIs emulator (API 29+) to generate:
 * `.\gradlew :app:generateBaselineProfile`
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generate() = baselineRule.collect(
        packageName = "com.heckmannch.birthdaybuddy",
        includeInStartupProfile = true
    ) {
        // Launch the target app
        pressHome()
        startActivityAndWait()
        
        // Wait to allow the app to draw its first frame and profileinstaller
        // to successfully flush target profiles to disk.
        Thread.sleep(5000)
    }
}
