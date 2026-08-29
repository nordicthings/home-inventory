package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.ItemAcquisitionDetails
import org.nordicthings.homeinventory.inventory.application.ItemDetails
import org.nordicthings.homeinventory.inventory.application.ItemListEntry
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.SourceId
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

data class ItemReportPageView(
    val generatedDate: String,
    val filters: List<ItemReportFilterView>,
    val items: List<ItemReportItemView>,
    val totalQuantity: String,
    val totalValue: String,
)

data class ItemReportFilterView(
    val label: String,
    val value: String,
)

data class ItemReportItemView(
    val name: String,
    val categoryName: String,
    val status: String?,
    val quantity: String,
    val averageValue: String,
    val totalValue: String,
    val acquisitions: List<ItemReportAcquisitionView>,
)

data class ItemReportAcquisitionView(
    val sourceName: String,
    val quantity: String,
    val purchaseDate: String,
    val purchasePrice: String,
)

fun toReportItemView(
    entry: ItemListEntry,
    details: ItemDetails,
    sourceId: SourceId?,
): ItemReportItemView =
    ItemReportItemView(
        name = entry.name.value,
        categoryName = entry.categoryName.value,
        status = entry.toReportStatusLabel(),
        quantity = entry.totalQuantity.value.formatIntegerForView(),
        averageValue = entry.averageValue.formatForView(),
        totalValue = entry.totalValue.formatForView(),
        acquisitions = details.acquisitions
            .filter { sourceId == null || it.sourceId == sourceId }
            .map { it.toReportAcquisitionView() },
    )

fun formatReportTotalValue(amount: BigDecimal): String =
    "${amount.setScale(2, RoundingMode.HALF_UP).formatDecimalForReport()} ${Currency.getInstance("EUR").currencyCode}"

private fun ItemListEntry.toReportStatusLabel(): String? =
    when {
        reviewRequired -> "Zu prüfen"
        hasInvoice -> "Rechnung liegt vor"
        estimatedValue.isKnown -> "Schätzwert"
        else -> null
    }

private fun ItemAcquisitionDetails.toReportAcquisitionView(): ItemReportAcquisitionView =
    ItemReportAcquisitionView(
        sourceName = sourceName.value,
        quantity = quantity.value.formatIntegerForView(),
        purchaseDate = purchaseDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "unbekannt",
        purchasePrice = purchasePrice.formatForView(),
    )

private fun BigDecimal.formatDecimalForReport(): String =
    NumberFormat.getNumberInstance(Locale.GERMANY)
        .apply {
            isGroupingUsed = true
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }
        .format(this)
