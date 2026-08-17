package org.nordicthings.homeinventory.inventory.domain

import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ItemSourceTest {

    @Test
    fun `item source quantities must be positive`() {
        assertFailsWith<IllegalArgumentException> {
            testItem().recordAcquisition(
                sourceId = SourceId.newId(),
                quantity = Quantity.of(0),
                purchasePrice = MonetaryValue.unknown(),
                purchaseDate = null,
            )
        }
    }

    @Test
    fun `item source has a key`() {
        val today = LocalDate.now()
        val itemSource = ItemSource(
            id = ItemSourceId.newId(),
            sourceId = SourceId.newId(),
            purchasePrice = MonetaryValue.unknown(),
            purchaseDate = today,
            quantity = Quantity.of(1),
        )
        assertThat(itemSource.key.sourceId).isEqualTo(itemSource.sourceId)
        assertThat(itemSource.key.purchasePrice).isEqualTo(itemSource.purchasePrice)
        assertThat(itemSource.key.purchaseDate).isEqualTo(itemSource.purchaseDate)
    }

    @Test
    fun `item source increases quantity`() {
        val itemSource = ItemSource(
            id = ItemSourceId.newId(),
            sourceId = SourceId.newId(),
            purchasePrice = MonetaryValue.unknown(),
            purchaseDate = null,
            quantity = Quantity.of(1),
        )
        val increased = itemSource.increaseBy(Quantity.of(2))
        assertThat(increased.quantity).isEqualTo(Quantity.of(3))
    }

    @Test
    fun `item source equality uses identity`() {
        val id = ItemSourceId.newId()
        val source = ItemSource(
            id = id,
            sourceId = SourceId.newId(),
            purchasePrice = MonetaryValue.of("10"),
            purchaseDate = null,
            quantity = Quantity.of(1),
        )
        val changedSource = ItemSource(
            id = id,
            sourceId = SourceId.newId(),
            purchasePrice = MonetaryValue.of("20"),
            purchaseDate = LocalDate.of(2026, 1, 3),
            quantity = Quantity.of(3),
        )

        assertEquals(source, changedSource)
    }
}
