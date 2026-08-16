package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.ValueObject

@ValueObject
@JvmInline
value class Quantity private constructor(val value: Int) {
    operator fun plus(other: Quantity): Quantity = of(value + other.value)

    operator fun minus(other: Quantity): Quantity {
        require(other.value <= value) { "Quantity to subtract must not exceed current quantity." }
        return of(value - other.value)
    }

    val isZero: Boolean
        get() = value == 0

    companion object {
        val ZERO = of(0)
        fun of(value: Int): Quantity {
            require(value >= 0) { "Quantity must not be negative." }
            return Quantity(value)
        }
    }

}
