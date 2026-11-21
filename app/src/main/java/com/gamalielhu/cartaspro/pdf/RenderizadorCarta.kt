package com.gamalielhu.cartaspro.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.gamalielhu.cartaspro.data.FirmaStore
import com.gamalielhu.cartaspro.model.Alineacion
import com.gamalielhu.cartaspro.model.Bloque
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.ComposicionCarta
import com.gamalielhu.cartaspro.model.Fuente
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.model.Segmento
import kotlin.math.max
import kotlin.math.min

/**
 * Maqueta la carta en páginas (unidades = puntos tipográficos) y las dibuja sobre cualquier
 * Canvas: el de PdfDocument (exportación) o el de un Bitmap (vista previa). Así la vista previa
 * es idéntica al PDF.
 */
class RenderizadorCarta(private val carta: Carta, firmas: FirmaStore) {

    val anchoPt = carta.papel.anchoPt
    val altoPt = carta.papel.altoPt

    private val margenIzq = 85f   // 3 cm
    private val margenDer = 71f   // 2,5 cm
    private val margenSup = 71f
    private val margenInf = 64f
    private val anchoUtil = anchoPt - margenIzq - margenDer
    private val limite = altoPt - margenInf - 14f // reserva para el n° de página
    private val anchoEtiqueta = 80f

    private val tipoBase: Typeface = if (carta.fuente == Fuente.SERIF) Typeface.SERIF else Typeface.SANS_SERIF
    private val tipoNegrita: Typeface = Typeface.create(tipoBase, Typeface.BOLD)

