package com.heckmannch.birthdaybuddy.data.local

import android.util.Log
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun fromList_serializesListToJsonString() {
        val list = listOf("Family", "Friends")
        val json = converters.fromList(list)
        assertThat(json).isEqualTo("[\"Family\",\"Friends\"]")
    }

    @Test
    fun toList_deserializesJsonStringToList() {
        val json = "[\"Family\",\"Friends\"]"
        val list = converters.toList(json)
        assertThat(list).containsExactly("Family", "Friends").inOrder()
    }

    @Test
    fun toList_handlesLegacyPipeSeparatedData() {
        val legacyData = "Family|Friends"
        val list = converters.toList(legacyData)
        assertThat(list).containsExactly("Family", "Friends").inOrder()
    }

    @Test
    fun toList_handlesSpecialCharactersInJsonCorrectly() {
        // Characters like | should be safe in JSON now
        val listWithSpecialChars = listOf("Work|Project", "Family;Home")
        val json = converters.fromList(listWithSpecialChars)
        val result = converters.toList(json)
        assertThat(result).containsExactly("Work|Project", "Family;Home").inOrder()
    }

    @Test
    fun toList_returnsEmptyListForNullOrBlank() {
        assertThat(converters.toList(null)).isEmpty()
        assertThat(converters.toList("")).isEmpty()
        assertThat(converters.toList("  ")).isEmpty()
    }

    @Test
    fun fromThemeMode_convertsEnumToString() {
        assertThat(converters.fromThemeMode(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.DARK)).isEqualTo("DARK")
        assertThat(converters.fromThemeMode(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.LIGHT)).isEqualTo("LIGHT")
        assertThat(converters.fromThemeMode(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.SYSTEM)).isEqualTo("SYSTEM")
        assertThat(converters.fromThemeMode(null)).isNull()
    }

    @Test
    fun toThemeMode_convertsStringToEnum() {
        assertThat(converters.toThemeMode("DARK")).isEqualTo(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.DARK)
        assertThat(converters.toThemeMode("LIGHT")).isEqualTo(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.LIGHT)
        assertThat(converters.toThemeMode("SYSTEM")).isEqualTo(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.SYSTEM)
        assertThat(converters.toThemeMode(null)).isNull()
    }

    @Test
    fun toThemeMode_fallsBackToSystemOnInvalidString() {
        assertThat(converters.toThemeMode("UNKNOWN_VALUE")).isEqualTo(com.heckmannch.birthdaybuddy.domain.model.ThemeMode.SYSTEM)
    }
}
