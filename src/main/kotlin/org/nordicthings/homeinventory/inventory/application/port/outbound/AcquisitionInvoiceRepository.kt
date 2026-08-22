package org.nordicthings.homeinventory.inventory.application.port.outbound

import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoice
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId

interface AcquisitionInvoiceRepository {
    fun findByAcquisitionId(acquisitionId: ItemSourceId): AcquisitionInvoice?

    fun save(invoice: AcquisitionInvoice): AcquisitionInvoice

    fun deleteByAcquisitionId(acquisitionId: ItemSourceId)
}
