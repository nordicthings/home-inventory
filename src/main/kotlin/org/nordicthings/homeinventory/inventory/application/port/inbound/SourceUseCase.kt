package org.nordicthings.homeinventory.inventory.application.port.inbound

import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName

interface SourceUseCase {
    fun createSource(name: SourceName, details: String = ""): Source

    fun renameSource(id: SourceId, name: SourceName): Source

    fun changeSourceDetails(id: SourceId, details: String): Source

    fun deleteSource(id: SourceId)
}
