package com.gamalielhu.cartaspro.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.gamalielhu.cartaspro.docx.GeneradorDocx
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.pdf.GeneradorPdf
import java.io.File

enum class Formato(val extension: String, val mime: String, val titulo: String) {
    PDF("pdf", "application/pdf", "PDF"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "Word (.docx)")
}

object Exportador {

    fun nombreArchivo(c: Carta, formato: Formato): String {
        val base = listOf(c.tipo.titulo, c.proyecto.ifBlank { c.campo("profesional") })
            .filter { it.isNotBlank() }.joinToString(" - ")
            .replace(Regex("[^\\p{L}\\p{N} _-]"), " ")
            .replace(Regex("\\s+"), " ").trim().take(70).ifBlank { "Carta" }
        return "$base.${formato.extension}"
    }

    /** Genera el archivo en cache/compartir y lo devuelve. */
    fun generar(context: Context, c: Carta, formato: Formato): File {
        val dir = File(context.cacheDir, "compartir").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val archivo = File(dir, nombreArchivo(c, formato))
        val firmas = FirmaStore(context)
        val logos = LogoStore(context)
        archivo.outputStream().use { out ->
            when (formato) {
                Formato.PDF -> GeneradorPdf.escribir(c, firmas, logos, out)
                Formato.DOCX -> GeneradorDocx.escribir(c, firmas, logos, out)
            }
        }
        return archivo
    }

    fun copiarA(context: Context, archivo: File, destino: Uri) {
        context.contentResolver.openOutputStream(destino, "wt")?.use { out ->
            archivo.inputStream().use { it.copyTo(out) }
        } ?: error("No se pudo abrir el destino")
    }

    fun compartir(context: Context, archivo: File, formato: Formato) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
        val envio = Intent(Intent.ACTION_SEND).apply {
            type = formato.mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, archivo.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(envio, "Compartir carta"))
    }
}
