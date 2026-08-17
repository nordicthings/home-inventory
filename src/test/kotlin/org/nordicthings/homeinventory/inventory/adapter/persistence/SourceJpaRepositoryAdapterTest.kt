package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest(classes = [HomeInventoryApplication::class, PersistenceAdapterTestConfiguration::class])
class SourceJpaRepositoryAdapterTest {

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Autowired
    private lateinit var sourceJpaEntityRepository: SourceJpaEntityRepository

    @BeforeEach
    fun clearDatabase() {
        sourceJpaEntityRepository.deleteAll()
    }

    @Test
    fun `saves and finds source by id`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"), "https://example.test")

        sourceRepository.save(source)

        assertEquals(source, sourceRepository.findById(source.id))
    }

    @Test
    fun `finds source by normalized name`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"), "https://example.test")
        sourceRepository.save(source)

        val foundSource = sourceRepository.findByNormalizedName("amazon")

        assertEquals(source, foundSource)
    }

    @Test
    fun `finds all sources ordered by normalized name`() {
        sourceRepository.save(Source.create(SourceId.newId(), SourceName.of("Toom")))
        sourceRepository.save(Source.create(SourceId.newId(), SourceName.of("Amazon")))
        sourceRepository.save(Source.create(SourceId.newId(), SourceName.of("Saturn")))

        val sourceNames = sourceRepository.findAllOrderByName().map { it.name.value }

        assertEquals(listOf("Amazon", "Saturn", "Toom"), sourceNames)
    }

    @Test
    fun `updates source name normalized name and details`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"), "https://example.test")
        sourceRepository.save(source)

        val changedSource = source
            .rename(SourceName.of("Lüchau"))
            .changeDetails("Baumarkt")
        sourceRepository.save(changedSource)

        assertEquals(changedSource, sourceRepository.findById(source.id))
        assertNull(sourceRepository.findByNormalizedName("amazon"))
        assertEquals(changedSource, sourceRepository.findByNormalizedName("lüchau"))
    }

    @Test
    fun `deletes source by id`() {
        val source = Source.create(SourceId.newId(), SourceName.of("Amazon"), "https://example.test")
        sourceRepository.save(source)

        sourceRepository.deleteById(source.id)

        assertNull(sourceRepository.findById(source.id))
    }

    @Test
    fun `enforces unique normalized source name`() {
        sourceRepository.save(Source.create(SourceId.newId(), SourceName.of("Amazon")))

        assertFailsWith<DataIntegrityViolationException> {
            sourceRepository.save(Source.create(SourceId.newId(), SourceName.of("amazon")))
        }
    }
}
