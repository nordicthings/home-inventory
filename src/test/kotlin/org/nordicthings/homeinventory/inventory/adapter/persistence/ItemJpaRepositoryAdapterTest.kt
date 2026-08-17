package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.ItemSearchCriteria
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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [HomeInventoryApplication::class, PersistenceAdapterTestConfiguration::class])
class ItemJpaRepositoryAdapterTest {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Autowired
    private lateinit var itemJpaEntityRepository: ItemJpaEntityRepository

    @Autowired
    private lateinit var itemLocationQuantityJpaEntityRepository: ItemLocationQuantityJpaEntityRepository

    @Autowired
    private lateinit var itemSourceJpaEntityRepository: ItemSourceJpaEntityRepository

    @Autowired
    private lateinit var categoryJpaEntityRepository: CategoryJpaEntityRepository

    @Autowired
    private lateinit var locationJpaEntityRepository: LocationJpaEntityRepository

    @Autowired
    private lateinit var sourceJpaEntityRepository: SourceJpaEntityRepository

    @BeforeEach
    fun clearDatabase() {
        itemSourceJpaEntityRepository.deleteAll()
        itemLocationQuantityJpaEntityRepository.deleteAll()
        itemJpaEntityRepository.deleteAll()
        categoryJpaEntityRepository.deleteAll()
        locationJpaEntityRepository.deleteAll()
        sourceJpaEntityRepository.deleteAll()
    }

