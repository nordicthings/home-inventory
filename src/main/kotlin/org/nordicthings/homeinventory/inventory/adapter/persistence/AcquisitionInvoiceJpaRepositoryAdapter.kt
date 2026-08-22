package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.application.port.outbound.AcquisitionInvoiceRepository
import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoice
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId
import org.springframework.stereotype.Repository

@Repository
class AcquisitionInvoiceJpaRepositoryAdapter(
    private val repository: AcquisitionInvoiceJpaEntityRepository,
) : AcquisitionInvoiceRepository {
    override fun findByAcquisitionId(acquisitionId: ItemSourceId): AcquisitionInvoice? =
        repository.findByAcquisitionId(acquisitionId.value)?.toDomain()

    override fun save(invoice: AcquisitionInvoice): AcquisitionInvoice =
        repository.save(invoice.toJpaEntity()).toDomain()

    override fun deleteByAcquisitionId(acquisitionId: ItemSourceId) {
        repository.deleteByAcquisitionId(acquisitionId.value)
    }
}
