package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.DuplicateNameException
import org.nordicthings.homeinventory.inventory.application.EntityInUseException
import org.nordicthings.homeinventory.inventory.application.EntityNotFoundException
import org.nordicthings.homeinventory.inventory.application.port.inbound.CategoryUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetCategoryListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetLocationListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetSourceListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.LocationUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.SourceUseCase
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.CategoryName
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.LocationName
import org.nordicthings.homeinventory.inventory.domain.LocationType
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.nordicthings.homeinventory.inventory.domain.SourceName
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Controller
class MasterDataWebController(
    private val getCategoryListUseCase: GetCategoryListUseCase,
    private val categoryUseCase: CategoryUseCase,
    private val getLocationListUseCase: GetLocationListUseCase,
    private val locationUseCase: LocationUseCase,
    private val getSourceListUseCase: GetSourceListUseCase,
    private val sourceUseCase: SourceUseCase,
) {

    @GetMapping("/categories")
    fun categories(model: Model): String {
        model.addAttribute("page", categoryListPage())
        return "master-data/categories"
    }

    @GetMapping("/categories/new")
    fun newCategory(model: Model): String {
        model.addAttribute("page", CategoryFormPageView(null, CategoryForm()))
        return "master-data/category-form"
    }

    @GetMapping("/categories/{id}/edit")
    fun editCategory(@PathVariable id: UUID, model: Model): String {
        val category = getCategoryListUseCase.getCategoryList().firstOrNull { it.id.value == id }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie wurde nicht gefunden.")
        model.addAttribute("page", CategoryFormPageView(id.toString(), CategoryForm(category.name.value)))
        return "master-data/category-form"
    }

    @PostMapping("/categories")
    fun createCategory(@ModelAttribute form: CategoryForm, model: Model): String {
        val errors = validateName(form.name, "Name ist erforderlich.")
        if (errors.isNotEmpty()) {
            model.addAttribute("page", CategoryFormPageView(null, form, errors))
            return "master-data/category-form"
        }
        return try {
            categoryUseCase.createCategory(CategoryName.of(form.name))
            "redirect:/categories"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", CategoryFormPageView(null, form, listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "master-data/category-form"
        }
    }

    @PostMapping("/categories/{id}")
    fun updateCategory(@PathVariable id: UUID, @ModelAttribute form: CategoryForm, model: Model): String {
        val errors = validateName(form.name, "Name ist erforderlich.")
        if (errors.isNotEmpty()) {
            model.addAttribute("page", CategoryFormPageView(id.toString(), form, errors))
            return "master-data/category-form"
        }
        return try {
            categoryUseCase.renameCategory(CategoryId(id), CategoryName.of(form.name))
            "redirect:/categories"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", CategoryFormPageView(id.toString(), form, listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "master-data/category-form"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie wurde nicht gefunden.", exception)
        }
    }

    @GetMapping("/categories/{id}/delete")
    fun confirmDeleteCategory(@PathVariable id: UUID, model: Model): String =
        try {
            if (!categoryUseCase.canDeleteCategory(CategoryId(id))) {
                model.addAttribute("page", categoryListPage(listOf(FormErrorView(null, "Kategorie wird noch verwendet und kann nicht gelöscht werden."))))
                return "master-data/categories"
            }
            val category = getCategoryListUseCase.getCategoryList().firstOrNull { it.id.value == id }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie wurde nicht gefunden.")
            model.addAttribute("category", category.toRowView())
            "master-data/category-delete"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie wurde nicht gefunden.", exception)
        }

    @PostMapping("/categories/{id}/delete")
    fun deleteCategory(@PathVariable id: UUID, model: Model): String =
        try {
            categoryUseCase.deleteCategory(CategoryId(id))
            "redirect:/categories"
        } catch (exception: EntityInUseException) {
            model.addAttribute("page", categoryListPage(listOf(FormErrorView(null, "Kategorie wird noch verwendet und kann nicht gelöscht werden."))))
            "master-data/categories"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Kategorie wurde nicht gefunden.", exception)
        }

    @GetMapping("/locations")
    fun locations(model: Model): String {
        model.addAttribute("page", locationListPage())
        return "master-data/locations"
    }

    @GetMapping("/locations/new")
    fun newLocation(model: Model): String {
        model.addAttribute("page", LocationFormPageView(null, LocationForm(), locationTypeOptions()))
        return "master-data/location-form"
    }

    @GetMapping("/locations/{id}/edit")
    fun editLocation(@PathVariable id: UUID, model: Model): String {
        val location = getLocationListUseCase.getLocationList().firstOrNull { it.id.value == id }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ort wurde nicht gefunden.")
        model.addAttribute("page", LocationFormPageView(id.toString(), LocationForm(location.name.value, location.type.name), locationTypeOptions()))
        return "master-data/location-form"
    }

    @PostMapping("/locations")
    fun createLocation(@ModelAttribute form: LocationForm, model: Model): String {
        val errors = validateLocationForm(form)
        if (errors.isNotEmpty()) {
            model.addAttribute("page", LocationFormPageView(null, form, locationTypeOptions(), errors))
            return "master-data/location-form"
        }
        return try {
            locationUseCase.createLocation(LocationName.of(form.name), LocationType.valueOf(form.type))
            "redirect:/locations"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", LocationFormPageView(null, form, locationTypeOptions(), listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "master-data/location-form"
        }
    }

    @PostMapping("/locations/{id}")
    fun updateLocation(@PathVariable id: UUID, @ModelAttribute form: LocationForm, model: Model): String {
        val errors = validateLocationForm(form)
        if (errors.isNotEmpty()) {
            model.addAttribute("page", LocationFormPageView(id.toString(), form, locationTypeOptions(), errors))
            return "master-data/location-form"
        }
        return try {
            locationUseCase.renameLocation(LocationId(id), LocationName.of(form.name))
            locationUseCase.changeLocationType(LocationId(id), LocationType.valueOf(form.type))
            "redirect:/locations"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", LocationFormPageView(id.toString(), form, locationTypeOptions(), listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "master-data/location-form"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ort wurde nicht gefunden.", exception)
        }
    }

    @GetMapping("/locations/{id}/delete")
    fun confirmDeleteLocation(@PathVariable id: UUID, model: Model): String =
        try {
            if (!locationUseCase.canDeleteLocation(LocationId(id))) {
                model.addAttribute("page", locationListPage(listOf(FormErrorView(null, "Ort wird noch verwendet und kann nicht gelöscht werden."))))
                return "master-data/locations"
            }
            val location = getLocationListUseCase.getLocationList().firstOrNull { it.id.value == id }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ort wurde nicht gefunden.")
            model.addAttribute("location", location.toRowView())
            "master-data/location-delete"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ort wurde nicht gefunden.", exception)
        }

    @PostMapping("/locations/{id}/delete")
    fun deleteLocation(@PathVariable id: UUID, model: Model): String =
        try {
            locationUseCase.deleteLocation(LocationId(id))
            "redirect:/locations"
        } catch (exception: EntityInUseException) {
            model.addAttribute("page", locationListPage(listOf(FormErrorView(null, "Ort wird noch verwendet und kann nicht gelöscht werden."))))
            "master-data/locations"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Ort wurde nicht gefunden.", exception)
        }

    @GetMapping("/sources")
    fun sources(model: Model): String {
        model.addAttribute("page", sourceListPage())
        return "master-data/sources"
    }

    @GetMapping("/sources/new")
    fun newSource(model: Model): String {
        model.addAttribute("page", SourceFormPageView(null, SourceForm()))
        return "master-data/source-form"
    }

    @GetMapping("/sources/{id}/edit")
    fun editSource(@PathVariable id: UUID, model: Model): String {
        val source = getSourceListUseCase.getSourceList().firstOrNull { it.id.value == id }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bezugsquelle wurde nicht gefunden.")
        model.addAttribute("page", SourceFormPageView(id.toString(), SourceForm(source.name.value, source.details)))
        return "master-data/source-form"
    }

    @PostMapping("/sources")
    fun createSource(@ModelAttribute form: SourceForm, model: Model): String {
        val errors = validateName(form.name, "Name ist erforderlich.")
        if (errors.isNotEmpty()) {
            model.addAttribute("page", SourceFormPageView(null, form, errors))
            return "master-data/source-form"
        }
        return try {
            sourceUseCase.createSource(SourceName.of(form.name), form.details.trim())
            "redirect:/sources"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", SourceFormPageView(null, form, listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "master-data/source-form"
        }
    }

    @PostMapping("/sources/{id}")
    fun updateSource(@PathVariable id: UUID, @ModelAttribute form: SourceForm, model: Model): String {
        val errors = validateName(form.name, "Name ist erforderlich.")
        if (errors.isNotEmpty()) {
            model.addAttribute("page", SourceFormPageView(id.toString(), form, errors))
            return "master-data/source-form"
        }
        return try {
            sourceUseCase.renameSource(SourceId(id), SourceName.of(form.name))
            sourceUseCase.changeSourceDetails(SourceId(id), form.details.trim())
            "redirect:/sources"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", SourceFormPageView(id.toString(), form, listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "master-data/source-form"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bezugsquelle wurde nicht gefunden.", exception)
        }
    }

    @GetMapping("/sources/{id}/delete")
    fun confirmDeleteSource(@PathVariable id: UUID, model: Model): String =
        try {
            if (!sourceUseCase.canDeleteSource(SourceId(id))) {
                model.addAttribute("page", sourceListPage(listOf(FormErrorView(null, "Bezugsquelle wird noch verwendet und kann nicht gelöscht werden."))))
                return "master-data/sources"
            }
            val source = getSourceListUseCase.getSourceList().firstOrNull { it.id.value == id }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bezugsquelle wurde nicht gefunden.")
            model.addAttribute("source", source.toRowView())
            "master-data/source-delete"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bezugsquelle wurde nicht gefunden.", exception)
        }

    @PostMapping("/sources/{id}/delete")
    fun deleteSource(@PathVariable id: UUID, model: Model): String =
        try {
            sourceUseCase.deleteSource(SourceId(id))
            "redirect:/sources"
        } catch (exception: EntityInUseException) {
            model.addAttribute("page", sourceListPage(listOf(FormErrorView(null, "Bezugsquelle wird noch verwendet und kann nicht gelöscht werden."))))
            "master-data/sources"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bezugsquelle wurde nicht gefunden.", exception)
        }

    private fun categoryListPage(errors: List<FormErrorView> = emptyList()): CategoryListPageView =
        CategoryListPageView(getCategoryListUseCase.getCategoryList().map { it.toRowView() }, errors)

    private fun locationListPage(errors: List<FormErrorView> = emptyList()): LocationListPageView =
        LocationListPageView(getLocationListUseCase.getLocationList().map { it.toRowView() }, errors)

    private fun sourceListPage(errors: List<FormErrorView> = emptyList()): SourceListPageView =
        SourceListPageView(getSourceListUseCase.getSourceList().map { it.toRowView() }, errors)

    private fun validateName(name: String, requiredMessage: String): List<FormErrorView> =
        if (name.isBlank()) listOf(FormErrorView("name", requiredMessage)) else emptyList()

    private fun validateLocationForm(form: LocationForm): List<FormErrorView> =
        buildList {
            if (form.name.isBlank()) {
                add(FormErrorView("name", "Name ist erforderlich."))
            }
            if (form.type.isBlank()) {
                add(FormErrorView("type", "Ortstyp ist erforderlich."))
            } else if (runCatching { LocationType.valueOf(form.type) }.isFailure) {
                add(FormErrorView("type", "Ortstyp ist ungültig."))
            }
        }

    private fun locationTypeOptions(): List<SelectOptionView> =
        LocationType.entries.map { SelectOptionView(it.name, it.toViewLabel()) }
}
