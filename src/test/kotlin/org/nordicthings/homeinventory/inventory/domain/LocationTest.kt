package org.nordicthings.homeinventory.inventory.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocationTest {

    @Test
    fun `location name keeps display value and exposes normalized value`() {
        val locationName = LocationName.of(" Küche ")

        assertEquals("Küche", locationName.value)
        assertEquals("küche", locationName.normalize())
    }

    @Test
    fun `location changes create a new instance`() {
        val location = testLocation(
            name = "Küche",
            type = LocationType.INTERNAL,
        )

        val renamedLocation = location.rename(LocationName.of("Büro"))
        val externalLocation = location.changeType(LocationType.EXTERNAL)

        assertFalse { renamedLocation === location }
        assertFalse { externalLocation === location }
        assertTrue { renamedLocation == location }
        assertTrue { externalLocation == location }

        assertEquals("Küche", location.name.value)
        assertEquals(LocationType.INTERNAL, location.type)
        assertEquals("Büro", renamedLocation.name.value)
        assertEquals(LocationType.EXTERNAL, externalLocation.type)
    }
}
