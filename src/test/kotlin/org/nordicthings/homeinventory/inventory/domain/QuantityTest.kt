package org.nordicthings.homeinventory.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QuantityTest {

    @Test
    fun `quantities must not be negative`() {
        assertEquals(0, Quantity.of(0).value)

        assertFailsWith<IllegalArgumentException> {
            Quantity.of(-1)
        }
    }

    @Test
    fun `quantities can be added`() {
        val quantity1 = Quantity.of(2)
        val quantity2 = Quantity.of(3)

        val result = quantity1 + quantity2

        assertEquals(5, result.value)
    }

    @Test
    fun `quantities can be subtracted`() {
        val quantity1 = Quantity.of(5)
        val quantity2 = Quantity.of(3)

        val result = quantity1 - quantity2

        assertEquals(2, result.value)
    }

    @Test
    fun `a quantity has a zero-detection`() {
        assertTrue(Quantity.of(0).isZero)
    }

}
