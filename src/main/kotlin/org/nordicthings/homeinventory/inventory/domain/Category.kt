package org.nordicthings.homeinventory.inventory.domain

import org.jmolecules.ddd.annotation.ValueObject
import java.util.Locale

@ValueObject
@JvmInline
value class Category private constructor(
    val name: String,
) {
    override fun toString(): String = name

    fun normalize(): String = name.lowercase(Locale.ROOT)

    companion object {
        fun of(name: String): Category {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Category name must not be blank." }
            return Category(trimmed)
        }
    }
}
