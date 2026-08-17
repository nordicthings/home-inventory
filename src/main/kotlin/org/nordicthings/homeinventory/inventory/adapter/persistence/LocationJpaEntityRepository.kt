package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface LocationJpaEntityRepository : JpaRepository<LocationJpaEntity, UUID> {
    fun findByNormalizedName(normalizedName: String): LocationJpaEntity?
}
