package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.Entity
import org.jmolecules.ddd.annotation.Identity
import org.jmolecules.ddd.annotation.ValueObject
import java.time.LocalDate
import java.util.UUID

@Entity
class ItemSource(
    @Identity
    val id: ItemSourceId,
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
        ItemSource(
            id = id,
            sourceId = sourceId,
            purchasePrice = purchasePrice,
            purchaseDate = purchaseDate,
            quantity = this.quantity + quantity,
        )

    override fun equals(other: Any?): Boolean =
        this === other || (other is ItemSource && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}

@ValueObject
@JvmInline
value class ItemSourceId(val value: UUID) {
    companion object {
        fun newId(): ItemSourceId = ItemSourceId(UUID.randomUUID())
    }
}

@ValueObject
data class ItemSourceKey(
    val sourceId: SourceId,
    val purchasePrice: MonetaryValue,
    val purchaseDate: LocalDate?,
)
