package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.application.port.inbound.LocationUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.springframework.stereotype.Service

@Service
class LocationService(
    private val locationRepository: LocationRepository,
    private val itemRepository: ItemRepository,
) : LocationUseCase {
    override fun createLocation(name: LocationName, type: LocationType): Location {
        ensureUniqueName(name)
        return locationRepository.save(Location.create(LocationId.newId(), name, type))
    }

    override fun renameLocation(id: LocationId, name: LocationName): Location {
        val location = locationRepository.findById(id)
            ?: throw EntityNotFoundException("Location does not exist: $id")
        ensureUniqueName(name, existingId = id)
        return locationRepository.save(location.rename(name))
    }

    override fun changeLocationType(id: LocationId, type: LocationType): Location {
        val location = locationRepository.findById(id)
            ?: throw EntityNotFoundException("Location does not exist: $id")
        return locationRepository.save(location.changeType(type))
    }

    override fun deleteLocation(id: LocationId) {
        locationRepository.findById(id)
            ?: throw EntityNotFoundException("Location does not exist: $id")
        if (itemRepository.existsByLocationId(id)) {
            throw EntityInUseException("Location still contains items: $id")
        }
        locationRepository.deleteById(id)
    }

    private fun ensureUniqueName(name: LocationName, existingId: LocationId? = null) {
        val existingLocation = locationRepository.findByNormalizedName(name.normalize())
        if (existingLocation != null && existingLocation.id != existingId) {
            throw DuplicateNameException("Location name already exists: ${name.value}")
        }
    }
}
