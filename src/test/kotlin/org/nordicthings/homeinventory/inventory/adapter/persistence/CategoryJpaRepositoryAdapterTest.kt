package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest(classes = [HomeInventoryApplication::class, PersistenceAdapterTestConfiguration::class])
class CategoryJpaRepositoryAdapterTest {

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var categoryJpaEntityRepository: CategoryJpaEntityRepository

    @BeforeEach
    fun clearDatabase() {
        categoryJpaEntityRepository.deleteAll()
    }

    @Test
    fun `saves and finds category by id`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))

        categoryRepository.save(category)

        assertEquals(category, categoryRepository.findById(category.id))
    }

    @Test
    fun `finds category by normalized name`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        categoryRepository.save(category)

        val foundCategory = categoryRepository.findByNormalizedName("möbel")

        assertEquals(category, foundCategory)
    }

    @Test
    fun `updates category name and normalized name`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        categoryRepository.save(category)

        val renamedCategory = category.rename(CategoryName.of("Computer & Peripherie"))
        categoryRepository.save(renamedCategory)

        assertEquals("Computer & Peripherie", categoryRepository.findById(category.id)?.name?.value)
        assertNull(categoryRepository.findByNormalizedName("möbel"))
        assertEquals(renamedCategory, categoryRepository.findByNormalizedName("computer & peripherie"))
    }

    @Test
    fun `deletes category by id`() {
        val category = Category(CategoryId.newId(), CategoryName.of("Möbel"))
        categoryRepository.save(category)

        categoryRepository.deleteById(category.id)

        assertNull(categoryRepository.findById(category.id))
    }

    @Test
    fun `enforces unique normalized category name`() {
        categoryRepository.save(Category(CategoryId.newId(), CategoryName.of("Möbel")))

        assertFailsWith<DataIntegrityViolationException> {
            categoryRepository.save(Category(CategoryId.newId(), CategoryName.of("möbel")))
        }
    }
}
