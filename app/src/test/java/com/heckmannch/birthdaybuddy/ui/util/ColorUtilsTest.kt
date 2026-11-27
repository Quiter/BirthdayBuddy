package com.heckmannch.birthdaybuddy.ui.util

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for `ColorUtils.kt` hex formatting, parsing, HSV conversion, darkening, and contrast
 * helpers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ColorUtilsTest {

    @Test
    fun toHexString_formatsRgbWithAndWithoutHash() {
        val pink = Color(0xFFE91E63)
        assertThat(pink.toHexString()).isEqualTo("#E91E63")
        assertThat(pink.toHexString(includeHash = false)).isEqualTo("E91E63")
    }

    @Test
    fun toHexString_formatsArgbWhenIncludeAlphaIsTrue() {
        val semiBlue = Color(0x802196F3)
        assertThat(semiBlue.toHexString(includeAlpha = true)).isEqualTo("#802196F3")
        assertThat(semiBlue.toHexString(includeHash = false, includeAlpha = true)).isEqualTo("802196F3")
    }

    @Test
    fun toColorOrNull_parsesValid6And8DigitHex() {
        assertThat("#E91E63".toColorOrNull()).isEqualTo(Color(0xFFE91E63))
        assertThat("e91e63".toColorOrNull()).isEqualTo(Color(0xFFE91E63))
        assertThat("  #802196F3  ".toColorOrNull()).isEqualTo(Color(0x802196F3))
    }

    @Test
    fun toColorOrNull_returnsNullForInvalidOrPartialInputs() {
        assertThat(null.toColorOrNull()).isNull()
        assertThat("".toColorOrNull()).isNull()
        assertThat("#FFF".toColorOrNull()).isNull()
        assertThat("#12345".toColorOrNull()).isNull()
        assertThat("#GGGGGG".toColorOrNull()).isNull()
        assertThat("red".toColorOrNull()).isNull()
    }

    @Test
    fun toHsv_returnsExpectedCoordinatesForPrimaryColors() {
        val (redH, redS, redV) = Color.Red.toHsv()
        assertThat(redH).isWithin(0.1f).of(0f)
        assertThat(redS).isWithin(0.01f).of(1f)
        assertThat(redV).isWithin(0.01f).of(1f)

        val (blueH, blueS, blueV) = Color.Blue.toHsv()
        assertThat(blueH).isWithin(0.1f).of(240f)
        assertThat(blueS).isWithin(0.01f).of(1f)
        assertThat(blueV).isWithin(0.01f).of(1f)
    }

    @Test
    fun darken_scalesRgbChannelsAndPreservesAlpha() {
        val base = Color(red = 1f, green = 0.5f, blue = 0.25f, alpha = 0.8f)
        val darkened = base.darken(0.4f)

        assertThat(darkened.red).isWithin(0.005f).of(0.6f)
        assertThat(darkened.green).isWithin(0.005f).of(0.3f)
        assertThat(darkened.blue).isWithin(0.005f).of(0.15f)
        assertThat(darkened.alpha).isWithin(0.005f).of(0.8f)
    }

    @Test
    fun darken_clampsFactorOutsideZeroToOne() {
        val base = Color(0xFF2196F3)
        assertThat(base.darken(-0.5f)).isEqualTo(base)
        assertThat(base.darken(2f)).isEqualTo(Color.Black)
    }

    @Test
    fun contrastingContent_returnsWhiteOnDarkAndBlackOnLight() {
        assertThat(Color.Black.requiresLightContent()).isTrue()
        assertThat(Color.Black.contrastingContent()).isEqualTo(Color.White)

        assertThat(Color.White.requiresLightContent()).isFalse()
        assertThat(Color.White.contrastingContent()).isEqualTo(Color.Black)
    }
}
