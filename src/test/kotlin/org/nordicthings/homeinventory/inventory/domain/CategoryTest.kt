package org.nordicthings.homeinventory.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoryTest {

    @Test
    fun `categories keep their display name and expose normalized name`() {
        val categoryName = CategoryName.of(" Möbel ")

        assertEquals("Möbel", categoryName.value)
        assertEquals("möbel", categoryName.normalize())
    }

    @Test
    fun `category rename creates new instance with same identity`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))

        val renamedCategory = category.rename(CategoryName.of("Computer & Peripherie"))

        assertEquals(category.id, renamedCategory.id)
        assertEquals("Möbel", category.name.value)
        assertEquals("Computer & Peripherie", renamedCategory.name.value)
    }

    @Test
    fun `categories with empty or blank names are not allowed`() {
        assertFailsWith<IllegalArgumentException> {
            CategoryName.of(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            CategoryName.of("")
        }

    }
}
