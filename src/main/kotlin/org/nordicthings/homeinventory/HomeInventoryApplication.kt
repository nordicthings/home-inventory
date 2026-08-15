package org.nordicthings.homeinventory

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HomeInventoryApplication

fun main(args: Array<String>) {
    runApplication<HomeInventoryApplication>(*args)
}
