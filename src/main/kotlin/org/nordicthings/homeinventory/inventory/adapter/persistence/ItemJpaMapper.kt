package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.ItemSource
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId
import java.util.UUID

fun Item.toJpaEntity(): ItemJpaEntity =
    ItemJpaEntity(
        id = id.value,
        name = name.value,
        normalizedName = name.normalize(),
        categoryId = categoryId.value,
        estimatedValueAmount = estimatedValue.amount,
        estimatedValueCurrency = estimatedValue.currency.currencyCode,
        note = note,
    )

fun Item.toLocationQuantityJpaEntities(): List<ItemLocationQuantityJpaEntity> =
    locationQuantities.map { (locationId, quantity) ->
        ItemLocationQuantityJpaEntity(
            id = ItemLocationQuantityJpaId(
                itemId = id.value,
                locationId = locationId.value,
            ),
            quantity = quantity.value,
        )
    }

fun Item.toItemSourceJpaEntities(): List<ItemSourceJpaEntity> =
    sources.map { itemSource ->
        ItemSourceJpaEntity(
            id = UUID.randomUUID(),
            itemId = id.value,
            sourceId = itemSource.sourceId.value,
            quantity = itemSource.quantity.value,
            purchasePriceAmount = itemSource.purchasePrice.amount,
            purchasePriceCurrency = itemSource.purchasePrice.currency.currencyCode,
            purchaseDate = itemSource.purchaseDate,
        )
    }

fun ItemJpaEntity.toDomain(
    locationQuantities: Collection<ItemLocationQuantityJpaEntity>,
    sources: Collection<ItemSourceJpaEntity>,
): Item {
    val item = Item(
        id = ItemId(id),
        name = ItemName.of(name),
        categoryId = CategoryId(categoryId),
        estimatedValue = MonetaryValue.of(estimatedValueAmount),
        note = note,
    )

    locationQuantities.forEach {
        item.setLocationQuantity(
            locationId = LocationId(it.id.locationId),
            quantity = Quantity.of(it.quantity),
        )
    }

    sources.forEach {
        item.recordAcquisition(
            sourceId = SourceId(it.sourceId),
            quantity = Quantity.of(it.quantity),
            purchasePrice = MonetaryValue.of(it.purchasePriceAmount),
            purchaseDate = it.purchaseDate,
        )
    }

    return item
}

fun ItemSource.toJpaEntity(itemId: ItemId): ItemSourceJpaEntity =
    ItemSourceJpaEntity(
        id = UUID.randomUUID(),
        itemId = itemId.value,
        sourceId = sourceId.value,
        quantity = quantity.value,
        purchasePriceAmount = purchasePrice.amount,
        purchasePriceCurrency = purchasePrice.currency.currencyCode,
        purchaseDate = purchaseDate,
    )
