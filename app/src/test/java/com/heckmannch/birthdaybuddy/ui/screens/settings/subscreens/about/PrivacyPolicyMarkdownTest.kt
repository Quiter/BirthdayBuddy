package com.heckmannch.birthdaybuddy.ui.screens.settings.subscreens.about

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrivacyPolicyMarkdownTest {

    @Test
    fun parseMarkdownBlocks_parsesHeadingsBulletsDividersAndInlineFormatting() {
        val markdown = """
            # Datenschutzerklärung

            ## Allgemeines
            * **Lokal**: Daten bleiben auf dem Gerät.
            ---
            Siehe [Website](https://example.com) für **Details**.
            ***
        """.trimIndent()

        val blocks = parseMarkdownBlocks(markdown, linkColor = Color.Blue)

        assertThat(blocks).hasSize(6)
        assertThat(blocks[0]).isEqualTo(MarkdownBlock.Heading1("Datenschutzerklärung"))
        assertThat(blocks[1]).isEqualTo(MarkdownBlock.Heading2("Allgemeines"))

        val bullet = blocks[2] as MarkdownBlock.BulletItem
        assertThat(bullet.text.text).isEqualTo("Lokal: Daten bleiben auf dem Gerät.")
        assertThat(bullet.text.spanStyles.any { it.item.fontWeight == FontWeight.Bold }).isTrue()

        assertThat(blocks[3]).isEqualTo(MarkdownBlock.Divider)

        val paragraph = blocks[4] as MarkdownBlock.Paragraph
        assertThat(paragraph.text.text).isEqualTo("Siehe Website für Details.")
        val links = paragraph.text.getLinkAnnotations(0, paragraph.text.length)
        assertThat(links).hasSize(1)
        assertThat((links.first().item as LinkAnnotation.Url).url).isEqualTo("https://example.com")

        assertThat(blocks[5]).isEqualTo(MarkdownBlock.Divider)
    }
}
