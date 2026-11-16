package com.gamalielhu.cartaspro.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ComposicionCarta {

    private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-BO"))

    fun lugarFecha(ciudad: String, fecha: LocalDate): String {
        val c = ciudad.trim().ifEmpty { "Sucre" }
        return "$c, ${formatoFecha.format(fecha)}"
    }

    fun ciudadDe(lugarFecha: String): String = lugarFecha.substringBefore(",").trim()

    /** Divide "**texto**" en segmentos con negrita. */
    fun segmentos(linea: String, base: Segmento = Segmento("")): List<Segmento> =
        linea.split("**").mapIndexedNotNull { i, p ->
            if (p.isEmpty()) null else base.copy(texto = p, negrita = base.negrita || i % 2 == 1)
        }

    fun nombreFirmante1(c: Carta) = c.firmante1Nombre.ifBlank { c.remNombre }
    fun cargoFirmante1(c: Carta) = c.firmante1Cargo.ifBlank { c.remCargo }

    fun componer(c: Carta): List<Bloque> {
        val b = mutableListOf<Bloque>()
        val izq = Alineacion.IZQUIERDA

        if (c.lugarFecha.isNotBlank()) b += Bloque.Parrafo(listOf(Segmento(c.lugarFecha.trim())), Alineacion.DERECHA)

        // Destinatario
        var primero = true
        fun linea(texto: String, negrita: Boolean = true, subrayado: Boolean = false) {
            if (texto.isBlank()) return
            b += Bloque.Parrafo(
                listOf(Segmento(texto.trim(), negrita, subrayado)), izq,
                espacioAntesPt = if (primero) 26f else 0f
            )
            primero = false
        }
        linea(c.destEncabezado, negrita = false)
        linea(listOf(c.destTratamiento.trim(), c.destNombre.trim()).filter { it.isNotEmpty() }.joinToString(" ")
            .takeIf { c.destNombre.isNotBlank() } ?: "")
        linea(c.destCargo.uppercase())
        linea(c.destInstitucion.uppercase())
        linea("Presente.-", negrita = true, subrayado = true)

        if (c.referencia.isNotBlank()) {
            b += Bloque.Parrafo(
                listOf(Segmento("REF.: ", negrita = true), Segmento(c.referencia.trim().uppercase(), negrita = true, subrayado = true)),
                Alineacion.JUSTIFICADO, espacioAntesPt = 20f, sangria = 0.38f
            )
        }

        // Cuerpo: cada línea con texto es un párrafo.
        var primerParrafo = true
        c.cuerpo.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { p ->
            b += Bloque.Parrafo(segmentos(p), Alineacion.JUSTIFICADO, espacioAntesPt = if (primerParrafo) 20f else 10f)
            primerParrafo = false
        }

        if (c.despedida.isNotBlank()) b += Bloque.Parrafo(listOf(Segmento(c.despedida.trim())), izq, espacioAntesPt = 14f)

        // Firmas
        val firmas = mutableListOf<FirmaHoja>()
        val l1 = mutableListOf<Segmento>()
        nombreFirmante1(c).takeIf { it.isNotBlank() }?.let { l1 += Segmento(it.trim(), negrita = true) }
        cargoFirmante1(c).takeIf { it.isNotBlank() }?.let { l1 += Segmento(it.trim()) }
        if (c.datosBajoFirma) {
            if (c.remCi.isNotBlank()) l1 += Segmento("C.I./NIT: ${c.remCi.trim()}")
            if (c.remTelefono.isNotBlank()) l1 += Segmento("Telf.: ${c.remTelefono.trim()}")
        }
        firmas += FirmaHoja(1, l1)
        if (c.posicionFirma == PosicionFirma.DOBLE) {
            val l2 = mutableListOf<Segmento>()
            if (c.firmante2Nombre.isNotBlank()) l2 += Segmento(c.firmante2Nombre.trim(), negrita = true)
            if (c.firmante2Cargo.isNotBlank()) l2 += Segmento(c.firmante2Cargo.trim())
            firmas += FirmaHoja(2, l2)
        }
        b += Bloque.Firmas(firmas, c.posicionFirma, espacioAntesPt = 34f)
        return b
    }
}
