package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId

data class SearchItemsFilter(
    val name: String? = null,
    val categoryId: CategoryId? = null,
    val locationId: LocationId? = null,
    val sourceId: SourceId? = null,
    val sort: ItemListSort = ItemListSort(),
)

data class ItemSearchCriteria(
    val normalizedNameContains: String? = null,
    val categoryId: CategoryId? = null,
    val locationId: LocationId? = null,
    val sourceId: SourceId? = null,
)

data class ItemListEntry(
    val id: ItemId,
    val name: ItemName,
    val categoryId: CategoryId,
    val categoryName: CategoryName,
    val totalQuantity: Quantity,
    val averageValue: MonetaryValue?,
    val totalValue: MonetaryValue?,
)

data class ItemListSort(
    val field: ItemListSortField = ItemListSortField.NAME,
    val direction: SortDirection = SortDirection.ASCENDING,
)

enum class ItemListSortField {
    NAME,
    CATEGORY,
    TOTAL_QUANTITY,
    AVERAGE_VALUE,
    TOTAL_VALUE,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}
