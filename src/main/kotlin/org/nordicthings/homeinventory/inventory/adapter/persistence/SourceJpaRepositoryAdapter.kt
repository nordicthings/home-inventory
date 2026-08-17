package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.springframework.stereotype.Repository

@Repository
class SourceJpaRepositoryAdapter(
    private val repository: SourceJpaEntityRepository,
) : SourceRepository {

    override fun findById(id: SourceId): Source? =
        repository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByNormalizedName(normalizedName: String): Source? =
        repository.findByNormalizedName(normalizedName)?.toDomain()

    override fun save(source: Source): Source =
        repository.save(source.toJpaEntity()).toDomain()

    override fun deleteById(id: SourceId) {
        repository.deleteById(id.value)
    }
}
