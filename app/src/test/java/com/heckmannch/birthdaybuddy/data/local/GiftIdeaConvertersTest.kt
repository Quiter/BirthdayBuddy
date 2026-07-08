package com.heckmannch.birthdaybuddy.data.local

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import org.junit.Test

class GiftIdeaConvertersTest {
    private val converters = GiftIdeaConverters()

    @Test
    fun fromGiftIdeaList_serializesListToJsonString() {
        val ideas = listOf(
            GiftIdea(id = "1", text = "Socken", isChecked = false),
            GiftIdea(id = "2", text = "Wein", isChecked = true)
        )
        val json = converters.fromGiftIdeaList(ideas)
        assertThat(json).contains("\"id\":\"1\"")
        assertThat(json).contains("\"text\":\"Socken\"")
        assertThat(json).contains("\"isChecked\":false")
        assertThat(json).contains("\"id\":\"2\"")
        assertThat(json).contains("\"text\":\"Wein\"")
        assertThat(json).contains("\"isChecked\":true")
    }

    @Test
    fun fromGiftIdeaList_returnsEmptyJsonArrayForNullOrEmpty() {
        assertThat(converters.fromGiftIdeaList(null)).isEqualTo("[]")
        assertThat(converters.fromGiftIdeaList(emptyList())).isEqualTo("[]")
    }

    @Test
    fun toGiftIdeaList_deserializesJsonStringToList() {
        val json =
            "[{\"id\":\"1\",\"text\":\"Socken\",\"isChecked\":false},{\"id\":\"2\",\"text\":\"Wein\",\"isChecked\":true}]"
        val list = converters.toGiftIdeaList(json)
        assertThat(list).hasSize(2)
        assertThat(list[0].id).isEqualTo("1")
        assertThat(list[0].text).isEqualTo("Socken")
        assertThat(list[0].isChecked).isFalse()
        assertThat(list[1].id).isEqualTo("2")
        assertThat(list[1].text).isEqualTo("Wein")
        assertThat(list[1].isChecked).isTrue()
    }

    @Test
    fun toGiftIdeaList_handlesLegacyPipeAndSemicolonSeparatedData() {
        // Old format: "id|isChecked|text;;id|isChecked|text"
        val legacyData = "1|0|Socken;;2|1|Wein"
        val list = converters.toGiftIdeaList(legacyData)
        assertThat(list).hasSize(2)
        assertThat(list[0].id).isEqualTo("1")
        assertThat(list[0].text).isEqualTo("Socken")
        assertThat(list[0].isChecked).isFalse()
        assertThat(list[1].id).isEqualTo("2")
        assertThat(list[1].text).isEqualTo("Wein")
        assertThat(list[1].isChecked).isTrue()
    }

    @Test
    fun toGiftIdeaList_returnsEmptyListForNullOrBlank() {
        assertThat(converters.toGiftIdeaList(null)).isEmpty()
        assertThat(converters.toGiftIdeaList("")).isEmpty()
        assertThat(converters.toGiftIdeaList("   ")).isEmpty()
    }
}
