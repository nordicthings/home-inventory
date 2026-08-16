package org.nordicthings.homeinventory.inventory.domain

fun testItem(
    estimatedValue: MonetaryValue = MonetaryValue.unknown(),
): Item =
    Item(
        id = ItemId.newId(),
        name = ItemName.of("Laptop"),
        categoryId = CategoryId.newId(),
        estimatedValue = estimatedValue,
    )

fun testSource(name: String = "Amazon", details: String = "https://www.amazon.de"): Source =
    Source.create(SourceId.newId(), SourceName.of(name), details)

fun testLocation(name: String = "Küche", type: LocationType = LocationType.INTERNAL): Location =
    Location.create(
        LocationId.newId(),
        LocationName.of(name),
        type,
    )
