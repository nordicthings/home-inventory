package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ItemSourceJpaEntityRepository : JpaRepository<ItemSourceJpaEntity, UUID> {
    fun findByItemId(itemId: UUID): List<ItemSourceJpaEntity>

    fun existsBySourceId(sourceId: UUID): Boolean

    fun deleteByItemId(itemId: UUID)
}
