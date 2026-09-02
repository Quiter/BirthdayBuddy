package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.ui.model.SampleData
import org.junit.Before
import org.junit.Test

class FastScrollStateTest {

    private val density = Density(density = 2.0f, fontScale = 1.0f)
    private lateinit var listState: LazyListState
    private lateinit var fastScrollState: FastScrollState

    @Before
    fun setUp() {
        listState = LazyListState()
        fastScrollState = FastScrollState(listState = listState, density = density)
    }

    @Test
    fun buildSections_groupsConsecutiveMatchingLabels() {
        val sample1 = SampleData.sampleContacts[0].copy(id = "1", fullName = "Alice", monthName = "January")
        val sample2 = SampleData.sampleContacts[0].copy(id = "2", fullName = "Bob", monthName = "January")
        val sample3 = SampleData.sampleContacts[0].copy(id = "3", fullName = "Charlie", monthName = "February")
        val contacts = listOf(sample1, sample2, sample3)

        val sections = fastScrollState.buildSections(
            contacts = contacts,
            getLabel = { it.monthName },
            headerCount = 1
        )

        assertThat(sections).hasSize(2)
        assertThat(sections[0].label).isEqualTo("January")
        assertThat(sections[0].startIndex).isEqualTo(1)
        assertThat(sections[0].count).isEqualTo(2)

        assertThat(sections[1].label).isEqualTo("February")
        assertThat(sections[1].startIndex).isEqualTo(3)
        assertThat(sections[1].count).isEqualTo(1)
    }

    @Test
    fun buildSections_emptyContacts_returnsEmptyList() {
        val sections = fastScrollState.buildSections(
            contacts = emptyList(),
            getLabel = { it.monthName },
            headerCount = 2
        )

        assertThat(sections).isEmpty()
    }

    @Test
    fun calculateTotalHeight_sumsHeaderAndContactHeights() {
        val headerCount = 2
        val totalItems = 5

        val headerPx = fastScrollState.headerHeightPx
        val contactPx = fastScrollState.contactHeightPx

        val totalHeight = fastScrollState.calculateTotalHeight(totalItems, headerCount)
        assertThat(totalHeight).isEqualTo(2 * headerPx + 5 * contactPx)
    }

    @Test
    fun cumulativeHeight_calculatesCorrectPrefixSums() {
        val headerCount = 2
        val headerPx = fastScrollState.headerHeightPx
        val contactPx = fastScrollState.contactHeightPx

        assertThat(fastScrollState.cumulativeHeight(0, headerCount)).isEqualTo(0f)
        assertThat(fastScrollState.cumulativeHeight(1, headerCount)).isEqualTo(1 * headerPx)
        assertThat(fastScrollState.cumulativeHeight(2, headerCount)).isEqualTo(2 * headerPx)
        assertThat(fastScrollState.cumulativeHeight(3, headerCount)).isEqualTo(2 * headerPx + 1 * contactPx)
        assertThat(fastScrollState.cumulativeHeight(5, headerCount)).isEqualTo(2 * headerPx + 3 * contactPx)
    }

    @Test
    fun getItemHeightPx_returnsHeaderOrContactHeightBasedOnIndex() {
        val headerCount = 2
        val headerPx = fastScrollState.headerHeightPx
        val contactPx = fastScrollState.contactHeightPx

        assertThat(fastScrollState.getItemHeightPx(0, headerCount)).isEqualTo(headerPx)
        assertThat(fastScrollState.getItemHeightPx(1, headerCount)).isEqualTo(headerPx)
        assertThat(fastScrollState.getItemHeightPx(2, headerCount)).isEqualTo(contactPx)
        assertThat(fastScrollState.getItemHeightPx(5, headerCount)).isEqualTo(contactPx)
    }

    @Test
    fun calculateCurrentLabel_resolvesCorrectSection() {
        val sections = listOf(
            ScrollSection("A", startIndex = 0, count = 2),
            ScrollSection("B", startIndex = 2, count = 3),
            ScrollSection("C", startIndex = 5, count = 1)
        )

        // Track height 300dp
        val trackHeight = 300.dp

        // At start (0dp) -> Section A
        assertThat(fastScrollState.calculateCurrentLabel(0.dp, trackHeight, sections)).isEqualTo("A")

        // At ~40% (120dp) -> Section B (index 1)
        assertThat(fastScrollState.calculateCurrentLabel(120.dp, trackHeight, sections)).isEqualTo("B")

        // At ~80% (240dp) -> Section C (index 2)
        assertThat(fastScrollState.calculateCurrentLabel(240.dp, trackHeight, sections)).isEqualTo("C")
    }

    @Test
    fun resetOnContactsChange_resetsUserScrolledAndOffset() {
        fastScrollState.hasUserScrolled = true
        fastScrollState.dragOffsetPx = 150f

        fastScrollState.resetOnContactsChange()

        assertThat(fastScrollState.hasUserScrolled).isFalse()
        assertThat(fastScrollState.dragOffsetPx).isEqualTo(0f)
    }
}
