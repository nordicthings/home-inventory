package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.ItemListEntry
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import java.math.RoundingMode

data class ItemListPageView(
    val filter: ItemFilterView,
    val categories: List<SelectOptionView>,
    val locations: List<SelectOptionView>,
    val sources: List<SelectOptionView>,
    val items: List<ItemListRowView>,
)

data class ItemFilterView(
    val name: String = "",
    val categoryId: String = "",
    val locationId: String = "",
    val sourceId: String = "",
)

data class SelectOptionView(
    val id: String,
    val label: String,
)

data class ItemListRowView(
    val id: String,
    val name: String,
    val categoryName: String,
    val totalQuantity: String,
    val averageValue: String,
    val totalValue: String,
)

fun ItemListEntry.toRowView(): ItemListRowView =
    ItemListRowView(
        id = id.value.toString(),
        name = name.value,
        categoryName = categoryName.value,
        totalQuantity = totalQuantity.value.toString(),
        averageValue = averageValue.formatForView(),
        totalValue = totalValue.formatForView(),
    )

fun MonetaryValue?.formatForView(): String =
    this?.let {
        "${it.amount.setScale(2, RoundingMode.HALF_UP).toPlainString()} ${it.currency.currencyCode}"
    } ?: "unbekannt"
