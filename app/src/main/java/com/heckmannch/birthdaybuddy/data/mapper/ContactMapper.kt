package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.ContactEntity
import com.heckmannch.birthdaybuddy.domain.model.Contact
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper for converting database entities ([ContactEntity]) to domain models ([Contact]).
 * Keeps the data layer fully decoupled from the UI representation.
 */
@Reusable
class ContactMapper @Inject constructor() {

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
            hasWhatsApp = entity.hasWhatsApp,
            hasSignal = entity.hasSignal,
            labels = entity.labels,
            giftIdeas = entity.giftIdeas,
            spouseLookupKey = entity.spouseLookupKey
        )
    }
}
