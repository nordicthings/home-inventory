package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.domain.Category
import org.nordicthings.homeinventory.inventory.domain.Location
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.nordicthings.homeinventory.inventory.domain.Source

data class CategoryForm(
    var name: String = "",
)

data class LocationForm(
    var name: String = "",
    var type: String = "",
)

data class SourceForm(
    var name: String = "",
    var details: String = "",
)

data class CategoryListPageView(
    val categories: List<CategoryRowView>,
    val errors: List<FormErrorView> = emptyList(),
)

data class LocationListPageView(
    val locations: List<LocationRowView>,
    val errors: List<FormErrorView> = emptyList(),
)

data class SourceListPageView(
    val sources: List<SourceRowView>,
    val errors: List<FormErrorView> = emptyList(),
)

data class CategoryFormPageView(
    val id: String?,
    val form: CategoryForm,
    override val errors: List<FormErrorView> = emptyList(),
) : FormPageView

data class LocationFormPageView(
    val id: String?,
    val form: LocationForm,
    val typeOptions: List<SelectOptionView>,
    override val errors: List<FormErrorView> = emptyList(),
) : FormPageView

data class SourceFormPageView(
    val id: String?,
    val form: SourceForm,
    override val errors: List<FormErrorView> = emptyList(),
) : FormPageView

interface FormPageView {
    val errors: List<FormErrorView>

    val globalErrors: List<FormErrorView>
        get() = errors.filter { it.field == null }

    fun errorFor(field: String): String? =
        errors.firstOrNull { it.field == field }?.message
}

data class CategoryRowView(
    val id: String,
    val name: String,
)

data class LocationRowView(
    val id: String,
    val name: String,
    val type: String,
)

data class SourceRowView(
    val id: String,
    val name: String,
    val details: String,
)

fun Category.toRowView(): CategoryRowView =
    CategoryRowView(
        id = id.value.toString(),
        name = name.value,
    )

fun Location.toRowView(): LocationRowView =
    LocationRowView(
        id = id.value.toString(),
        name = name.value,
        type = type.toViewLabel(),
    )

fun Source.toRowView(): SourceRowView =
    SourceRowView(
        id = id.value.toString(),
        name = name.value,
        details = details,
    )

fun LocationType.toViewLabel(): String =
    when (this) {
        LocationType.INTERNAL -> "intern"
        LocationType.EXTERNAL -> "extern"
    }
