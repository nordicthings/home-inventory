package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.ItemAcquisitionDetails
import org.nordicthings.homeinventory.inventory.application.ItemDetails
import org.nordicthings.homeinventory.inventory.application.ItemLocationQuantityDetails
import org.nordicthings.homeinventory.inventory.domain.LocationType
import java.time.format.DateTimeFormatter

data class ItemDetailPageView(
    val id: String,
    val name: String,
    val categoryName: String,
    val estimatedValue: String,
    val note: String,
    val totalQuantity: String,
    val averageValue: String,
    val totalValue: String,
    val locationQuantities: List<ItemLocationQuantityView>,
    val acquisitions: List<ItemAcquisitionView>,
    val notices: List<String> = emptyList(),
)

data class ItemLocationQuantityView(
    val locationId: String,
    val locationName: String,
    val locationType: String,
    val quantity: String,
)

data class ItemAcquisitionView(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val quantity: String,
    val purchasePrice: String,
    val purchaseDate: String,
)

data class ItemAcquisitionDeleteView(
    val id: String,
    val sourceName: String,
    val quantity: String,
    val purchasePrice: String,
    val purchaseDate: String,
)

fun ItemDetails.toDetailPageView(notices: List<String> = emptyList()): ItemDetailPageView =
    ItemDetailPageView(
        id = id.value.toString(),
        name = name.value,
        categoryName = categoryName.value,
        estimatedValue = estimatedValue.formatForView(),
        note = note,
        totalQuantity = totalQuantity.value.formatIntegerForView(),
        averageValue = averageValue.formatForView(),
        totalValue = totalValue.formatForView(),
        locationQuantities = locationQuantities.map { it.toLocationQuantityView() },
        acquisitions = acquisitions.map { it.toAcquisitionView() },
        notices = notices,
    )

private fun ItemLocationQuantityDetails.toLocationQuantityView(): ItemLocationQuantityView =
    ItemLocationQuantityView(
        locationId = locationId.value.toString(),
        locationName = locationName.value,
        locationType = locationType.toViewLabel(),
        quantity = quantity.value.formatIntegerForView(),
    )

private fun ItemAcquisitionDetails.toAcquisitionView(): ItemAcquisitionView =
    ItemAcquisitionView(
        id = id.value.toString(),
        sourceId = sourceId.value.toString(),
        sourceName = sourceName.value,
        quantity = quantity.value.formatIntegerForView(),
        purchasePrice = purchasePrice.formatForView(),
        purchaseDate = purchaseDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "unbekannt",
    )

fun ItemAcquisitionDetails.toDeletePageView(): ItemAcquisitionDeleteView =
    ItemAcquisitionDeleteView(
        id = id.value.toString(),
        sourceName = sourceName.value,
        quantity = quantity.value.formatIntegerForView(),
        purchasePrice = purchasePrice.formatForView(),
        purchaseDate = purchaseDate?.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) ?: "unbekannt",
    )

private fun LocationType.toViewLabel(): String =
    when (this) {
        LocationType.INTERNAL -> "intern"
        LocationType.EXTERNAL -> "extern"
    }
