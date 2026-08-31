package com.heckmannch.birthdaybuddy.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.hasYear
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class SystemContactDataSourceTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var dataSource: SystemContactDataSource

    @Before
    fun setUp() {
        dataSource = SystemContactDataSource(context)
    }

    @Test
    fun `parseDate parses leap day without year to NO_YEAR_MARKER`() {
        val result = dataSource.parseDate("--02-29")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 2, 29))
        assertThat(result!!.hasYear).isFalse()
    }

    @Test
    fun `parseDate parses compact leap day without year to NO_YEAR_MARKER`() {
        val result = dataSource.parseDate("--0229")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 2, 29))
        assertThat(result!!.hasYear).isFalse()
    }

    @Test
    fun `parseDate parses standard date without year to NO_YEAR_MARKER`() {
        val result = dataSource.parseDate("--05-15")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(NO_YEAR_MARKER, 5, 15))
        assertThat(result!!.hasYear).isFalse()
    }

    @Test
    fun `parseDate parses ISO date with leap year correctly`() {
        val result = dataSource.parseDate("2024-02-29")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(result!!.hasYear).isTrue()
    }

    @Test
    fun `parseDate parses standard ISO date correctly`() {
        val result = dataSource.parseDate("1990-05-15")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(1990, 5, 15))
        assertThat(result!!.hasYear).isTrue()
    }

    @Test
    fun `parseDate parses yyyyMMdd date format correctly`() {
        val result = dataSource.parseDate("19900515")

        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(LocalDate.of(1990, 5, 15))
        assertThat(result!!.hasYear).isTrue()
    }

    @Test
    fun `parseDate returns null for invalid date strings`() {
        assertThat(dataSource.parseDate(null)).isNull()
        assertThat(dataSource.parseDate("")).isNull()
        assertThat(dataSource.parseDate("invalid-date")).isNull()
        assertThat(dataSource.parseDate("--02-30")).isNull()
        assertThat(dataSource.parseDate("--13-01")).isNull()
    }
}
