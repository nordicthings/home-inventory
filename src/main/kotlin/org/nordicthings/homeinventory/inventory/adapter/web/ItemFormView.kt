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
