package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.Entity
import org.jmolecules.ddd.annotation.Identity
import org.jmolecules.ddd.annotation.ValueObject
import java.util.Locale
import java.util.UUID

@Entity
class Category(
    @Identity
    val id: CategoryId,
    val name: CategoryName,
) {
    fun rename(name: CategoryName): Category =
        Category(id, name)

    override fun equals(other: Any?): Boolean =
        this === other || (other is Category && id == other.id)

    override fun hashCode(): Int = id.hashCode()
}

@ValueObject
@JvmInline
value class CategoryId(val value: UUID) {
    companion object {
        fun newId(): CategoryId = CategoryId(UUID.randomUUID())
    }
}

@ValueObject
@JvmInline
value class CategoryName private constructor(val value: String) {
    fun normalize(): String = value.lowercase(Locale.ROOT)

    override fun toString(): String = value

    companion object {
        fun of(value: String): CategoryName {
            val trimmed = value.trim()
            require(trimmed.isNotEmpty()) { "Category name must not be blank." }
            return CategoryName(trimmed)
        }
    }
}
