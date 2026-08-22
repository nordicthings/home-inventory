package org.nordicthings.homeinventory.inventory.adapter.web

import org.nordicthings.homeinventory.inventory.application.AcquisitionInvoiceAlreadyExistsException
import org.nordicthings.homeinventory.inventory.application.DuplicateNameException
import org.nordicthings.homeinventory.inventory.application.EntityNotFoundException
import org.nordicthings.homeinventory.inventory.application.InvalidAcquisitionInvoiceException
import org.nordicthings.homeinventory.inventory.application.ItemListSort
import org.nordicthings.homeinventory.inventory.application.ItemListSortField
import org.nordicthings.homeinventory.inventory.application.SearchItemsFilter
import org.nordicthings.homeinventory.inventory.application.SortDirection
import org.nordicthings.homeinventory.inventory.application.port.inbound.AcquisitionInvoiceUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetCategoryListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetItemDetailsUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetLocationListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.GetSourceListUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.ItemUseCase
import org.nordicthings.homeinventory.inventory.application.port.inbound.SearchItemsUseCase
import org.nordicthings.homeinventory.inventory.domain.CategoryId
import org.nordicthings.homeinventory.inventory.domain.Item
import org.nordicthings.homeinventory.inventory.domain.ItemId
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.ItemSourceId
import org.nordicthings.homeinventory.inventory.domain.LocationId
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.nordicthings.homeinventory.inventory.domain.SourceId
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import jakarta.servlet.http.HttpServletRequest
import java.io.Serializable
import java.time.LocalDate
import java.util.UUID

