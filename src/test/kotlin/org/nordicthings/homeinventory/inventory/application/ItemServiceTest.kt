package org.nordicthings.homeinventory.inventory.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ItemServiceTest {

    private val itemRepository = mockk<ItemRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val locationRepository = mockk<LocationRepository>()
    private val sourceRepository = mockk<SourceRepository>()
    private val service = ItemService(
        itemRepository = itemRepository,
        categoryRepository = categoryRepository,
        locationRepository = locationRepository,
        sourceRepository = sourceRepository,
    )

    @Test
    fun `loads item details with master data names calculated values and sorted child entries`() {
        val category = existingCategory()
        val kitchen = Location.create(LocationId.newId(), LocationName.of("Küche"), LocationType.INTERNAL)
        val office = Location.create(LocationId.newId(), LocationName.of("Büro"), LocationType.EXTERNAL)
        val amazon = Source.create(SourceId.newId(), SourceName.of("Amazon"))
        val saturn = Source.create(SourceId.newId(), SourceName.of("Saturn"))
        val item = Item(
            id = ItemId.newId(),
            name = ItemName.of("Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("1000"),
            note = "Arbeitsgerät",
        )
        item.setLocationQuantity(kitchen.id, Quantity.of(1))
        item.setLocationQuantity(office.id, Quantity.of(2))
        item.recordAcquisition(saturn.id, Quantity.of(1), MonetaryValue.of("900"), LocalDate.of(2026, 1, 3))
        item.recordAcquisition(amazon.id, Quantity.of(2), MonetaryValue.of("600"), null)
        every { itemRepository.findById(item.id) } returns item
        every { categoryRepository.findById(category.id) } returns category
        every { locationRepository.findById(kitchen.id) } returns kitchen
        every { locationRepository.findById(office.id) } returns office
        every { sourceRepository.findById(amazon.id) } returns amazon
        every { sourceRepository.findById(saturn.id) } returns saturn

        val details = service.getItemDetails(item.id)

        assertEquals(item.id, details.id)
        assertEquals("Laptop", details.name.value)
        assertEquals("Computer & Peripherie", details.categoryName.value)
        assertEquals(MonetaryValue.of("1000"), details.estimatedValue)
        assertEquals("Arbeitsgerät", details.note)
        assertEquals(listOf("Büro", "Küche"), details.locationQuantities.map { it.locationName.value })
        assertEquals(listOf(Quantity.of(2), Quantity.of(1)), details.locationQuantities.map { it.quantity })
        assertEquals(listOf("Amazon", "Saturn"), details.acquisitions.map { it.sourceName.value })
        assertEquals(item.sources.sortedBy { it.sourceId.value }.map { it.id }.toSet(), details.acquisitions.map { it.id }.toSet())
        assertEquals(Quantity.of(3), details.totalQuantity)
        assertEquals(MonetaryValue.of("700"), details.averageValue)
        assertEquals(MonetaryValue.of("2100"), details.totalValue)
    }

    @Test
    fun `does not load details for missing item`() {
        val itemId = ItemId.newId()
        every { itemRepository.findById(itemId) } returns null

        assertFailsWith<EntityNotFoundException> {
            service.getItemDetails(itemId)
        }
    }

    @Test
    fun `searches items with normalized name filter`() {
        val categoryId = CategoryId.newId()
        val criteria = ItemSearchCriteria(
            normalizedNameContains = "lap",
            categoryId = categoryId,
        )
        val result = listOf(
            ItemListEntry(
                id = ItemId.newId(),
                name = ItemName.of("Laptop"),
                categoryId = categoryId,
                categoryName = CategoryName.of("Computer & Peripherie"),
                totalQuantity = Quantity.of(2),
                averageValue = MonetaryValue.of("800"),
                totalValue = MonetaryValue.of("1600"),
            ),
        )
        every { itemRepository.search(criteria) } returns result

        val foundItems = service.searchItems(
            SearchItemsFilter(
                name = " Lap ",
                categoryId = categoryId,
            ),
        )

        assertEquals(result, foundItems)
        verify { itemRepository.search(criteria) }
    }

    @Test
    fun `creates item when category exists and normalized name is unique`() {
        val category = existingCategory()
        every { itemRepository.findByNormalizedName("laptop") } returns null
        every { categoryRepository.findById(category.id) } returns category
        every { itemRepository.save(any()) } answers { firstArg() }

        val item = service.createItem(
            name = ItemName.of(" Laptop "),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("100"),
            note = "Arbeitsgerät",
        )

        assertEquals("Laptop", item.name.value)
        assertEquals(category.id, item.categoryId)
        assertEquals("Arbeitsgerät", item.note)
        verify { itemRepository.findByNormalizedName("laptop") }
        verify { categoryRepository.findById(category.id) }
        verify { itemRepository.save(item) }
    }

    @Test
    fun `rejects duplicate item name ignoring case`() {
        val category = existingCategory()
        val existingItem = existingItem(categoryId = category.id)
        every { itemRepository.findByNormalizedName("laptop") } returns existingItem

        assertFailsWith<DuplicateNameException> {
            service.createItem(ItemName.of(" laptop "), category.id, MonetaryValue.of("50"), "")
        }

        verify(exactly = 0) { categoryRepository.findById(any()) }
        verify(exactly = 0) { itemRepository.save(any()) }
    }

    @Test
    fun `does not create item for missing category`() {
        val missingCategoryId = CategoryId.newId()
        every { itemRepository.findByNormalizedName("laptop") } returns null
        every { categoryRepository.findById(missingCategoryId) } returns null

        assertFailsWith<EntityNotFoundException> {
            service.createItem(
                name = ItemName.of("Laptop"),
                categoryId = missingCategoryId,
                estimatedValue = MonetaryValue.of("100"),
                note = "",
            )
        }

        verify(exactly = 0) { itemRepository.save(any()) }
    }

    @Test
    fun `renames item when normalized name is still unique`() {
        val item = existingItem()
        every { itemRepository.findById(item.id) } returns item
        every { itemRepository.findByNormalizedName("notebook") } returns null
        every { itemRepository.save(item) } returns item

        val renamedItem = service.renameItem(item.id, ItemName.of("Notebook"))

        assertEquals(item.id, renamedItem.id)
        assertEquals("Notebook", renamedItem.name.value)
        verify { itemRepository.save(renamedItem) }
    }

    @Test
    fun `records location quantity for existing location`() {
        val item = existingItem()
        val location = existingLocation()
        every { itemRepository.findById(item.id) } returns item
        every { locationRepository.findById(location.id) } returns location
        every { itemRepository.save(item) } returns item

        val changedItem = service.setLocationQuantity(item.id, location.id, Quantity.of(2))

        assertEquals(Quantity.of(2), changedItem.locationQuantities[location.id])
        verify { locationRepository.findById(location.id) }
        verify { itemRepository.save(changedItem) }
    }

    @Test
    fun `does not record location quantity for missing location`() {
        val item = existingItem()
        val missingLocationId = LocationId.newId()
        every { itemRepository.findById(item.id) } returns item
        every { locationRepository.findById(missingLocationId) } returns null

        assertFailsWith<EntityNotFoundException> {
            service.setLocationQuantity(item.id, missingLocationId, Quantity.of(1))
        }

        verify(exactly = 0) { itemRepository.save(any()) }
    }

    @Test
    fun `records acquisition for existing source`() {
        val item = existingItem()
        val source = existingSource()
        val purchaseDate = LocalDate.of(2026, 1, 3)
        every { itemRepository.findById(item.id) } returns item
        every { sourceRepository.findById(source.id) } returns source
        every { itemRepository.save(item) } returns item

        val changedItem = service.recordAcquisition(
            id = item.id,
            sourceId = source.id,
            quantity = Quantity.of(1),
            purchasePrice = MonetaryValue.of("80"),
            purchaseDate = purchaseDate,
        )

        assertEquals(source.id, changedItem.sources.single().sourceId)
        verify { sourceRepository.findById(source.id) }
        verify { itemRepository.save(changedItem) }
    }

    @Test
    fun `updates acquisition for existing source`() {
        val item = existingItem()
        val source = existingSource()
        val newSource = Source.create(SourceId.newId(), SourceName.of("Saturn"))
        item.recordAcquisition(source.id, Quantity.of(1), MonetaryValue.of("80"), null)
        val itemSourceId = item.sources.single().id
        every { itemRepository.findById(item.id) } returns item
        every { sourceRepository.findById(newSource.id) } returns newSource
        every { itemRepository.save(item) } returns item

        val changedItem = service.updateAcquisition(
            id = item.id,
            itemSourceId = itemSourceId,
            sourceId = newSource.id,
            quantity = Quantity.of(3),
            purchasePrice = MonetaryValue.of("75"),
            purchaseDate = LocalDate.of(2026, 2, 1),
        )

        val changedSource = changedItem.sources.single()
        assertEquals(itemSourceId, changedSource.id)
        assertEquals(newSource.id, changedSource.sourceId)
        assertEquals(Quantity.of(3), changedSource.quantity)
        verify { itemRepository.save(changedItem) }
    }

    @Test
    fun `deletes acquisition`() {
        val item = existingItem()
        item.recordAcquisition(SourceId.newId(), Quantity.of(1), MonetaryValue.of("80"), null)
        val itemSourceId = item.sources.single().id
        every { itemRepository.findById(item.id) } returns item
        every { itemRepository.save(item) } returns item

        val changedItem = service.deleteAcquisition(item.id, itemSourceId)

        assertEquals(emptyList(), changedItem.sources)
        verify { itemRepository.save(changedItem) }
    }

    @Test
    fun `does not record acquisition for missing source`() {
        val item = existingItem()
        val missingSourceId = SourceId.newId()
        every { itemRepository.findById(item.id) } returns item
        every { sourceRepository.findById(missingSourceId) } returns null

        assertFailsWith<EntityNotFoundException> {
            service.recordAcquisition(
                id = item.id,
                sourceId = missingSourceId,
                quantity = Quantity.of(1),
                purchasePrice = MonetaryValue.of("80"),
                purchaseDate = null,
            )
        }

        verify(exactly = 0) { itemRepository.save(any()) }
    }

    @Test
    fun `deletes item`() {
        val item = existingItem()
        every { itemRepository.findById(item.id) } returns item
        every { itemRepository.deleteById(item.id) } returns Unit

        service.deleteItem(item.id)

        verify { itemRepository.deleteById(item.id) }
    }

    private fun existingCategory(): Category =
        Category(CategoryId.newId(), CategoryName.of("Computer & Peripherie"))

    private fun existingItem(
        categoryId: CategoryId = CategoryId.newId(),
    ): Item =
        Item(
            id = ItemId.newId(),
            name = ItemName.of("Laptop"),
            categoryId = categoryId,
            estimatedValue = MonetaryValue.of("100"),
        )

    private fun existingLocation(): Location =
        Location.create(
            id = LocationId.newId(),
            name = LocationName.of("Arbeitszimmer"),
            type = LocationType.INTERNAL,
        )

    private fun existingSource(): Source =
        Source.create(SourceId.newId(), SourceName.of("Amazon"))
}
