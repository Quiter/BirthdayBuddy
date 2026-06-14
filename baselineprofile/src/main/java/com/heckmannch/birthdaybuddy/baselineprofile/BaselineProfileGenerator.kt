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
        // --- Startup ---
        pressHome()
        startActivityAndWait()

        // Wait to allow the app to draw its first frame and profileinstaller
        // to successfully flush target profiles to disk.
        Thread.sleep(5000)

        // TODO: Kritische User Journeys hinzufügen, um die Baseline-Profile-Abdeckung
        //  über den reinen Startup hinaus zu erweitern. Jeder Pfad, der hier durchlaufen
        //  wird, wird bei der Installation AOT-kompiliert und läuft damit schneller.
        //
        // --- Home-Screen Scrollen ---
        // TODO: Durch die Geburtstagsliste scrollen (LazyColumn + BirthdayItem rendering)
        //  device.findObject(By.res("birthday_list")).also { list ->
        //      list.setGestureMargin(device.displayWidth / 5)
        //      list.fling(Direction.DOWN)
        //      list.fling(Direction.UP)
        //  }
        //
        // --- Suche / Filter ---
        // TODO: Suchfeld öffnen und Suchanfrage eingeben (Search-Debounce + Filter-Pipeline)
        //  device.findObject(By.res("search_field")).click()
        //  device.findObject(By.res("search_field")).text = "Max"
        //  device.waitForIdle()
        //
        // --- Kontakt-Details ---
        // TODO: Ersten Kontakt antippen → BirthdayItem expandieren (GiftIdea-Liste, Actions)
        //  device.findObject(By.res("birthday_item_0")).click()
        //  device.waitForIdle()
        //
        // --- Navigation zu Settings ---
        // TODO: Settings öffnen und Sub-Settings navigieren
        //  device.findObject(By.res("settings_button")).click()
        //  device.waitForIdle()
    }
}
