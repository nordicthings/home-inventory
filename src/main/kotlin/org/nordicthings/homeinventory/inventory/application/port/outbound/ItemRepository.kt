package org.nordicthings.homeinventory.inventory.application.port.outbound

import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.application.ItemListEntry
import org.nordicthings.homeinventory.inventory.application.ItemSearchCriteria
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.SourceId

interface ItemRepository {
    fun findById(id: ItemId): Item?

    fun findByNormalizedName(normalizedName: String): Item?

    fun search(criteria: ItemSearchCriteria): List<ItemListEntry>

    fun existsByCategoryId(categoryId: CategoryId): Boolean

    fun existsByLocationId(locationId: LocationId): Boolean

    fun existsBySourceId(sourceId: SourceId): Boolean

    fun save(item: Item): Item

    fun deleteById(id: ItemId)
}
