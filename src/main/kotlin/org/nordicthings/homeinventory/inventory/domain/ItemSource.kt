package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.ValueObject
import java.time.LocalDate

@ValueObject
data class ItemSource(
    val sourceId: SourceId,
    val purchasePrice: MonetaryValue,
    val purchaseDate: LocalDate?,
    val quantity: Quantity,
) {
    init {
        require(quantity.value > 0) { "Item source quantity must be greater than zero." }
    }

    val key: ItemSourceKey
        get() = ItemSourceKey(sourceId, purchasePrice, purchaseDate)

    fun increaseBy(quantity: Quantity): ItemSource =
        copy(quantity = this.quantity + quantity)
}

@ValueObject
data class ItemSourceKey(
    val sourceId: SourceId,
    val purchasePrice: MonetaryValue,
    val purchaseDate: LocalDate?,
)
