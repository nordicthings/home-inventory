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
)

data class ItemLocationQuantityView(
    val locationName: String,
    val locationType: String,
    val quantity: String,
)

data class ItemAcquisitionView(
    val sourceName: String,
    val quantity: String,
    val purchasePrice: String,
    val purchaseDate: String,
)

fun ItemDetails.toDetailPageView(): ItemDetailPageView =
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
    )

private fun ItemLocationQuantityDetails.toLocationQuantityView(): ItemLocationQuantityView =
    ItemLocationQuantityView(
        locationName = locationName.value,
        locationType = locationType.toViewLabel(),
        quantity = quantity.value.formatIntegerForView(),
    )

private fun ItemAcquisitionDetails.toAcquisitionView(): ItemAcquisitionView =
    ItemAcquisitionView(
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
