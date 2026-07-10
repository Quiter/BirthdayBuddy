package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [ContactMapper] verifying database-to-domain layer mapping.
 */
class ContactMapperTest {

    private val mapper = ContactMapper()

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val entity = ContactEntity(
            localId = 42L,
            contactId = "123",
            lookupKey = "abc",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 20),
            anniversary = LocalDate.of(2020, 6, 15),
            nameDay = LocalDate.of(1990, 10, 10),
            imageUri = "content://image",
            phoneNumber = "0123456789",
            hasWhatsApp = true,
            hasSignal = false,
            labels = listOf("Freunde"),
            giftIdeas = listOf(GiftIdea(text = "Buch")),
            spouseLookupKey = "spouse_abc"
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.localId).isEqualTo(42L)
        assertThat(domain.contactId).isEqualTo("123")
        assertThat(domain.lookupKey).isEqualTo("abc")
        assertThat(domain.fullName).isEqualTo("Max Mustermann")
        assertThat(domain.birthday).isEqualTo(LocalDate.of(1990, 5, 20))
        assertThat(domain.anniversary).isEqualTo(LocalDate.of(2020, 6, 15))
        assertThat(domain.nameDay).isEqualTo(LocalDate.of(1990, 10, 10))
        assertThat(domain.imageUri).isEqualTo("content://image")
        assertThat(domain.phoneNumber).isEqualTo("0123456789")
        assertThat(domain.hasWhatsApp).isTrue()
        assertThat(domain.hasSignal).isFalse()
        assertThat(domain.labels).containsExactly("Freunde")
        assertThat(domain.giftIdeas.first().text).isEqualTo("Buch")
        assertThat(domain.spouseLookupKey).isEqualTo("spouse_abc")
    }
}
