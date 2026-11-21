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

    fun importar(perfilId: String, uri: Uri): Boolean {
        val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return false
        guardar(perfilId, bmp)
        return true
    }
}
