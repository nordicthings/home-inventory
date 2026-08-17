package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName
import java.time.LocalDate

data class ItemDetails(
    val id: ItemId,
    val name: ItemName,
    val categoryId: CategoryId,
    val categoryName: CategoryName,
    val estimatedValue: MonetaryValue,
    val note: String,
    val locationQuantities: List<ItemLocationQuantityDetails>,
    val acquisitions: List<ItemAcquisitionDetails>,
    val totalQuantity: Quantity,
    val averageValue: MonetaryValue?,
    val totalValue: MonetaryValue?,
)

data class ItemLocationQuantityDetails(
    val locationId: LocationId,
    val locationName: LocationName,
    val locationType: LocationType,
    val quantity: Quantity,
)

data class ItemAcquisitionDetails(
    val id: ItemSourceId,
    val sourceId: SourceId,
    val sourceName: SourceName,
    val quantity: Quantity,
    val purchasePrice: MonetaryValue,
    val purchaseDate: LocalDate?,
)
