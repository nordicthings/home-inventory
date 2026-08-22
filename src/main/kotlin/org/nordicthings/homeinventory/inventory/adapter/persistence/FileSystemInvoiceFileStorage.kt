package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.application.AcquisitionInvoiceProperties
import org.nordicthings.homeinventory.inventory.application.EntityNotFoundException
import org.nordicthings.homeinventory.inventory.application.port.outbound.InvoiceFileStorage
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

@Component
class FileSystemInvoiceFileStorage(
    properties: AcquisitionInvoiceProperties,
) : InvoiceFileStorage {
    private val storagePath: Path = Path.of(properties.storagePath).normalize()

    override fun store(storedFilename: String, content: ByteArray) {
        try {
            Files.createDirectories(storagePath)
            Files.write(
                resolve(storedFilename),
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            )
        } catch (exception: IOException) {
            throw IllegalStateException("Could not store invoice file: $storedFilename", exception)
        }
    }

    override fun load(storedFilename: String): ByteArray {
        val file = resolve(storedFilename)
        if (!Files.exists(file)) {
            throw EntityNotFoundException("Invoice file does not exist: $storedFilename")
        }
        try {
            return Files.readAllBytes(file)
        } catch (exception: IOException) {
            throw IllegalStateException("Could not load invoice file: $storedFilename", exception)
        }
    }

    override fun delete(storedFilename: String) {
        try {
            Files.deleteIfExists(resolve(storedFilename))
        } catch (exception: IOException) {
            throw IllegalStateException("Could not delete invoice file: $storedFilename", exception)
        }
    }

    private fun resolve(storedFilename: String): Path {
        require(storedFilename == Path.of(storedFilename).fileName.toString()) {
            "Stored filename must not contain path segments."
        }
        return storagePath.resolve(storedFilename).normalize()
    }
}
