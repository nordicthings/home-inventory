package org.nordicthings.homeinventory.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonetaryValueTest {

    @Test
    fun `zero monetary value is unknown`() {
        val value = MonetaryValue.unknown()

        assertEquals(MonetaryValue.of("0"), value)
        assertFalse(value.isKnown)
    }

    @Test
    fun `positive monetary value is known`() {
        val value = MonetaryValue.of("12.50")

        assertTrue(value.isKnown)
        assertEquals("EUR", value.currency.currencyCode)
    }

    @Test
    fun `monetary values must not be negative`() {
        assertFailsWith<IllegalArgumentException> {
            MonetaryValue.of("-1")
        }
    }

    @Test
    fun `monetary value can be multiplied by quantity`() {
        assertEquals(
            MonetaryValue.of("37.50"),
            MonetaryValue.of("12.50") * Quantity.of(3),
        )
    }
}
