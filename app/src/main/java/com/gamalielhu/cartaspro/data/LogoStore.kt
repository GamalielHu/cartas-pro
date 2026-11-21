package com.gamalielhu.cartaspro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Logo del encabezado de cada perfil: filesDir/logo_<perfilId>.png */
class LogoStore(private val context: Context) {

    private fun ruta(perfilId: String) = File(context.filesDir, "logo_${perfilId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.png")

    fun archivo(perfilId: String): File? =
        if (perfilId.isBlank()) null else ruta(perfilId).takeIf { it.exists() && it.length() > 0 }

    fun borrar(perfilId: String) {
        ruta(perfilId).delete()
    }

    fun guardar(perfilId: String, bmp: Bitmap) {
        val escala = 1200f / max(bmp.width, bmp.height)
        val final = if (escala < 1f)
            Bitmap.createScaledBitmap(bmp, (bmp.width * escala).roundToInt(), (bmp.height * escala).roundToInt(), true)
        else bmp
        ruta(perfilId).outputStream().use { final.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** Copia una sola vez los logos de fábrica (assets/logos/<perfil>.png). */
    fun instalarDeFabrica(ids: List<String>) {
        val marca = File(context.filesDir, "logos_fabrica_v1")
        if (marca.exists()) return
        ids.forEach { id ->
            if (archivo(id) != null) return@forEach
            runCatching {
                context.assets.open("logos/$id.png").use { entrada -> ruta(id).outputStream().use { entrada.copyTo(it) } }
            }
        }
        marca.writeText("ok")
    }

    fun importar(perfilId: String, uri: Uri): Boolean {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return false
        guardar(perfilId, bmp)
        return true
    }
}
