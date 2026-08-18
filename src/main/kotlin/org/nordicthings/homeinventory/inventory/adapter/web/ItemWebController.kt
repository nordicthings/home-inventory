package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.DuplicateNameException
import org.nordicthings.homeinventory.inventory.application.EntityNotFoundException
import org.nordicthings.homeinventory.inventory.application.SearchItemsFilter
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetCategoryListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetItemDetailsUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetLocationListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetSourceListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.ItemUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.SearchItemsUseCase
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.util.UUID

@Controller
class ItemWebController(
    private val searchItemsUseCase: SearchItemsUseCase,
    private val getItemDetailsUseCase: GetItemDetailsUseCase,
    private val itemUseCase: ItemUseCase,
    private val getCategoryListUseCase: GetCategoryListUseCase,
    private val getLocationListUseCase: GetLocationListUseCase,
    private val getSourceListUseCase: GetSourceListUseCase,
) {

    @GetMapping("/", "/items")
    fun listItems(
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "categoryId", required = false) categoryId: String?,
        @RequestParam(name = "locationId", required = false) locationId: String?,
        @RequestParam(name = "sourceId", required = false) sourceId: String?,
        model: Model,
    ): String {
        val page = createPageView(name, categoryId, locationId, sourceId)
        model.addAttribute("page", page)
        model.addAttribute("items", page.items)
        return "items/list"
    }

    @GetMapping("/items/table")
    fun itemTable(
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "categoryId", required = false) categoryId: String?,
        @RequestParam(name = "locationId", required = false) locationId: String?,
        @RequestParam(name = "sourceId", required = false) sourceId: String?,
        model: Model,
    ): String {
        model.addAttribute("items", searchItems(name, categoryId, locationId, sourceId).map { it.toRowView() })
        return "items/_table :: itemTable"
    }

    @GetMapping("/items/new")
    fun newItem(model: Model): String {
        model.addAttribute("page", createCreatePageView(ItemCreateForm()))
        return "items/new"
    }

    @GetMapping("/items/{id}")
    fun itemDetails(
        @PathVariable id: UUID,
        model: Model,
    ): String =
        try {
            model.addAttribute("page", getItemDetailsUseCase.getItemDetails(ItemId(id)).toDetailPageView())
            "items/detail"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @PostMapping("/items")
    fun createItem(
        @ModelAttribute form: ItemCreateForm,
        model: Model,
    ): String {
        val errors = validateCreateForm(form)
        if (errors.isNotEmpty()) {
            model.addAttribute("page", createCreatePageView(form, errors))
            return "items/new"
        }

        return try {
            itemUseCase.createItem(
                name = ItemName.of(form.name),
                categoryId = CategoryId(UUID.fromString(form.categoryId)),
                estimatedValue = MonetaryValue.of(BigDecimal(form.estimatedValue.trim())),
                note = form.note.trim(),
            )
            "redirect:/items"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", createCreatePageView(form, listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "items/new"
        } catch (exception: EntityNotFoundException) {
            model.addAttribute("page", createCreatePageView(form, listOf(FormErrorView("categoryId", "Kategorie wurde nicht gefunden."))))
            "items/new"
        } catch (exception: IllegalArgumentException) {
            model.addAttribute("page", createCreatePageView(form, listOf(FormErrorView(null, "Die Eingaben sind ungültig."))))
            "items/new"
        }
    }

    private fun createPageView(
        name: String?,
        categoryId: String?,
        locationId: String?,
        sourceId: String?,
    ): ItemListPageView =
        ItemListPageView(
            filter = ItemFilterView(
                name = name.orEmpty(),
                categoryId = categoryId.orEmpty(),
                locationId = locationId.orEmpty(),
                sourceId = sourceId.orEmpty(),
            ),
            categories = getCategoryListUseCase.getCategoryList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            locations = getLocationListUseCase.getLocationList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            sources = getSourceListUseCase.getSourceList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            items = searchItems(name, categoryId, locationId, sourceId).map { it.toRowView() },
        )

    private fun createCreatePageView(
        form: ItemCreateForm,
        errors: List<FormErrorView> = emptyList(),
    ): ItemCreatePageView =
        ItemCreatePageView(
            form = form,
            categories = getCategoryListUseCase.getCategoryList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            errors = errors,
        )

    private fun validateCreateForm(form: ItemCreateForm): List<FormErrorView> =
        buildList {
            if (form.name.isBlank()) {
                add(FormErrorView("name", "Name ist erforderlich."))
            }
            if (form.categoryId.isBlank()) {
                add(FormErrorView("categoryId", "Kategorie ist erforderlich."))
            } else if (form.categoryId.toUuidOrNull() == null) {
                add(FormErrorView("categoryId", "Kategorie ist ungültig."))
            }
            val estimatedValue = form.estimatedValue.trim()
            if (estimatedValue.isBlank()) {
                add(FormErrorView("estimatedValue", "Schätzwert ist erforderlich."))
            } else {
                val amount = estimatedValue.toBigDecimalOrNull()
                if (amount == null || amount < BigDecimal.ZERO) {
                    add(FormErrorView("estimatedValue", "Schätzwert muss 0 oder größer sein."))
                }
            }
        }

    private fun searchItems(
        name: String?,
        categoryId: String?,
        locationId: String?,
        sourceId: String?,
    ) =
        searchItemsUseCase.searchItems(
            SearchItemsFilter(
                name = name,
                categoryId = categoryId.toCategoryIdOrNull(),
                locationId = locationId.toLocationIdOrNull(),
                sourceId = sourceId.toSourceIdOrNull(),
            ),
        )

    private fun String?.toCategoryIdOrNull(): CategoryId? =
        toUuidOrNull()?.let(::CategoryId)

    private fun String?.toLocationIdOrNull(): LocationId? =
        toUuidOrNull()?.let(::LocationId)

    private fun String?.toSourceIdOrNull(): SourceId? =
        toUuidOrNull()?.let(::SourceId)

    private fun String?.toUuidOrNull(): UUID? =
        this?.takeIf { it.isNotBlank() }?.let { UUID.fromString(it) }
}
