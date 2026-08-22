package org.nordicthings.homeinventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class HomeInventoryApplication

fun main(args: Array<String>) {
    runApplication<HomeInventoryApplication>(*args)
}
