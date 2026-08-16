package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.ValueObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

@ValueObject
class MonetaryValue private constructor(
    val amount: BigDecimal,
    val currency: Currency,
) {
    val isKnown: Boolean
        get() = amount.compareTo(BigDecimal.ZERO) != 0

    operator fun times(quantity: Quantity): MonetaryValue =
        of(amount.multiply(BigDecimal(quantity.value)))

    override fun equals(other: Any?): Boolean =
        this === other ||
            (other is MonetaryValue &&
                amount.compareTo(other.amount) == 0 &&
                currency == other.currency)

    override fun hashCode(): Int =
        31 * amount.stripTrailingZeros().hashCode() + currency.hashCode()

    override fun toString(): String = "$amount ${currency.currencyCode}"

    companion object {
        private val DEFAULT_CURRENCY: Currency = Currency.getInstance("EUR")

        fun unknown(): MonetaryValue = of(BigDecimal.ZERO)

        fun of(amount: String): MonetaryValue = of(BigDecimal(amount))

        fun of(amount: BigDecimal): MonetaryValue {
            require(amount >= BigDecimal.ZERO) { "MonetaryValue amount must not be negative." }
            return MonetaryValue(amount.setScale(2, RoundingMode.HALF_UP), DEFAULT_CURRENCY)
        }

        fun weightedAverage(values: Collection<Pair<MonetaryValue, Quantity>>): MonetaryValue? {
            val knownValues = values.filter { (monetaryValue, _) -> monetaryValue.isKnown }
            if (knownValues.isEmpty()) {
                return null
            }

            val currency = knownValues.first().first.currency
            require(knownValues.all { (monetaryValue, _) -> monetaryValue.currency == currency }) {
                "Cannot average monetary values with different currencies."
            }

            val weightedSum = knownValues.fold(BigDecimal.ZERO) { sum, (monetaryValue, quantity) ->
                sum + monetaryValue.amount.multiply(BigDecimal(quantity.value))
            }
            val totalQuantity = knownValues.sumOf { (_, quantity) -> quantity.value }

            return of(
                weightedSum.divide(BigDecimal(totalQuantity), 2, RoundingMode.HALF_UP),
            )
        }
    }
}
