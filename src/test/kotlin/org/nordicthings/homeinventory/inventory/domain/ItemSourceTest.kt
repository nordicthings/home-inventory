package org.nordicthings.homeinventory.inventory.domain

import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDate
import kotlin.test.Test
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
        val itemSource = ItemSource( SourceId.newId(), MonetaryValue.unknown(), today, Quantity.of(1))
        assertThat(itemSource.key.sourceId).isEqualTo(itemSource.sourceId)
        assertThat(itemSource.key.purchasePrice).isEqualTo(itemSource.purchasePrice)
        assertThat(itemSource.key.purchaseDate).isEqualTo(itemSource.purchaseDate)
    }

    @Test
    fun `item source increases quantity`() {
        val itemSource = ItemSource( SourceId.newId(), MonetaryValue.unknown(), null, Quantity.of(1))
        val increased = itemSource.increaseBy(Quantity.of(2))
        assertThat(increased.quantity).isEqualTo(Quantity.of(3))
    }
}
