package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType

fun Location.toJpaEntity(): LocationJpaEntity =
    LocationJpaEntity(
        id = id.value,
        name = name.value,
        normalizedName = name.normalize(),
        type = type.name,
    )

fun LocationJpaEntity.toDomain(): Location =
    Location.create(
        id = LocationId(id),
        name = LocationName.of(name),
        type = LocationType.valueOf(type),
    )
