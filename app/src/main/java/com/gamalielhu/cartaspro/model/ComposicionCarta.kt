package com.gamalielhu.cartaspro.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object ComposicionCarta {

    private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-BO"))

    fun fecha(fecha: LocalDate): String = formatoFecha.format(fecha)

    fun lugarFecha(ciudad: String, fecha: LocalDate): String {
        val c = ciudad.trim().ifEmpty { "Potosí" }
        return "$c, ${fecha(fecha)}"
    }

    fun ciudadDe(lugarFecha: String): String = lugarFecha.substringBefore(",").trim()
    fun soloFecha(lugarFecha: String): String =
        if (lugarFecha.contains(",")) lugarFecha.substringAfter(",").trim() else lugarFecha.trim()

    /** Divide "**texto**" en segmentos con negrita. */
    fun segmentos(linea: String, base: Segmento = Segmento("")): List<Segmento> =
        linea.split("**").mapIndexedNotNull { i, p ->
            if (p.isEmpty()) null else base.copy(texto = p, negrita = base.negrita || i % 2 == 1)
        }

    fun nombreFirmante1(c: Carta) = c.firmante1Nombre.ifBlank { c.remNombre }
    fun cargoFirmante1(c: Carta) = c.firmante1Cargo.ifBlank { c.remCargo }

    private fun conTexto(vararg s: String) = s.map { it.trim() }.filter { it.isNotEmpty() }

    fun componer(c: Carta): List<Bloque> {
        val b = mutableListOf<Bloque>()
        val izq = Alineacion.IZQUIERDA

        // Membrete de la empresa
        if (c.mostrarMembrete && c.membrete.isNotBlank()) {
            c.membrete.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach {
                b += Bloque.Parrafo(listOf(Segmento(it, negrita = true)), Alineacion.CENTRO, tamano = 12.5f)
            }
        }

        when (c.estilo) {
            Estilo.CLASICA -> encabezadoClasico(c, b)
            Estilo.MEMORANDO -> encabezadoMemorando(c, b)
            Estilo.CERTIFICADO -> if (c.titulo.isNotBlank()) {
                b += Bloque.Parrafo(
                    listOf(Segmento(c.titulo.trim().uppercase(), negrita = true, subrayado = true)),
                    Alineacion.CENTRO, espacioAntesPt = 40f, tamano = 14f
                )
            }
        }

        // Cuerpo: párrafos separados por línea en blanco; dentro, cada línea va pegada.
        var primero = true
        c.cuerpo.trim().split(Regex("\\n\\s*\\n")).forEach { parrafo ->
            val lineas = parrafo.lines().map { it.trim() }.filter { it.isNotEmpty() }
            lineas.forEachIndexed { i, l ->
                val espacio = when {
                    i > 0 -> 0f
                    primero -> if (c.estilo == Estilo.CERTIFICADO) 30f else 20f
                    else -> 10f
                }
                b += Bloque.Parrafo(segmentos(l), Alineacion.JUSTIFICADO, espacioAntesPt = espacio)
            }
            if (lineas.isNotEmpty()) primero = false
        }

        if (c.despedida.isNotBlank()) b += Bloque.Parrafo(segmentos(c.despedida.trim()), izq, espacioAntesPt = 14f)

        if (c.estilo == Estilo.CERTIFICADO && c.lugarFecha.isNotBlank()) {
            b += Bloque.Parrafo(listOf(Segmento(c.lugarFecha.trim(), negrita = true)), Alineacion.DERECHA, espacioAntesPt = 20f)
        }

        // Firmas (todas las líneas en negrita, como en los modelos)
        val l1 = conTexto(nombreFirmante1(c), cargoFirmante1(c), c.remEmpresa).map { Segmento(it, negrita = true) }.toMutableList()
        if (c.datosBajoFirma) {
            if (c.remCi.isNotBlank()) l1 += Segmento("C.I. ${c.remCi.trim()}")
            if (c.remTelefono.isNotBlank()) l1 += Segmento("Cel. ${c.remTelefono.trim()}")
        }
        val firmas = mutableListOf(FirmaHoja(1, l1))
        if (c.posicionFirma == PosicionFirma.DOBLE) {
            firmas += FirmaHoja(2, conTexto(c.firmante2Nombre, c.firmante2Cargo).map { Segmento(it, negrita = true) })
        }
        b += Bloque.Firmas(firmas, c.posicionFirma, espacioAntesPt = 30f, linea = c.lineaFirma)

        if (c.copiaArchivo) b += Bloque.Parrafo(listOf(Segmento("Cc/arch.")), izq, espacioAntesPt = 24f, tamano = 9.5f)
        return b
    }

    private fun encabezadoClasico(c: Carta, b: MutableList<Bloque>) {
        val izq = Alineacion.IZQUIERDA
        val tieneMembrete = c.mostrarMembrete && c.membrete.isNotBlank()
        if (c.lugarFecha.isNotBlank()) {
            b += Bloque.Parrafo(listOf(Segmento(c.lugarFecha.trim())), izq, espacioAntesPt = if (tieneMembrete) 24f else 0f)
        }
        var primero = true
        fun linea(texto: String, negrita: Boolean = true) {
            if (texto.isBlank()) return
            b += Bloque.Parrafo(listOf(Segmento(texto.trim(), negrita)), izq, espacioAntesPt = if (primero) 24f else 0f)
            primero = false
        }
        linea(c.destEncabezado, negrita = false)
        if (c.destNombre.isNotBlank()) linea(conTexto(c.destTratamiento, c.destNombre).joinToString(" "))
        linea(c.destCargo.uppercase())
        linea(c.destInstitucion.uppercase())
        if (c.destNombre.isNotBlank() || c.destInstitucion.isNotBlank() || c.destCargo.isNotBlank()) {
            b += Bloque.Parrafo(listOf(Segmento("Presente.")), izq, espacioAntesPt = 6f)
        }
        if (c.asunto.isNotBlank() || c.proyecto.isNotBlank()) {
            b += Bloque.Parrafo(
                listOf(Segmento("REF.: ", negrita = true), Segmento(c.asunto.trim().uppercase(), negrita = true)),
                Alineacion.JUSTIFICADO, espacioAntesPt = 18f
            )
            if (c.proyecto.isNotBlank()) {
                b += Bloque.Parrafo(listOf(Segmento("“${c.proyecto.trim().uppercase()}”", negrita = true)), Alineacion.JUSTIFICADO, sangria = 0.08f)
            }
        }
    }

    private fun encabezadoMemorando(c: Carta, b: MutableList<Bloque>) {
        fun lineasDe(vararg s: String, negrita: Boolean = false) =
            conTexto(*s).map { listOf(Segmento(it, negrita)) }
        val a = lineasDe(
            conTexto(c.destTratamiento, c.destNombre).joinToString(" "),
            conTexto(c.destCargo.uppercase(), c.destInstitucion.uppercase()).joinToString(" ")
        )
        val de = lineasDe(c.remNombre, conTexto(c.remCargo.uppercase(), c.remEmpresa.uppercase()).joinToString(" "))
        b += Bloque.Fila("A:", a.ifEmpty { listOf(listOf(Segmento(" "))) }, espacioAntesPt = 28f)
        b += Bloque.Fila("DE:", de.ifEmpty { listOf(listOf(Segmento(" "))) }, espacioAntesPt = 10f)
        if (c.proyecto.isNotBlank()) b += Bloque.Fila("REF.:", lineasDe(c.proyecto.uppercase(), negrita = true), espacioAntesPt = 10f)
        if (c.asunto.isNotBlank()) b += Bloque.Fila("OBJETO:", lineasDe(c.asunto.uppercase(), negrita = true), espacioAntesPt = 10f)
        if (c.lugarFecha.isNotBlank()) b += Bloque.Fila("FECHA:", lineasDe(soloFecha(c.lugarFecha)), espacioAntesPt = 10f)
    }
}
