package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.NotificationRuleEntity
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper to convert between [NotificationRuleEntity] and [NotificationRule].
 */
@Reusable
class NotificationRuleMapper @Inject constructor() {

    fun toDomain(entity: NotificationRuleEntity): NotificationRule {
        return NotificationRule(
            id = entity.id,
            daysBefore = entity.daysBefore,
            hour = entity.hour,
            minute = entity.minute
        )
    }

    fun toDomainList(entities: List<NotificationRuleEntity>): List<NotificationRule> = entities.map(::toDomain)

    fun toEntity(domain: NotificationRule): NotificationRuleEntity {
        return NotificationRuleEntity(
            id = domain.id,
            daysBefore = domain.daysBefore,
            hour = domain.hour,
            minute = domain.minute
        )
    }

    fun toEntityList(domains: List<NotificationRule>): List<NotificationRuleEntity> = domains.map(::toEntity)
}
