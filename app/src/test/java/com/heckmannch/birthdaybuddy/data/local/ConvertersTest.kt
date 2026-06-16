package com.heckmannch.birthdaybuddy.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

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
}
