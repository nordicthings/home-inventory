package org.nordicthings.homeinventory.inventory.application.port.outbound

import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId

interface CategoryRepository {
    fun findById(id: CategoryId): Category?

    fun findByNormalizedName(normalizedName: String): Category?

    fun save(category: Category): Category

    fun deleteById(id: CategoryId)
}