    @Test
    fun `saves and finds complete item aggregate by id`() {
        val category = existingCategory()
        val kitchen = existingLocation("Küche")
        val office = existingLocation("Büro", LocationType.EXTERNAL)
        val amazon = existingSource("Amazon")
        val luechau = existingSource("Lüchau")
        val item = Item(
            id = ItemId.newId(),
            name = ItemName.of("Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("1000"),
            note = "Arbeitsgerät",
        )
        item.setLocationQuantity(kitchen.id, Quantity.of(1))
        item.setLocationQuantity(office.id, Quantity.of(2))
        item.recordAcquisition(amazon.id, Quantity.of(1), MonetaryValue.of("800"), LocalDate.of(2026, 1, 3))
        item.recordAcquisition(luechau.id, Quantity.of(2), MonetaryValue.unknown(), null)

        itemRepository.save(item)

        val foundItem = itemRepository.findById(item.id)
        assertEquals(item.id, foundItem?.id)
        assertEquals("Laptop", foundItem?.name?.value)
        assertEquals(category.id, foundItem?.categoryId)
        assertEquals(MonetaryValue.of("1000"), foundItem?.estimatedValue)
        assertEquals("Arbeitsgerät", foundItem?.note)
        assertEquals(Quantity.of(3), foundItem?.totalQuantity)
        assertEquals(Quantity.of(1), foundItem?.locationQuantities?.get(kitchen.id))
        assertEquals(Quantity.of(2), foundItem?.locationQuantities?.get(office.id))
        assertEquals(2, foundItem?.sources?.size)
        assertEquals(MonetaryValue.of("800"), foundItem?.value)
    }

    @Test
    fun `keeps acquisition id when saving and loading item aggregate`() {
        val category = existingCategory()
        val source = existingSource("Amazon")
        val item = existingItem(category.id)
        item.recordAcquisition(source.id, Quantity.of(1), MonetaryValue.of("800"), null)
        val itemSourceId = item.sources.single().id

        itemRepository.save(item)

        val foundItem = assertNotNull(itemRepository.findById(item.id))
        assertEquals(itemSourceId, foundItem.sources.single().id)
    }

    @Test
    fun `finds item by normalized name`() {
        val category = existingCategory()
        val item = existingItem(category.id)
        itemRepository.save(item)

        val foundItem = itemRepository.findByNormalizedName("laptop")

        assertEquals(item.id, foundItem?.id)
    }

    @Test
    fun `searches items for main list ordered by normalized name`() {
        val category = existingCategory()
        val kitchen = existingLocation("Küche")
        val source = existingSource("Amazon")
        val laptop = existingItem(category.id, "Laptop")
        laptop.setLocationQuantity(kitchen.id, Quantity.of(2))
        laptop.recordAcquisition(source.id, Quantity.of(2), MonetaryValue.of("800"), null)
        val monitor = existingItem(category.id, "Monitor")
        monitor.setLocationQuantity(kitchen.id, Quantity.of(1))
        itemRepository.save(monitor)
        itemRepository.save(laptop)

        val entries = itemRepository.search(ItemSearchCriteria())

        assertEquals(listOf("Laptop", "Monitor"), entries.map { it.name.value })
        assertEquals("Computer & Peripherie", entries.first().categoryName.value)
        assertEquals(Quantity.of(2), entries.first().totalQuantity)
        assertEquals(MonetaryValue.of("800"), entries.first().averageValue)
        assertEquals(MonetaryValue.of("1600"), entries.first().totalValue)
    }

    @Test
    fun `searches items by normalized name substring`() {
        val category = existingCategory()
        itemRepository.save(existingItem(category.id, "Laptop"))
        itemRepository.save(existingItem(category.id, "Monitor"))

        val entries = itemRepository.search(ItemSearchCriteria(normalizedNameContains = "top"))

        assertEquals(listOf("Laptop"), entries.map { it.name.value })
    }

    @Test
    fun `searches items by category location and source filters`() {
        val computer = existingCategory("Computer & Peripherie")
        val books = existingCategory("Bücher")
        val kitchen = existingLocation("Küche")
        val office = existingLocation("Büro", LocationType.EXTERNAL)
        val amazon = existingSource("Amazon")
        val saturn = existingSource("Saturn")
        val laptop = existingItem(computer.id, "Laptop")
        laptop.setLocationQuantity(kitchen.id, Quantity.of(1))
        laptop.recordAcquisition(amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        val monitor = existingItem(computer.id, "Monitor")
        monitor.setLocationQuantity(office.id, Quantity.of(1))
        monitor.recordAcquisition(saturn.id, Quantity.of(1), MonetaryValue.of("300"), null)
        val novel = existingItem(books.id, "Roman")
        novel.setLocationQuantity(kitchen.id, Quantity.of(1))
        novel.recordAcquisition(amazon.id, Quantity.of(1), MonetaryValue.unknown(), null)
        itemRepository.save(laptop)
        itemRepository.save(monitor)
        itemRepository.save(novel)

        val entries = itemRepository.search(
            ItemSearchCriteria(
                categoryId = computer.id,
                locationId = kitchen.id,
                sourceId = amazon.id,
            ),
        )

        assertEquals(listOf("Laptop"), entries.map { it.name.value })
    }

    @Test
    fun `updates item and replaces location quantities and sources`() {
        val category = existingCategory()
        val kitchen = existingLocation("Küche")
        val office = existingLocation("Büro", LocationType.EXTERNAL)
        val amazon = existingSource("Amazon")
        val item = existingItem(category.id)
        item.setLocationQuantity(kitchen.id, Quantity.of(2))
        item.recordAcquisition(amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        itemRepository.save(item)

        item.rename(ItemName.of("Notebook"))
        item.changeNote("Neu erfasst")
        item.setLocationQuantity(kitchen.id, Quantity.ZERO)
        item.setLocationQuantity(office.id, Quantity.of(3))
        item.recordAcquisition(amazon.id, Quantity.of(2), MonetaryValue.of("750"), LocalDate.of(2026, 2, 1))
        itemRepository.save(item)

        val foundItem = assertNotNull(itemRepository.findById(item.id))
        assertEquals("Notebook", foundItem.name.value)
        assertEquals("Neu erfasst", foundItem.note)
        assertFalse(foundItem.locationQuantities.containsKey(kitchen.id))
        assertEquals(Quantity.of(3), foundItem.locationQuantities[office.id])
        assertEquals(2, foundItem.sources.size)
    }

    @Test
    fun `answers existence checks for category location and source usage`() {
        val category = existingCategory()
        val kitchen = existingLocation("Küche")
        val amazon = existingSource("Amazon")
        val item = existingItem(category.id)
        item.setLocationQuantity(kitchen.id, Quantity.of(1))
        item.recordAcquisition(amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        itemRepository.save(item)

        assertTrue(itemRepository.existsByCategoryId(category.id))
        assertTrue(itemRepository.existsByLocationId(kitchen.id))
        assertTrue(itemRepository.existsBySourceId(amazon.id))
        assertFalse(itemRepository.existsByCategoryId(CategoryId.newId()))
        assertFalse(itemRepository.existsByLocationId(LocationId.newId()))
        assertFalse(itemRepository.existsBySourceId(SourceId.newId()))
    }

    @Test
    fun `deletes item with location quantities and sources`() {
        val category = existingCategory()
        val kitchen = existingLocation("Küche")
        val amazon = existingSource("Amazon")
        val item = existingItem(category.id)
        item.setLocationQuantity(kitchen.id, Quantity.of(1))
        item.recordAcquisition(amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        itemRepository.save(item)

        itemRepository.deleteById(item.id)

        assertNull(itemRepository.findById(item.id))
        assertFalse(itemRepository.existsByLocationId(kitchen.id))
        assertFalse(itemRepository.existsBySourceId(amazon.id))
    }

    @Test
    fun `enforces unique normalized item name`() {
        val category = existingCategory()
        itemRepository.save(existingItem(category.id, "Laptop"))

        assertFailsWith<DataIntegrityViolationException> {
            itemRepository.save(existingItem(category.id, "laptop"))
        }
    }

    private fun existingCategory(name: String = "Computer & Peripherie"): Category =
        categoryRepository.save(Category(CategoryId.newId(), CategoryName.of(name)))

    private fun existingLocation(
        name: String,
        type: LocationType = LocationType.INTERNAL,
    ): Location =
        locationRepository.save(Location.create(LocationId.newId(), LocationName.of(name), type))

    private fun existingSource(name: String): Source =
        sourceRepository.save(Source.create(SourceId.newId(), SourceName.of(name)))

    private fun existingItem(
        categoryId: CategoryId,
        name: String = "Laptop",
    ): Item =
        Item(
            id = ItemId.newId(),
            name = ItemName.of(name),
            categoryId = categoryId,
            estimatedValue = MonetaryValue.of("1000"),
            note = "Arbeitsgerät",
        )
}
