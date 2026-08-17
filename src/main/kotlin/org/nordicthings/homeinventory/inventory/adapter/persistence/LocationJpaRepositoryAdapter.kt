package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.springframework.stereotype.Repository

@Repository
class LocationJpaRepositoryAdapter(
    private val repository: LocationJpaEntityRepository,
) : LocationRepository {

    override fun findById(id: LocationId): Location? =
        repository.findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByNormalizedName(normalizedName: String): Location? =
        repository.findByNormalizedName(normalizedName)?.toDomain()

    override fun findAllOrderByName(): List<Location> =
        repository.findAllByOrderByNormalizedNameAsc().map { it.toDomain() }

    override fun save(location: Location): Location =
        repository.save(location.toJpaEntity()).toDomain()

    override fun deleteById(id: LocationId) {
        repository.deleteById(id.value)
    }
}
