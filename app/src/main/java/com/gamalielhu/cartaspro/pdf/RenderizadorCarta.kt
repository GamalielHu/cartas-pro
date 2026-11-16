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

        for (bloque in ComposicionCarta.componer(carta)) {
            when (bloque) {
                is Bloque.Parrafo -> {
                    val sangriaPt = anchoUtil * bloque.sangria
                    val layout = crearLayout(bloque, (anchoUtil - sangriaPt).toInt())
                    if (y > margenSup) y += bloque.espacioAntesPt
                    var linea = 0
                    while (linea < layout.lineCount) {
                        val topLinea = layout.getLineTop(linea).toFloat()
                        var fin = linea
                        while (fin < layout.lineCount && y + (layout.getLineBottom(fin) - topLinea) <= limite) fin++
                        if (fin == linea) {
                            if (y <= margenSup) fin = linea + 1 else { nuevaPagina(); continue }
                        }
                        val bottom = layout.getLineBottom(fin - 1).toFloat()
                        val yy = y
                        val x = margenIzq + sangriaPt
                        val w = anchoUtil - sangriaPt
                        ops() += { c ->
                            c.save()
                            c.translate(x, yy - topLinea)
                            c.clipRect(-2f, topLinea, w + 2f, bottom)
                            layout.draw(c)
                            c.restore()
                        }
                        y += bottom - topLinea
                        linea = fin
                        if (linea < layout.lineCount) nuevaPagina()
                    }
                }
                is Bloque.Firmas -> {
                    val altoImagen = 58f
                    val altoLinea = 13.5f
                    val maxLineas = bloque.firmas.maxOf { it.lineas.size }
                    val alto = altoImagen + 6f + maxLineas * altoLinea
                    if (y > margenSup) y += bloque.espacioAntesPt
                    if (y + alto > limite) nuevaPagina()
                    val yy = y
                    val centros = when (bloque.posicion) {
                        PosicionFirma.CENTRADA -> listOf(margenIzq + anchoUtil / 2f)
                        PosicionFirma.IZQUIERDA -> listOf(margenIzq + 95f)
                        PosicionFirma.DOBLE -> listOf(margenIzq + anchoUtil * 0.25f, margenIzq + anchoUtil * 0.75f)
                    }
                    bloque.firmas.forEachIndexed { i, firma ->
                        val cx = centros.getOrElse(i) { centros.last() }
                        val img = imagenes[firma.numero]
                        ops() += { c -> dibujarFirma(c, cx, yy, altoImagen, altoLinea, img, firma.lineas) }
                    }
                    y += alto
                }
            }
        }
        return paginas
    }

    private fun dibujarFirma(
        c: Canvas, cx: Float, y: Float, altoImagen: Float, altoLinea: Float,
        img: Bitmap?, lineas: List<com.gamalielhu.cartaspro.model.Segmento>
    ) {
        val anchoLinea = 180f
        val yLinea = y + altoImagen
        if (img != null) {
            val maxW = 170f
            val maxH = altoImagen + 8f
            val esc = min(maxW / img.width, maxH / img.height)
            val w = img.width * esc
            val h = img.height * esc
            val dst = RectF(cx - w / 2f, yLinea + 8f - h, cx + w / 2f, yLinea + 8f)
            c.drawBitmap(img, null, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        }
        val trazo = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 0.7f }
        c.drawLine(cx - anchoLinea / 2f, yLinea, cx + anchoLinea / 2f, yLinea, trazo)
        var yy = yLinea + 6f + altoLinea - 3f
        lineas.forEach { s ->
            val p = TextPaint(pintura).apply {
                textAlign = Paint.Align.CENTER
                textSize = 10.5f
                typeface = if (s.negrita) tipoNegrita else tipoBase
            }
            c.drawText(s.texto, cx, yy, p)
            yy += altoLinea
        }
    }

    private fun crearLayout(p: Bloque.Parrafo, ancho: Int): StaticLayout {
        val texto = SpannableStringBuilder()
        p.segmentos.forEach { s ->
            val ini = texto.length
            texto.append(s.texto)
            if (s.negrita) texto.setSpan(StyleSpan(Typeface.BOLD), ini, texto.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (s.subrayado) texto.setSpan(UnderlineSpan(), ini, texto.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val alineacion = when (p.alineacion) {
            Alineacion.DERECHA -> Layout.Alignment.ALIGN_OPPOSITE
            Alineacion.CENTRO -> Layout.Alignment.ALIGN_CENTER
            else -> Layout.Alignment.ALIGN_NORMAL
        }
        val b = StaticLayout.Builder.obtain(texto, 0, texto.length, pintura, ancho.coerceAtLeast(10))
            .setAlignment(alineacion)
            .setLineSpacing(0f, 1.18f)
            .setIncludePad(false)
        if (p.alineacion == Alineacion.JUSTIFICADO) b.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD)
        return b.build()
    }
}
