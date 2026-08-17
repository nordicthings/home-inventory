package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.application.port.inbound.GetItemDetailsUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.ItemUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.SearchItemsUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.ItemSource
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Locale

@Service
class ItemService(
    private val itemRepository: ItemRepository,
    private val categoryRepository: CategoryRepository,
    private val locationRepository: LocationRepository,
    private val sourceRepository: SourceRepository,
) : ItemUseCase, SearchItemsUseCase, GetItemDetailsUseCase {
    override fun getItemDetails(id: ItemId): ItemDetails {
        val item = findItem(id)
        val category = categoryRepository.findById(item.categoryId)
            ?: throw EntityNotFoundException("Category does not exist: ${item.categoryId}")
        return ItemDetails(
            id = item.id,
            name = item.name,
            categoryId = item.categoryId,
            categoryName = category.name,
            estimatedValue = item.estimatedValue,
            note = item.note,
            locationQuantities = item.locationQuantities
                .map { (locationId, quantity) -> toLocationQuantityDetails(locationId, quantity) }
                .sortedBy { it.locationName.normalize() },
            acquisitions = item.sources
                .map { source -> toAcquisitionDetails(source) }
                .sortedWith(
                    compareBy<ItemAcquisitionDetails> { it.sourceName.normalize() }
                        .thenBy { it.purchaseDate }
                        .thenBy { it.purchasePrice.amount },
                ),
            totalQuantity = item.totalQuantity,
            averageValue = item.value,
            totalValue = item.totalValue,
        )
    }

    override fun searchItems(filter: SearchItemsFilter): List<ItemListEntry> =
        itemRepository.search(
            ItemSearchCriteria(
                normalizedNameContains = filter.name?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.lowercase(Locale.ROOT),
                categoryId = filter.categoryId,
                locationId = filter.locationId,
                sourceId = filter.sourceId,
            ),
        )

    override fun createItem(
        name: ItemName,
        categoryId: CategoryId,
        estimatedValue: MonetaryValue,
        note: String,
    ): Item {
        ensureUniqueName(name)
        ensureCategoryExists(categoryId)
        return itemRepository.save(
            Item(
                id = ItemId.newId(),
                name = name,
                categoryId = categoryId,
                estimatedValue = estimatedValue,
                note = note,
            ),
        )
    }

    override fun renameItem(id: ItemId, name: ItemName): Item {
        val item = findItem(id)
        ensureUniqueName(name, existingId = id)
        item.rename(name)
        return itemRepository.save(item)
    }

    override fun changeItemCategory(id: ItemId, categoryId: CategoryId): Item {
        val item = findItem(id)
        ensureCategoryExists(categoryId)
        item.changeCategory(categoryId)
        return itemRepository.save(item)
    }

    override fun changeEstimatedValue(id: ItemId, estimatedValue: MonetaryValue): Item {
        val item = findItem(id)
        item.changeEstimatedValue(estimatedValue)
        return itemRepository.save(item)
    }

    override fun changeNote(id: ItemId, note: String): Item {
        val item = findItem(id)
        item.changeNote(note)
        return itemRepository.save(item)
    }

    override fun setLocationQuantity(id: ItemId, locationId: LocationId, quantity: Quantity): Item {
        val item = findItem(id)
        ensureLocationExists(locationId)
        item.setLocationQuantity(locationId, quantity)
        return itemRepository.save(item)
    }

    override fun relocateItem(
        id: ItemId,
        sourceLocationId: LocationId,
        targetLocationId: LocationId,
        quantity: Quantity,
    ): Item {
        val item = findItem(id)
        ensureLocationExists(sourceLocationId)
        ensureLocationExists(targetLocationId)
        item.relocate(sourceLocationId, targetLocationId, quantity)
        return itemRepository.save(item)
    }

    override fun removeFromLocation(id: ItemId, locationId: LocationId, quantity: Quantity): Item {
        val item = findItem(id)
        ensureLocationExists(locationId)
        item.removeFromLocation(locationId, quantity)
        return itemRepository.save(item)
    }

    override fun recordAcquisition(
        id: ItemId,
        sourceId: SourceId,
        quantity: Quantity,
        purchasePrice: MonetaryValue,
        purchaseDate: LocalDate?,
    ): Item {
        val item = findItem(id)
        ensureSourceExists(sourceId)
        item.recordAcquisition(sourceId, quantity, purchasePrice, purchaseDate)
        return itemRepository.save(item)
    }

    override fun deleteItem(id: ItemId) {
        findItem(id)
        itemRepository.deleteById(id)
    }

    private fun findItem(id: ItemId): Item =
        itemRepository.findById(id)
            ?: throw EntityNotFoundException("Item does not exist: $id")

    private fun ensureUniqueName(name: ItemName, existingId: ItemId? = null) {
        val existingItem = itemRepository.findByNormalizedName(name.normalize())
        if (existingItem != null && existingItem.id != existingId) {
            throw DuplicateNameException("Item name already exists: ${name.value}")
        }
    }

    private fun ensureCategoryExists(categoryId: CategoryId) {
        categoryRepository.findById(categoryId)
            ?: throw EntityNotFoundException("Category does not exist: $categoryId")
    }

    private fun ensureLocationExists(locationId: LocationId) {
        locationRepository.findById(locationId)
            ?: throw EntityNotFoundException("Location does not exist: $locationId")
    }

    private fun ensureSourceExists(sourceId: SourceId) {
        sourceRepository.findById(sourceId)
            ?: throw EntityNotFoundException("Source does not exist: $sourceId")
    }

    private fun toLocationQuantityDetails(
        locationId: LocationId,
        quantity: Quantity,
    ): ItemLocationQuantityDetails {
        val location = locationRepository.findById(locationId)
            ?: throw EntityNotFoundException("Location does not exist: $locationId")
        return ItemLocationQuantityDetails(
            locationId = location.id,
            locationName = location.name,
            locationType = location.type,
            quantity = quantity,
        )
    }

    private fun toAcquisitionDetails(source: ItemSource): ItemAcquisitionDetails {
        val sourceDetails = sourceRepository.findById(source.sourceId)
            ?: throw EntityNotFoundException("Source does not exist: ${source.sourceId}")
        return ItemAcquisitionDetails(
            sourceId = source.sourceId,
            sourceName = sourceDetails.name,
            quantity = source.quantity,
            purchasePrice = source.purchasePrice,
            purchaseDate = source.purchaseDate,
        )
    }
}
