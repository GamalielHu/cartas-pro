package com.gamalielhu.cartaspro.pdf

import android.graphics.pdf.PdfDocument
import com.gamalielhu.cartaspro.data.FirmaStore
import com.gamalielhu.cartaspro.data.LogoStore
import com.gamalielhu.cartaspro.model.Carta
import java.io.OutputStream

/** PDF nativo con android.graphics.pdf.PdfDocument (1 unidad = 1 punto). */
object GeneradorPdf {
    fun escribir(carta: Carta, firmas: FirmaStore, logos: LogoStore, salida: OutputStream) {
        val r = RenderizadorCarta(carta, firmas, logos)
        val doc = PdfDocument()
        try {
            for (i in 0 until r.totalPaginas) {
                val info = PdfDocument.PageInfo.Builder(r.anchoPt, r.altoPt, i + 1).create()
                val pagina = doc.startPage(info)
                r.dibujarPagina(i, pagina.canvas)
                doc.finishPage(pagina)
            }
            doc.writeTo(salida)
        } finally {
            doc.close()
        }
    }
}
