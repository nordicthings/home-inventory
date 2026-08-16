package org.nordicthings.homeinventory.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceTest {

    @Test
    fun `source name keeps display value and exposes normalized value`() {
        val sourceName = SourceName.of(" Amazon ")

        assertEquals("Amazon", sourceName.value)
        assertEquals("amazon", sourceName.normalize())
    }

    @Test
    fun `source changes create a new instance`() {
        val source = testSource(
            name = "Amazon",
            details = " https://example.test ",
        )

        val renamedSource = source.rename(SourceName.of("IKEA"))
        val changedDetailsSource = source.changeDetails(" Berlin ")

        assertFalse(source === renamedSource)
        assertTrue(source == renamedSource)
        assertFalse(source === changedDetailsSource)
        assertTrue(source == changedDetailsSource)

        assertEquals("Amazon", source.name.value)
        assertEquals(" https://example.test ", source.details)
        assertEquals("IKEA", renamedSource.name.value)
        assertEquals(" Berlin ", changedDetailsSource.details)
    }
}
