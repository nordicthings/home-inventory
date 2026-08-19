package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName

interface CategoryUseCase {
    fun createCategory(name: CategoryName): Category

    fun renameCategory(id: CategoryId, name: CategoryName): Category

    fun canDeleteCategory(id: CategoryId): Boolean

    fun deleteCategory(id: CategoryId)
}
