package com.heckmannch.birthdaybuddy.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
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

        // --- Home-Screen Scrollen ---
        // Durch die Geburtstagsliste scrollen (LazyColumn + BirthdayItem rendering)
        val list = device.findObject(By.res("birthday_list"))
        if (list != null) {
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }

        // --- Suche / Filter ---
        // Suchfeld öffnen und Suchanfrage eingeben (Search-Debounce + Filter-Pipeline)
        val searchField = device.findObject(By.res("search_field"))
        if (searchField != null) {
            searchField.click()
            searchField.text = "Max"
            device.waitForIdle()
            // Suche leeren, um den Normalzustand wiederherzustellen
            searchField.text = ""
            device.waitForIdle()
        }

        // --- Kontakt-Details ---
        // Ersten Kontakt antippen → BirthdayItem expandieren (GiftIdea-Liste, Actions)
        val firstItem = device.findObject(By.res("birthday_item_0"))
        if (firstItem != null) {
            firstItem.click()
            device.waitForIdle()
            // Erneut anklicken, um wieder einzuklappen
            firstItem.click()
            device.waitForIdle()
        }

        // --- Navigation zu Settings ---
        // Settings öffnen und zur Home-Ansicht zurückkehren
        val settingsButton = device.findObject(By.res("settings_button"))
        if (settingsButton != null) {
            settingsButton.click()
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()
        }
    }
}
