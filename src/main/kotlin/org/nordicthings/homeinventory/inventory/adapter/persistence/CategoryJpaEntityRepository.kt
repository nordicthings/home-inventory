package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryJpaEntityRepository : JpaRepository<CategoryJpaEntity, UUID> {
    fun findByNormalizedName(normalizedName: String): CategoryJpaEntity?
}
