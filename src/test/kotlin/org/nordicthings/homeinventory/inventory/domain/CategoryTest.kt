package org.nordicthings.homeinventory.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoryTest {

    @Test
    fun `categories keep their display name and expose normalized name`() {
        val category = Category.of(" Möbel ")

        assertEquals("Möbel", category.name)
        assertEquals("möbel", category.normalize())
    }

    @Test
    fun `categories with empty or blank names are not allowed`() {
        assertFailsWith<IllegalArgumentException> {
            Category.of(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            Category.of("")
        }

    }
}
