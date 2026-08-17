package org.nordicthings.homeinventory.inventory.adapter.persistence

import org.nordicthings.homeinventory.inventory.domain.Source
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName

fun Source.toJpaEntity(): SourceJpaEntity =
    SourceJpaEntity(
        id = id.value,
        name = name.value,
        normalizedName = name.normalize(),
        details = details,
    )

fun SourceJpaEntity.toDomain(): Source =
    Source.create(
        id = SourceId(id),
        name = SourceName.of(name),
        details = details,
    )
