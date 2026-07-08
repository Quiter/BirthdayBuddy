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
        // Pre-grant permissions to prevent system permission dialogs and sync contacts
        device.executeShellCommand("pm grant com.heckmannch.birthdaybuddy android.permission.READ_CONTACTS")
        device.executeShellCommand("pm grant com.heckmannch.birthdaybuddy android.permission.WRITE_CONTACTS")
        device.executeShellCommand("pm grant com.heckmannch.birthdaybuddy android.permission.POST_NOTIFICATIONS")
        device.executeShellCommand("pm grant com.heckmannch.birthdaybuddy android.permission.READ_CALENDAR")
        device.executeShellCommand("pm grant com.heckmannch.birthdaybuddy android.permission.WRITE_CALENDAR")

        // --- Startup ---
        pressHome()
        startActivityAndWait()

        // --- Onboarding Walkthrough (if present) ---
        var nextButton = device.findObject(By.res("onboarding_next_button"))
        while (nextButton != null) {
            nextButton.click()
            device.waitForIdle()
            nextButton = device.findObject(By.res("onboarding_next_button"))
        }

        val startButton = device.findObject(By.res("onboarding_start_button"))
        if (startButton != null) {
            startButton.click()
            device.waitForIdle()
        }

        // Wait to allow the app to draw its first frame and profileinstaller
        // to successfully flush target profiles to disk.
        Thread.sleep(5000)

        // --- Home-Screen Scrollen ---
        // Durch die Geburtstagsliste scrollen (LazyColumn + FastScrollbar + BirthdayItem rendering)
        val list = device.findObject(By.res("birthday_list"))
        if (list != null) {
            list.setGestureMargin(device.displayWidth / 5)
            // Fling down to trigger scrollbar visibility
            list.fling(Direction.DOWN)
            device.waitForIdle()

            // Look for the fast scrollbar thumb via its content description "Scrollbar"
            val scrollbar = device.findObject(By.desc("Scrollbar"))
            if (scrollbar != null) {
                val bounds = scrollbar.visibleBounds
                val startX = bounds.centerX()
                val startY = bounds.centerY()
                // Drag down to scroll quickly
                device.drag(startX, startY, startX, startY + 200, 20)
                device.waitForIdle()
                // Drag back up
                device.drag(startX, startY + 200, startX, startY, 20)
                device.waitForIdle()
            } else {
                list.fling(Direction.UP)
                device.waitForIdle()
            }
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
        // Ersten Kontakt antippen → Detail-Pane öffnen (Tablet) oder BirthdayItem expandieren (Phone)
        val firstItem = device.findObject(By.res("birthday_item_0"))
        if (firstItem != null) {
            firstItem.click()
            device.waitForIdle()

            // Check if we are in list-detail layout (detail pane is showing)
            val detailPane = device.findObject(By.res("birthday_detail_pane"))
            if (detailPane != null) {
                // Click close button in detail pane
                val closeButton = device.findObject(By.res("detail_close_button"))
                if (closeButton != null) {
                    closeButton.click()
                } else {
                    device.pressBack()
                }
                device.waitForIdle()
            } else {
                // Compact screen: Item is expanded inside the list, click again to collapse
                firstItem.click()
                device.waitForIdle()
            }
        }

        // --- Navigation zu Settings ---
        // Settings öffnen und zur Home-Ansicht zurückkehren
        val settingsButton = device.findObject(By.res("settings_button"))
        if (settingsButton != null) {
            settingsButton.click()
            device.waitForIdle()

            // Try to find and click the settings back button
            val settingsBackButton = device.findObject(By.res("settings_back_button"))
            if (settingsBackButton != null) {
                settingsBackButton.click()
            } else {
                device.pressBack()
            }
            device.waitForIdle()
        }
    }
}
