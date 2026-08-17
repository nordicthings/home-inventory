package org.nordicthings.homeinventory.inventory.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "inventory_item")
class ItemJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "normalized_name", nullable = false, unique = true)
    var normalizedName: String,

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID,

    @Column(name = "estimated_value_amount", nullable = false)
    var estimatedValueAmount: BigDecimal,

    @Column(name = "estimated_value_currency", nullable = false)
    var estimatedValueCurrency: String,

    @Column(name = "note", nullable = false)
    var note: String,
)
