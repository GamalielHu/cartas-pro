package com.gamalielhu.cartaspro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.util.zip.ZipInputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Lee el encabezado y pie de página de un .docx de la empresa: logo (con su recorte y tamaño),
 * texto del membrete, línea bajo el encabezado y si el pie dice "Cc/arch.".
 */
object ImportadorDocx {

    data class FormatoWord(
        val logo: Bitmap?,
        val logoAnchoPt: Float?,
        val membrete: String,
        val linea: Boolean,
        val copiaArchivo: Boolean,
        val arial: Boolean
    )

    fun leer(context: Context, uri: Uri): FormatoWord? {
        val partes = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { entrada ->
            ZipInputStream(entrada).use { zip ->
                while (true) {
                    val e = zip.nextEntry ?: break
                    val n = e.name
                    if (n.endsWith(".xml") || n.endsWith(".rels") || n.startsWith("word/media/")) {
                        partes[n] = zip.readBytes()
                    }
                }
            }
        } ?: return null
        val documento = partes["word/document.xml"]?.toString(Charsets.UTF_8) ?: return null
        val relsDoc = partes["word/_rels/document.xml.rels"]?.toString(Charsets.UTF_8).orEmpty()

        fun destino(rels: String, id: String): String? =
            Regex("<Relationship[^>]*Id=\"${Regex.escape(id)}\"[^>]*>").find(rels)?.value
                ?.let { Regex("Target=\"([^\"]+)\"").find(it)?.groupValues?.get(1) }

        fun parte(tipo: String): Pair<String, String>? {
            val sect = Regex("<w:sectPr[\\s\\S]*?</w:sectPr>").findAll(documento).lastOrNull()?.value ?: documento
            val refs = Regex("<w:${tipo}Reference[^>]*>").findAll(sect).map { it.value }.toList()
            val ref = refs.firstOrNull { it.contains("w:type=\"default\"") } ?: refs.firstOrNull() ?: return null
            val id = Regex("r:id=\"([^\"]+)\"").find(ref)?.groupValues?.get(1) ?: return null
            val target = destino(relsDoc, id) ?: return null
            val ruta = "word/" + target.removePrefix("/word/").removePrefix("./")
            val xml = partes[ruta]?.toString(Charsets.UTF_8) ?: return null
            return ruta to xml
        }

        fun textos(xml: String): List<String> =
            Regex("<w:p[ >][\\s\\S]*?</w:p>").findAll(xml).map { p ->
                Regex("<w:t(?: [^>]*)?>([^<]*)</w:t>").findAll(p.value).joinToString("") { it.groupValues[1] }
                    .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"")
                    .replace(Regex("\\s+"), " ").trim()
            }.filter { it.isNotEmpty() }.toList()

        // ---- encabezado
        var logo: Bitmap? = null
        var logoAncho: Float? = null
        var membrete = ""
        var linea = false
        parte("header")?.let { (ruta, xml) ->
            membrete = textos(xml).joinToString("\n")
            linea = xml.contains("<wps:wsp") || xml.contains("<v:line") || xml.contains("<w:bottom ") ||
                Regex("<wp:extent cx=\"\\d+\" cy=\"(\\d{1,5})\"").find(xml) != null
            val blip = Regex("<a:blip r:embed=\"([^\"]+)\"").find(xml)?.groupValues?.get(1)
            if (blip != null) {
                val relsRuta = ruta.replace("word/", "word/_rels/") + ".rels"
                val target = destino(partes[relsRuta]?.toString(Charsets.UTF_8).orEmpty(), blip)
                val bytes = target?.let { partes["word/" + it.removePrefix("/word/")] }
                var bmp = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                if (bmp != null) {
                    // Recorte de Word (srcRect en milésimas de porcentaje)
                    val rect = Regex("<a:srcRect([^/]*)/>").find(xml)?.groupValues?.get(1).orEmpty()
                    fun v(k: String) = Regex("\\b$k=\"(-?\\d+)\"").find(rect)?.groupValues?.get(1)?.toFloatOrNull()?.div(100000f) ?: 0f
                    val l = v("l").coerceIn(0f, 0.9f); val t = v("t").coerceIn(0f, 0.9f)
                    val r = v("r").coerceIn(0f, 0.9f); val b = v("b").coerceIn(0f, 0.9f)
                    if (l + r < 0.95f && t + b < 0.95f && (l + r + t + b) > 0f) {
                        val x = (bmp.width * l).roundToInt(); val y = (bmp.height * t).roundToInt()
                        val w = max(1, (bmp.width * (1 - l - r)).roundToInt()); val h = max(1, (bmp.height * (1 - t - b)).roundToInt())
                        bmp = Bitmap.createBitmap(bmp, x, y, w.coerceAtMost(bmp.width - x), h.coerceAtMost(bmp.height - y))
                    }
                    logo = bmp
                    // Tamaño en la hoja: el wp:extent que rodea a esta imagen
                    val antes = xml.substring(0, xml.indexOf("r:embed=\"$blip\"").coerceAtLeast(0))
                    logoAncho = Regex("<wp:extent cx=\"(\\d+)\"").findAll(antes).lastOrNull()
                        ?.groupValues?.get(1)?.toLongOrNull()?.let { it / 12700f }
                }
            }
            // Una imagen fina de ancho completo (línea dibujada) no es logo.
        }

        // ---- pie
        val copia = parte("footer")?.let { (_, xml) -> textos(xml).any { it.lowercase().replace(" ", "").startsWith("cc") } } ?: false
        val arial = Regex("w:ascii=\"([^\"]+)\"").find(documento)?.groupValues?.get(1)?.contains("Arial", true) ?: false

        return FormatoWord(logo, logoAncho, membrete, linea, copia, arial)
    }
}
