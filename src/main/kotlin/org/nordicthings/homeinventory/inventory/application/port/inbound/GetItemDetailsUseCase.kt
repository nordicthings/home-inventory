package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.application.ItemDetails
import org.nordicthings.homeinventory.inventory.domain.ItemId

interface GetItemDetailsUseCase {
    fun getItemDetails(id: ItemId): ItemDetails
}
