package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AcquisitionInvoiceJpaEntityRepository : JpaRepository<AcquisitionInvoiceJpaEntity, UUID> {
    fun findByAcquisitionId(acquisitionId: UUID): AcquisitionInvoiceJpaEntity?

    fun existsByAcquisitionIdIn(acquisitionIds: Collection<UUID>): Boolean

    fun deleteByAcquisitionId(acquisitionId: UUID)
}
