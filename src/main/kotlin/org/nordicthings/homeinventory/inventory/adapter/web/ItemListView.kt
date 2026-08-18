package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.ItemListEntry
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

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
        totalQuantity = totalQuantity.value.formatIntegerForView(),
        averageValue = averageValue.formatForView(),
        totalValue = totalValue.formatForView(),
    )

fun MonetaryValue?.formatForView(): String =
    this?.let {
        "${it.amount.formatDecimalForView()} ${it.currency.currencyCode}"
    } ?: "unbekannt"

fun MonetaryValue.formatAmountForForm(): String =
    amount.formatDecimalForView()

fun Int.formatIntegerForView(): String =
    NumberFormat.getIntegerInstance(Locale.GERMANY).format(this)

private fun BigDecimal.formatDecimalForView(): String =
    NumberFormat.getNumberInstance(Locale.GERMANY)
        .apply {
            isGroupingUsed = true
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }
        .format(this)
