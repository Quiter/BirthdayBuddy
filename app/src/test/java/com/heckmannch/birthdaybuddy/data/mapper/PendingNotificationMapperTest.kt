package com.heckmannch.birthdaybuddy.data.mapper

import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.data.local.PendingNotificationEntity
import com.heckmannch.birthdaybuddy.domain.model.PendingNotification
import org.junit.Test

/**
 * Unit tests for [PendingNotificationMapper].
 */
class PendingNotificationMapperTest {

    private val mapper = PendingNotificationMapper()

    @Test
    fun toDomain_mapsAllFieldsCorrectly() {
        val entity = PendingNotificationEntity(
            id = 10,
            contactLookupKeys = listOf("key_1", "key_2"),
            daysBefore = 1,
            year = 2026,
            isDone = true,
            dismissCount = 2
        )

        val domain = mapper.toDomain(entity)

        assertThat(domain.id).isEqualTo(10)
        assertThat(domain.contactLookupKeys).containsExactly("key_1", "key_2").inOrder()
        assertThat(domain.daysBefore).isEqualTo(1)
        assertThat(domain.year).isEqualTo(2026)
        assertThat(domain.isDone).isTrue()
        assertThat(domain.dismissCount).isEqualTo(2)
    }

    @Test
    fun toEntity_mapsAllFieldsCorrectly() {
        val domain = PendingNotification(
            id = 20,
            contactLookupKeys = listOf("key_a", "key_b", "key_c"),
            daysBefore = 0,
            year = 2025,
            isDone = false,
            dismissCount = 0
        )

        val entity = mapper.toEntity(domain)

        assertThat(entity.id).isEqualTo(20)
        assertThat(entity.contactLookupKeys).containsExactly("key_a", "key_b", "key_c").inOrder()
        assertThat(entity.daysBefore).isEqualTo(0)
        assertThat(entity.year).isEqualTo(2025)
        assertThat(entity.isDone).isFalse()
        assertThat(entity.dismissCount).isEqualTo(0)
    }

    @Test
    fun toDomainList_mapsListOfEntitiesCorrectly() {
        val entities = listOf(
            PendingNotificationEntity(
                id = 1,
                contactLookupKeys = listOf("k1"),
                daysBefore = 0,
                year = 2026,
                isDone = false,
                dismissCount = 0
            ),
            PendingNotificationEntity(
                id = 2,
                contactLookupKeys = listOf("k2", "k3"),
                daysBefore = 7,
                year = 2026,
                isDone = true,
                dismissCount = 1
            )
        )

        val domainList = mapper.toDomainList(entities)

        assertThat(domainList).hasSize(2)
        assertThat(domainList[0].id).isEqualTo(1)
        assertThat(domainList[0].contactLookupKeys).containsExactly("k1")
        assertThat(domainList[0].daysBefore).isEqualTo(0)
        assertThat(domainList[0].year).isEqualTo(2026)
        assertThat(domainList[0].isDone).isFalse()
        assertThat(domainList[0].dismissCount).isEqualTo(0)

        assertThat(domainList[1].id).isEqualTo(2)
        assertThat(domainList[1].contactLookupKeys).containsExactly("k2", "k3").inOrder()
        assertThat(domainList[1].daysBefore).isEqualTo(7)
        assertThat(domainList[1].year).isEqualTo(2026)
        assertThat(domainList[1].isDone).isTrue()
        assertThat(domainList[1].dismissCount).isEqualTo(1)
    }

    @Test
    fun toDomainList_handlesEmptyList() {
        val domainList = mapper.toDomainList(emptyList())

        assertThat(domainList).isEmpty()
    }

    @Test
    fun toEntityList_mapsListOfDomainsCorrectly() {
        val domains = listOf(
            PendingNotification(
                id = 1,
                contactLookupKeys = listOf("k1"),
                daysBefore = 0,
                year = 2026,
                isDone = false,
                dismissCount = 0
            ),
            PendingNotification(
                id = 2,
                contactLookupKeys = listOf("k2", "k3"),
                daysBefore = 7,
                year = 2026,
                isDone = true,
                dismissCount = 1
            )
        )

        val entityList = mapper.toEntityList(domains)

        assertThat(entityList).hasSize(2)
        assertThat(entityList[0].id).isEqualTo(1)
        assertThat(entityList[0].contactLookupKeys).containsExactly("k1")
        assertThat(entityList[0].daysBefore).isEqualTo(0)
        assertThat(entityList[0].year).isEqualTo(2026)
        assertThat(entityList[0].isDone).isFalse()
        assertThat(entityList[0].dismissCount).isEqualTo(0)

        assertThat(entityList[1].id).isEqualTo(2)
        assertThat(entityList[1].contactLookupKeys).containsExactly("k2", "k3").inOrder()
        assertThat(entityList[1].daysBefore).isEqualTo(7)
        assertThat(entityList[1].year).isEqualTo(2026)
        assertThat(entityList[1].isDone).isTrue()
        assertThat(entityList[1].dismissCount).isEqualTo(1)
    }

    @Test
    fun toEntityList_handlesEmptyList() {
        val entityList = mapper.toEntityList(emptyList())

        assertThat(entityList).isEmpty()
    }
}
