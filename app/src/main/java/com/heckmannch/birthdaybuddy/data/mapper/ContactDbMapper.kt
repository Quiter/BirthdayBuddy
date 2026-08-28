package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.domain.model.Contact
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper zur Konvertierung zwischen der Datenbank-Entität [ContactEntity] und dem Domain-Modell [Contact].
 */
@Reusable
class ContactDbMapper @Inject constructor() {

    fun toDomain(entity: ContactEntity): Contact {
        return Contact(
            localId = entity.localId,
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
            giftIdeas = entity.giftIdeas,
            spouseLookupKey = entity.spouseLookupKey
        )
    }

    fun toEntity(domain: Contact): ContactEntity {
        return ContactEntity(
            localId = domain.localId,
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
            giftIdeas = domain.giftIdeas,
            spouseLookupKey = domain.spouseLookupKey
        )
    }
}
