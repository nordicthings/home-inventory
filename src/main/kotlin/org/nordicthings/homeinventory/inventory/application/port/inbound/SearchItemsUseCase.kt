package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.application.ItemListEntry
import org.nordicthings.homeinventory.inventory.application.SearchItemsFilter

interface SearchItemsUseCase {
    fun searchItems(filter: SearchItemsFilter = SearchItemsFilter()): List<ItemListEntry>
}
