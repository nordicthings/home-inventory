package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ItemJpaEntityRepository : JpaRepository<ItemJpaEntity, UUID> {
    fun findByNormalizedName(normalizedName: String): ItemJpaEntity?

    fun existsByCategoryId(categoryId: UUID): Boolean
}
