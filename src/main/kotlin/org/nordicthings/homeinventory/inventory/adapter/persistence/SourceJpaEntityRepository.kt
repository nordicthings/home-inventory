package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SourceJpaEntityRepository : JpaRepository<SourceJpaEntity, UUID> {
    fun findByNormalizedName(normalizedName: String): SourceJpaEntity?
}
