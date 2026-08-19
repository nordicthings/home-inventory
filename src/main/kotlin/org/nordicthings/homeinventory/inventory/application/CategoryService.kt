package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.application.port.inbound.CategoryUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetCategoryListUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val itemRepository: ItemRepository,
) : CategoryUseCase, GetCategoryListUseCase {
    override fun getCategoryList(): List<Category> =
        categoryRepository.findAllOrderByName()

    override fun createCategory(name: CategoryName): Category {
        ensureUniqueName(name)
        return categoryRepository.save(Category(CategoryId.newId(), name))
    }

    override fun renameCategory(id: CategoryId, name: CategoryName): Category {
        val category = categoryRepository.findById(id)
            ?: throw EntityNotFoundException("Category does not exist: $id")
        ensureUniqueName(name, existingId = id)
        return categoryRepository.save(category.rename(name))
    }

    override fun canDeleteCategory(id: CategoryId): Boolean {
        categoryRepository.findById(id)
            ?: throw EntityNotFoundException("Category does not exist: $id")
        return !itemRepository.existsByCategoryId(id)
    }

    override fun deleteCategory(id: CategoryId) {
        categoryRepository.findById(id)
            ?: throw EntityNotFoundException("Category does not exist: $id")
        if (itemRepository.existsByCategoryId(id)) {
            throw EntityInUseException("Category is still assigned to items: $id")
        }
        categoryRepository.deleteById(id)
    }

    private fun ensureUniqueName(name: CategoryName, existingId: CategoryId? = null) {
        val existingCategory = categoryRepository.findByNormalizedName(name.normalize())
        if (existingCategory != null && existingCategory.id != existingId) {
            throw DuplicateNameException("Category name already exists: ${name.value}")
        }
    }
}
