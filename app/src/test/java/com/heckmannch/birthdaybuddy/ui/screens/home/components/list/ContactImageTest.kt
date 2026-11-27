package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.theme.BirthdayBuddyTheme
import kotlinx.coroutines.awaitCancellation
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(DelicateCoilApi::class)
class ContactImageTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        SingletonImageLoader.reset()
    }

    private fun installTestImageLoader(interceptor: Interceptor) {
        val loader = ImageLoader.Builder(context)
            .components {
                add(interceptor)
            }
            .build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @Test
    fun contactImage_whenImageUriIsNull_displaysInitials() {
        composeRule.setContent {
            BirthdayBuddyTheme {
                ContactImage(
                    imageUri = null,
                    fullName = "Max Mustermann",
                    initials = "MM",
                )
            }
        }

        composeRule.onNodeWithText("MM").assertIsDisplayed()
    }

    @Test
    fun contactImage_whenImageLoadFails_displaysInitialsFallback() {
        installTestImageLoader { chain ->
            ErrorResult(
                image = null,
                request = chain.request,
                throwable = IllegalStateException("Stale content URI"),
            )
        }

        composeRule.setContent {
            BirthdayBuddyTheme {
                ContactImage(
                    imageUri = "content://com.android.contacts/display_photo/999",
                    fullName = "Erika Mustermann",
                    initials = "EM",
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("EM").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.item_image_desc, "Erika Mustermann"),
        ).assertDoesNotExist()
    }

    @Test
    fun contactImage_whenImageIsLoading_displaysInitialsFallback() {
        installTestImageLoader {
            awaitCancellation()
        }

        composeRule.setContent {
            BirthdayBuddyTheme {
                ContactImage(
                    imageUri = "content://com.android.contacts/display_photo/loading",
                    fullName = "Erika Mustermann",
                    initials = "EM",
                )
            }
        }

        composeRule.onNodeWithText("EM").assertIsDisplayed()
    }

    @Test
    fun contactImage_whenImageLoadSucceeds_hidesInitialsToAvoidTransparentPngBleedThrough() {
        installTestImageLoader { chain ->
            SuccessResult(
                image = ColorImage(color = Color.GREEN, width = 64, height = 64),
                request = chain.request,
            )
        }

        composeRule.setContent {
            BirthdayBuddyTheme {
                ContactImage(
                    imageUri = "content://com.android.contacts/display_photo/1",
                    fullName = "Erika Mustermann",
                    initials = "EM",
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.item_image_desc, "Erika Mustermann"),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("EM").assertDoesNotExist()
    }

    @Test
    fun contactImage_dualAvatar_whenBothImagesFail_displaysBothInitialsFallbacks() {
        installTestImageLoader { chain ->
            ErrorResult(
                image = null,
                request = chain.request,
                throwable = SecurityException("Missing permission"),
            )
        }

        composeRule.setContent {
            BirthdayBuddyTheme {
                ContactImage(
                    imageUri = "content://com.android.contacts/display_photo/1",
                    fullName = "Max Mustermann",
                    initials = "M",
                    secondImageUri = "content://com.android.contacts/display_photo/2",
                    secondFullName = "Erika Mustermann",
                    secondInitials = "E",
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("M").assertIsDisplayed()
        composeRule.onNodeWithText("E").assertIsDisplayed()
    }
}
