package com.gamalielhu.cartaspro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Un trazo de la firma: puntos (x, y) en píxeles del lienzo. */
typealias Trazo = List<Pair<Float, Float>>

/** Guarda las firmas como PNG transparente en filesDir (firma1.png, firma2.png). */
class FirmaStore(private val context: Context) {

    fun archivo(numero: Int): File? = File(context.filesDir, "firma$numero.png").takeIf { it.exists() && it.length() > 0 }

    fun borrar(numero: Int) {
        File(context.filesDir, "firma$numero.png").delete()
    }

    fun guardarTrazos(numero: Int, trazos: List<Trazo>, grosorPx: Float, color: Int) {
        val puntos = trazos.flatten()
        if (puntos.isEmpty()) return
        val pad = grosorPx * 2
        val minX = puntos.minOf { it.first } - pad
        val minY = puntos.minOf { it.second } - pad
        val maxX = puntos.maxOf { it.first } + pad
        val maxY = puntos.maxOf { it.second } + pad
        val ancho = max(1, (maxX - minX).roundToInt())
        val alto = max(1, (maxY - minY).roundToInt())
        val bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.TRANSPARENT)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = grosorPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        trazos.forEach { t ->
            if (t.isEmpty()) return@forEach
            val path = Path()
            path.moveTo(t[0].first - minX, t[0].second - minY)
            if (t.size == 1) path.lineTo(t[0].first - minX + 0.1f, t[0].second - minY)
            for (i in 1 until t.size) path.lineTo(t[i].first - minX, t[i].second - minY)
            canvas.drawPath(path, paint)
        }
        escribir(numero, bmp)
    }

    /** Importa una imagen de la galería (se recomienda PNG con fondo transparente). */
    fun importar(numero: Int, uri: Uri): Boolean {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
        if (opciones.outWidth <= 0) return false
        var muestra = 1
        while (max(opciones.outWidth, opciones.outHeight) / muestra > 2000) muestra *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: return false
        val escala = min(1f, 1000f / bmp.width)
        val final = if (escala < 1f)
            Bitmap.createScaledBitmap(bmp, (bmp.width * escala).roundToInt(), (bmp.height * escala).roundToInt(), true)
        else bmp
        escribir(numero, final)
        return true
    }

    private fun escribir(numero: Int, bmp: Bitmap) {
        File(context.filesDir, "firma$numero.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
