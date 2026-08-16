package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.Entity
import org.jmolecules.ddd.annotation.Identity
import org.jmolecules.ddd.annotation.ValueObject
import java.util.Locale
import java.util.UUID

@Entity
class Location(
    @Identity
    val id: LocationId,
    val name: LocationName,
    val type: LocationType,
) {
    fun rename(name: LocationName): Location =
        Location(id, name, type)

    fun changeType(type: LocationType): Location =
        Location(id, name, type)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Location && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun create(
            id: LocationId,
            name: LocationName,
            type: LocationType,
        ): Location =
            Location(id, name, type)
    }
}

@ValueObject
@JvmInline
value class LocationId(val value: UUID) {
    companion object {
        fun newId(): LocationId = LocationId(UUID.randomUUID())
    }
}

@ValueObject
@JvmInline
value class LocationName private constructor(val value: String) {
    fun normalize(): String = value.lowercase(Locale.ROOT)

    override fun toString(): String = value

    companion object {
        fun of(value: String): LocationName {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "Location name must not be blank." }
            return LocationName(trimmed)
        }
    }
}

@ValueObject
enum class LocationType {
    INTERNAL,
    EXTERNAL,
}
