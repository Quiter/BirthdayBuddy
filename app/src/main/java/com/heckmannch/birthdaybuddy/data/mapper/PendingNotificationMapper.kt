package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.PendingNotificationEntity
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper to convert between [PendingNotificationEntity] and [PendingNotification].
 */
@Reusable
class PendingNotificationMapper @Inject constructor() {

    fun toDomain(entity: PendingNotificationEntity): PendingNotification {
        return PendingNotification(
            id = entity.id,
            contactLookupKeys = entity.contactLookupKeys,
            daysBefore = entity.daysBefore,
            year = entity.year,
            isDone = entity.isDone,
            dismissCount = entity.dismissCount
        )
    }

    fun toDomainList(entities: List<PendingNotificationEntity>): List<PendingNotification> = entities.map(::toDomain)

    fun toEntity(domain: PendingNotification): PendingNotificationEntity {
        return PendingNotificationEntity(
            id = domain.id,
            contactLookupKeys = domain.contactLookupKeys,
            daysBefore = domain.daysBefore,
            year = domain.year,
            isDone = domain.isDone,
            dismissCount = domain.dismissCount
        )
    }

    fun toEntityList(domains: List<PendingNotification>): List<PendingNotificationEntity> = domains.map(::toEntity)
}
