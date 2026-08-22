package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.application.port.inbound.AcquisitionInvoiceUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.AcquisitionInvoiceRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.InvoiceFileStorage
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoice
import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoiceId
import org.nordicthings.homeinventory.inventory.domain.InvoiceOriginalFilename
import org.nordicthings.homeinventory.inventory.domain.InvoiceStoredFilename
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.unit.DataSize
import java.util.Locale
import java.util.UUID

@Service
class AcquisitionInvoiceService(
    private val itemRepository: ItemRepository,
    private val invoiceRepository: AcquisitionInvoiceRepository,
    private val fileStorage: InvoiceFileStorage,
    private val properties: AcquisitionInvoiceProperties,
) : AcquisitionInvoiceUseCase {
    override fun findInvoice(acquisitionId: ItemSourceId): AcquisitionInvoiceDetails? =
        invoiceRepository.findByAcquisitionId(acquisitionId)?.toDetails()

    @Transactional
    override fun uploadInvoice(
        itemId: ItemId,
        acquisitionId: ItemSourceId,
        originalFilename: String,
        contentType: String?,
        content: ByteArray,
        replaceExisting: Boolean,
    ): AcquisitionInvoiceDetails {
        requireAcquisition(itemId, acquisitionId)
        validateInvoice(originalFilename, contentType, content)

        val existingInvoice = invoiceRepository.findByAcquisitionId(acquisitionId)
        if (existingInvoice != null && !replaceExisting) {
            throw AcquisitionInvoiceAlreadyExistsException(existingInvoice.originalFilename.value)
        }

        val invoice = AcquisitionInvoice(
            id = existingInvoice?.id ?: AcquisitionInvoiceId.newId(),
            acquisitionId = acquisitionId,
            originalFilename = InvoiceOriginalFilename.of(originalFilename.cleanFilename()),
            storedFilename = InvoiceStoredFilename.of("${UUID.randomUUID()}.pdf"),
        )

        fileStorage.store(invoice.storedFilename.value, content)
        try {
            invoiceRepository.save(invoice)
        } catch (exception: RuntimeException) {
            fileStorage.delete(invoice.storedFilename.value)
            throw exception
        }
        if (existingInvoice != null) {
            fileStorage.delete(existingInvoice.storedFilename.value)
        }
        return invoice.toDetails()
    }

    override fun downloadInvoice(itemId: ItemId, acquisitionId: ItemSourceId): AcquisitionInvoiceFile {
        requireAcquisition(itemId, acquisitionId)
        val invoice = invoiceRepository.findByAcquisitionId(acquisitionId)
            ?: throw EntityNotFoundException("Acquisition invoice does not exist: $acquisitionId")
        return AcquisitionInvoiceFile(
            originalFilename = invoice.originalFilename,
            content = fileStorage.load(invoice.storedFilename.value),
        )
    }

    @Transactional
    override fun deleteInvoice(itemId: ItemId, acquisitionId: ItemSourceId) {
        requireAcquisition(itemId, acquisitionId)
        deleteInvoiceFileAndReference(acquisitionId)
    }

    @Transactional
    override fun deleteInvoicesForItem(itemId: ItemId) {
        val item = findItem(itemId)
        item.sources.forEach { deleteInvoiceFileAndReference(it.id) }
    }

    private fun validateInvoice(
        originalFilename: String,
        contentType: String?,
        content: ByteArray,
    ) {
        val filename = originalFilename.cleanFilename()
        if (content.isEmpty()) {
            throw InvalidAcquisitionInvoiceException("Invoice file must not be empty.")
        }
        if (content.size > properties.maxUploadSize.toBytes()) {
            throw InvalidAcquisitionInvoiceException("Invoice file exceeds maximum upload size.")
        }
        if (!filename.lowercase(Locale.ROOT).endsWith(".pdf")) {
            throw InvalidAcquisitionInvoiceException("Invoice file must have a PDF filename.")
        }
        if (contentType?.substringBefore(";")?.trim()?.lowercase(Locale.ROOT) != "application/pdf") {
            throw InvalidAcquisitionInvoiceException("Invoice file must have PDF content type.")
        }
    }

    private fun deleteInvoiceFileAndReference(acquisitionId: ItemSourceId) {
        val invoice = invoiceRepository.findByAcquisitionId(acquisitionId) ?: return
        invoiceRepository.deleteByAcquisitionId(acquisitionId)
        fileStorage.delete(invoice.storedFilename.value)
    }

    private fun requireAcquisition(itemId: ItemId, acquisitionId: ItemSourceId) {
        val item = findItem(itemId)
        if (item.sources.none { it.id == acquisitionId }) {
            throw EntityNotFoundException("Acquisition does not exist: $acquisitionId")
        }
    }

    private fun findItem(itemId: ItemId): Item =
        itemRepository.findById(itemId)
            ?: throw EntityNotFoundException("Item does not exist: $itemId")
}

@ConfigurationProperties(prefix = "home-inventory.files")
data class AcquisitionInvoiceProperties(
    var storagePath: String = ".local/files",
    var maxUploadSize: DataSize = DataSize.ofMegabytes(10),
)

private fun AcquisitionInvoice.toDetails(): AcquisitionInvoiceDetails =
    AcquisitionInvoiceDetails(
        id = id,
        acquisitionId = acquisitionId,
        originalFilename = originalFilename,
    )

private fun String.cleanFilename(): String =
    replace('\\', '/').substringAfterLast('/').trim()
