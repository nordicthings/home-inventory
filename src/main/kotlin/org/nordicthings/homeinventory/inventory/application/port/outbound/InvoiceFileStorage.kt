package org.nordicthings.homeinventory.inventory.application.port.outbound

interface InvoiceFileStorage {
    fun store(storedFilename: String, content: ByteArray)

    fun load(storedFilename: String): ByteArray

    fun delete(storedFilename: String)
}
