package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.Location

interface GetLocationListUseCase {
    fun getLocationList(): List<Location>
}
