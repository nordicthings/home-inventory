package org.nordicthings.homeinventory.inventory.adapter.web

import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.ItemSearchCriteria
import org.nordicthings.homeinventory.inventory.application.port.inbound.ItemUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.ItemRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.MonetaryValue
import org.nordicthings.homeinventory.inventory.domain.Quantity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest(
    classes = [HomeInventoryApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:home_inventory_web;MODE=MariaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    ],
)
class ItemWebControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var itemUseCase: ItemUseCase

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    private val client = HttpClient.newHttpClient()

    @Test
    fun `renders item list page with filters and empty table`() {
        val response = get("/items")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Inventar</h1>")
        assertContains(response.body(), "Gegenstand hinzufügen")
        assertContains(response.body(), "Alle Kategorien")
        assertContains(response.body(), "Computer &amp; Peripherie")
        assertContains(response.body(), "Alle Orte")
        assertContains(response.body(), "Küche")
        assertContains(response.body(), "Alle Bezugsquellen")
        assertContains(response.body(), "Amazon")
    }

    @Test
    fun `renders item table fragment`() {
        val response = get("/items/table?name=KeinSolcherGegenstand")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<table class=\"data-table\">")
        assertContains(response.body(), "Keine Gegenstände gefunden")
    }

    @Test
    fun `renders sort controls and keeps sort parameters in filter form`() {
        val response = get("/items?sort=category&direction=desc")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), """<input type="hidden" name="sort" value="category">""")
        assertContains(response.body(), """<input type="hidden" name="direction" value="desc">""")
        assertContains(response.body(), "absteigend")
        assertContains(response.body(), "sort=quantity")
        assertContains(response.body(), "direction=asc")
    }

    @Test
    fun `renders item create form`() {
        val response = get("/items/new")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Gegenstand hinzufügen</h1>")
        assertContains(response.body(), "autofocus")
        assertContains(response.body(), "Name")
        assertContains(response.body(), "Kategorie auswählen")
        assertContains(response.body(), "Schätzwert")
        assertContains(response.body(), """type="text"""")
        assertContains(response.body(), "EUR")
        assertContains(response.body(), "Computer &amp; Peripherie")
    }

    @Test
    fun `shows validation errors for invalid item create form`() {
        val response = post(
            "/items",
            mapOf(
                "name" to "",
                "categoryId" to "",
                "estimatedValue" to "",
                "note" to "",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "Name ist erforderlich.")
        assertContains(response.body(), "Kategorie ist erforderlich.")
    }

    @Test
    fun `shows validation error for negative estimated value on item create form`() {
        val createForm = get("/items/new").body()
        val categoryId = Regex("""<option value="([^"]+)">Computer &amp; Peripherie</option>""")
            .find(createForm)
            ?.groupValues
            ?.get(1)
            ?: error("Category option not found.")

        val response = post(
            "/items",
            mapOf(
                "name" to "Negative-Value-Laptop",
                "categoryId" to categoryId,
                "estimatedValue" to "-1",
                "note" to "",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "Schätzwert muss 0 oder größer sein.")
    }

    @Test
    fun `creates item and redirects to detail page`() {
        val createForm = get("/items/new").body()
        val categoryId = Regex("""<option value="([^"]+)">Computer &amp; Peripherie</option>""")
            .find(createForm)
            ?.groupValues
            ?.get(1)
            ?: error("Category option not found.")

        val response = post(
            "/items",
            mapOf(
                "name" to "Laptop",
                "categoryId" to categoryId,
                "estimatedValue" to "800",
                "note" to "Arbeitsgerät",
            ),
        )

        assertEquals(302, response.statusCode())
        val item = assertNotNull(itemRepository.findByNormalizedName("laptop"))
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        assertEquals("Laptop", item.name.value)
        assertContains(itemRepository.search(ItemSearchCriteria()).map { it.name.value }, "Laptop")

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Laptop")
        assertContains(detailResponse.body(), "Computer &amp; Peripherie")
        assertContains(detailResponse.body(), "800,00 EUR")
    }

    @Test
    fun `creates item with unknown estimated value when value is blank`() {
        val createForm = get("/items/new").body()
        val categoryId = Regex("""<option value="([^"]+)">Computer &amp; Peripherie</option>""")
            .find(createForm)
            ?.groupValues
            ?.get(1)
            ?: error("Category option not found.")

        val response = post(
            "/items",
            mapOf(
                "name" to "Blank-Estimated-Value-Laptop",
                "categoryId" to categoryId,
                "estimatedValue" to "",
                "note" to "",
            ),
        )

        assertEquals(302, response.statusCode())
        val item = assertNotNull(itemRepository.findByNormalizedName("blank-estimated-value-laptop"))
        assertEquals(MonetaryValue.unknown(), item.estimatedValue)

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "unbekannt")
    }

    @Test
    fun `creates item with german formatted estimated value`() {
        val createForm = get("/items/new").body()
        val categoryId = Regex("""<option value="([^"]+)">Computer &amp; Peripherie</option>""")
            .find(createForm)
            ?.groupValues
            ?.get(1)
            ?: error("Category option not found.")

        val response = post(
            "/items",
            mapOf(
                "name" to "German-Format-Laptop",
                "categoryId" to categoryId,
                "estimatedValue" to "1.234,56",
                "note" to "",
            ),
        )

        assertEquals(302, response.statusCode())
        val item = assertNotNull(itemRepository.findByNormalizedName("german-format-laptop"))
        assertEquals(MonetaryValue.of("1234.56"), item.estimatedValue)
    }

    @Test
    fun `rejects non german decimal separator for item create form`() {
        val createForm = get("/items/new").body()
        val categoryId = Regex("""<option value="([^"]+)">Computer &amp; Peripherie</option>""")
            .find(createForm)
            ?.groupValues
            ?.get(1)
            ?: error("Category option not found.")

        val response = post(
            "/items",
            mapOf(
                "name" to "Invalid-Decimal-Laptop",
                "categoryId" to categoryId,
                "estimatedValue" to "999.99",
                "note" to "",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "Schätzwert muss im deutschen Zahlenformat angegeben werden.")
    }

    @Test
    fun `list entries link to item detail page`() {
        val createForm = get("/items/new").body()
        val categoryId = Regex("""<option value="([^"]+)">Computer &amp; Peripherie</option>""")
            .find(createForm)
            ?.groupValues
            ?.get(1)
            ?: error("Category option not found.")
        post(
            "/items",
            mapOf(
                "name" to "Detail-Link-Laptop",
                "categoryId" to categoryId,
                "estimatedValue" to "800",
                "note" to "",
            ),
        )
        val item = assertNotNull(itemRepository.findByNormalizedName("detail-link-laptop"))

        val response = get("/items")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), """href="/items/${item.id.value}"""")
        assertContains(response.body(), ">Detail-Link-Laptop</a>")
    }

    @Test
    fun `renders item detail page with master data quantities and acquisitions`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Detail-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("1234.56"),
            note = "Arbeitsgerät",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1000))
        itemUseCase.recordAcquisition(
            id = item.id,
            sourceId = amazon.id,
            quantity = Quantity.of(1000),
            purchasePrice = MonetaryValue.of("1234.56"),
            purchaseDate = LocalDate.of(2026, 1, 3),
        )

        val response = get("/items/${item.id.value}")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Detail-Laptop</h1>")
        assertContains(response.body(), "Computer &amp; Peripherie")
        assertContains(response.body(), "Arbeitsgerät")
        assertContains(response.body(), """href="/items/${item.id.value}/edit"""")
        assertContains(response.body(), """href="/items/${item.id.value}/delete"""")
        assertContains(response.body(), """href="/items/${item.id.value}/locations/edit"""")
        assertContains(response.body(), "Küche")
        assertContains(response.body(), "intern")
        assertContains(response.body(), "1.000")
        assertContains(response.body(), "Amazon")
        assertContains(response.body(), "1.234,56 EUR")
        assertContains(response.body(), "03.01.2026")
        assertContains(response.body(), "1.234.560,00 EUR")
    }

    @Test
    fun `renders item edit form with current item values`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Edit-Form-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "Arbeitsgerät",
        )

        val response = get("/items/${item.id.value}/edit")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Gegenstand bearbeiten</h1>")
        assertContains(response.body(), """value="Edit-Form-Laptop"""")
        assertContains(response.body(), """value="${category.id.value}"""")
        assertContains(response.body(), """selected="selected">Computer &amp; Peripherie</option>""")
        assertContains(response.body(), """value="800,00"""")
        assertContains(response.body(), "EUR")
        assertContains(response.body(), "Arbeitsgerät")
        assertContains(response.body(), "Änderungen speichern")
    }

    @Test
    fun `updates item and redirects to detail page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val newCategory = assertNotNull(categoryRepository.findByNormalizedName("möbel"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Edit-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "Arbeitsgerät",
        )

        val response = post(
            "/items/${item.id.value}",
            mapOf(
                "name" to "Edit-Schreibtisch",
                "categoryId" to newCategory.id.value.toString(),
                "estimatedValue" to "999,99",
                "note" to "Massivholz",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        val updatedItem = assertNotNull(itemRepository.findById(item.id))
        assertEquals("Edit-Schreibtisch", updatedItem.name.value)
        assertEquals(newCategory.id, updatedItem.categoryId)
        assertEquals(MonetaryValue.of("999.99"), updatedItem.estimatedValue)
        assertEquals("Massivholz", updatedItem.note)

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "<h1>Edit-Schreibtisch</h1>")
        assertContains(detailResponse.body(), "Möbel")
        assertContains(detailResponse.body(), "999,99 EUR")
        assertContains(detailResponse.body(), "Massivholz")
    }

    @Test
    fun `shows validation errors for invalid item edit form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Invalid-Edit-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}",
            mapOf(
                "name" to "",
                "categoryId" to "not-a-uuid",
                "estimatedValue" to "-1",
                "note" to "",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Gegenstand bearbeiten</h1>")
        assertContains(response.body(), "Name ist erforderlich.")
        assertContains(response.body(), "Kategorie ist ungültig.")
        assertContains(response.body(), "Schätzwert muss 0 oder größer sein.")
    }

    @Test
    fun `updates item with unknown estimated value when value is blank`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Blank-Edit-Estimated-Value-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}",
            mapOf(
                "name" to "Blank-Edit-Estimated-Value-Laptop",
                "categoryId" to category.id.value.toString(),
                "estimatedValue" to "",
                "note" to "",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals(MonetaryValue.unknown(), itemRepository.findById(item.id)?.estimatedValue)
    }

    @Test
    fun `shows duplicate name error for item edit form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Duplicate-Edit-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.createItem(
            name = ItemName.of("Duplicate-Edit-Monitor"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("300"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}",
            mapOf(
                "name" to "Duplicate-Edit-Monitor",
                "categoryId" to category.id.value.toString(),
                "estimatedValue" to "800",
                "note" to "",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Gegenstand bearbeiten</h1>")
        assertContains(response.body(), "Name ist bereits vergeben.")
    }

    @Test
    fun `renders item location quantity form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Location-Form-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = get("/items/${item.id.value}/locations/edit")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Bestand erfassen</h1>")
        assertContains(response.body(), "Location-Form-Laptop")
        assertContains(response.body(), "Ort auswählen")
        assertContains(response.body(), "Küche")
        assertContains(response.body(), "Menge")
        assertContains(response.body(), "Bestand speichern")
        assertContains(response.body(), """action="/items/${item.id.value}/locations"""")
    }

    @Test
    fun `sets item location quantity and redirects to detail page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Location-Quantity-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/locations",
            mapOf(
                "locationId" to kitchen.id.value.toString(),
                "quantity" to "1.000",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        assertEquals(Quantity.of(1000), itemRepository.findById(item.id)?.locationQuantities?.get(kitchen.id))

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Küche")
        assertContains(detailResponse.body(), "1.000")
        assertContains(detailResponse.body(), """href="/items/${item.id.value}/locations/${kitchen.id.value}/edit"""")
        assertContains(detailResponse.body(), """href="/items/${item.id.value}/locations/${kitchen.id.value}/relocate"""")
        assertContains(detailResponse.body(), """action="/items/${item.id.value}/locations/${kitchen.id.value}/delete"""")
    }

    @Test
    fun `replaces existing item location quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Location-Replace-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(2))

        val response = post(
            "/items/${item.id.value}/locations",
            mapOf(
                "locationId" to kitchen.id.value.toString(),
                "quantity" to "5",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals(Quantity.of(5), itemRepository.findById(item.id)?.locationQuantities?.get(kitchen.id))
    }

    @Test
    fun `renders existing item location quantity form with current values`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Existing-Location-Quantity-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1000))

        val response = get("/items/${item.id.value}/locations/${kitchen.id.value}/edit")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Bestand erfassen</h1>")
        assertContains(response.body(), "Existing-Location-Quantity-Laptop")
        assertContains(response.body(), """value="${kitchen.id.value}"""")
        assertContains(response.body(), """type="hidden" name="locationId" value="${kitchen.id.value}"""")
        assertContains(response.body(), """disabled="disabled"""")
        assertContains(response.body(), """selected="selected">Küche</option>""")
        assertContains(response.body(), """value="1.000"""")
    }

    @Test
    fun `shows notice when location quantity falls below acquisition quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Location-Quantity-Notice-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(5))
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(5), MonetaryValue.of("800"), null)

        val response = post(
            "/items/${item.id.value}/locations",
            mapOf(
                "locationId" to kitchen.id.value.toString(),
                "quantity" to "3",
            ),
        )

        assertEquals(302, response.statusCode())
        val redirect = URI.create(response.headers().firstValue("location").orElseThrow())
        assertEquals("/items/${item.id.value}", redirect.path)
        assertEquals("notice=acquisitionQuantityExceedsLocationQuantity", redirect.query)

        val detailResponse = get("${redirect.path}?${redirect.query}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Die Zugangsgesamtmenge ist größer als die aktuelle Ortsgesamtmenge.")
    }

    @Test
    fun `deletes existing item location quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Location-Delete-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(2))

        val response = post("/items/${item.id.value}/locations/${kitchen.id.value}/delete", emptyMap())

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        assertEquals(null, itemRepository.findById(item.id)?.locationQuantities?.get(kitchen.id))

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Keine Bestände erfasst")
    }

    @Test
    fun `shows validation errors for invalid item location quantity form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Invalid-Location-Quantity-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/locations",
            mapOf(
                "locationId" to "not-a-uuid",
                "quantity" to "0",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Bestand erfassen</h1>")
        assertContains(response.body(), "Ort ist ungültig.")
        assertContains(response.body(), "Menge muss eine positive ganze Zahl sein.")
    }

    @Test
    fun `shows missing location error for item location quantity form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Missing-Location-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/locations",
            mapOf(
                "locationId" to UUID.randomUUID().toString(),
                "quantity" to "1",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Bestand erfassen</h1>")
        assertContains(response.body(), "Ort wurde nicht gefunden.")
    }

    @Test
    fun `renders item relocation form for existing source location quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val livingRoom = assertNotNull(locationRepository.findByNormalizedName("wohnzimmer"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Relocation-Form-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1000))

        val response = get("/items/${item.id.value}/locations/${kitchen.id.value}/relocate")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Umlagerung erfassen</h1>")
        assertContains(response.body(), "Relocation-Form-Laptop")
        assertContains(response.body(), "Quellort")
        assertContains(response.body(), "Küche")
        assertContains(response.body(), "Aktueller Bestand")
        assertContains(response.body(), "1.000")
        assertContains(response.body(), "Zielort auswählen")
        assertContains(response.body(), """value="${livingRoom.id.value}"""")
        assertContains(response.body(), """action="/items/${item.id.value}/relocations"""")
        assertContains(response.body(), "Umlagerung speichern")
    }

    @Test
    fun `relocates item quantity and redirects to detail page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val livingRoom = assertNotNull(locationRepository.findByNormalizedName("wohnzimmer"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Relocation-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(5))
        itemUseCase.setLocationQuantity(item.id, livingRoom.id, Quantity.of(1))

        val response = post(
            "/items/${item.id.value}/relocations",
            mapOf(
                "sourceLocationId" to kitchen.id.value.toString(),
                "targetLocationId" to livingRoom.id.value.toString(),
                "quantity" to "2",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        val changedItem = assertNotNull(itemRepository.findById(item.id))
        assertEquals(Quantity.of(3), changedItem.locationQuantities[kitchen.id])
        assertEquals(Quantity.of(3), changedItem.locationQuantities[livingRoom.id])
    }

    @Test
    fun `relocation removes source location quantity when source reaches zero`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val livingRoom = assertNotNull(locationRepository.findByNormalizedName("wohnzimmer"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Relocation-Empty-Source-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(2))

        val response = post(
            "/items/${item.id.value}/relocations",
            mapOf(
                "sourceLocationId" to kitchen.id.value.toString(),
                "targetLocationId" to livingRoom.id.value.toString(),
                "quantity" to "2",
            ),
        )

        assertEquals(302, response.statusCode())
        val changedItem = assertNotNull(itemRepository.findById(item.id))
        assertEquals(null, changedItem.locationQuantities[kitchen.id])
        assertEquals(Quantity.of(2), changedItem.locationQuantities[livingRoom.id])
    }

    @Test
    fun `shows validation errors for invalid item relocation form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Invalid-Relocation-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(2))

        val response = post(
            "/items/${item.id.value}/relocations",
            mapOf(
                "sourceLocationId" to kitchen.id.value.toString(),
                "targetLocationId" to kitchen.id.value.toString(),
                "quantity" to "0",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Umlagerung erfassen</h1>")
        assertContains(response.body(), "Zielort muss sich vom Quellort unterscheiden.")
        assertContains(response.body(), "Menge muss eine positive ganze Zahl sein.")
    }

    @Test
    fun `shows validation error when relocation quantity exceeds source quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val livingRoom = assertNotNull(locationRepository.findByNormalizedName("wohnzimmer"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Excessive-Relocation-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(2))

        val response = post(
            "/items/${item.id.value}/relocations",
            mapOf(
                "sourceLocationId" to kitchen.id.value.toString(),
                "targetLocationId" to livingRoom.id.value.toString(),
                "quantity" to "3",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Umlagerung erfassen</h1>")
        assertContains(response.body(), "Menge darf den Bestand am Quellort nicht überschreiten.")
    }

    @Test
    fun `returns not found for missing item relocation form`() {
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))

        val response = get("/items/${UUID.randomUUID()}/locations/${kitchen.id.value}/relocate")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found for missing source location quantity relocation form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Missing-Relocation-Source-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = get("/items/${item.id.value}/locations/${kitchen.id.value}/relocate")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found when relocating missing item`() {
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val livingRoom = assertNotNull(locationRepository.findByNormalizedName("wohnzimmer"))

        val response = post(
            "/items/${UUID.randomUUID()}/relocations",
            mapOf(
                "sourceLocationId" to kitchen.id.value.toString(),
                "targetLocationId" to livingRoom.id.value.toString(),
                "quantity" to "1",
            ),
        )

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `renders item acquisition form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Acquisition-Form-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = get("/items/${item.id.value}/acquisitions/new")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Zugang erfassen</h1>")
        assertContains(response.body(), "Acquisition-Form-Laptop")
        assertContains(response.body(), "Bezugsquelle auswählen")
        assertContains(response.body(), """value="${amazon.id.value}"""")
        assertContains(response.body(), "Menge")
        assertContains(response.body(), "Kaufpreis")
        assertContains(response.body(), "EUR")
        assertContains(response.body(), "Kaufdatum")
        assertContains(response.body(), """action="/items/${item.id.value}/acquisitions"""")
    }

    @Test
    fun `records item acquisition and redirects to detail page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/acquisitions",
            mapOf(
                "sourceId" to amazon.id.value.toString(),
                "quantity" to "1.000",
                "purchasePrice" to "1.234,56",
                "purchaseDate" to "2026-01-03",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        val changedItem = assertNotNull(itemRepository.findById(item.id))
        val acquisition = changedItem.sources.single()
        assertEquals(amazon.id, acquisition.sourceId)
        assertEquals(Quantity.of(1000), acquisition.quantity)
        assertEquals(MonetaryValue.of("1234.56"), acquisition.purchasePrice)
        assertEquals(LocalDate.of(2026, 1, 3), acquisition.purchaseDate)

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Amazon")
        assertContains(detailResponse.body(), "1.000")
        assertContains(detailResponse.body(), "1.234,56 EUR")
        assertContains(detailResponse.body(), "03.01.2026")
        assertContains(detailResponse.body(), """href="/items/${item.id.value}/acquisitions/${acquisition.id.value}/edit"""")
        assertContains(detailResponse.body(), """href="/items/${item.id.value}/acquisitions/${acquisition.id.value}/delete"""")
    }

    @Test
    fun `records item acquisition with unknown purchase price and date when left blank`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Blank-Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/acquisitions",
            mapOf(
                "sourceId" to amazon.id.value.toString(),
                "quantity" to "1",
                "purchasePrice" to "",
                "purchaseDate" to "",
            ),
        )

        assertEquals(302, response.statusCode())
        val acquisition = assertNotNull(itemRepository.findById(item.id)).sources.single()
        assertEquals(MonetaryValue.unknown(), acquisition.purchasePrice)
        assertEquals(null, acquisition.purchaseDate)
    }

    @Test
    fun `shows notice when recorded acquisition quantity exceeds location quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Acquisition-Quantity-Notice-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1))

        val response = post(
            "/items/${item.id.value}/acquisitions",
            mapOf(
                "sourceId" to amazon.id.value.toString(),
                "quantity" to "2",
                "purchasePrice" to "10,00",
                "purchaseDate" to "",
            ),
        )

        assertEquals(302, response.statusCode())
        val redirect = URI.create(response.headers().firstValue("location").orElseThrow())
        assertEquals("/items/${item.id.value}", redirect.path)
        assertEquals("notice=acquisitionQuantityExceedsLocationQuantity", redirect.query)

        val detailResponse = get("${redirect.path}?${redirect.query}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Die Zugangsgesamtmenge ist größer als die aktuelle Ortsgesamtmenge.")
    }

    @Test
    fun `records matching item acquisition by merging quantity and showing notice`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Merged-Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1))
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1), MonetaryValue.of("10"), LocalDate.of(2026, 1, 3))

        val response = post(
            "/items/${item.id.value}/acquisitions",
            mapOf(
                "sourceId" to amazon.id.value.toString(),
                "quantity" to "2",
                "purchasePrice" to "10,00",
                "purchaseDate" to "2026-01-03",
            ),
        )

        assertEquals(302, response.statusCode())
        val redirect = URI.create(response.headers().firstValue("location").orElseThrow())
        assertEquals("/items/${item.id.value}", redirect.path)
        assertEquals("notice=acquisitionMerged,acquisitionQuantityExceedsLocationQuantity", redirect.query)
        val changedItem = assertNotNull(itemRepository.findById(item.id))
        assertEquals(1, changedItem.sources.size)
        assertEquals(Quantity.of(3), changedItem.sources.single().quantity)

        val detailResponse = get("${redirect.path}?${redirect.query}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Zugang wurde mit einem bestehenden Zugang zusammengeführt.")
        assertContains(detailResponse.body(), "Die Zugangsgesamtmenge ist größer als die aktuelle Ortsgesamtmenge.")
    }

    @Test
    fun `renders existing item acquisition form with current values`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Existing-Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1000), MonetaryValue.of("1234.56"), LocalDate.of(2026, 1, 3))
        val acquisitionId = assertNotNull(itemRepository.findById(item.id)).sources.single().id

        val response = get("/items/${item.id.value}/acquisitions/${acquisitionId.value}/edit")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Zugang bearbeiten</h1>")
        assertContains(response.body(), "Existing-Acquisition-Laptop")
        assertContains(response.body(), """selected="selected">Amazon</option>""")
        assertContains(response.body(), """value="1.000"""")
        assertContains(response.body(), """value="1.234,56"""")
        assertContains(response.body(), """value="2026-01-03"""")
        assertContains(response.body(), """action="/items/${item.id.value}/acquisitions/${acquisitionId.value}"""")
    }

    @Test
    fun `updates item acquisition and redirects to detail page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val saturn = assertNotNull(sourceRepository.findByNormalizedName("saturn"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Update-Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        val acquisitionId = assertNotNull(itemRepository.findById(item.id)).sources.single().id

        val response = post(
            "/items/${item.id.value}/acquisitions/${acquisitionId.value}",
            mapOf(
                "sourceId" to saturn.id.value.toString(),
                "quantity" to "2",
                "purchasePrice" to "750,50",
                "purchaseDate" to "2026-02-01",
            ),
        )

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        val acquisition = assertNotNull(itemRepository.findById(item.id)).sources.single()
        assertEquals(acquisitionId, acquisition.id)
        assertEquals(saturn.id, acquisition.sourceId)
        assertEquals(Quantity.of(2), acquisition.quantity)
        assertEquals(MonetaryValue.of("750.50"), acquisition.purchasePrice)
        assertEquals(LocalDate.of(2026, 2, 1), acquisition.purchaseDate)
    }

    @Test
    fun `shows notice when updated acquisition quantity exceeds location quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Update-Acquisition-Quantity-Notice-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1))
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        val acquisitionId = assertNotNull(itemRepository.findById(item.id)).sources.single().id

        val response = post(
            "/items/${item.id.value}/acquisitions/${acquisitionId.value}",
            mapOf(
                "sourceId" to amazon.id.value.toString(),
                "quantity" to "2",
                "purchasePrice" to "800,00",
                "purchaseDate" to "",
            ),
        )

        assertEquals(302, response.statusCode())
        val redirect = URI.create(response.headers().firstValue("location").orElseThrow())
        assertEquals("/items/${item.id.value}", redirect.path)
        assertEquals("notice=acquisitionQuantityExceedsLocationQuantity", redirect.query)

        val detailResponse = get("${redirect.path}?${redirect.query}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Die Zugangsgesamtmenge ist größer als die aktuelle Ortsgesamtmenge.")
    }

    @Test
    fun `shows validation errors for invalid item acquisition form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Invalid-Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/acquisitions",
            mapOf(
                "sourceId" to "not-a-uuid",
                "quantity" to "0",
                "purchasePrice" to "999.99",
                "purchaseDate" to "not-a-date",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Zugang erfassen</h1>")
        assertContains(response.body(), "Bezugsquelle ist ungültig.")
        assertContains(response.body(), "Menge muss eine positive ganze Zahl sein.")
        assertContains(response.body(), "Kaufpreis muss im deutschen Zahlenformat angegeben werden.")
        assertContains(response.body(), "Kaufdatum ist ungültig.")
    }

    @Test
    fun `shows validation error for future purchase date on item acquisition form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Future-Acquisition-Date-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post(
            "/items/${item.id.value}/acquisitions",
            mapOf(
                "sourceId" to amazon.id.value.toString(),
                "quantity" to "1",
                "purchasePrice" to "10,00",
                "purchaseDate" to LocalDate.now().plusDays(1).toString(),
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Zugang erfassen</h1>")
        assertContains(response.body(), "Kaufdatum darf nicht in der Zukunft liegen.")
    }

    @Test
    fun `renders item acquisition delete confirmation page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Delete-Acquisition-Confirm-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1), MonetaryValue.of("800"), LocalDate.of(2026, 1, 3))
        val acquisitionId = assertNotNull(itemRepository.findById(item.id)).sources.single().id

        val response = get("/items/${item.id.value}/acquisitions/${acquisitionId.value}/delete")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Zugang löschen</h1>")
        assertContains(response.body(), "Delete-Acquisition-Confirm-Laptop")
        assertContains(response.body(), "Amazon")
        assertContains(response.body(), "800,00 EUR")
        assertContains(response.body(), "Dieser Zugang wird dauerhaft gelöscht.")
        assertContains(response.body(), """action="/items/${item.id.value}/acquisitions/${acquisitionId.value}/delete"""")
        assertContains(response.body(), "Endgültig löschen")
    }

    @Test
    fun `deletes item acquisition and redirects to detail page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Delete-Acquisition-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)
        val acquisitionId = assertNotNull(itemRepository.findById(item.id)).sources.single().id

        val response = post("/items/${item.id.value}/acquisitions/${acquisitionId.value}/delete", emptyMap())

        assertEquals(302, response.statusCode())
        assertEquals("/items/${item.id.value}", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        assertEquals(emptyList(), itemRepository.findById(item.id)?.sources)

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(200, detailResponse.statusCode())
        assertContains(detailResponse.body(), "Keine Zugänge erfasst")
    }

    @Test
    fun `returns not found for missing item acquisition form`() {
        val response = get("/items/${UUID.randomUUID()}/acquisitions/new")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found for missing existing item acquisition form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Missing-Acquisition-Edit-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = get("/items/${item.id.value}/acquisitions/${UUID.randomUUID()}/edit")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found when deleting missing item acquisition`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Missing-Acquisition-Delete-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post("/items/${item.id.value}/acquisitions/${UUID.randomUUID()}/delete", emptyMap())

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `renders item delete confirmation page`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Delete-Confirm-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = get("/items/${item.id.value}/delete")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Gegenstand löschen</h1>")
        assertContains(response.body(), "Delete-Confirm-Laptop")
        assertContains(response.body(), "Computer &amp; Peripherie")
        assertContains(response.body(), "Dieser Gegenstand wird dauerhaft gelöscht.")
        assertContains(response.body(), """action="/items/${item.id.value}/delete"""")
        assertContains(response.body(), "Endgültig löschen")
        assertContains(response.body(), "Abbrechen")
    }

    @Test
    fun `deletes item and redirects to list`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Delete-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post("/items/${item.id.value}/delete", emptyMap())

        assertEquals(302, response.statusCode())
        assertEquals("/items", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        assertEquals(null, itemRepository.findById(item.id))

        val detailResponse = get("/items/${item.id.value}")
        assertEquals(404, detailResponse.statusCode())
    }

    @Test
    fun `returns not found for missing item detail page`() {
        val response = get("/items/${UUID.randomUUID()}")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found for missing item edit page`() {
        val response = get("/items/${UUID.randomUUID()}/edit")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found for missing item location quantity form`() {
        val response = get("/items/${UUID.randomUUID()}/locations/edit")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found for missing existing item location quantity form`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Missing-Existing-Location-Form-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = get("/items/${item.id.value}/locations/${kitchen.id.value}/edit")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found when setting location quantity for missing item`() {
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val response = post(
            "/items/${UUID.randomUUID()}/locations",
            mapOf(
                "locationId" to kitchen.id.value.toString(),
                "quantity" to "1",
            ),
        )

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found when deleting missing item location quantity`() {
        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(
            name = ItemName.of("Missing-Location-Delete-Laptop"),
            categoryId = category.id,
            estimatedValue = MonetaryValue.of("800"),
            note = "",
        )

        val response = post("/items/${item.id.value}/locations/${kitchen.id.value}/delete", emptyMap())

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found for missing item delete confirmation page`() {
        val response = get("/items/${UUID.randomUUID()}/delete")

        assertEquals(404, response.statusCode())
    }

    @Test
    fun `returns not found when deleting missing item`() {
        val response = post("/items/${UUID.randomUUID()}/delete", emptyMap())

        assertEquals(404, response.statusCode())
    }

    private fun get(path: String): HttpResponse<String> =
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun post(path: String, form: Map<String, String>): HttpResponse<String> =
        client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:$port$path"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.toFormBody()))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private fun Map<String, String>.toFormBody(): String =
        entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }

    private fun String.urlEncode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8)
}
