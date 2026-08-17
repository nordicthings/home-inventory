package org.nordicthings.homeinventory.inventory.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LocationServiceTest {

    private val locationRepository = mockk<LocationRepository>()
    private val itemRepository = mockk<ItemRepository>()
    private val service = LocationService(locationRepository, itemRepository)

    @Test
    fun `creates location with unique normalized name`() {
        every { locationRepository.findByNormalizedName("küche") } returns null
        every { locationRepository.save(any()) } answers { firstArg() }

        val location = service.createLocation(LocationName.of(" Küche "), LocationType.INTERNAL)

        assertEquals("Küche", location.name.value)
        assertEquals(LocationType.INTERNAL, location.type)
        verify { locationRepository.save(location) }
    }

    @Test
    fun `rejects duplicate location name ignoring case`() {
        val existingLocation = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)
        every { locationRepository.findByNormalizedName("küche") } returns existingLocation

        assertFailsWith<DuplicateNameException> {
            service.createLocation(LocationName.of(" küche "), LocationType.INTERNAL)
        }

        verify(exactly = 0) { locationRepository.save(any()) }
    }

    @Test
    fun `changes location type`() {
        val location = Location.create(LocationId.newId(), LocationName.of("Büro"), LocationType.EXTERNAL)
        every { locationRepository.findById(location.id) } returns location
        every { locationRepository.save(any()) } answers { firstArg() }

        val changedLocation = service.changeLocationType(location.id, LocationType.INTERNAL)

        assertEquals(LocationType.INTERNAL, changedLocation.type)
        verify { locationRepository.save(changedLocation) }
    }

    @Test
    fun `does not delete location containing items`() {
        val location = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)
        every { locationRepository.findById(location.id) } returns location
        every { itemRepository.existsByLocationId(location.id) } returns true

        assertFailsWith<EntityInUseException> {
            service.deleteLocation(location.id)
        }

        verify(exactly = 0) { locationRepository.deleteById(any()) }
    }
}
