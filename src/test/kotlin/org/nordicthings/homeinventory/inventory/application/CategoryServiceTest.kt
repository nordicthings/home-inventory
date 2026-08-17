package org.nordicthings.homeinventory.inventory.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CategoryServiceTest {

    private val categoryRepository = mockk<CategoryRepository>()
    private val itemRepository = mockk<ItemRepository>()
    private val service = CategoryService(categoryRepository, itemRepository)

    @Test
    fun `creates category with unique normalized name`() {
        every { categoryRepository.findByNormalizedName("möbel") } returns null
        every { categoryRepository.save(any()) } answers { firstArg() }

        val category = service.createCategory(CategoryName.of(" Möbel "))

        assertEquals("Möbel", category.name.value)
        verify { categoryRepository.findByNormalizedName("möbel") }
        verify { categoryRepository.save(category) }
    }

    @Test
    fun `rejects duplicate category name ignoring case`() {
        val existingCategory = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        every { categoryRepository.findByNormalizedName("möbel") } returns existingCategory

        assertFailsWith<DuplicateNameException> {
            service.createCategory(CategoryName.of(" möbel "))
        }

        verify(exactly = 0) { categoryRepository.save(any()) }
    }

    @Test
    fun `renames category when normalized name is still unique`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        every { categoryRepository.findById(category.id) } returns category
        every { categoryRepository.findByNormalizedName("computer & peripherie") } returns null
        every { categoryRepository.save(any()) } answers { firstArg() }

        val renamedCategory = service.renameCategory(category.id, CategoryName.of("Computer & Peripherie"))

        assertEquals(category.id, renamedCategory.id)
        assertEquals("Computer & Peripherie", renamedCategory.name.value)
        verify { categoryRepository.save(renamedCategory) }
    }

    @Test
    fun `does not delete category used by an item`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        every { categoryRepository.findById(category.id) } returns category
        every { itemRepository.existsByCategoryId(category.id) } returns true

        assertFailsWith<EntityInUseException> {
            service.deleteCategory(category.id)
        }

        verify(exactly = 0) { categoryRepository.deleteById(any()) }
    }

    @Test
    fun `deletes unused category`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        every { categoryRepository.findById(category.id) } returns category
        every { itemRepository.existsByCategoryId(category.id) } returns false
        every { categoryRepository.deleteById(category.id) } returns Unit

        service.deleteCategory(category.id)

        verify { categoryRepository.deleteById(category.id) }
    }
}