    private val pintura = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.BLACK
        textSize = 11.5f
        typeface = tipoBase
    }

    private val imagenes: Map<Int, Bitmap> = buildMap {
        for (n in 1..2) firmas.archivo(n)?.let { f -> BitmapFactory.decodeFile(f.path)?.let { put(n, it) } }
    }

    private val paginas: List<List<(Canvas) -> Unit>> = maquetar()

    val totalPaginas: Int get() = paginas.size

    fun dibujarPagina(indice: Int, canvas: Canvas) {
        paginas[indice].forEach { it(canvas) }
        if (paginas.size > 1) {
            val p = TextPaint(pintura).apply { textSize = 9f; textAlign = Paint.Align.CENTER; color = Color.DKGRAY }
            canvas.drawText("Página ${indice + 1} de ${paginas.size}", anchoPt / 2f, altoPt - margenInf + 8f, p)
        }
    }

    fun paginaBitmap(indice: Int, escala: Float): Bitmap {
        val bmp = Bitmap.createBitmap((anchoPt * escala).toInt(), (altoPt * escala).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.scale(escala, escala)
        dibujarPagina(indice, c)
        return bmp
    }

    // ---------------------------------------------------------------------------------------

    private fun maquetar(): List<List<(Canvas) -> Unit>> {
        val paginas = mutableListOf<MutableList<(Canvas) -> Unit>>(mutableListOf())
        var y = margenSup
        fun nuevaPagina() {
            paginas += mutableListOf<(Canvas) -> Unit>()
            y = margenSup
        }
        fun ops() = paginas.last()

        /** Coloca un layout partiéndolo por líneas entre páginas. */
        fun colocar(layout: StaticLayout, x: Float, extra: ((Canvas, Float) -> Unit)? = null) {
            var linea = 0
            var primeraParte = true
            while (linea < layout.lineCount) {
                val topLinea = layout.getLineTop(linea).toFloat()
                var fin = linea
                while (fin < layout.lineCount && y + (layout.getLineBottom(fin) - topLinea) <= limite) fin++
                if (fin == linea) {
                    if (y <= margenSup) fin = linea + 1 else { nuevaPagina(); continue }
                }
                val bottom = layout.getLineBottom(fin - 1).toFloat()
                val yy = y
                val w = layout.width.toFloat()
                val conExtra = primeraParte
                ops() += { c ->
                    if (conExtra) extra?.invoke(c, yy)
                    c.save()
                    c.translate(x, yy - topLinea)
                    c.clipRect(-2f, topLinea, w + 2f, bottom)
                    layout.draw(c)
                    c.restore()
                }
                primeraParte = false
                y += bottom - topLinea
                linea = fin
                if (linea < layout.lineCount) nuevaPagina()
            }
        }

        for (bloque in ComposicionCarta.componer(carta)) {
            when (bloque) {
                is Bloque.Parrafo -> {
                    val sangriaPt = anchoUtil * bloque.sangria
                    val layout = crearLayout(bloque.segmentos, bloque.alineacion, (anchoUtil - sangriaPt).toInt(), bloque.tamano)
                    if (y > margenSup) y += bloque.espacioAntesPt
                    colocar(layout, margenIzq + sangriaPt)
                }
                is Bloque.Fila -> {
                    if (y > margenSup) y += bloque.espacioAntesPt
                    val ancho = (anchoUtil - anchoEtiqueta).toInt()
                    bloque.lineas.forEachIndexed { i, segs ->
                        val layout = crearLayout(segs, Alineacion.IZQUIERDA, ancho, 11.5f)
                        val etiqueta = if (i == 0) bloque.etiqueta else null
                        colocar(layout, margenIzq + anchoEtiqueta, etiqueta?.let { e ->
                            { c: Canvas, yy: Float ->
                                val p = TextPaint(pintura).apply { typeface = tipoNegrita }
                                c.drawText(e, margenIzq, yy - p.fontMetrics.ascent, p)
                            }
                        })
                    }
                }
                is Bloque.Firmas -> {
                    val altoImagen = 62f
                    val anchoCol = when (bloque.posicion) {
                        PosicionFirma.CENTRADA -> anchoUtil
                        PosicionFirma.IZQUIERDA -> anchoUtil * 0.55f
                        PosicionFirma.DOBLE -> anchoUtil / 2f - 8f
                    }
                    val layouts = bloque.firmas.map { f ->
                        crearLayout(f.lineas.ifEmpty { listOf(Segmento(" ")) }, Alineacion.CENTRO, anchoCol.toInt(), 11f, 1.05f)
                    }
                    val alto = altoImagen + 6f + layouts.maxOf { it.height }
                    if (y > margenSup) y += bloque.espacioAntesPt
                    if (y + alto > limite) nuevaPagina()
                    val yy = y
                    val izquierdas = when (bloque.posicion) {
                        PosicionFirma.CENTRADA -> listOf(margenIzq)
                        PosicionFirma.IZQUIERDA -> listOf(margenIzq)
                        PosicionFirma.DOBLE -> listOf(margenIzq, margenIzq + anchoUtil / 2f + 8f)
                    }
                    bloque.firmas.forEachIndexed { i, firma ->
                        val x = izquierdas.getOrElse(i) { izquierdas.last() }
                        val img = imagenes[firma.numero]
                        val layout = layouts[i]
                        ops() += { c -> dibujarFirma(c, x, anchoCol, yy, altoImagen, img, layout, bloque.linea) }
                    }
                    y += alto
                }
            }
        }
        return paginas
    }

    private fun dibujarFirma(
        c: Canvas, x: Float, ancho: Float, y: Float, altoImagen: Float,
        img: Bitmap?, texto: StaticLayout, linea: Boolean
    ) {
        val cx = x + ancho / 2f
        val yBase = y + altoImagen
        if (img != null) {
            val esc = min(170f / img.width, (altoImagen + 6f) / img.height)
            val w = img.width * esc
            val h = img.height * esc
            val dst = RectF(cx - w / 2f, yBase + 6f - h, cx + w / 2f, yBase + 6f)
            c.drawBitmap(img, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        if (linea) {
            val anchoLinea = max(150f, min(ancho - 10f, texto.getLineWidth(0) + 20f))
            val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 0.7f }
            c.drawLine(cx - anchoLinea / 2f, yBase + 2f, cx + anchoLinea / 2f, yBase + 2f, trazo)
        }
        c.save()
        c.translate(x, yBase + 6f)
        texto.draw(c)
        c.restore()
    }

    private fun crearLayout(
        segmentos: List<Segmento>, alineacionP: Alineacion, ancho: Int, tamano: Float, interlineado: Float = 1.18f
    ): StaticLayout {
        val texto = SpannableStringBuilder()
        segmentos.forEach { s ->
            val ini = texto.length
            texto.append(s.texto)
            if (s.negrita) texto.setSpan(StyleSpan(Typeface.BOLD), ini, texto.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (s.subrayado) texto.setSpan(UnderlineSpan(), ini, texto.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val alineacion = when (alineacionP) {
            Alineacion.DERECHA -> Layout.Alignment.ALIGN_OPPOSITE
            Alineacion.CENTRO -> Layout.Alignment.ALIGN_CENTER
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val p = if (tamano == pintura.textSize) pintura else TextPaint(pintura).apply { textSize = tamano }
        val b = StaticLayout.Builder.obtain(texto, 0, texto.length, p, ancho.coerceAtLeast(10))
            .setAlignment(alineacion)
            .setLineSpacing(0f, interlineado)
            .setIncludePad(false)
        if (alineacionP == Alineacion.JUSTIFICADO) b.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
        return b.build()
    }
}
