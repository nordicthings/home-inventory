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
        "spring.datasource.url=jdbc:h2:mem:home_inventory_web;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
        assertContains(response.body(), "Gegenstand anlegen")
        assertContains(response.body(), "Alle Kategorien")
        assertContains(response.body(), "Computer &amp; Peripherie")
        assertContains(response.body(), "Alle Orte")
        assertContains(response.body(), "Küche")
        assertContains(response.body(), "Alle Bezugsquellen")
        assertContains(response.body(), "Amazon")
        assertContains(response.body(), "Keine Gegenstände gefunden")
    }

    @Test
    fun `renders item table fragment`() {
        val response = get("/items/table?name=Lap")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<table class=\"data-table\">")
        assertContains(response.body(), "Keine Gegenstände gefunden")
    }

    @Test
    fun `renders item create form`() {
        val response = get("/items/new")

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "<h1>Gegenstand anlegen</h1>")
        assertContains(response.body(), "Name")
        assertContains(response.body(), "Kategorie auswählen")
        assertContains(response.body(), "Schätzwert")
        assertContains(response.body(), "Computer &amp; Peripherie")
    }

    @Test
    fun `shows validation errors for invalid item create form`() {
        val response = post(
            "/items",
            mapOf(
                "name" to "",
                "categoryId" to "",
                "estimatedValue" to "-1",
                "note" to "",
            ),
        )

        assertEquals(200, response.statusCode())
        assertContains(response.body(), "Name ist erforderlich.")
        assertContains(response.body(), "Kategorie ist erforderlich.")
        assertContains(response.body(), "Schätzwert muss 0 oder größer sein.")
    }

    @Test
    fun `creates item and redirects to list`() {
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
        assertEquals("/items", URI.create(response.headers().firstValue("location").orElseThrow()).path)
        assertEquals("Laptop", itemRepository.findByNormalizedName("laptop")?.name?.value)
        assertContains(itemRepository.search(ItemSearchCriteria()).map { it.name.value }, "Laptop")

        val listResponse = get("/items")
        assertEquals(200, listResponse.statusCode())
        assertContains(listResponse.body(), "Laptop")
        assertContains(listResponse.body(), "Computer &amp; Peripherie")
        assertContains(listResponse.body(), "0")
        assertContains(listResponse.body(), "800,00 EUR")
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
        assertContains(response.body(), "Küche")
        assertContains(response.body(), "intern")
        assertContains(response.body(), "1.000")
        assertContains(response.body(), "Amazon")
        assertContains(response.body(), "1.234,56 EUR")
        assertContains(response.body(), "03.01.2026")
        assertContains(response.body(), "1.234.560,00 EUR")
    }

    @Test
    fun `returns not found for missing item detail page`() {
        val response = get("/items/${UUID.randomUUID()}")

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
