package com.heckmannch.birthdaybuddy.data.mapper

import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import dagger.Reusable
import javax.inject.Inject

/**
 * Mapper to convert between [LabelConfigEntity] and [LabelConfig].
 */
@Reusable
class LabelConfigMapper @Inject constructor() {

    fun toDomain(entity: LabelConfigEntity): LabelConfig {
        return LabelConfig(
            name = entity.name,
            isHiddenFromFilter = entity.isHiddenFromFilter,
            isIgnored = entity.isIgnored,
            isSystem = entity.isSystem,
            notificationsEnabled = entity.notificationsEnabled,
            showInWidget = entity.showInWidget
        )
    }

    fun toDomainList(entities: List<LabelConfigEntity>): List<LabelConfig> = entities.map(::toDomain)

    fun toEntity(domain: LabelConfig): LabelConfigEntity {
        return LabelConfigEntity(
            name = domain.name,
            isHiddenFromFilter = domain.isHiddenFromFilter,
            isIgnored = domain.isIgnored,
            isSystem = domain.isSystem,
            notificationsEnabled = domain.notificationsEnabled,
            showInWidget = domain.showInWidget
        )
    }

    fun toEntityList(domains: List<LabelConfig>): List<LabelConfigEntity> = domains.map(::toEntity)
}
