package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest(classes = [HomeInventoryApplication::class, PersistenceAdapterTestConfiguration::class])
class LocationJpaRepositoryAdapterTest {

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var locationJpaEntityRepository: LocationJpaEntityRepository

    @BeforeEach
    fun clearDatabase() {
        locationJpaEntityRepository.deleteAll()
    }

    @Test
    fun `saves and finds location by id`() {
        val location = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)

        locationRepository.save(location)

        assertEquals(location, locationRepository.findById(location.id))
    }

    @Test
    fun `finds location by normalized name`() {
        val location = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)
        locationRepository.save(location)

        val foundLocation = locationRepository.findByNormalizedName("küche")

        assertEquals(location, foundLocation)
    }

    @Test
    fun `finds all locations ordered by normalized name`() {
        locationRepository.save(Location.create(LocationId.newId(), LocationName.of("Wohnzimmer"), LocationType.INTERNAL))
        locationRepository.save(Location.create(LocationId.newId(), LocationName.of("Bad"), LocationType.INTERNAL))
        locationRepository.save(Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL))

        val locationNames = locationRepository.findAllOrderByName().map { it.name.value }

        assertEquals(listOf("Bad", "Küche", "Wohnzimmer"), locationNames)
    }

    @Test
    fun `updates location name normalized name and type`() {
        val location = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)
        locationRepository.save(location)

        val changedLocation = location
            .rename(LocationName.of("Büro"))
            .changeType(LocationType.EXTERNAL)
        locationRepository.save(changedLocation)

        assertEquals(changedLocation, locationRepository.findById(location.id))
        assertNull(locationRepository.findByNormalizedName("küche"))
        assertEquals(changedLocation, locationRepository.findByNormalizedName("büro"))
    }

    @Test
    fun `deletes location by id`() {
        val location = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)
        locationRepository.save(location)

        locationRepository.deleteById(location.id)

        assertNull(locationRepository.findById(location.id))
    }

    @Test
    fun `enforces unique normalized location name`() {
        locationRepository.save(Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL))

        assertFailsWith<DataIntegrityViolationException> {
            locationRepository.save(Location.create(LocationId.newId(), LocationName.of("küche"), LocationType.EXTERNAL))
        }
    }
}
