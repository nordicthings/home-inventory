package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType

interface LocationUseCase {
    fun createLocation(name: LocationName, type: LocationType): Location

    fun renameLocation(id: LocationId, name: LocationName): Location

    fun changeLocationType(id: LocationId, type: LocationType): Location

    fun deleteLocation(id: LocationId)
}
