package org.nordicthings.homeinventory.inventory.domain

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ItemTest {

    fun `item id is initialized to new id`() {
        val item = testItem()

        assertEquals(item.id, item.id)
    }
    @Test
    fun `item name keeps display value and exposes normalized value`() {
        val itemName = ItemName.of(" Laptop ")

        assertEquals("Laptop", itemName.value)
        assertEquals("laptop", itemName.normalize())
    }

    @Test
    fun `item rename changes item name`() {
        val item = testItem()
        item.rename(ItemName.of(" Printer "))
        assertEquals("Printer", item.name.value)
    }

    @Test
    fun `item changeCategory changes item category`() {
        val item = testItem()
        val currentCategory = item.category
        val newCategory = Category.of(currentCategory.name.uppercase())
        item.changeCategory(newCategory)
        assertEquals(newCategory, item.category)
    }

    @Test
    fun `item changeEstimatedValue changes item estimated value`() {
        val item = testItem()
        val currentEstimatedValue = item.estimatedValue
        val newEstimatedValue = currentEstimatedValue * Quantity.of(2)
        item.changeEstimatedValue(newEstimatedValue)
        assertEquals(newEstimatedValue, item.estimatedValue)
    }

    @Test
    fun `item changeNote changes item note`() {
        val item = testItem()
        val currentNote = item.note
        val newNote = "$currentNote Updated."
        item.changeNote(newNote)
        assertEquals(newNote, item.note)
    }

    @Test
    fun `item can exist without location quantity`() {
        val item = testItem()

        assertEquals(Quantity.ZERO, item.totalQuantity)
        assertNull(item.totalValue)
        assertEquals(emptyMap(), item.locationQuantities)
    }

    @Test
    fun `removing last item from a location removes the location assignment`() {
        val item = testItem()
        val locationId = LocationId.newId()
        item.setLocationQuantity(locationId, Quantity.of(2))

        item.removeFromLocation(locationId, Quantity.of(2))

        assertEquals(Quantity.of(0), item.totalQuantity)
        assertFalse(item.locationQuantities.containsKey(locationId))
    }

    @Test
    fun `setting a location quantity to zero removes the location assignment`() {
        val item = testItem()
        val locationId = LocationId.newId()
        item.setLocationQuantity(locationId, Quantity.of(2))

        item.setLocationQuantity(locationId, Quantity.of(0))

        assertEquals(Quantity.of(0), item.totalQuantity)
        assertFalse(item.locationQuantities.containsKey(locationId))
    }

    @Test
    fun `relocation atomically decrements source and increments target`() {
        val item = testItem()
        val sourceLocationId = LocationId.newId()
        val targetLocationId = LocationId.newId()
        item.setLocationQuantity(sourceLocationId, Quantity.of(5))
        item.setLocationQuantity(targetLocationId, Quantity.of(1))

        item.relocate(sourceLocationId, targetLocationId, Quantity.of(2))

        assertEquals(3, item.locationQuantities[sourceLocationId]?.value)
        assertEquals(3, item.locationQuantities[targetLocationId]?.value)
        assertEquals(Quantity.of(6), item.totalQuantity)
    }

    @Test
    fun `recording acquisition does not change location quantity`() {
        val item = testItem()

        item.recordAcquisition(
            sourceId = SourceId.newId(),
            quantity = Quantity.of(4),
            purchasePrice = MonetaryValue.of("10"),
            purchaseDate = LocalDate.of(2026, 1, 3),
        )

        assertEquals(Quantity.of(0), item.totalQuantity)
        assertEquals(4, item.sources.single().quantity.value)
    }

    @Test
    fun `same item source is merged by increasing quantity`() {
        val item = testItem()
        val sourceId = SourceId.newId()
        val purchaseDate = LocalDate.of(2026, 1, 3)

        item.recordAcquisition(sourceId, Quantity.of(1), MonetaryValue.of("10"), purchaseDate)
        item.recordAcquisition(sourceId, Quantity.of(2), MonetaryValue.of("10"), purchaseDate)

        assertEquals(1, item.sources.size)
        assertEquals(3, item.sources.single().quantity.value)
    }

    @Test
    fun `same source with different price creates separate item source`() {
        val item = testItem()
        val sourceId = SourceId.newId()
        val purchaseDate = LocalDate.of(2026, 1, 3)

        item.recordAcquisition(sourceId, Quantity.of(1), MonetaryValue.of("10"), purchaseDate)
        item.recordAcquisition(sourceId, Quantity.of(1), MonetaryValue.of("12"), purchaseDate)

        assertEquals(2, item.sources.size)
    }

    @Test
    fun `item value is weighted average of known purchase prices`() {
        val item = testItem(estimatedValue = MonetaryValue.of("99"))
        val sourceId = SourceId.newId()

        item.recordAcquisition(sourceId, Quantity.of(4), MonetaryValue.of("10"), null)
        item.recordAcquisition(sourceId, Quantity.of(2), MonetaryValue.of("20"), LocalDate.of(2026, 2, 1))
        item.recordAcquisition(sourceId, Quantity.of(10), MonetaryValue.unknown(), LocalDate.of(2026, 3, 1))

        assertEquals(MonetaryValue.of("13.33"), item.value)
    }

    @Test
    fun `item value falls back to known estimated value`() {
        val item = testItem(estimatedValue = MonetaryValue.of("42"))

        item.recordAcquisition(SourceId.newId(), Quantity.of(1), MonetaryValue.unknown(), null)

        assertEquals(MonetaryValue.of("42"), item.value)
    }

    @Test
    fun `item value is unknown when purchase prices and estimated value are unknown`() {
        val item = testItem(estimatedValue = MonetaryValue.unknown())

        item.recordAcquisition(SourceId.newId(), Quantity.of(1), MonetaryValue.unknown(), null)

        assertNull(item.value)
    }

    @Test
    fun `total value is total location quantity times average item value`() {
        val item = testItem()
        item.setLocationQuantity(LocationId.newId(), Quantity.of(3))
        item.recordAcquisition(SourceId.newId(), Quantity.of(1), MonetaryValue.of("12.50"), null)

        assertEquals(MonetaryValue.of("37.50"), item.totalValue)
    }
}
