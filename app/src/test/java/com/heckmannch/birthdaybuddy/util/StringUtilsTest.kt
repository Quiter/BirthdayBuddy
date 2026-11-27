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
    fun `getInitials returns empty string without special characters for empty or blank string`() {
        assertThat("".getInitials()).isEqualTo("")
        assertThat("   ".getInitials()).isEqualTo("")
        assertThat("? ! #".getInitials()).isEqualTo("")
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

    @Test
    fun `getInitials ignores text inside parentheses and extracts letters from remaining name`() {
        assertThat("Max Mustermann (Muster)".getInitials()).isEqualTo("MM")
        assertThat("Erika (geb. Schmidt) Mustermann".getInitials()).isEqualTo("EM")
        assertThat("Mustermann (Max)".getInitials()).isEqualTo("M")
        assertThat("(Muster) Hans".getInitials()).isEqualTo("H")
        assertThat("(Dr.) Hans Müller".getInitials()).isEqualTo("HM")
    }

    @Test
    fun `getInitials strips special characters and returns only letters`() {
        assertThat("Hans-Peter Müller".getInitials()).isEqualTo("HM")
        assertThat("Max & Erika".getInitials()).isEqualTo("ME")
        assertThat("O'Connor".getInitials()).isEqualTo("O")
    }
}
