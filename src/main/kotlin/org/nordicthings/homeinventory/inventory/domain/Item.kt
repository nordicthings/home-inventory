package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.AggregateRoot
import org.jmolecules.ddd.annotation.Identity
import org.jmolecules.ddd.annotation.ValueObject
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@AggregateRoot
class Item(
    @Identity
    val id: ItemId,
    name: ItemName,
    categoryId: CategoryId,
    estimatedValue: MonetaryValue,
    note: String = "",
) {
    private val mutableLocationQuantities = linkedMapOf<LocationId, Quantity>()
    private val mutableSources = linkedMapOf<ItemSourceKey, ItemSource>()

    var name: ItemName = name
        private set

    var categoryId: CategoryId = categoryId
        private set

    var estimatedValue: MonetaryValue = estimatedValue
        private set

    var note: String = note
        private set

    val locationQuantities: Map<LocationId, Quantity>
        get() = mutableLocationQuantities.toMap()

    val sources: List<ItemSource>
        get() = mutableSources.values.toList()

    val totalQuantity: Quantity
        get() = Quantity.of(mutableLocationQuantities.values.sumOf { it.value })

    val value: MonetaryValue?
        get() = MonetaryValue.weightedAverage(sources.map { it.purchasePrice to it.quantity })
            ?: estimatedValue.takeIf { it.isKnown }

    val totalValue: MonetaryValue?
        get() = value?.times(totalQuantity)

    fun rename(name: ItemName) {
        this.name = name
    }

    fun changeCategory(categoryId: CategoryId) {
        this.categoryId = categoryId
    }

    fun changeEstimatedValue(estimatedValue: MonetaryValue) {
        this.estimatedValue = estimatedValue
    }

    fun changeNote(note: String) {
        this.note = note
    }

    fun setLocationQuantity(locationId: LocationId, quantity: Quantity) {
        if (quantity.isZero) {
            mutableLocationQuantities.remove(locationId)
        } else {
            mutableLocationQuantities[locationId] = quantity
        }
    }

    fun relocate(sourceLocationId: LocationId, targetLocationId: LocationId, quantity: Quantity) {
        require(sourceLocationId != targetLocationId) { "Source and target location must differ." }

        val sourceQuantity = mutableLocationQuantities[sourceLocationId]
            ?: error("Source location quantity does not exist.")
        val reducedSourceQuantity = sourceQuantity - quantity
        if (reducedSourceQuantity.isZero) {
            mutableLocationQuantities.remove(sourceLocationId)
        } else {
            mutableLocationQuantities[sourceLocationId] = reducedSourceQuantity
        }

        mutableLocationQuantities[targetLocationId] =
            mutableLocationQuantities[targetLocationId]?.plus(quantity) ?: quantity
    }

    fun removeFromLocation(locationId: LocationId, quantity: Quantity) {
        val currentQuantity = mutableLocationQuantities[locationId]
            ?: error("Location quantity does not exist.")
        val newQuantity = currentQuantity - quantity
        if (newQuantity.isZero) {
            mutableLocationQuantities.remove(locationId)
        } else {
            mutableLocationQuantities[locationId] = newQuantity
        }
    }

    fun recordAcquisition(
        sourceId: SourceId,
        quantity: Quantity,
        purchasePrice: MonetaryValue,
        purchaseDate: LocalDate?,
    ) {
        require(quantity.value > 0) { "Acquisition quantity must be greater than zero." }
        val key = ItemSourceKey(sourceId, purchasePrice, purchaseDate)
        mutableSources[key] = mutableSources[key]?.increaseBy(quantity)
            ?: ItemSource(
                id = ItemSourceId.newId(),
                sourceId = sourceId,
                purchasePrice = purchasePrice,
                purchaseDate = purchaseDate,
                quantity = quantity,
            )
    }

    internal fun addAcquisition(source: ItemSource) {
        require(!mutableSources.containsKey(source.key)) {
            "Item source with same business key already exists."
        }
        mutableSources[source.key] = source
    }

    fun updateAcquisition(
        itemSourceId: ItemSourceId,
        sourceId: SourceId,
        quantity: Quantity,
        purchasePrice: MonetaryValue,
        purchaseDate: LocalDate?,
    ) {
        require(quantity.value > 0) { "Acquisition quantity must be greater than zero." }
        val currentSource = sources.firstOrNull { it.id == itemSourceId }
            ?: error("Item source does not exist.")
        mutableSources.remove(currentSource.key)

        val newSource = ItemSource(
            id = itemSourceId,
            sourceId = sourceId,
            purchasePrice = purchasePrice,
            purchaseDate = purchaseDate,
            quantity = quantity,
        )
        val existingSource = mutableSources[newSource.key]
        mutableSources[newSource.key] = if (existingSource == null) {
            newSource
        } else {
            existingSource.increaseBy(quantity)
        }
    }

    fun deleteAcquisition(itemSourceId: ItemSourceId) {
        val source = sources.firstOrNull { it.id == itemSourceId }
            ?: error("Item source does not exist.")
        mutableSources.remove(source.key)
    }
}

@ValueObject
@JvmInline
value class ItemId(val value: UUID) {
    companion object {
        fun newId(): ItemId = ItemId(UUID.randomUUID())
    }
}

@ValueObject
@JvmInline
value class ItemName private constructor(val value: String) {
    fun normalize(): String = value.lowercase(Locale.ROOT)

    override fun toString(): String = value

    companion object {
        fun of(value: String): ItemName {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "Item name must not be blank." }
            return ItemName(trimmed)
        }
    }
}
