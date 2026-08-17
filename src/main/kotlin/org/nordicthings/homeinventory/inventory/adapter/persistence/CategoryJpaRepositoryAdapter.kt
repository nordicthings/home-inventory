package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.springframework.stereotype.Repository

@Repository
class CategoryJpaRepositoryAdapter(
    private val repository: CategoryJpaEntityRepository,
) : CategoryRepository {

    override fun findById(id: CategoryId): Category? =
        repository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByNormalizedName(normalizedName: String): Category? =
        repository.findByNormalizedName(normalizedName)?.toDomain()

    override fun save(category: Category): Category =
        repository.save(category.toJpaEntity()).toDomain()

    override fun deleteById(id: CategoryId) {
        repository.deleteById(id.value)
    }
}
