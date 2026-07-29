package com.heckmannch.birthdaybuddy.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.screens.home.components.list.BirthdayQuotePlaceholder
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class BirthdayQuotePlaceholderTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun placeholder_rendersTextsCorrectly_withoutCrashes() {
        composeTestRule.setContent {
            BirthdayBuddyTheme {
                BirthdayQuotePlaceholder()
            }
        }

        // Title
        val titleText = composeTestRule.activity.getString(R.string.detail_placeholder_title)
        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()

        // Quote
        val quoteText = composeTestRule.activity.getString(R.string.detail_placeholder_quote)
        composeTestRule.onNodeWithText(quoteText).assertIsDisplayed()

        // Subtitle
        val subtitleText = composeTestRule.activity.getString(R.string.detail_placeholder_subtitle)
        composeTestRule.onNodeWithText(subtitleText).assertIsDisplayed()
    }
}
