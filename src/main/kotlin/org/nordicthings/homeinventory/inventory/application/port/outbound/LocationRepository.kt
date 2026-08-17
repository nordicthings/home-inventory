package org.nordicthings.homeinventory.inventory.application.port.outbound

import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId

interface LocationRepository {
    fun findById(id: LocationId): Location?

    fun findByNormalizedName(normalizedName: String): Location?

    fun save(location: Location): Location

    fun deleteById(id: LocationId)
}
