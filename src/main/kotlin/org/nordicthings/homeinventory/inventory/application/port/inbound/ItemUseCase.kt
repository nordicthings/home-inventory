package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId
import java.time.LocalDate

interface ItemUseCase {
    fun createItem(
        name: ItemName,
        categoryId: CategoryId,
        estimatedValue: MonetaryValue,
        note: String = "",
    ): Item

    fun renameItem(id: ItemId, name: ItemName): Item

    fun changeItemCategory(id: ItemId, categoryId: CategoryId): Item

    fun changeEstimatedValue(id: ItemId, estimatedValue: MonetaryValue): Item

    fun changeNote(id: ItemId, note: String): Item

    fun setLocationQuantity(id: ItemId, locationId: LocationId, quantity: Quantity): Item

    fun relocateItem(
        id: ItemId,
        sourceLocationId: LocationId,
        targetLocationId: LocationId,
        quantity: Quantity,
    ): Item

    fun removeFromLocation(id: ItemId, locationId: LocationId, quantity: Quantity): Item

    fun recordAcquisition(
        id: ItemId,
        sourceId: SourceId,
        quantity: Quantity,
        purchasePrice: MonetaryValue,
        purchaseDate: LocalDate?,
    ): Item

    fun updateAcquisition(
        id: ItemId,
        itemSourceId: ItemSourceId,
        sourceId: SourceId,
        quantity: Quantity,
        purchasePrice: MonetaryValue,
        purchaseDate: LocalDate?,
    ): Item

    fun deleteAcquisition(id: ItemId, itemSourceId: ItemSourceId): Item

    fun deleteItem(id: ItemId)
}
