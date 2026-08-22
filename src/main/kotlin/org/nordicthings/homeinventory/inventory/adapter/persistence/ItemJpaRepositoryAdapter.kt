package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.application.ItemListEntry
import org.nordicthings.homeinventory.inventory.application.ItemSearchCriteria
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ItemJpaRepositoryAdapter(
    private val itemRepository: ItemJpaEntityRepository,
    private val categoryRepository: CategoryJpaEntityRepository,
    private val locationQuantityRepository: ItemLocationQuantityJpaEntityRepository,
    private val sourceRepository: ItemSourceJpaEntityRepository,
) : ItemRepository {

    override fun findById(id: ItemId): Item? =
        itemRepository.findById(id.value)
            .map { it.toDomain(loadLocationQuantities(id), loadSources(id)) }
            .orElse(null)

    override fun findByNormalizedName(normalizedName: String): Item? {
        val itemEntity = itemRepository.findByNormalizedName(normalizedName) ?: return null
        val itemId = ItemId(itemEntity.id)
        return itemEntity.toDomain(
            locationQuantities = loadLocationQuantities(itemId),
            sources = loadSources(itemId),
        )
    }

    override fun search(criteria: ItemSearchCriteria): List<ItemListEntry> =
        itemRepository.search(
            normalizedNameContains = criteria.normalizedNameContains,
            categoryId = criteria.categoryId?.value,
            locationId = criteria.locationId?.value,
            sourceId = criteria.sourceId?.value,
        )
            .map { itemEntity ->
                val itemId = ItemId(itemEntity.id)
                val item = itemEntity.toDomain(
                    locationQuantities = loadLocationQuantities(itemId),
                    sources = loadSources(itemId),
                )
                ItemListEntry(
                    id = item.id,
                    name = item.name,
                    categoryId = item.categoryId,
                    categoryName = loadCategoryName(item.categoryId),
                    totalQuantity = item.totalQuantity,
                    averageValue = item.value,
                    totalValue = item.totalValue,
                )
            }

    override fun existsByCategoryId(categoryId: CategoryId): Boolean =
        itemRepository.existsByCategoryId(categoryId.value)

    override fun existsByLocationId(locationId: LocationId): Boolean =
        locationQuantityRepository.existsByIdLocationId(locationId.value)

    override fun existsBySourceId(sourceId: SourceId): Boolean =
        sourceRepository.existsBySourceId(sourceId.value)

    @Transactional
    override fun save(item: Item): Item {
        itemRepository.save(item.toJpaEntity())
        locationQuantityRepository.deleteByIdItemId(item.id.value)
        locationQuantityRepository.saveAll(item.toLocationQuantityJpaEntities())
        val itemSourceEntities = item.toItemSourceJpaEntities()
        if (itemSourceEntities.isEmpty()) {
            sourceRepository.deleteByItemId(item.id.value)
        } else {
            sourceRepository.deleteByItemIdAndIdNotIn(item.id.value, itemSourceEntities.map { it.id })
            sourceRepository.saveAll(itemSourceEntities)
        }
        return item
    }

    @Transactional
    override fun deleteById(id: ItemId) {
        locationQuantityRepository.deleteByIdItemId(id.value)
        sourceRepository.deleteByItemId(id.value)
        itemRepository.deleteById(id.value)
    }

    private fun loadLocationQuantities(id: ItemId): List<ItemLocationQuantityJpaEntity> =
        locationQuantityRepository.findByIdItemId(id.value)

    private fun loadSources(id: ItemId): List<ItemSourceJpaEntity> =
        sourceRepository.findByItemId(id.value)

    private fun loadCategoryName(id: CategoryId): CategoryName =
        categoryRepository.findById(id.value)
            .map { it.toDomain().name }
            .orElseThrow { IllegalStateException("Category does not exist for item: $id") }
}
