package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [HomeInventoryApplication::class])
@TestPropertySource(
    properties = [
        // Uses an isolated database because other persistence adapter tests delete table contents.
        "spring.datasource.url=jdbc:h2:mem:home_inventory_seed;MODE=MariaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    ],
)
class InventoryMasterDataSeedTest {

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Test
    fun `seeds initial categories ordered by normalized name`() {
        val categoryNames = categoryRepository.findAllOrderByName().map { it.name.value }

        assertEquals(
            listOf(
                "Audio / Video",
                "Beleuchtung",
                "Bücher",
                "Computer & Peripherie",
                "Dekoration",
                "Haushaltsgeräte",
                "Kameras & Zubehör",
                "Kleidung",
                "Körper / Gesundheit / Sport",
                "Küchenausstattung",
                "Multimedia & Unterhaltung",
                "Möbel",
                "Outdoor",
                "Schmuck",
                "Software",
                "Sonstiges",
                "Spielzeug",
                "Werkzeug",
            ),
            categoryNames,
        )
    }

    @Test
    fun `seeds initial internal locations ordered by normalized name`() {
        val locations = locationRepository.findAllOrderByName()

        assertEquals(
            listOf(
                "Ankleidezimmer",
                "Bad",
                "Gästezimmer",
                "Irenas Zimmer",
                "Jens' Zimmer",
                "Keller",
                "Küche",
                "Schlafzimmer",
                "Wohnzimmer",
            ),
            locations.map { it.name.value },
        )
        assertTrue(locations.all { it.type == LocationType.INTERNAL })
    }

    @Test
    fun `seeds initial sources ordered by normalized name`() {
        val sources = sourceRepository.findAllOrderByName()

        assertEquals(
            listOf(
                "Amazon",
                "Euronics",
                "Lüchau",
                "Mediamarkt",
                "rsMöbel",
                "Saturn",
                "Sonstige",
                "Toom",
            ),
            sources.map { it.name.value },
        )
        assertTrue(sources.all { it.details.isEmpty() })
    }
}
