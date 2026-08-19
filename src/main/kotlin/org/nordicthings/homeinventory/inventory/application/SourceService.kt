package org.nordicthings.homeinventory.inventory.application

import org.nordicthings.homeinventory.inventory.application.port.inbound.GetSourceListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.SourceUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName
import org.springframework.stereotype.Service

@Service
class SourceService(
    private val sourceRepository: SourceRepository,
    private val itemRepository: ItemRepository,
) : SourceUseCase, GetSourceListUseCase {
    override fun getSourceList(): List<Source> =
        sourceRepository.findAllOrderByName()

    override fun createSource(name: SourceName, details: String): Source {
        ensureUniqueName(name)
        return sourceRepository.save(Source.create(SourceId.newId(), name, details))
    }

    override fun renameSource(id: SourceId, name: SourceName): Source {
        val source = sourceRepository.findById(id)
            ?: throw EntityNotFoundException("Source does not exist: $id")
        ensureUniqueName(name, existingId = id)
        return sourceRepository.save(source.rename(name))
    }

    override fun changeSourceDetails(id: SourceId, details: String): Source {
        val source = sourceRepository.findById(id)
            ?: throw EntityNotFoundException("Source does not exist: $id")
        return sourceRepository.save(source.changeDetails(details))
    }

    override fun canDeleteSource(id: SourceId): Boolean {
        sourceRepository.findById(id)
            ?: throw EntityNotFoundException("Source does not exist: $id")
        return !itemRepository.existsBySourceId(id)
    }

    override fun deleteSource(id: SourceId) {
        sourceRepository.findById(id)
            ?: throw EntityNotFoundException("Source does not exist: $id")
        if (itemRepository.existsBySourceId(id)) {
            throw EntityInUseException("Source is still assigned to items: $id")
        }
        sourceRepository.deleteById(id)
    }

    private fun ensureUniqueName(name: SourceName, existingId: SourceId? = null) {
        val existingSource = sourceRepository.findByNormalizedName(name.normalize())
        if (existingSource != null && existingSource.id != existingId) {
            throw DuplicateNameException("Source name already exists: ${name.value}")
        }
    }
}
