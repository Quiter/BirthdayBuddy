package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.data.local.GiftIdeaConverters
import com.heckmannch.birthdaybuddy.domain.model.Contact
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper for converting between database entity [ContactEntity] and domain model [Contact].
 */
@Reusable
class ContactDbMapper @Inject constructor(
    private val giftIdeaConverters: GiftIdeaConverters
) {

    constructor() : this(GiftIdeaConverters())

    /**
     * Converts a database [ContactEntity] into a domain [Contact] model.
     */
    fun toDomain(entity: ContactEntity): Contact {
        return Contact(
            contactId = entity.contactId,
            lookupKey = entity.lookupKey,
            fullName = entity.fullName,
            birthday = entity.birthday,
            anniversary = entity.anniversary,
            nameDay = entity.nameDay,
            imageUri = entity.imageUri,
            phoneNumber = entity.phoneNumber,
            isFavorite = entity.isFavorite,
            hasWhatsApp = entity.hasWhatsApp,
            hasSignal = entity.hasSignal,
            labels = entity.labels,
            giftIdeas = giftIdeaConverters.toGiftIdeaList(entity.giftIdeasJson),
            spouseLookupKey = entity.spouseLookupKey
        )
    }

    /**
     * Converts a list of database [ContactEntity] objects into domain [Contact] models.
     */
    fun toDomainList(entities: List<ContactEntity>): List<Contact> = entities.map(::toDomain)

    /**
     * Converts a domain [Contact] model into a database [ContactEntity].
     */
    fun toEntity(domain: Contact, localId: Long = 0): ContactEntity {
        return ContactEntity(
            localId = localId,
            contactId = domain.contactId,
            lookupKey = domain.lookupKey,
            fullName = domain.fullName,
            birthday = domain.birthday,
            anniversary = domain.anniversary,
            nameDay = domain.nameDay,
            imageUri = domain.imageUri,
            phoneNumber = domain.phoneNumber,
            isFavorite = domain.isFavorite,
            hasWhatsApp = domain.hasWhatsApp,
            hasSignal = domain.hasSignal,
            labels = domain.labels,
            giftIdeasJson = giftIdeaConverters.fromGiftIdeaList(domain.giftIdeas),
            spouseLookupKey = domain.spouseLookupKey
        )
    }

    /**
     * Converts a list of domain [Contact] models into database [ContactEntity] objects.
     */
    fun toEntityList(domains: List<Contact>): List<ContactEntity> = domains.map { toEntity(it) }
}
