package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.ItemDetails

data class ItemCreateForm(
    var name: String = "",
    var categoryId: String = "",
    var estimatedValue: String = "",
    var note: String = "",
)

data class ItemEditForm(
    var name: String = "",
    var categoryId: String = "",
    var estimatedValue: String = "",
    var note: String = "",
)

data class ItemLocationQuantityForm(
    var locationId: String = "",
    var quantity: String = "",
)

data class ItemRelocationForm(
    var sourceLocationId: String = "",
    var targetLocationId: String = "",
    var quantity: String = "",
)

data class ItemAcquisitionForm(
    var sourceId: String = "",
    var quantity: String = "",
    var purchasePrice: String = "",
    var purchaseDate: String = "",
)

data class ItemCreatePageView(
    val form: ItemCreateForm,
    val categories: List<SelectOptionView>,
    val errors: List<FormErrorView> = emptyList(),
) {
    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class ItemEditPageView(
    val id: String,
    val form: ItemEditForm,
    val categories: List<SelectOptionView>,
    val errors: List<FormErrorView> = emptyList(),
) {
    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class ItemLocationQuantityPageView(
    val itemId: String,
    val itemName: String,
    val form: ItemLocationQuantityForm,
    val locations: List<SelectOptionView>,
    val locationReadOnly: Boolean = false,
    val errors: List<FormErrorView> = emptyList(),
) {
    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class ItemRelocationPageView(
    val itemId: String,
    val itemName: String,
    val sourceLocationName: String,
    val sourceQuantity: String,
    val form: ItemRelocationForm,
    val locations: List<SelectOptionView>,
    val errors: List<FormErrorView> = emptyList(),
) {
    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class ItemAcquisitionPageView(
    val itemId: String,
    val acquisitionId: String?,
    val itemName: String,
    val form: ItemAcquisitionForm,
    val sources: List<SelectOptionView>,
    val errors: List<FormErrorView> = emptyList(),
) {
    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class FormErrorView(
    val field: String?,
    val message: String,
)

fun ItemDetails.toEditForm(): ItemEditForm =
    ItemEditForm(
        name = name.value,
        categoryId = categoryId.value.toString(),
        estimatedValue = estimatedValue.formatAmountForForm(),
        note = note,
    )