@Controller
class ItemWebController(
    private val searchItemsUseCase: SearchItemsUseCase,
    private val getItemDetailsUseCase: GetItemDetailsUseCase,
    private val itemUseCase: ItemUseCase,
    private val getCategoryListUseCase: GetCategoryListUseCase,
    private val getLocationListUseCase: GetLocationListUseCase,
    private val getSourceListUseCase: GetSourceListUseCase,
    private val acquisitionInvoiceUseCase: AcquisitionInvoiceUseCase,
) {

    @GetMapping("/", "/items")
    fun listItems(
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "categoryId", required = false) categoryId: String?,
        @RequestParam(name = "locationId", required = false) locationId: String?,
        @RequestParam(name = "sourceId", required = false) sourceId: String?,
        @RequestParam(name = "sort", required = false) sort: String?,
        @RequestParam(name = "direction", required = false) direction: String?,
        request: HttpServletRequest,
        model: Model,
    ): String {
        val listState = resolveItemListState(name, categoryId, locationId, sourceId, sort, direction, request)
        val page = createPageView(listState)
        model.addAttribute("page", page)
        model.addAttribute("items", page.items)
        model.addAttribute("sort", page.sort)
        return "items/list"
    }

    @GetMapping("/items/table")
    fun itemTable(
        @RequestParam(name = "name", required = false) name: String?,
        @RequestParam(name = "categoryId", required = false) categoryId: String?,
        @RequestParam(name = "locationId", required = false) locationId: String?,
        @RequestParam(name = "sourceId", required = false) sourceId: String?,
        @RequestParam(name = "sort", required = false) sort: String?,
        @RequestParam(name = "direction", required = false) direction: String?,
        request: HttpServletRequest,
        model: Model,
    ): String {
        val listState = resolveItemListState(name, categoryId, locationId, sourceId, sort, direction, request)
        val page = createPageView(listState)
        model.addAttribute("page", page)
        model.addAttribute("items", page.items)
        model.addAttribute("sort", page.sort)
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
        @RequestParam(name = "notice", required = false) notice: String?,
        model: Model,
    ): String =
        try {
            model.addAttribute("page", getItemDetailsUseCase.getItemDetails(ItemId(id)).toDetailPageView(notice.toNoticeList()))
            "items/detail"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/edit")
    fun editItem(
        @PathVariable id: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            model.addAttribute("page", createEditPageView(id, details.toEditForm()))
            "items/edit"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/delete")
    fun confirmDeleteItem(
        @PathVariable id: UUID,
        model: Model,
    ): String =
        try {
            model.addAttribute("page", getItemDetailsUseCase.getItemDetails(ItemId(id)).toDetailPageView())
            "items/delete"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/locations/edit")
    fun editItemLocationQuantity(
        @PathVariable id: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            model.addAttribute("page", createLocationQuantityPageView(details.id.value, details.name.value, ItemLocationQuantityForm()))
            "items/location-quantity"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/locations/{locationId}/edit")
    fun editExistingItemLocationQuantity(
        @PathVariable id: UUID,
        @PathVariable locationId: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val locationQuantity = details.locationQuantities.firstOrNull { it.locationId.value == locationId }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bestand wurde nicht gefunden.")
            model.addAttribute(
                "page",
                createLocationQuantityPageView(
                    itemId = details.id.value,
                    itemName = details.name.value,
                    form = ItemLocationQuantityForm(
                        locationId = locationQuantity.locationId.value.toString(),
                        quantity = locationQuantity.quantity.value.formatIntegerForView(),
                    ),
                    locationReadOnly = true,
                ),
            )
            "items/location-quantity"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/locations/{locationId}/relocate")
    fun relocateItemForm(
        @PathVariable id: UUID,
        @PathVariable locationId: UUID,
        model: Model,
    ): String =
        renderRelocationForm(
            id = id,
            sourceLocationId = locationId,
            form = ItemRelocationForm(sourceLocationId = locationId.toString()),
            errors = emptyList(),
            model = model,
        )

    @GetMapping("/items/{id}/acquisitions/new")
    fun newItemAcquisition(
        @PathVariable id: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            model.addAttribute("page", createAcquisitionPageView(details.id.value, null, details.name.value, ItemAcquisitionForm()))
            "items/acquisition"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/acquisitions/{acquisitionId}/edit")
    fun editItemAcquisition(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val acquisition = details.acquisitions.firstOrNull { it.id.value == acquisitionId }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.")
            model.addAttribute(
                "page",
                createAcquisitionPageView(
                    itemId = details.id.value,
                    acquisitionId = acquisition.id.value,
                    itemName = details.name.value,
                    form = ItemAcquisitionForm(
                        sourceId = acquisition.sourceId.value.toString(),
                        quantity = acquisition.quantity.value.formatIntegerForView(),
                        purchasePrice = acquisition.purchasePrice.formatAmountForForm(),
                        purchaseDate = acquisition.purchaseDate?.toString().orEmpty(),
                    ),
                ),
            )
            "items/acquisition"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/acquisitions/{acquisitionId}/delete")
    fun confirmDeleteItemAcquisition(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val acquisition = details.acquisitions.firstOrNull { it.id.value == acquisitionId }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.")
            model.addAttribute("item", details.toDetailPageView())
            model.addAttribute("acquisition", acquisition.toDeletePageView())
            "items/acquisition-delete"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @GetMapping("/items/{id}/acquisitions/{acquisitionId}/invoice/upload")
    fun uploadAcquisitionInvoiceForm(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
        model: Model,
    ): String =
        renderAcquisitionInvoiceUploadForm(id, acquisitionId, emptyList(), model)

    @PostMapping("/items/{id}/acquisitions/{acquisitionId}/invoice")
    fun uploadAcquisitionInvoice(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(name = "replaceExisting", required = false, defaultValue = "false") replaceExisting: Boolean,
        model: Model,
    ): String {
        if (file.isEmpty) {
            return renderAcquisitionInvoiceUploadForm(
                id,
                acquisitionId,
                listOf(FormErrorView("file", "Bitte eine PDF-Rechnung auswählen.")),
                model,
            )
        }

        return try {
            acquisitionInvoiceUseCase.uploadInvoice(
                itemId = ItemId(id),
                acquisitionId = ItemSourceId(acquisitionId),
                originalFilename = file.originalFilename.orEmpty(),
                contentType = file.contentType,
                content = file.bytes,
                replaceExisting = replaceExisting,
            )
            "redirect:/items/$id?notice=${if (replaceExisting) "invoiceReplaced" else "invoiceUploaded"}"
        } catch (exception: AcquisitionInvoiceAlreadyExistsException) {
            renderAcquisitionInvoiceUploadForm(
                id,
                acquisitionId,
                listOf(FormErrorView(null, "Es ist bereits die Rechnung ${exception.existingFilename} hinterlegt.")),
                model,
            )
        } catch (exception: InvalidAcquisitionInvoiceException) {
            renderAcquisitionInvoiceUploadForm(id, acquisitionId, listOf(FormErrorView("file", "Bitte eine PDF-Datei bis maximal 10 MB auswählen.")), model)
        } catch (exception: EntityNotFoundException) {
            throw invoiceNotFoundResponse(exception)
        }
    }

    @GetMapping("/items/{id}/acquisitions/{acquisitionId}/invoice/download")
    fun downloadAcquisitionInvoice(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
    ): ResponseEntity<ByteArray> =
        try {
            val invoice = acquisitionInvoiceUseCase.downloadInvoice(ItemId(id), ItemSourceId(acquisitionId))
            ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment()
                        .filename(invoice.originalFilename.value, Charsets.UTF_8)
                        .build()
                        .toString(),
                )
                .body(invoice.content)
        } catch (exception: EntityNotFoundException) {
            throw invoiceNotFoundResponse(exception)
        }

    @GetMapping("/items/{id}/acquisitions/{acquisitionId}/invoice/delete")
    fun confirmDeleteAcquisitionInvoice(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val acquisition = details.acquisitions.firstOrNull { it.id.value == acquisitionId }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.")
            val invoice = acquisition.invoice
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Rechnung wurde nicht gefunden.")
            model.addAttribute(
                "page",
                AcquisitionInvoiceDeletePageView(
                    itemId = details.id.value.toString(),
                    acquisitionId = acquisition.id.value.toString(),
                    itemName = details.name.value,
                    filename = invoice.originalFilename.value,
                ),
            )
            "items/invoice-delete"
        } catch (exception: EntityNotFoundException) {
            throw invoiceNotFoundResponse(exception)
        }

    @PostMapping("/items/{id}/acquisitions/{acquisitionId}/invoice/delete")
    fun deleteAcquisitionInvoice(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
    ): String =
        try {
            acquisitionInvoiceUseCase.deleteInvoice(ItemId(id), ItemSourceId(acquisitionId))
            "redirect:/items/$id?notice=invoiceDeleted"
        } catch (exception: EntityNotFoundException) {
            throw invoiceNotFoundResponse(exception)
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
            val item = itemUseCase.createItem(
                name = ItemName.of(form.name),
                categoryId = CategoryId(UUID.fromString(form.categoryId)),
                estimatedValue = MonetaryValue.of(parseEstimatedValue(form.estimatedValue)),
                note = form.note.trim(),
            )
            "redirect:/items/${item.id.value}"
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

    @PostMapping("/items/{id}")
    fun updateItem(
        @PathVariable id: UUID,
        @ModelAttribute form: ItemEditForm,
        model: Model,
    ): String {
        val errors = validateEditForm(form)
        if (errors.isNotEmpty()) {
            model.addAttribute("page", createEditPageView(id, form, errors))
            return "items/edit"
        }

        return try {
            itemUseCase.updateItem(
                id = ItemId(id),
                name = ItemName.of(form.name),
                categoryId = CategoryId(UUID.fromString(form.categoryId)),
                estimatedValue = MonetaryValue.of(parseEstimatedValue(form.estimatedValue)),
                note = form.note.trim(),
            )
            "redirect:/items/$id"
        } catch (exception: DuplicateNameException) {
            model.addAttribute("page", createEditPageView(id, form, listOf(FormErrorView("name", "Name ist bereits vergeben."))))
            "items/edit"
        } catch (exception: EntityNotFoundException) {
            if (exception.message?.startsWith("Item does not exist") == true) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
            }
            model.addAttribute("page", createEditPageView(id, form, listOf(FormErrorView("categoryId", "Kategorie wurde nicht gefunden."))))
            "items/edit"
        } catch (exception: IllegalArgumentException) {
            model.addAttribute("page", createEditPageView(id, form, listOf(FormErrorView(null, "Die Eingaben sind ungültig."))))
            "items/edit"
        }
    }

    @PostMapping("/items/{id}/delete")
    fun deleteItem(@PathVariable id: UUID): String =
        try {
            acquisitionInvoiceUseCase.deleteInvoicesForItem(ItemId(id))
            itemUseCase.deleteItem(ItemId(id))
            "redirect:/items"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @PostMapping("/items/{id}/locations")
    fun setItemLocationQuantity(
        @PathVariable id: UUID,
        @ModelAttribute form: ItemLocationQuantityForm,
        model: Model,
    ): String {
        val errors = validateLocationQuantityForm(form)
        if (errors.isNotEmpty()) {
            return renderLocationQuantityForm(id, form, errors, model)
        }

        return try {
            itemUseCase.setLocationQuantity(
                id = ItemId(id),
                locationId = LocationId(UUID.fromString(form.locationId)),
                quantity = Quantity.of(parseQuantity(form.quantity)),
            ).toItemDetailRedirect(id)
        } catch (exception: EntityNotFoundException) {
            if (exception.message?.startsWith("Item does not exist") == true) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
            }
            renderLocationQuantityForm(id, form, listOf(FormErrorView("locationId", "Ort wurde nicht gefunden.")), model)
        } catch (exception: IllegalArgumentException) {
            renderLocationQuantityForm(id, form, listOf(FormErrorView(null, "Die Eingaben sind ungültig.")), model)
        }
    }

    @PostMapping("/items/{id}/locations/{locationId}/delete")
    fun deleteItemLocationQuantity(
        @PathVariable id: UUID,
        @PathVariable locationId: UUID,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            if (details.locationQuantities.none { it.locationId.value == locationId }) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bestand wurde nicht gefunden.")
            }
            itemUseCase.setLocationQuantity(
                id = ItemId(id),
                locationId = LocationId(locationId),
                quantity = Quantity.ZERO,
            ).toItemDetailRedirect(id)
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    @PostMapping("/items/{id}/relocations")
    fun relocateItem(
        @PathVariable id: UUID,
        @ModelAttribute form: ItemRelocationForm,
        model: Model,
    ): String {
        val sourceLocationId = form.sourceLocationId.toUuidOrNull()
        if (sourceLocationId == null) {
            return renderRelocationForm(
                id = id,
                sourceLocationId = null,
                form = form,
                errors = listOf(FormErrorView("sourceLocationId", "Quellort ist ungültig.")),
                model = model,
            )
        }

        return try {
            val errors = validateRelocationForm(id, form)
            if (errors.isNotEmpty()) {
                return renderRelocationForm(id, sourceLocationId, form, errors, model)
            }
            itemUseCase.relocateItem(
                id = ItemId(id),
                sourceLocationId = LocationId(sourceLocationId),
                targetLocationId = LocationId(UUID.fromString(form.targetLocationId)),
                quantity = Quantity.of(parseQuantity(form.quantity)),
            )
            "redirect:/items/$id"
        } catch (exception: EntityNotFoundException) {
            if (exception.message?.startsWith("Item does not exist") == true) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
            }
            renderRelocationForm(id, sourceLocationId, form, listOf(FormErrorView("targetLocationId", "Zielort wurde nicht gefunden.")), model)
        } catch (exception: IllegalArgumentException) {
            renderRelocationForm(id, sourceLocationId, form, listOf(FormErrorView(null, "Die Eingaben sind ungültig.")), model)
        } catch (exception: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bestand wurde nicht gefunden.", exception)
        }
    }

    @PostMapping("/items/{id}/acquisitions")
    fun createItemAcquisition(
        @PathVariable id: UUID,
        @ModelAttribute form: ItemAcquisitionForm,
        model: Model,
    ): String {
        val errors = validateAcquisitionForm(form)
        if (errors.isNotEmpty()) {
            return renderAcquisitionForm(id, null, form, errors, model)
        }

        return try {
            val detailsBeforeSave = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val sourceId = SourceId(UUID.fromString(form.sourceId))
            val quantity = Quantity.of(parseQuantity(form.quantity))
            val purchasePrice = MonetaryValue.of(parseMoneyValue(form.purchasePrice))
            val purchaseDate = parsePurchaseDate(form.purchaseDate)
            val merged = detailsBeforeSave.acquisitions.any {
                it.sourceId == sourceId && it.purchasePrice == purchasePrice && it.purchaseDate == purchaseDate
            }

            itemUseCase.recordAcquisition(
                id = ItemId(id),
                sourceId = sourceId,
                quantity = quantity,
                purchasePrice = purchasePrice,
                purchaseDate = purchaseDate,
            ).toItemDetailRedirect(
                id = id,
                notices = listOfNotNull("acquisitionMerged".takeIf { merged }),
            )
        } catch (exception: EntityNotFoundException) {
            if (exception.message?.startsWith("Item does not exist") == true) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
            }
            renderAcquisitionForm(id, null, form, listOf(FormErrorView("sourceId", "Bezugsquelle wurde nicht gefunden.")), model)
        } catch (exception: IllegalArgumentException) {
            renderAcquisitionForm(id, null, form, listOf(FormErrorView(null, "Die Eingaben sind ungültig.")), model)
        }
    }

    @PostMapping("/items/{id}/acquisitions/{acquisitionId}")
    fun updateItemAcquisition(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
        @ModelAttribute form: ItemAcquisitionForm,
        model: Model,
    ): String {
        val errors = validateAcquisitionForm(form)
        if (errors.isNotEmpty()) {
            return renderAcquisitionForm(id, acquisitionId, form, errors, model)
        }

        return try {
            val detailsBeforeSave = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val sourceId = SourceId(UUID.fromString(form.sourceId))
            val quantity = Quantity.of(parseQuantity(form.quantity))
            val purchasePrice = MonetaryValue.of(parseMoneyValue(form.purchasePrice))
            val purchaseDate = parsePurchaseDate(form.purchaseDate)
            val merged = detailsBeforeSave.acquisitions.any {
                it.id.value != acquisitionId && it.sourceId == sourceId && it.purchasePrice == purchasePrice && it.purchaseDate == purchaseDate
            }
            if (merged) {
                acquisitionInvoiceUseCase.deleteInvoice(ItemId(id), ItemSourceId(acquisitionId))
            }
            itemUseCase.updateAcquisition(
                id = ItemId(id),
                itemSourceId = ItemSourceId(acquisitionId),
                sourceId = sourceId,
                quantity = quantity,
                purchasePrice = purchasePrice,
                purchaseDate = purchaseDate,
            ).toItemDetailRedirect(id)
        } catch (exception: EntityNotFoundException) {
            if (exception.message?.startsWith("Item does not exist") == true) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
            }
            renderAcquisitionForm(id, acquisitionId, form, listOf(FormErrorView("sourceId", "Bezugsquelle wurde nicht gefunden.")), model)
        } catch (exception: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.", exception)
        } catch (exception: IllegalArgumentException) {
            renderAcquisitionForm(id, acquisitionId, form, listOf(FormErrorView(null, "Die Eingaben sind ungültig.")), model)
        }
    }

    @PostMapping("/items/{id}/acquisitions/{acquisitionId}/delete")
    fun deleteItemAcquisition(
        @PathVariable id: UUID,
        @PathVariable acquisitionId: UUID,
    ): String =
        try {
            acquisitionInvoiceUseCase.deleteInvoice(ItemId(id), ItemSourceId(acquisitionId))
            itemUseCase.deleteAcquisition(ItemId(id), ItemSourceId(acquisitionId))
            "redirect:/items/$id"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        } catch (exception: IllegalStateException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.", exception)
        }

    private fun createPageView(
        state: ItemListSessionState,
    ): ItemListPageView =
        ItemListPageView(
            filter = ItemFilterView(
                name = state.name,
                categoryId = state.categoryId,
                locationId = state.locationId,
                sourceId = state.sourceId,
            ),
            sort = ItemSortView(
                field = state.sort.toItemSortField().requestValue,
                direction = state.direction.toSortDirection().requestValue,
            ),
            categories = getCategoryListUseCase.getCategoryList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            locations = getLocationListUseCase.getLocationList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            sources = getSourceListUseCase.getSourceList()
                .map { SelectOptionView(it.id.value.toString(), it.name.value) },
            items = searchItems(state).map { it.toRowView() },
        )

    private fun categoryOptions(): List<SelectOptionView> =
        getCategoryListUseCase.getCategoryList()
            .map { SelectOptionView(it.id.value.toString(), it.name.value) }

    private fun locationOptions(): List<SelectOptionView> =
        getLocationListUseCase.getLocationList()
            .map { SelectOptionView(it.id.value.toString(), it.name.value) }

    private fun sourceOptions(): List<SelectOptionView> =
        getSourceListUseCase.getSourceList()
            .map { SelectOptionView(it.id.value.toString(), it.name.value) }

    private fun createCreatePageView(
        form: ItemCreateForm,
        errors: List<FormErrorView> = emptyList(),
    ): ItemCreatePageView =
        ItemCreatePageView(
            form = form,
            categories = categoryOptions(),
            errors = errors,
        )

    private fun createAcquisitionPageView(
        itemId: UUID,
        acquisitionId: UUID?,
        itemName: String,
        form: ItemAcquisitionForm,
        errors: List<FormErrorView> = emptyList(),
    ): ItemAcquisitionPageView =
        ItemAcquisitionPageView(
            itemId = itemId.toString(),
            acquisitionId = acquisitionId?.toString(),
            itemName = itemName,
            form = form,
            sources = sourceOptions(),
            errors = errors,
        )

    private fun renderAcquisitionForm(
        id: UUID,
        acquisitionId: UUID?,
        form: ItemAcquisitionForm,
        errors: List<FormErrorView>,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            if (acquisitionId != null && details.acquisitions.none { it.id.value == acquisitionId }) {
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.")
            }
            model.addAttribute("page", createAcquisitionPageView(id, acquisitionId, details.name.value, form, errors))
            "items/acquisition"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    private fun renderAcquisitionInvoiceUploadForm(
        id: UUID,
        acquisitionId: UUID,
        errors: List<FormErrorView>,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val acquisition = details.acquisitions.firstOrNull { it.id.value == acquisitionId }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.")
            model.addAttribute(
                "page",
                AcquisitionInvoiceUploadPageView(
                    itemId = details.id.value.toString(),
                    acquisitionId = acquisition.id.value.toString(),
                    itemName = details.name.value,
                    existingFilename = acquisition.invoice?.originalFilename?.value,
                    errors = errors,
                ),
            )
            "items/invoice-upload"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    private fun createEditPageView(
        id: UUID,
        form: ItemEditForm,
        errors: List<FormErrorView> = emptyList(),
    ): ItemEditPageView =
        ItemEditPageView(
            id = id.toString(),
            form = form,
            categories = categoryOptions(),
            errors = errors,
        )

    private fun createLocationQuantityPageView(
        itemId: UUID,
        itemName: String,
        form: ItemLocationQuantityForm,
        locationReadOnly: Boolean = false,
        errors: List<FormErrorView> = emptyList(),
    ): ItemLocationQuantityPageView =
        ItemLocationQuantityPageView(
            itemId = itemId.toString(),
            itemName = itemName,
            form = form,
            locations = locationOptions(),
            locationReadOnly = locationReadOnly,
            errors = errors,
        )

    private fun renderLocationQuantityForm(
        id: UUID,
        form: ItemLocationQuantityForm,
        errors: List<FormErrorView>,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            model.addAttribute("page", createLocationQuantityPageView(id, details.name.value, form, locationReadOnly = false, errors = errors))
            "items/location-quantity"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    private fun createRelocationPageView(
        itemId: UUID,
        itemName: String,
        sourceLocationName: String,
        sourceQuantity: Int,
        form: ItemRelocationForm,
        errors: List<FormErrorView> = emptyList(),
    ): ItemRelocationPageView =
        ItemRelocationPageView(
            itemId = itemId.toString(),
            itemName = itemName,
            sourceLocationName = sourceLocationName,
            sourceQuantity = sourceQuantity.formatIntegerForView(),
            form = form,
            locations = locationOptions(),
            errors = errors,
        )

    private fun renderRelocationForm(
        id: UUID,
        sourceLocationId: UUID?,
        form: ItemRelocationForm,
        errors: List<FormErrorView>,
        model: Model,
    ): String =
        try {
            val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
            val sourceLocationQuantity = sourceLocationId?.let { sourceId ->
                details.locationQuantities.firstOrNull { it.locationId.value == sourceId }
            } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bestand wurde nicht gefunden.")
            model.addAttribute(
                "page",
                createRelocationPageView(
                    itemId = id,
                    itemName = details.name.value,
                    sourceLocationName = sourceLocationQuantity.locationName.value,
                    sourceQuantity = sourceLocationQuantity.quantity.value,
                    form = form,
                    errors = errors,
                ),
            )
            "items/relocation"
        } catch (exception: EntityNotFoundException) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        }

    private fun validateCreateForm(form: ItemCreateForm): List<FormErrorView> =
        validateItemForm(
            name = form.name,
            categoryId = form.categoryId,
            estimatedValue = form.estimatedValue,
        )

    private fun validateEditForm(form: ItemEditForm): List<FormErrorView> =
        validateItemForm(
            name = form.name,
            categoryId = form.categoryId,
            estimatedValue = form.estimatedValue,
        )

    private fun validateLocationQuantityForm(form: ItemLocationQuantityForm): List<FormErrorView> =
        buildList {
            if (form.locationId.isBlank()) {
                add(FormErrorView("locationId", "Ort ist erforderlich."))
            } else if (form.locationId.toUuidOrNull() == null) {
                add(FormErrorView("locationId", "Ort ist ungültig."))
            }

            val quantity = form.quantity.trim()
            if (quantity.isBlank()) {
                add(FormErrorView("quantity", "Menge ist erforderlich."))
            } else {
                val amount = quantity.toQuantityIntOrNull()
                if (amount == null || amount <= 0) {
                    add(FormErrorView("quantity", "Menge muss eine positive ganze Zahl sein."))
                }
            }
        }

    private fun validateAcquisitionForm(form: ItemAcquisitionForm): List<FormErrorView> =
        buildList {
            if (form.sourceId.isBlank()) {
                add(FormErrorView("sourceId", "Bezugsquelle ist erforderlich."))
            } else if (form.sourceId.toUuidOrNull() == null) {
                add(FormErrorView("sourceId", "Bezugsquelle ist ungültig."))
            }

            val quantity = form.quantity.trim()
            if (quantity.isBlank()) {
                add(FormErrorView("quantity", "Menge ist erforderlich."))
            } else {
                val amount = quantity.toQuantityIntOrNull()
                if (amount == null || amount <= 0) {
                    add(FormErrorView("quantity", "Menge muss eine positive ganze Zahl sein."))
                }
            }

            val purchasePrice = form.purchasePrice.trim()
            if (purchasePrice.isNotBlank()) {
                val amount = purchasePrice.toEstimatedValueOrNull()
                if (amount == null && purchasePrice.isNegativeGermanDecimal()) {
                    add(FormErrorView("purchasePrice", "Kaufpreis muss 0 oder größer sein."))
                } else if (amount == null) {
                    add(FormErrorView("purchasePrice", "Kaufpreis muss im deutschen Zahlenformat angegeben werden."))
                }
            }

            if (form.purchaseDate.isNotBlank()) {
                val purchaseDate = form.purchaseDate.toLocalDateOrNull()
                if (purchaseDate == null) {
                    add(FormErrorView("purchaseDate", "Kaufdatum ist ungültig."))
                } else if (purchaseDate.isAfter(LocalDate.now())) {
                    add(FormErrorView("purchaseDate", "Kaufdatum darf nicht in der Zukunft liegen."))
                }
            }
        }

    private fun validateRelocationForm(id: UUID, form: ItemRelocationForm): List<FormErrorView> =
        buildList {
            val sourceLocationId = if (form.sourceLocationId.isBlank()) {
                add(FormErrorView("sourceLocationId", "Quellort ist erforderlich."))
                null
            } else {
                val parsedSourceLocationId = form.sourceLocationId.toUuidOrNull()
                if (parsedSourceLocationId == null) {
                    add(FormErrorView("sourceLocationId", "Quellort ist ungültig."))
                }
                parsedSourceLocationId
            }

            val targetLocationId = if (form.targetLocationId.isBlank()) {
                add(FormErrorView("targetLocationId", "Zielort ist erforderlich."))
                null
            } else {
                val parsedTargetLocationId = form.targetLocationId.toUuidOrNull()
                if (parsedTargetLocationId == null) {
                    add(FormErrorView("targetLocationId", "Zielort ist ungültig."))
                }
                parsedTargetLocationId
            }

            if (sourceLocationId != null && targetLocationId != null && sourceLocationId == targetLocationId) {
                add(FormErrorView("targetLocationId", "Zielort muss sich vom Quellort unterscheiden."))
            }

            val quantity = form.quantity.trim()
            val amount = if (quantity.isBlank()) {
                add(FormErrorView("quantity", "Menge ist erforderlich."))
                null
            } else {
                val parsedAmount = quantity.toQuantityIntOrNull()
                if (parsedAmount == null || parsedAmount <= 0) {
                    add(FormErrorView("quantity", "Menge muss eine positive ganze Zahl sein."))
                }
                parsedAmount
            }

            if (sourceLocationId != null && amount != null && amount > 0) {
                val details = getItemDetailsUseCase.getItemDetails(ItemId(id))
                val sourceQuantity = details.locationQuantities
                    .firstOrNull { it.locationId.value == sourceLocationId }
                    ?.quantity
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Bestand wurde nicht gefunden.")
                if (amount > sourceQuantity.value) {
                    add(FormErrorView("quantity", "Menge darf den Bestand am Quellort nicht überschreiten."))
                }
            }
        }

    private fun validateItemForm(
        name: String,
        categoryId: String,
        estimatedValue: String,
    ): List<FormErrorView> =
        buildList {
            if (name.isBlank()) {
                add(FormErrorView("name", "Name ist erforderlich."))
            }
            if (categoryId.isBlank()) {
                add(FormErrorView("categoryId", "Kategorie ist erforderlich."))
            } else if (categoryId.toUuidOrNull() == null) {
                add(FormErrorView("categoryId", "Kategorie ist ungültig."))
            }
            val estimatedAmount = estimatedValue.trim()
            if (estimatedAmount.isNotBlank()) {
                val amount = estimatedAmount.toEstimatedValueOrNull()
                if (amount == null && estimatedAmount.isNegativeGermanDecimal()) {
                    add(FormErrorView("estimatedValue", "Schätzwert muss 0 oder größer sein."))
                } else if (amount == null) {
                    add(FormErrorView("estimatedValue", "Schätzwert muss im deutschen Zahlenformat angegeben werden."))
                }
            }
        }

    private fun searchItems(
        state: ItemListSessionState,
    ) =
        searchItemsUseCase.searchItems(
            SearchItemsFilter(
                name = state.name,
                categoryId = state.categoryId.toCategoryIdOrNull(),
                locationId = state.locationId.toLocationIdOrNull(),
                sourceId = state.sourceId.toSourceIdOrNull(),
                sort = ItemListSort(
                    field = state.sort.toItemSortField(),
                    direction = state.direction.toSortDirection(),
                ),
            ),
        )

    private fun parseEstimatedValue(value: String) =
        parseMoneyValue(value)

    private fun resolveItemListState(
        name: String?,
        categoryId: String?,
        locationId: String?,
        sourceId: String?,
        sort: String?,
        direction: String?,
        request: HttpServletRequest,
    ): ItemListSessionState {
        if (request.parameterMap.isEmpty()) {
            return request.getSession(false)?.getAttribute(ITEM_LIST_SESSION_STATE) as? ItemListSessionState
                ?: ItemListSessionState()
        }

        val state = ItemListSessionState(
            name = name.orEmpty(),
            categoryId = categoryId.orEmpty(),
            locationId = locationId.orEmpty(),
            sourceId = sourceId.orEmpty(),
            sort = sort.toItemSortField().requestValue,
            direction = direction.toSortDirection().requestValue,
        )
        request.session.setAttribute(ITEM_LIST_SESSION_STATE, state)
        return state
    }

    private fun String?.toItemSortField(): ItemListSortField =
        when (this) {
            "category" -> ItemListSortField.CATEGORY
            "quantity" -> ItemListSortField.TOTAL_QUANTITY
            "averageValue" -> ItemListSortField.AVERAGE_VALUE
            "totalValue" -> ItemListSortField.TOTAL_VALUE
            else -> ItemListSortField.NAME
        }

    private fun String?.toSortDirection(): SortDirection =
        when (this) {
            "desc" -> SortDirection.DESCENDING
            else -> SortDirection.ASCENDING
        }

    private val ItemListSortField.requestValue: String
        get() = when (this) {
            ItemListSortField.NAME -> "name"
            ItemListSortField.CATEGORY -> "category"
            ItemListSortField.TOTAL_QUANTITY -> "quantity"
            ItemListSortField.AVERAGE_VALUE -> "averageValue"
            ItemListSortField.TOTAL_VALUE -> "totalValue"
        }

    private val SortDirection.requestValue: String
        get() = when (this) {
            SortDirection.ASCENDING -> "asc"
            SortDirection.DESCENDING -> "desc"
        }

    private fun String?.toNoticeList(): List<String> =
        when (this) {
            null -> emptyList()
            else -> split(",").mapNotNull { it.toNoticeMessage() }
        }

    private fun String.toNoticeMessage(): String? =
        when (this) {
            "acquisitionMerged" -> "Zugang wurde mit einem bestehenden Zugang zusammengeführt."
            "acquisitionQuantityExceedsLocationQuantity" -> "Die Zugangsgesamtmenge ist größer als die aktuelle Ortsgesamtmenge."
            "invoiceUploaded" -> "Rechnung wurde hochgeladen."
            "invoiceReplaced" -> "Rechnung wurde ersetzt."
            "invoiceDeleted" -> "Rechnung wurde gelöscht."
            else -> null
        }

    private fun Item.toItemDetailRedirect(
        id: UUID,
        notices: List<String> = emptyList(),
    ): String {
        val allNotices = buildList {
            addAll(notices)
            if (acquisitionQuantityExceedsLocationQuantity()) {
                add("acquisitionQuantityExceedsLocationQuantity")
            }
        }.distinct()
        val noticeQuery = allNotices.takeIf { it.isNotEmpty() }?.joinToString(prefix = "?notice=", separator = ",").orEmpty()
        return "redirect:/items/$id$noticeQuery"
    }

    private fun Item.acquisitionQuantityExceedsLocationQuantity(): Boolean {
        val acquisitionQuantity = sources.sumOf { it.quantity.value }
        return acquisitionQuantity > totalQuantity.value
    }

    private fun invoiceNotFoundResponse(exception: EntityNotFoundException): ResponseStatusException =
        if (exception.message?.startsWith("Item does not exist") == true) {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Gegenstand wurde nicht gefunden.", exception)
        } else if (exception.message?.startsWith("Acquisition does not exist") == true) {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Zugang wurde nicht gefunden.", exception)
        } else {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Rechnung wurde nicht gefunden.", exception)
        }
}

private const val ITEM_LIST_SESSION_STATE = "inventory.itemListState"

private data class ItemListSessionState(
    val name: String = "",
    val categoryId: String = "",
    val locationId: String = "",
    val sourceId: String = "",
    val sort: String = "name",
    val direction: String = "asc",
) : Serializable
