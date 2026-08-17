package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName

fun Category.toJpaEntity(): CategoryJpaEntity =
    CategoryJpaEntity(
        id = id.value,
        name = name.value,
        normalizedName = name.normalize(),
    )

fun CategoryJpaEntity.toDomain(): Category =
    Category(
        id = CategoryId(id),
        name = CategoryName.of(name),
    )
