package org.nordicthings.homeinventory

import io.mockk.mockk
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@SpringBootTest(classes = [HomeInventoryApplication::class, HomeInventoryApplicationTests.TestPorts::class])
class HomeInventoryApplicationTests {

    @Test
    fun contextLoads() {
    }

    @TestConfiguration
    class TestPorts {
        @Bean
        fun categoryRepository(): CategoryRepository = mockk(relaxed = true)

        @Bean
        fun itemRepository(): ItemRepository = mockk(relaxed = true)

        @Bean
        fun locationRepository(): LocationRepository = mockk(relaxed = true)

        @Bean
        fun sourceRepository(): SourceRepository = mockk(relaxed = true)
    }
}
