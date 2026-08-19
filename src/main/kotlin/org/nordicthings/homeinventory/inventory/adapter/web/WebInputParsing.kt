package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.SourceId
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

internal fun String?.toCategoryIdOrNull(): CategoryId? =
    toUuidOrNull()?.let(::CategoryId)

internal fun String?.toLocationIdOrNull(): LocationId? =
    toUuidOrNull()?.let(::LocationId)

internal fun String?.toSourceIdOrNull(): SourceId? =
    toUuidOrNull()?.let(::SourceId)

internal fun String?.toUuidOrNull(): UUID? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { UUID.fromString(it) }.getOrNull() }

internal fun parseMoneyValue(value: String): BigDecimal {
    val normalizedInput = value.trim()
    if (normalizedInput.isBlank()) {
        return BigDecimal.ZERO
    }
    return normalizedInput.toEstimatedValueOrNull()
        ?: throw IllegalArgumentException("Money value is invalid.")
}

internal fun String.toEstimatedValueOrNull(): BigDecimal? {
    val normalizedInput = trim()
    if (!GERMAN_DECIMAL_PATTERN.matches(normalizedInput)) {
        return null
    }
    return normalizedInput
        .replace(".", "")
        .replace(',', '.')
        .toBigDecimalOrNull()
}

internal fun String.isNegativeGermanDecimal(): Boolean =
    startsWith("-") && GERMAN_DECIMAL_PATTERN.matches(drop(1))

internal fun parseQuantity(value: String): Int =
    value.trim().toQuantityIntOrNull()
        ?: throw IllegalArgumentException("Quantity is invalid.")

internal fun String.toQuantityIntOrNull(): Int? {
    val normalizedInput = trim()
    if (!GERMAN_INTEGER_PATTERN.matches(normalizedInput)) {
        return null
    }
    return normalizedInput.replace(".", "").toIntOrNull()
}

internal fun parsePurchaseDate(value: String): LocalDate? =
    value.trim()
        .takeIf { it.isNotBlank() }
        ?.toLocalDateOrNull()
        ?: if (value.trim().isBlank()) null else throw IllegalArgumentException("Purchase date is invalid.")

internal fun String.toLocalDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(trim()) }.getOrNull()

private val GERMAN_DECIMAL_PATTERN = Regex("""(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2})?""")
private val GERMAN_INTEGER_PATTERN = Regex("""(?:\d+|\d{1,3}(?:\.\d{3})+)""")
