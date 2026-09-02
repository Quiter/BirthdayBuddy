package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.NotificationRuleEntity
import com.heckmannch.birthdaybuddy.domain.model.NotificationRule
import org.junit.Test

/**
 * Unit tests for [NotificationRuleMapper].
 */
class NotificationRuleMapperTest {

    private val mapper = NotificationRuleMapper()

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val entity = NotificationRuleEntity(
            id = 1,
            daysBefore = 3,
            hour = 9,
            minute = 30
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.id).isEqualTo(1)
        assertThat(domain.daysBefore).isEqualTo(3)
        assertThat(domain.hour).isEqualTo(9)
        assertThat(domain.minute).isEqualTo(30)
    }

    @Test
    fun toEntity_mapsAllFieldsCorrectly() {
        val domain = NotificationRule(
            id = 2,
            daysBefore = 0,
            hour = 10,
            minute = 0
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.id).isEqualTo(2)
        assertThat(entity.daysBefore).isEqualTo(0)
        assertThat(entity.hour).isEqualTo(10)
        assertThat(entity.minute).isEqualTo(0)
    }

    @Test
    fun toDomainList_mapsListOfEntitiesCorrectly() {
        val entities = listOf(
            NotificationRuleEntity(id = 1, daysBefore = 0, hour = 9, minute = 0),
            NotificationRuleEntity(id = 2, daysBefore = 1, hour = 18, minute = 30)
        )

        val domainList = mapper.toDomainList(entities)

        assertThat(domainList).hasSize(2)
        assertThat(domainList[0].id).isEqualTo(1)
        assertThat(domainList[0].daysBefore).isEqualTo(0)
        assertThat(domainList[0].hour).isEqualTo(9)
        assertThat(domainList[0].minute).isEqualTo(0)
        assertThat(domainList[1].id).isEqualTo(2)
        assertThat(domainList[1].daysBefore).isEqualTo(1)
        assertThat(domainList[1].hour).isEqualTo(18)
        assertThat(domainList[1].minute).isEqualTo(30)
    }

    @Test
    fun toDomainList_handlesEmptyList() {
        val domainList = mapper.toDomainList(emptyList())

        assertThat(domainList).isEmpty()
    }

    @Test
    fun toEntityList_mapsListOfDomainsCorrectly() {
        val domains = listOf(
            NotificationRule(id = 1, daysBefore = 0, hour = 9, minute = 0),
            NotificationRule(id = 2, daysBefore = 1, hour = 18, minute = 30)
        )

        val entityList = mapper.toEntityList(domains)

        assertThat(entityList).hasSize(2)
        assertThat(entityList[0].id).isEqualTo(1)
        assertThat(entityList[0].daysBefore).isEqualTo(0)
        assertThat(entityList[0].hour).isEqualTo(9)
        assertThat(entityList[0].minute).isEqualTo(0)
        assertThat(entityList[1].id).isEqualTo(2)
        assertThat(entityList[1].daysBefore).isEqualTo(1)
        assertThat(entityList[1].hour).isEqualTo(18)
        assertThat(entityList[1].minute).isEqualTo(30)
    }

    @Test
    fun toEntityList_handlesEmptyList() {
        val entityList = mapper.toEntityList(emptyList())

        assertThat(entityList).isEmpty()
    }
}
