package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.GiftIdea
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [ContactDbMapper] verifying database-to-domain and domain-to-database layer mapping.
 */
class ContactDbMapperTest {

    private val mapper = ContactDbMapper()

    @Test
    fun toDomain_mapsAll15FieldsCorrectly() {
        val entity = ContactEntity(
            localId = 42L,
            contactId = "123",
            lookupKey = "lookup_abc",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 20),
            anniversary = LocalDate.of(2020, 6, 15),
            nameDay = LocalDate.of(1990, 10, 10),
            imageUri = "content://image/uri",
            phoneNumber = "+49123456789",
            isFavorite = true,
            hasWhatsApp = true,
            hasSignal = false,
            labels = listOf("Freunde", "Familie"),
            giftIdeas = listOf(
                GiftIdea(id = "1", text = "Buch", isChecked = false),
                GiftIdea(id = "2", text = "Gutschein", isChecked = true)
            ),
            spouseLookupKey = "spouse_lookup_xyz"
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.localId).isEqualTo(42L)
        assertThat(domain.contactId).isEqualTo("123")
        assertThat(domain.lookupKey).isEqualTo("lookup_abc")
        assertThat(domain.fullName).isEqualTo("Max Mustermann")
        assertThat(domain.birthday).isEqualTo(LocalDate.of(1990, 5, 20))
        assertThat(domain.anniversary).isEqualTo(LocalDate.of(2020, 6, 15))
        assertThat(domain.nameDay).isEqualTo(LocalDate.of(1990, 10, 10))
        assertThat(domain.imageUri).isEqualTo("content://image/uri")
        assertThat(domain.phoneNumber).isEqualTo("+49123456789")
        assertThat(domain.isFavorite).isTrue()
        assertThat(domain.hasWhatsApp).isTrue()
        assertThat(domain.hasSignal).isFalse()
        assertThat(domain.labels).containsExactly("Freunde", "Familie").inOrder()
        assertThat(domain.giftIdeas).hasSize(2)
        assertThat(domain.giftIdeas[0].id).isEqualTo("1")
        assertThat(domain.giftIdeas[0].text).isEqualTo("Buch")
        assertThat(domain.giftIdeas[0].isChecked).isFalse()
        assertThat(domain.giftIdeas[1].id).isEqualTo("2")
        assertThat(domain.giftIdeas[1].text).isEqualTo("Gutschein")
        assertThat(domain.giftIdeas[1].isChecked).isTrue()
        assertThat(domain.spouseLookupKey).isEqualTo("spouse_lookup_xyz")
    }

    @Test
    fun toDomain_handlesNullAndDefaultFieldsCorrectly() {
        val entity = ContactEntity(
            localId = 1L,
            contactId = "c1",
            lookupKey = "k1",
            fullName = "Erika Musterfrau",
            birthday = null,
            anniversary = null,
            nameDay = null,
            imageUri = null,
            phoneNumber = null,
            isFavorite = false,
            hasWhatsApp = false,
            hasSignal = false,
            labels = emptyList(),
            giftIdeas = emptyList(),
            spouseLookupKey = null
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.localId).isEqualTo(1L)
        assertThat(domain.contactId).isEqualTo("c1")
        assertThat(domain.lookupKey).isEqualTo("k1")
        assertThat(domain.fullName).isEqualTo("Erika Musterfrau")
        assertThat(domain.birthday).isNull()
        assertThat(domain.anniversary).isNull()
        assertThat(domain.nameDay).isNull()
        assertThat(domain.imageUri).isNull()
        assertThat(domain.phoneNumber).isNull()
        assertThat(domain.isFavorite).isFalse()
        assertThat(domain.hasWhatsApp).isFalse()
        assertThat(domain.hasSignal).isFalse()
        assertThat(domain.labels).isEmpty()
        assertThat(domain.giftIdeas).isEmpty()
        assertThat(domain.spouseLookupKey).isNull()
    }

    @Test
    fun toEntity_mapsAll15FieldsCorrectly() {
        val domain = Contact(
            localId = 42L,
            contactId = "123",
            lookupKey = "lookup_abc",
            fullName = "Max Mustermann",
            birthday = LocalDate.of(1990, 5, 20),
            anniversary = LocalDate.of(2020, 6, 15),
            nameDay = LocalDate.of(1990, 10, 10),
            imageUri = "content://image/uri",
            phoneNumber = "+49123456789",
            isFavorite = true,
            hasWhatsApp = true,
            hasSignal = false,
            labels = listOf("Freunde", "Familie"),
            giftIdeas = listOf(
                GiftIdea(id = "1", text = "Buch", isChecked = false),
                GiftIdea(id = "2", text = "Gutschein", isChecked = true)
            ),
            spouseLookupKey = "spouse_lookup_xyz"
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.localId).isEqualTo(42L)
        assertThat(entity.contactId).isEqualTo("123")
        assertThat(entity.lookupKey).isEqualTo("lookup_abc")
        assertThat(entity.fullName).isEqualTo("Max Mustermann")
        assertThat(entity.birthday).isEqualTo(LocalDate.of(1990, 5, 20))
        assertThat(entity.anniversary).isEqualTo(LocalDate.of(2020, 6, 15))
        assertThat(entity.nameDay).isEqualTo(LocalDate.of(1990, 10, 10))
        assertThat(entity.imageUri).isEqualTo("content://image/uri")
        assertThat(entity.phoneNumber).isEqualTo("+49123456789")
        assertThat(entity.isFavorite).isTrue()
        assertThat(entity.hasWhatsApp).isTrue()
        assertThat(entity.hasSignal).isFalse()
        assertThat(entity.labels).containsExactly("Freunde", "Familie").inOrder()
        assertThat(entity.giftIdeas).hasSize(2)
        assertThat(entity.giftIdeas[0].id).isEqualTo("1")
        assertThat(entity.giftIdeas[0].text).isEqualTo("Buch")
        assertThat(entity.giftIdeas[0].isChecked).isFalse()
        assertThat(entity.giftIdeas[1].id).isEqualTo("2")
        assertThat(entity.giftIdeas[1].text).isEqualTo("Gutschein")
        assertThat(entity.giftIdeas[1].isChecked).isTrue()
        assertThat(entity.spouseLookupKey).isEqualTo("spouse_lookup_xyz")
    }

    @Test
    fun toEntity_handlesNullAndDefaultFieldsCorrectly() {
        val domain = Contact(
            localId = 1L,
            contactId = "c1",
            lookupKey = "k1",
            fullName = "Erika Musterfrau",
            birthday = null,
            anniversary = null,
            nameDay = null,
            imageUri = null,
            phoneNumber = null,
            isFavorite = false,
            hasWhatsApp = false,
            hasSignal = false,
            labels = emptyList(),
            giftIdeas = emptyList(),
            spouseLookupKey = null
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.localId).isEqualTo(1L)
        assertThat(entity.contactId).isEqualTo("c1")
        assertThat(entity.lookupKey).isEqualTo("k1")
        assertThat(entity.fullName).isEqualTo("Erika Musterfrau")
        assertThat(entity.birthday).isNull()
        assertThat(entity.anniversary).isNull()
        assertThat(entity.nameDay).isNull()
        assertThat(entity.imageUri).isNull()
        assertThat(entity.phoneNumber).isNull()
        assertThat(entity.isFavorite).isFalse()
        assertThat(entity.hasWhatsApp).isFalse()
        assertThat(entity.hasSignal).isFalse()
        assertThat(entity.labels).isEmpty()
        assertThat(entity.giftIdeas).isEmpty()
        assertThat(entity.spouseLookupKey).isNull()
    }

    @Test
    fun toDomainList_mapsListOfEntitiesCorrectly() {
        val entities = listOf(
            ContactEntity(localId = 1L, contactId = "c1", lookupKey = "k1", fullName = "Contact 1"),
            ContactEntity(localId = 2L, contactId = "c2", lookupKey = "k2", fullName = "Contact 2")
        )

        val result = mapper.toDomainList(entities)

        assertThat(result).hasSize(2)
        assertThat(result[0].contactId).isEqualTo("c1")
        assertThat(result[1].contactId).isEqualTo("c2")
    }

    @Test
    fun toDomainList_handlesEmptyList() {
        val result = mapper.toDomainList(emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun toEntityList_mapsListOfDomainsCorrectly() {
        val domains = listOf(
            Contact(localId = 1L, contactId = "c1", lookupKey = "k1", fullName = "Contact 1"),
            Contact(localId = 2L, contactId = "c2", lookupKey = "k2", fullName = "Contact 2")
        )

        val result = mapper.toEntityList(domains)

        assertThat(result).hasSize(2)
        assertThat(result[0].contactId).isEqualTo("c1")
        assertThat(result[1].contactId).isEqualTo("c2")
    }

    @Test
    fun toEntityList_handlesEmptyList() {
        val result = mapper.toEntityList(emptyList())

        assertThat(result).isEmpty()
    }
}
