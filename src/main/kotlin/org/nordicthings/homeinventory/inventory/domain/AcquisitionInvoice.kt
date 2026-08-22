package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.Entity
import org.jmolecules.ddd.annotation.Identity
import org.jmolecules.ddd.annotation.ValueObject
import java.util.UUID

@Entity
class AcquisitionInvoice(
    @Identity
    val id: AcquisitionInvoiceId,
    val acquisitionId: ItemSourceId,
    val originalFilename: InvoiceOriginalFilename,
    val storedFilename: InvoiceStoredFilename,
) {
    override fun equals(other: Any?): Boolean =
        this === other || (other is AcquisitionInvoice && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}

@ValueObject
@JvmInline
value class AcquisitionInvoiceId(val value: UUID) {
    companion object {
        fun newId(): AcquisitionInvoiceId = AcquisitionInvoiceId(UUID.randomUUID())
    }
}

@ValueObject
@JvmInline
value class InvoiceOriginalFilename private constructor(val value: String) {
    companion object {
        fun of(value: String): InvoiceOriginalFilename {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "Original filename must not be blank." }
            require(trimmed.length <= 255) { "Original filename must not exceed 255 characters." }
            return InvoiceOriginalFilename(trimmed)
        }
    }
}

@ValueObject
@JvmInline
value class InvoiceStoredFilename private constructor(val value: String) {
    companion object {
        fun of(value: String): InvoiceStoredFilename {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "Stored filename must not be blank." }
            require(trimmed.length <= 255) { "Stored filename must not exceed 255 characters." }
            return InvoiceStoredFilename(trimmed)
        }
    }
}
