package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ItemLocationQuantityJpaEntityRepository :
    JpaRepository<ItemLocationQuantityJpaEntity, ItemLocationQuantityJpaId> {
    fun findByIdItemId(itemId: UUID): List<ItemLocationQuantityJpaEntity>

    fun existsByIdLocationId(locationId: UUID): Boolean

    fun deleteByIdItemId(itemId: UUID)
}
