package org.nordicthings.homeinventory.inventory.adapter.web

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import org.springframework.stereotype.Component
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.io.ByteArrayOutputStream
import java.util.Locale

@Component
class ItemReportPdfRenderer(
    private val templateEngine: SpringTemplateEngine,
) {
    fun render(page: ItemReportPageView): ByteArray {
        val context = Context(Locale.GERMANY).apply {
            setVariable("page", page)
        }
        val html = templateEngine.process("items/report-pdf", context).withoutDoctype()
        return ByteArrayOutputStream().use { output ->
            PdfRendererBuilder()
                .useFastMode()
                .withHtmlContent(html, null)
                .toStream(output)
                .run()
            output.toByteArray()
        }
    }

    private fun String.withoutDoctype(): String =
        substring(indexOf("<html"))
}
