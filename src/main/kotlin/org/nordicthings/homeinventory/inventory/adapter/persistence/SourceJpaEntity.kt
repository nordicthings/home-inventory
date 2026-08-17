package org.nordicthings.homeinventory.inventory.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "inventory_source")
class SourceJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "normalized_name", nullable = false, unique = true)
    var normalizedName: String,

    @Column(name = "details", nullable = false)
    var details: String,
)
