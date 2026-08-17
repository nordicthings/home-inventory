package org.nordicthings.homeinventory.inventory.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

@Entity
@Table(name = "inventory_item_location_quantity")
class ItemLocationQuantityJpaEntity(
    @EmbeddedId
    var id: ItemLocationQuantityJpaId,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,
)

@Embeddable
data class ItemLocationQuantityJpaId(
    @Column(name = "item_id", nullable = false)
    var itemId: UUID = UUID(0, 0),

    @Column(name = "location_id", nullable = false)
    var locationId: UUID = UUID(0, 0),
) : Serializable
