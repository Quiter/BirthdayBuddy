package com.heckmannch.birthdaybuddy.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StringUtilsTest {

    @Test
    fun `mergeNames combines names with matching last names correctly`() {
        val merged = mergeNames("Max Mustermann", "Erika Mustermann")
        assertThat(merged).isEqualTo("Max & Erika Mustermann")
    }

    @Test
    fun `mergeNames combines names with different last names correctly`() {
        val merged = mergeNames("Max Schmidt", "Erika Mustermann")
        assertThat(merged).isEqualTo("Max Schmidt & Erika Mustermann")
    }

    @Test
    fun `mergeNames combines single names correctly`() {
        val merged = mergeNames("Max", "Erika")
        assertThat(merged).isEqualTo("Max & Erika")
    }

    @Test
    fun `getInitials returns question mark for empty or blank string`() {
        assertThat("".getInitials()).isEqualTo("?")
        assertThat("   ".getInitials()).isEqualTo("?")
    }

    @Test
    fun `getInitials returns first letter uppercase for single word`() {
        assertThat("max".getInitials()).isEqualTo("M")
        assertThat("Erika".getInitials()).isEqualTo("E")
    }

    @Test
    fun `getInitials returns first and last initials for multi-word names`() {
        assertThat("Max Mustermann".getInitials()).isEqualTo("MM")
        assertThat("Johann Sebastian Bach".getInitials()).isEqualTo("JB")
        assertThat(" anna maria schmidt ".getInitials()).isEqualTo("AS")
    }
}
