package org.nordicthings.homeinventory.inventory.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "inventory_acquisition_invoice")
class AcquisitionInvoiceJpaEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "acquisition_id", nullable = false, unique = true)
    var acquisitionId: UUID,

    @Column(name = "original_filename", nullable = false)
    var originalFilename: String,

    @Column(name = "stored_filename", nullable = false, unique = true)
    var storedFilename: String,
)
