package org.nordicthings.homeinventory.inventory.adapter.web

data class AcquisitionInvoiceUploadPageView(
    val itemId: String,
    val acquisitionId: String,
    val itemName: String,
    val existingFilename: String?,
    val errors: List<FormErrorView> = emptyList(),
) {
    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class AcquisitionInvoiceDeletePageView(
    val itemId: String,
    val acquisitionId: String,
    val itemName: String,
    val filename: String,
)
