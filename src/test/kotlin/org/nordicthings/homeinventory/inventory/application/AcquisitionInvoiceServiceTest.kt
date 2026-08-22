package org.nordicthings.homeinventory.inventory.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.nordicthings.homeinventory.inventory.application.port.outbound.AcquisitionInvoiceRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.InvoiceFileStorage
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoice
import org.nordicthings.homeinventory.inventory.domain.AcquisitionInvoiceId
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.InvoiceOriginalFilename
import org.nordicthings.homeinventory.inventory.domain.InvoiceStoredFilename
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.springframework.util.unit.DataSize
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AcquisitionInvoiceServiceTest {
    private val itemRepository = mockk<ItemRepository>()
    private val invoiceRepository = mockk<AcquisitionInvoiceRepository>()
    private val fileStorage = mockk<InvoiceFileStorage>()
    private val service = AcquisitionInvoiceService(
        itemRepository = itemRepository,
        invoiceRepository = invoiceRepository,
        fileStorage = fileStorage,
        properties = AcquisitionInvoiceProperties(maxUploadSize = DataSize.ofMegabytes(10)),
    )

    @Test
    fun `uploads pdf invoice for existing acquisition`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        val savedInvoice = slot<AcquisitionInvoice>()
        every { itemRepository.findById(item.id) } returns item
        every { invoiceRepository.findByAcquisitionId(acquisitionId) } returns null
        every { fileStorage.store(any(), byteArrayOf(1, 2, 3)) } returns Unit
        every { invoiceRepository.save(capture(savedInvoice)) } answers { firstArg() }

        val result = service.uploadInvoice(
            itemId = item.id,
            acquisitionId = acquisitionId,
            originalFilename = "rechnung.pdf",
            contentType = "application/pdf",
            content = byteArrayOf(1, 2, 3),
        )

        assertEquals(acquisitionId, result.acquisitionId)
        assertEquals("rechnung.pdf", result.originalFilename.value)
        assertEquals("rechnung.pdf", savedInvoice.captured.originalFilename.value)
        verify { fileStorage.store(savedInvoice.captured.storedFilename.value, byteArrayOf(1, 2, 3)) }
    }

    @Test
    fun `rejects upload when invoice already exists and replacement was not confirmed`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        every { itemRepository.findById(item.id) } returns item
        every { invoiceRepository.findByAcquisitionId(acquisitionId) } returns existingInvoice(acquisitionId)

        val exception = assertFailsWith<AcquisitionInvoiceAlreadyExistsException> {
            service.uploadInvoice(
                itemId = item.id,
                acquisitionId = acquisitionId,
                originalFilename = "neu.pdf",
                contentType = "application/pdf",
                content = byteArrayOf(1),
            )
        }

        assertEquals("alt.pdf", exception.existingFilename)
        verify(exactly = 0) { fileStorage.store(any(), any()) }
        verify(exactly = 0) { invoiceRepository.save(any()) }
    }

    @Test
    fun `replaces existing invoice after confirmation`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        val existingInvoice = existingInvoice(acquisitionId)
        every { itemRepository.findById(item.id) } returns item
        every { invoiceRepository.findByAcquisitionId(acquisitionId) } returns existingInvoice
        every { fileStorage.store(any(), byteArrayOf(4, 5, 6)) } returns Unit
        every { invoiceRepository.save(any()) } answers { firstArg() }
        every { fileStorage.delete("old-stored.pdf") } returns Unit

        service.uploadInvoice(
            itemId = item.id,
            acquisitionId = acquisitionId,
            originalFilename = "neu.pdf",
            contentType = "application/pdf",
            content = byteArrayOf(4, 5, 6),
            replaceExisting = true,
        )

        verify { invoiceRepository.save(match { it.id == existingInvoice.id && it.originalFilename.value == "neu.pdf" }) }
        verify { fileStorage.delete("old-stored.pdf") }
    }

    @Test
    fun `rejects non pdf invoice`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        every { itemRepository.findById(item.id) } returns item

        assertFailsWith<InvalidAcquisitionInvoiceException> {
            service.uploadInvoice(
                itemId = item.id,
                acquisitionId = acquisitionId,
                originalFilename = "rechnung.txt",
                contentType = "text/plain",
                content = byteArrayOf(1),
            )
        }

        verify(exactly = 0) { fileStorage.store(any(), any()) }
    }

    @Test
    fun `rejects invoice above configured maximum size`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        val smallLimitService = AcquisitionInvoiceService(
            itemRepository = itemRepository,
            invoiceRepository = invoiceRepository,
            fileStorage = fileStorage,
            properties = AcquisitionInvoiceProperties(maxUploadSize = DataSize.ofBytes(2)),
        )
        every { itemRepository.findById(item.id) } returns item

        assertFailsWith<InvalidAcquisitionInvoiceException> {
            smallLimitService.uploadInvoice(
                itemId = item.id,
                acquisitionId = acquisitionId,
                originalFilename = "rechnung.pdf",
                contentType = "application/pdf",
                content = byteArrayOf(1, 2, 3),
            )
        }
    }

    @Test
    fun `downloads invoice content`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        every { itemRepository.findById(item.id) } returns item
        every { invoiceRepository.findByAcquisitionId(acquisitionId) } returns existingInvoice(acquisitionId)
        every { fileStorage.load("old-stored.pdf") } returns byteArrayOf(9, 8, 7)

        val invoice = service.downloadInvoice(item.id, acquisitionId)

        assertEquals("alt.pdf", invoice.originalFilename.value)
        assertContentEquals(byteArrayOf(9, 8, 7), invoice.content)
    }

    @Test
    fun `deletes invoice reference and file`() {
        val item = existingItemWithAcquisition()
        val acquisitionId = item.sources.single().id
        every { itemRepository.findById(item.id) } returns item
        every { invoiceRepository.findByAcquisitionId(acquisitionId) } returns existingInvoice(acquisitionId)
        every { invoiceRepository.deleteByAcquisitionId(acquisitionId) } returns Unit
        every { fileStorage.delete("old-stored.pdf") } returns Unit

        service.deleteInvoice(item.id, acquisitionId)

        verify { invoiceRepository.deleteByAcquisitionId(acquisitionId) }
        verify { fileStorage.delete("old-stored.pdf") }
    }

    private fun existingItemWithAcquisition(): Item {
        val item = Item(
            id = ItemId.newId(),
            name = ItemName.of("Laptop"),
            categoryId = CategoryId.newId(),
            estimatedValue = MonetaryValue.of("800"),
        )
        item.recordAcquisition(SourceId.newId(), Quantity.of(1), MonetaryValue.of("800"), null)
        return item
    }

    private fun existingInvoice(acquisitionId: ItemSourceId): AcquisitionInvoice =
        AcquisitionInvoice(
            id = AcquisitionInvoiceId.newId(),
            acquisitionId = acquisitionId,
            originalFilename = InvoiceOriginalFilename.of("alt.pdf"),
            storedFilename = InvoiceStoredFilename.of("old-stored.pdf"),
        )
}
