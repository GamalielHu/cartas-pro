package com.gamalielhu.cartaspro.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Montos en Bs: "18500,85" -> "18.500,85" y "DIECIOCHO MIL QUINIENTOS 85/100 BOLIVIANOS". */
object NumeroLetras {

    fun leer(texto: String): BigDecimal? {
        var t = texto.trim().replace(" ", "").replace("Bs.", "").replace("Bs", "")
        if (t.isEmpty()) return null
        val punto = t.lastIndexOf('.')
        val coma = t.lastIndexOf(',')
        t = when {
            punto >= 0 && coma >= 0 -> if (coma > punto) t.replace(".", "").replace(',', '.') else t.replace(",", "")
            coma >= 0 -> t.replace(".", "").replace(',', '.')
            punto >= 0 && t.length - punto - 1 == 3 && t.count { it == '.' } >= 1 && t.length > 4 -> t.replace(".", "")
            else -> t
        }
        return t.toBigDecimalOrNull()?.setScale(2, RoundingMode.HALF_UP)
    }

    fun formatear(monto: BigDecimal): String {
        val s = DecimalFormatSymbols(Locale.ROOT).apply { groupingSeparator = '.'; decimalSeparator = ',' }
        return DecimalFormat("#,##0.00", s).format(monto)
    }

    fun literal(monto: BigDecimal): String {
        val entero = monto.toBigInteger().toLong()
        val centavos = monto.subtract(BigDecimal(entero)).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toInt()
        return "${letras(entero)} ${"%02d".format(centavos)}/100 BOLIVIANOS"
    }

    /** "Bs. 18.500,85 (DIECIOCHO MIL QUINIENTOS 85/100 BOLIVIANOS)" o null si no es número. */
    fun montoCompleto(texto: String): String? = leer(texto)?.let { "Bs. ${formatear(it)} (${literal(it)})" }

    private val hasta29 = arrayOf(
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ",
        "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
        "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS",
        "VEINTISIETE", "VEINTIOCHO", "VEINTINUEVE"
    )
    private val decenas = arrayOf("", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA")
    private val centenas = arrayOf(
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
        "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    )

    private fun apocope(s: String) = when {
        s.endsWith("VEINTIUNO") -> s.removeSuffix("VEINTIUNO") + "VEINTIÚN"
        s.endsWith("UNO") -> s.removeSuffix("UNO") + "UN"
        else -> s
    }

    private fun menorCien(n: Int): String = when {
        n < 30 -> hasta29[n]
        n % 10 == 0 -> decenas[n / 10]
        else -> "${decenas[n / 10]} Y ${hasta29[n % 10]}"
    }

    private fun menorMil(n: Int): String = when {
        n == 0 -> ""
        n == 100 -> "CIEN"
        else -> listOf(centenas[n / 100], menorCien(n % 100)).filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun letras(n: Long): String {
        if (n == 0L) return "CERO"
        val partes = mutableListOf<String>()
        val millones = n / 1_000_000
        val miles = ((n / 1000) % 1000).toInt()
        val resto = (n % 1000).toInt()
        if (millones > 0) partes += if (millones == 1L) "UN MILLÓN" else "${apocope(letras(millones))} MILLONES"
        if (miles > 0) partes += if (miles == 1) "MIL" else "${apocope(menorMil(miles))} MIL"
        if (resto > 0) partes += apocope(menorMil(resto))
        return partes.joinToString(" ")
    }
}
