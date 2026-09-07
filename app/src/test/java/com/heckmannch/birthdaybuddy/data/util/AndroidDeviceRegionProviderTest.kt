package com.heckmannch.birthdaybuddy.data.util

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for [AndroidDeviceRegionProvider].
 */
class AndroidDeviceRegionProviderTest {

    private lateinit var originalLocale: Locale
    private val provider = AndroidDeviceRegionProvider()

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `getCountryIso returns current default locale country`() {
        Locale.setDefault(Locale.GERMANY)
        assertThat(provider.getCountryIso()).isEqualTo("DE")

        Locale.setDefault(Locale.US)
        assertThat(provider.getCountryIso()).isEqualTo("US")

        Locale.setDefault(Locale.UK)
        assertThat(provider.getCountryIso()).isEqualTo("GB")
    }
}
