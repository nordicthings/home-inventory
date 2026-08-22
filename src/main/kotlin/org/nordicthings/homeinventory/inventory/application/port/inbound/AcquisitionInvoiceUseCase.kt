package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.application.AcquisitionInvoiceDetails
import org.nordicthings.homeinventory.inventory.application.AcquisitionInvoiceFile
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId

interface AcquisitionInvoiceUseCase {
    fun findInvoice(acquisitionId: ItemSourceId): AcquisitionInvoiceDetails?

    fun uploadInvoice(
        itemId: ItemId,
        acquisitionId: ItemSourceId,
        originalFilename: String,
        contentType: String?,
        content: ByteArray,
        replaceExisting: Boolean = false,
    ): AcquisitionInvoiceDetails

    fun downloadInvoice(itemId: ItemId, acquisitionId: ItemSourceId): AcquisitionInvoiceFile

    fun deleteInvoice(itemId: ItemId, acquisitionId: ItemSourceId)

    fun deleteInvoicesForItem(itemId: ItemId)
}
