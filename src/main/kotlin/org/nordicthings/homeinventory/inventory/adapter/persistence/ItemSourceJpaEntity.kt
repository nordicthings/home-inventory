package org.nordicthings.homeinventory.inventory.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "inventory_item_source")
class ItemSourceJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "item_id", nullable = false)
    var itemId: UUID,

    @Column(name = "source_id", nullable = false)
    var sourceId: UUID,

    @Column(name = "quantity", nullable = false)
    var quantity: Int,

    @Column(name = "purchase_price_amount", nullable = false)
    var purchasePriceAmount: BigDecimal,

    @Column(name = "purchase_price_currency", nullable = false)
    var purchasePriceCurrency: String,

    @Column(name = "purchase_date")
    var purchaseDate: LocalDate?,
)
