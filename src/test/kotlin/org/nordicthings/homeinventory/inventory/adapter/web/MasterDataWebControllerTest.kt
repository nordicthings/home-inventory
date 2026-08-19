package org.nordicthings.homeinventory.inventory.adapter.web

import org.junit.jupiter.api.Test
import org.nordicthings.homeinventory.HomeInventoryApplication
import org.nordicthings.homeinventory.inventory.application.port.inbound.ItemUseCase
import org.nordicthings.homeinventory.inventory.application.port.outbound.CategoryRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.LocationRepository
import org.nordicthings.homeinventory.inventory.application.port.outbound.SourceRepository
import org.nordicthings.homeinventory.inventory.domain.ItemName
import org.nordicthings.homeinventory.inventory.domain.LocationType
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
        "spring.datasource.url=jdbc:h2:mem:home_inventory_master_data_web;MODE=MariaDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    ],
)
class MasterDataWebControllerTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var sourceRepository: SourceRepository

    @Autowired
    private lateinit var itemUseCase: ItemUseCase

    private val client = HttpClient.newHttpClient()

    @Test
    fun `manages categories`() {
        val listResponse = get("/categories")
        assertEquals(200, listResponse.statusCode())
        assertContains(listResponse.body(), "<h1>Kategorien</h1>")
        assertContains(listResponse.body(), "Kategorie hinzufügen")
        assertContains(listResponse.body(), "Computer &amp; Peripherie")

        val createForm = get("/categories/new")
        assertEquals(200, createForm.statusCode())
        assertContains(createForm.body(), "<h1>Kategorie hinzufügen</h1>")
        assertContains(createForm.body(), "autofocus")

        val createResponse = post("/categories", mapOf("name" to "Testkategorie"))
        assertEquals(302, createResponse.statusCode())
        assertEquals("/categories", URI.create(createResponse.headers().firstValue("location").orElseThrow()).path)
        val category = assertNotNull(categoryRepository.findByNormalizedName("testkategorie"))

        val editForm = get("/categories/${category.id.value}/edit")
        assertEquals(200, editForm.statusCode())
        assertContains(editForm.body(), "<h1>Kategorie bearbeiten</h1>")
        assertContains(editForm.body(), """value="Testkategorie"""")

        val updateResponse = post("/categories/${category.id.value}", mapOf("name" to "Geänderte Kategorie"))
        assertEquals(302, updateResponse.statusCode())
        assertEquals("Geänderte Kategorie", categoryRepository.findById(category.id)?.name?.value)

        val deleteForm = get("/categories/${category.id.value}/delete")
        assertEquals(200, deleteForm.statusCode())
        assertContains(deleteForm.body(), "<h1>Kategorie löschen</h1>")
        assertContains(deleteForm.body(), "Geänderte Kategorie")

        val deleteResponse = post("/categories/${category.id.value}/delete", emptyMap())
        assertEquals(302, deleteResponse.statusCode())
        assertEquals(null, categoryRepository.findById(category.id))
    }

    @Test
    fun `shows category validation and in use errors`() {
        val invalidResponse = post("/categories", mapOf("name" to ""))
        assertEquals(200, invalidResponse.statusCode())
        assertContains(invalidResponse.body(), "Name ist erforderlich.")

        val existing = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val duplicateResponse = post("/categories", mapOf("name" to " COMPUTER & PERIPHERIE "))
        assertEquals(200, duplicateResponse.statusCode())
        assertContains(duplicateResponse.body(), "Name ist bereits vergeben.")

        itemUseCase.createItem(ItemName.of("Category-In-Use-Laptop"), existing.id, MonetaryValue.of("800"), "")
        val deleteResponse = get("/categories/${existing.id.value}/delete")
        assertEquals(200, deleteResponse.statusCode())
        assertContains(deleteResponse.body(), "<h1>Kategorien</h1>")
        assertContains(deleteResponse.body(), "Kategorie wird noch verwendet und kann nicht gelöscht werden.")
        assertEquals(false, deleteResponse.body().contains("<h1>Kategorie löschen</h1>"))
    }

    @Test
    fun `manages locations`() {
        val listResponse = get("/locations")
        assertEquals(200, listResponse.statusCode())
        assertContains(listResponse.body(), "<h1>Orte</h1>")
        assertContains(listResponse.body(), "Ort hinzufügen")
        assertContains(listResponse.body(), "Küche")

        val createResponse = post("/locations", mapOf("name" to "Garage", "type" to "EXTERNAL"))
        assertEquals(302, createResponse.statusCode())
        val location = assertNotNull(locationRepository.findByNormalizedName("garage"))
        assertEquals(LocationType.EXTERNAL, location.type)

        val editForm = get("/locations/${location.id.value}/edit")
        assertEquals(200, editForm.statusCode())
        assertContains(editForm.body(), "<h1>Ort bearbeiten</h1>")
        assertContains(editForm.body(), "autofocus")
        assertContains(editForm.body(), """value="Garage"""")
        assertContains(editForm.body(), """selected="selected">extern</option>""")

        val updateResponse = post("/locations/${location.id.value}", mapOf("name" to "Werkstatt", "type" to "INTERNAL"))
        assertEquals(302, updateResponse.statusCode())
        val changedLocation = assertNotNull(locationRepository.findById(location.id))
        assertEquals("Werkstatt", changedLocation.name.value)
        assertEquals(LocationType.INTERNAL, changedLocation.type)

        val deleteForm = get("/locations/${location.id.value}/delete")
        assertEquals(200, deleteForm.statusCode())
        assertContains(deleteForm.body(), "<h1>Ort löschen</h1>")
        assertContains(deleteForm.body(), "Werkstatt")

        val deleteResponse = post("/locations/${location.id.value}/delete", emptyMap())
        assertEquals(302, deleteResponse.statusCode())
        assertEquals(null, locationRepository.findById(location.id))
    }

    @Test
    fun `shows location validation and in use errors`() {
        val invalidResponse = post("/locations", mapOf("name" to "", "type" to "UNKNOWN"))
        assertEquals(200, invalidResponse.statusCode())
        assertContains(invalidResponse.body(), "Name ist erforderlich.")
        assertContains(invalidResponse.body(), "Ortstyp ist ungültig.")

        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val kitchen = assertNotNull(locationRepository.findByNormalizedName("küche"))
        val item = itemUseCase.createItem(ItemName.of("Location-In-Use-Laptop"), category.id, MonetaryValue.of("800"), "")
        itemUseCase.setLocationQuantity(item.id, kitchen.id, Quantity.of(1))

        val deleteResponse = get("/locations/${kitchen.id.value}/delete")
        assertEquals(200, deleteResponse.statusCode())
        assertContains(deleteResponse.body(), "<h1>Orte</h1>")
        assertContains(deleteResponse.body(), "Ort wird noch verwendet und kann nicht gelöscht werden.")
        assertEquals(false, deleteResponse.body().contains("<h1>Ort löschen</h1>"))
    }

    @Test
    fun `manages sources`() {
        val listResponse = get("/sources")
        assertEquals(200, listResponse.statusCode())
        assertContains(listResponse.body(), "<h1>Bezugsquellen</h1>")
        assertContains(listResponse.body(), "Bezugsquelle hinzufügen")
        assertContains(listResponse.body(), "Amazon")

        val createResponse = post("/sources", mapOf("name" to "Flohmarkt", "details" to "Samstag"))
        assertEquals(302, createResponse.statusCode())
        val source = assertNotNull(sourceRepository.findByNormalizedName("flohmarkt"))
        assertEquals("Samstag", source.details)

        val editForm = get("/sources/${source.id.value}/edit")
        assertEquals(200, editForm.statusCode())
        assertContains(editForm.body(), "<h1>Bezugsquelle bearbeiten</h1>")
        assertContains(editForm.body(), """value="Flohmarkt"""")
        assertContains(editForm.body(), "Samstag")

        val updateResponse = post("/sources/${source.id.value}", mapOf("name" to "Kleinanzeigen", "details" to "Privat"))
        assertEquals(302, updateResponse.statusCode())
        val changedSource = assertNotNull(sourceRepository.findById(source.id))
        assertEquals("Kleinanzeigen", changedSource.name.value)
        assertEquals("Privat", changedSource.details)

        val deleteForm = get("/sources/${source.id.value}/delete")
        assertEquals(200, deleteForm.statusCode())
        assertContains(deleteForm.body(), "<h1>Bezugsquelle löschen</h1>")
        assertContains(deleteForm.body(), "Kleinanzeigen")

        val deleteResponse = post("/sources/${source.id.value}/delete", emptyMap())
        assertEquals(302, deleteResponse.statusCode())
        assertEquals(null, sourceRepository.findById(source.id))
    }

    @Test
    fun `shows source validation and in use errors`() {
        val invalidResponse = post("/sources", mapOf("name" to "", "details" to ""))
        assertEquals(200, invalidResponse.statusCode())
        assertContains(invalidResponse.body(), "Name ist erforderlich.")

        val category = assertNotNull(categoryRepository.findByNormalizedName("computer & peripherie"))
        val amazon = assertNotNull(sourceRepository.findByNormalizedName("amazon"))
        val item = itemUseCase.createItem(ItemName.of("Source-In-Use-Laptop"), category.id, MonetaryValue.of("800"), "")
        itemUseCase.recordAcquisition(item.id, amazon.id, Quantity.of(1), MonetaryValue.of("800"), null)

        val deleteResponse = get("/sources/${amazon.id.value}/delete")
        assertEquals(200, deleteResponse.statusCode())
        assertContains(deleteResponse.body(), "<h1>Bezugsquellen</h1>")
        assertContains(deleteResponse.body(), "Bezugsquelle wird noch verwendet und kann nicht gelöscht werden.")
        assertEquals(false, deleteResponse.body().contains("<h1>Bezugsquelle löschen</h1>"))
    }

    @Test
    fun `returns not found for missing master data edit and delete pages`() {
        assertEquals(404, get("/categories/${UUID.randomUUID()}/edit").statusCode())
        assertEquals(404, get("/categories/${UUID.randomUUID()}/delete").statusCode())
        assertEquals(404, get("/locations/${UUID.randomUUID()}/edit").statusCode())
        assertEquals(404, get("/locations/${UUID.randomUUID()}/delete").statusCode())
        assertEquals(404, get("/sources/${UUID.randomUUID()}/edit").statusCode())
        assertEquals(404, get("/sources/${UUID.randomUUID()}/delete").statusCode())
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
