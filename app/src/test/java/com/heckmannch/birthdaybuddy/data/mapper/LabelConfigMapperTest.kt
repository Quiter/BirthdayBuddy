package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.LabelConfigEntity
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import org.junit.Test

/**
 * Unit tests for [LabelConfigMapper].
 */
class LabelConfigMapperTest {

    private val mapper = LabelConfigMapper()

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val entity = LabelConfigEntity(
            name = "VIP",
            isHiddenFromFilter = true,
            isIgnored = false,
            isSystem = true,
            notificationsEnabled = true,
            showInWidget = false
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.name).isEqualTo("VIP")
        assertThat(domain.isHiddenFromFilter).isTrue()
        assertThat(domain.isIgnored).isFalse()
        assertThat(domain.isSystem).isTrue()
        assertThat(domain.notificationsEnabled).isTrue()
        assertThat(domain.showInWidget).isFalse()
    }

    @Test
    fun toEntity_mapsAllFieldsCorrectly() {
        val domain = LabelConfig(
            name = "Colleagues",
            isHiddenFromFilter = false,
            isIgnored = true,
            isSystem = false,
            notificationsEnabled = false,
            showInWidget = true
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.name).isEqualTo("Colleagues")
        assertThat(entity.isHiddenFromFilter).isFalse()
        assertThat(entity.isIgnored).isTrue()
        assertThat(entity.isSystem).isFalse()
        assertThat(entity.notificationsEnabled).isFalse()
        assertThat(entity.showInWidget).isTrue()
    }

    @Test
    fun toDomainList_mapsListOfEntitiesCorrectly() {
        val entities = listOf(
            LabelConfigEntity(name = "Family", isHiddenFromFilter = false, isIgnored = false, isSystem = true, notificationsEnabled = true, showInWidget = true),
            LabelConfigEntity(name = "Work", isHiddenFromFilter = true, isIgnored = true, isSystem = false, notificationsEnabled = false, showInWidget = false)
        )

        val domainList = mapper.toDomainList(entities)

        assertThat(domainList).hasSize(2)
        assertThat(domainList[0].name).isEqualTo("Family")
        assertThat(domainList[0].isSystem).isTrue()
        assertThat(domainList[1].name).isEqualTo("Work")
        assertThat(domainList[1].isIgnored).isTrue()
    }

    @Test
    fun toDomainList_handlesEmptyList() {
        val domainList = mapper.toDomainList(emptyList())

        assertThat(domainList).isEmpty()
    }

    @Test
    fun toEntityList_mapsListOfDomainsCorrectly() {
        val domains = listOf(
            LabelConfig(name = "Family", isHiddenFromFilter = false, isIgnored = false, isSystem = true, notificationsEnabled = true, showInWidget = true),
            LabelConfig(name = "Work", isHiddenFromFilter = true, isIgnored = true, isSystem = false, notificationsEnabled = false, showInWidget = false)
        )

        val entityList = mapper.toEntityList(domains)

        assertThat(entityList).hasSize(2)
        assertThat(entityList[0].name).isEqualTo("Family")
        assertThat(entityList[0].isSystem).isTrue()
        assertThat(entityList[1].name).isEqualTo("Work")
        assertThat(entityList[1].isIgnored).isTrue()
    }

    @Test
    fun toEntityList_handlesEmptyList() {
        val entityList = mapper.toEntityList(emptyList())

        assertThat(entityList).isEmpty()
    }
}
