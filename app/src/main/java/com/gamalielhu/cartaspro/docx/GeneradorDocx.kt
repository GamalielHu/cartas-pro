package com.gamalielhu.cartaspro.docx

import android.graphics.BitmapFactory
import com.gamalielhu.cartaspro.data.FirmaStore
import com.gamalielhu.cartaspro.model.Alineacion
import com.gamalielhu.cartaspro.model.Bloque
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.ComposicionCarta
import com.gamalielhu.cartaspro.model.FirmaHoja
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.model.Segmento
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min
import kotlin.math.roundToLong

/** DOCX escrito a mano (OpenXML + ZipOutputStream), sin Apache POI. */
object GeneradorDocx {

    private const val MARGEN_IZQ = 1700   // 3 cm (twips)
    private const val MARGEN_DER = 1420   // 2,5 cm
    private const val MARGEN_SUP = 1420
    private const val MARGEN_INF = 1280
    private const val EMU_POR_PT = 12700L

    private class Imagen(val numero: Int, val archivo: File, val cx: Long, val cy: Long) {
        val rid get() = "rIdFirma$numero"
        val nombre get() = "firma$numero.png"
    }

    fun escribir(carta: Carta, firmas: FirmaStore, salida: OutputStream) {
        val imagenes = (1..2).mapNotNull { n ->
            val f = firmas.archivo(n) ?: return@mapNotNull null
            val op = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(f.path, op)
            if (op.outWidth <= 0) return@mapNotNull null
            val esc = min(170.0 / op.outWidth, 66.0 / op.outHeight)
            Imagen(n, f, (op.outWidth * esc * EMU_POR_PT).roundToLong(), (op.outHeight * esc * EMU_POR_PT).roundToLong())
        }.associateBy { it.numero }

        ZipOutputStream(salida).use { zip ->
            fun entrada(nombre: String, contenido: String) {
                zip.putNextEntry(ZipEntry(nombre))
                zip.write(contenido.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entrada("[Content_Types].xml", tiposContenido())
            entrada("_rels/.rels", RELS_RAIZ)
            entrada("docProps/core.xml", core(carta))
            entrada("word/styles.xml", estilos(carta))
            entrada("word/_rels/document.xml.rels", relsDocumento(imagenes.values))
            entrada("word/document.xml", documento(carta, imagenes))
            imagenes.values.forEach { img ->
                zip.putNextEntry(ZipEntry("word/media/${img.nombre}"))
                img.archivo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    // ------------------------------------------------------------------ documento

    private fun documento(carta: Carta, imagenes: Map<Int, Imagen>): String {
        val util = carta.papel.anchoTwip - MARGEN_IZQ - MARGEN_DER
        val cuerpo = StringBuilder()
        var primero = true
        for (b in ComposicionCarta.componer(carta)) {
            when (b) {
                is Bloque.Parrafo -> {
                    val antes = if (primero) 0 else (b.espacioAntesPt * 20).toInt()
                    cuerpo.append(parrafo(b.segmentos, b.alineacion, antes, (util * b.sangria).toInt()))
                }
                is Bloque.Firmas -> cuerpo.append(firmas(b, util, imagenes))
            }
            primero = false
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><w:body>$cuerpo<w:sectPr><w:pgSz w:w="${carta.papel.anchoTwip}" w:h="${carta.papel.altoTwip}"/><w:pgMar w:top="$MARGEN_SUP" w:right="$MARGEN_DER" w:bottom="$MARGEN_INF" w:left="$MARGEN_IZQ" w:header="708" w:footer="708" w:gutter="0"/></w:sectPr></w:body></w:document>"""
    }

    private fun jc(a: Alineacion) = when (a) {
        Alineacion.IZQUIERDA -> "left"
        Alineacion.DERECHA -> "right"
        Alineacion.CENTRO -> "center"
        Alineacion.JUSTIFICADO -> "both"
    }

    /** Propiedades en el orden del esquema OpenXML (b, sz, u). */
    private fun run(s: Segmento, medioPuntos: Int? = null): String {
        val rpr = buildString {
            if (s.negrita) append("<w:b/>")
            if (medioPuntos != null) append("<w:sz w:val=\"$medioPuntos\"/>")
            if (s.subrayado) append("<w:u w:val=\"single\"/>")
        }
        return "<w:r>${if (rpr.isNotEmpty()) "<w:rPr>$rpr</w:rPr>" else ""}<w:t xml:space=\"preserve\">${esc(s.texto)}</w:t></w:r>"
    }

    private fun parrafo(
        segmentos: List<Segmento>, alineacion: Alineacion, antes: Int, sangriaIzq: Int = 0,
        sangriaDer: Int = 0, extraPPr: String = ""
    ): String {
        val ind = if (sangriaIzq > 0 || sangriaDer > 0) "<w:ind w:left=\"$sangriaIzq\" w:right=\"$sangriaDer\"/>" else ""
        val runs = segmentos.joinToString("") { run(it) }
        return "<w:p><w:pPr>$extraPPr<w:spacing w:before=\"$antes\" w:after=\"0\" w:line=\"283\" w:lineRule=\"auto\"/>$ind<w:jc w:val=\"${jc(alineacion)}\"/></w:pPr>$runs</w:p>"
    }

    // ------------------------------------------------------------------ firmas

    private fun firmas(b: Bloque.Firmas, util: Int, imagenes: Map<Int, Imagen>): String {
        val anchoFirma = 3600 // 180 pt
        val antes = (b.espacioAntesPt * 20).toInt()
        return when (b.posicion) {
            PosicionFirma.CENTRADA -> {
                val lado = ((util - anchoFirma) / 2).coerceAtLeast(0)
                bloqueFirma(b.firmas.first(), lado, lado, antes, imagenes)
            }
            PosicionFirma.IZQUIERDA ->
                bloqueFirma(b.firmas.first(), 0, (util - anchoFirma).coerceAtLeast(0), antes, imagenes)
            PosicionFirma.DOBLE -> {
                val col = util / 2
                val lado = ((col - anchoFirma) / 2).coerceAtLeast(0)
                val celdas = b.firmas.joinToString("") { f ->
                    "<w:tc><w:tcPr><w:tcW w:w=\"$col\" w:type=\"dxa\"/></w:tcPr>${bloqueFirma(f, lado, lado, 0, imagenes)}</w:tc>"
                }
                val sinBordes = listOf("top", "left", "bottom", "right", "insideH", "insideV")
                    .joinToString("") { "<w:$it w:val=\"nil\"/>" }
                parrafo(emptyList(), Alineacion.IZQUIERDA, antes, extraPPr = "<w:keepNext/>") +
                    "<w:tbl><w:tblPr><w:tblW w:w=\"$util\" w:type=\"dxa\"/><w:tblBorders>$sinBordes</w:tblBorders>" +
                    "<w:tblLayout w:type=\"fixed\"/><w:tblCellMar><w:left w:w=\"0\" w:type=\"dxa\"/><w:right w:w=\"0\" w:type=\"dxa\"/></w:tblCellMar></w:tblPr>" +
                    "<w:tblGrid><w:gridCol w:w=\"$col\"/><w:gridCol w:w=\"$col\"/></w:tblGrid><w:tr><w:trPr><w:cantSplit/></w:trPr>$celdas</w:tr></w:tbl>" +
                    "<w:p/>"
            }
        }
    }

    private fun bloqueFirma(f: FirmaHoja, izq: Int, der: Int, antes: Int, imagenes: Map<Int, Imagen>): String {
        val sb = StringBuilder()
        val ind = "<w:ind w:left=\"$izq\" w:right=\"$der\"/>"
        val img = imagenes[f.numero]
        val contenidoImagen = if (img != null) dibujo(img) else ""
        // Espacio/imagen de la firma sobre la línea
        sb.append("<w:p><w:pPr><w:keepNext/><w:spacing w:before=\"$antes\" w:after=\"0\"/>$ind<w:jc w:val=\"center\"/></w:pPr>")
        if (img == null) sb.append("<w:r><w:rPr><w:sz w:val=\"60\"/></w:rPr><w:t xml:space=\"preserve\"> </w:t></w:r>")
        sb.append(contenidoImagen).append("</w:p>")
        val lineas = f.lineas.ifEmpty { listOf(Segmento(" ")) }
        lineas.forEachIndexed { i, s ->
            val borde = if (i == 0) "<w:pBdr><w:top w:val=\"single\" w:sz=\"6\" w:space=\"2\" w:color=\"000000\"/></w:pBdr>" else ""
            val keep = if (i < lineas.lastIndex) "<w:keepNext/>" else ""
            sb.append("<w:p><w:pPr>$keep$borde<w:spacing w:before=\"0\" w:after=\"0\"/>$ind<w:jc w:val=\"center\"/></w:pPr>")
            sb.append(run(s, 21))
            sb.append("</w:p>")
        }
        return sb.toString()
    }

    private fun dibujo(img: Imagen): String = """<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="${img.cx}" cy="${img.cy}"/><wp:docPr id="${img.numero}" name="Firma ${img.numero}"/><wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="${img.numero}" name="${img.nombre}"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed="${img.rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${img.cx}" cy="${img.cy}"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"""

    // ------------------------------------------------------------------ partes fijas

    private fun tiposContenido() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/></Types>"""

    private const val RELS_RAIZ = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/></Relationships>"""

    private fun relsDocumento(imagenes: Collection<Imagen>): String {
        val img = imagenes.joinToString("") {
            "<Relationship Id=\"${it.rid}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/${it.nombre}\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rIdEstilos" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>$img</Relationships>"""
    }

    private fun estilos(carta: Carta): String {
        val f = carta.fuente.familiaDocx
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="$f" w:hAnsi="$f" w:eastAsia="$f" w:cs="$f"/><w:sz w:val="23"/><w:szCs w:val="23"/><w:lang w:val="es-BO"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="0" w:line="283" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:qFormat/></w:style></w:styles>"""
    }

    private fun core(carta: Carta) = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>${esc(carta.referencia.ifBlank { carta.tipo.titulo })}</dc:title><dc:creator>${esc(ComposicionCarta.nombreFirmante1(carta).ifBlank { "CartasPro" })}</dc:creator></cp:coreProperties>"""

    private fun esc(s: String) = buildString(s.length) {
        s.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\t' -> append(' ')
                else -> if (ch.code >= 0x20 || ch == '\n') append(ch)
            }
        }
    }
}
