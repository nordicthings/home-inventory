package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoiceId
import org.nordicthings.homeinventory.inventory.domain.InvoiceOriginalFilename
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId

data class AcquisitionInvoiceDetails(
    val id: AcquisitionInvoiceId,
    val acquisitionId: ItemSourceId,
    val originalFilename: InvoiceOriginalFilename,
)

data class AcquisitionInvoiceFile(
    val originalFilename: InvoiceOriginalFilename,
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is AcquisitionInvoiceFile &&
                originalFilename == other.originalFilename &&
                content.contentEquals(other.content))

    override fun hashCode(): Int =
        31 * originalFilename.hashCode() + content.contentHashCode()
}
