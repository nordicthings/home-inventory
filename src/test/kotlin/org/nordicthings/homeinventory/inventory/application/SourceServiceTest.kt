package org.nordicthings.homeinventory.inventory.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceServiceTest {

    private val sourceRepository = mockk<SourceRepository>()
    private val itemRepository = mockk<ItemRepository>()
    private val service = SourceService(sourceRepository, itemRepository)

    @Test
    fun `loads source list from repository`() {
        val sources = listOf(
            Source.create(SourceId.newId(), SourceName.of("Amazon")),
            Source.create(SourceId.newId(), SourceName.of("Lüchau")),
        )
        every { sourceRepository.findAllOrderByName() } returns sources

        val result = service.getSourceList()

        assertEquals(sources, result)
        verify { sourceRepository.findAllOrderByName() }
    }

    @Test
    fun `creates source with unique normalized name`() {
        every { sourceRepository.findByNormalizedName("amazon") } returns null
        every { sourceRepository.save(any()) } answers { firstArg() }

        val source = service.createSource(SourceName.of(" Amazon "), "https://example.test")

        assertEquals("Amazon", source.name.value)
        assertEquals("https://example.test", source.details)
        verify { sourceRepository.save(source) }
    }

    @Test
    fun `rejects duplicate source name ignoring case`() {
        val existingSource = Source.create(SourceId.newId(), SourceName.of("Amazon"))
        every { sourceRepository.findByNormalizedName("amazon") } returns existingSource

        assertFailsWith<DuplicateNameException> {
            service.createSource(SourceName.of(" amazon "), "")
        }

        verify(exactly = 0) { sourceRepository.save(any()) }
    }

    @Test
    fun `changes source details`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"), "https://example.test")
        every { sourceRepository.findById(source.id) } returns source
        every { sourceRepository.save(any()) } answers { firstArg() }

        val changedSource = service.changeSourceDetails(source.id, "Online-Shop")

        assertEquals("Online-Shop", changedSource.details)
        verify { sourceRepository.save(changedSource) }
    }

    @Test
    fun `does not delete source assigned to items`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"))
        every { sourceRepository.findById(source.id) } returns source
        every { itemRepository.existsBySourceId(source.id) } returns true

        assertFailsWith<EntityInUseException> {
            service.deleteSource(source.id)
        }

        verify(exactly = 0) { sourceRepository.deleteById(any()) }
    }

    @Test
    fun `reports whether source can be deleted`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"))
        every { sourceRepository.findById(source.id) } returns source
        every { itemRepository.existsBySourceId(source.id) } returns false

        assertEquals(true, service.canDeleteSource(source.id))

        every { itemRepository.existsBySourceId(source.id) } returns true

        assertEquals(false, service.canDeleteSource(source.id))
    }
}
