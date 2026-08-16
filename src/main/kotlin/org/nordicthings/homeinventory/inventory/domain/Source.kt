package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.Entity
import org.jmolecules.ddd.annotation.Identity
import org.jmolecules.ddd.annotation.ValueObject
import java.util.Locale
import java.util.UUID

@Entity
class Source private constructor(
    @Identity
    val id: SourceId,
    val name: SourceName,
    val details: String = "",
) {
    fun rename(name: SourceName): Source =
        Source(id, name, details)

    fun changeDetails(details: String): Source =
        Source(id, name, details)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Source && id == other.id)

    override fun hashCode(): Int = id.hashCode()

    companion object {
        fun create(
            id: SourceId,
            name: SourceName,
            details: String = "",
        ): Source =
            Source(id, name, details)
    }
}

@ValueObject
@JvmInline
value class SourceId(val value: UUID) {
    companion object {
        fun newId(): SourceId = SourceId(UUID.randomUUID())
    }
}

@ValueObject
@JvmInline
value class SourceName private constructor(val value: String) {
    fun normalize(): String = value.lowercase(Locale.ROOT)

    override fun toString(): String = value

    companion object {
        fun of(value: String): SourceName {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "Source name must not be blank." }
            return SourceName(trimmed)
        }
    }
}
