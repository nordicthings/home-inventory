package org.nordicthings.homeinventory.inventory.application.port.outbound

import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId

interface SourceRepository {
    fun findById(id: SourceId): Source?

    fun findByNormalizedName(normalizedName: String): Source?

    fun save(source: Source): Source

    fun deleteById(id: SourceId)
}
