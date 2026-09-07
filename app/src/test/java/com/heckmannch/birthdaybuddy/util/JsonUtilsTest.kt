package com.heckmannch.birthdaybuddy.util

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.Serializable
import org.junit.Test

class JsonUtilsTest {

    @Serializable
    private data class SampleData(
        val name: String,
        val count: Int = 42,
        val extra: String = "default"
    )

    @Test
    fun defaultJson_encodesDefaults() {
        val data = SampleData(name = "Test")
        val jsonString = JsonUtils.defaultJson.encodeToString(data)
        assertThat(jsonString).contains("\"count\":42")
        assertThat(jsonString).contains("\"extra\":\"default\"")
    }

    @Test
    fun defaultJson_ignoresUnknownKeys() {
        val jsonString = "{\"name\":\"Test\",\"count\":99,\"unknownField\":true}"
        val decoded = JsonUtils.defaultJson.decodeFromString<SampleData>(jsonString)
        assertThat(decoded.name).isEqualTo("Test")
        assertThat(decoded.count).isEqualTo(99)
        assertThat(decoded.extra).isEqualTo("default")
    }

    @Test
    fun prettyJson_formatsWithNewlinesAndIndentation() {
        val data = SampleData(name = "Test")
        val jsonString = JsonUtils.prettyJson.encodeToString(data)
        assertThat(jsonString).contains("\n")
        assertThat(jsonString).contains("    \"name\": \"Test\"")
    }
}
