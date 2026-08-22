package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoice
import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoiceId
import org.nordicthings.homeinventory.inventory.domain.InvoiceOriginalFilename
import org.nordicthings.homeinventory.inventory.domain.InvoiceStoredFilename
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId

fun AcquisitionInvoice.toJpaEntity(): AcquisitionInvoiceJpaEntity =
    AcquisitionInvoiceJpaEntity(
        id = id.value,
        acquisitionId = acquisitionId.value,
        originalFilename = originalFilename.value,
        storedFilename = storedFilename.value,
    )

fun AcquisitionInvoiceJpaEntity.toDomain(): AcquisitionInvoice =
    AcquisitionInvoice(
        id = AcquisitionInvoiceId(id),
        acquisitionId = ItemSourceId(acquisitionId),
        originalFilename = InvoiceOriginalFilename.of(originalFilename),
        storedFilename = InvoiceStoredFilename.of(storedFilename),
    )
