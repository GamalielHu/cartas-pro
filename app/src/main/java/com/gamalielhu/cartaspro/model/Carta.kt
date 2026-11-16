package com.gamalielhu.cartaspro.model

enum class TipoCarta(val titulo: String, val descripcion: String) {
    SOLICITUD("Solicitud formal", "Pedir algo a una autoridad, institución o empresa"),
    NOTIFICACION("Notificación / Comunicado", "Comunicar o notificar algo de forma oficial"),
    PRESENTACION("Presentación / Cotización", "Presentar tu empresa o enviar una cotización"),
    LIBRE("Carta libre", "Estructura formal con el contenido que quieras")
}

enum class PosicionFirma(val titulo: String) {
    CENTRADA("Centrada"), IZQUIERDA("Izquierda"), DOBLE("Doble firma")
}

/** Medidas en puntos (1/72") para el PDF y en twips (1/1440") para el DOCX. */
enum class Papel(val titulo: String, val anchoPt: Int, val altoPt: Int, val anchoTwip: Int, val altoTwip: Int) {
    CARTA("Carta", 612, 792, 12240, 15840),
    A4("A4", 595, 842, 11906, 16838)
}

enum class Fuente(val titulo: String, val familiaDocx: String) {
    SERIF("Serif (Times)", "Times New Roman"),
    SANS("Sans (Arial)", "Arial")
}

data class Carta(
    val tipo: TipoCarta = TipoCarta.SOLICITUD,
    val lugarFecha: String = "",
    val remNombre: String = "",
    val remCargo: String = "",
    val remCi: String = "",
    val remTelefono: String = "",
    val destEncabezado: String = "Señor:",
    val destTratamiento: String = "Sr.",
    val destNombre: String = "",
    val destCargo: String = "",
    val destInstitucion: String = "",
    val referencia: String = "",
    val cuerpo: String = "",
    val despedida: String = "Atentamente,",
    val posicionFirma: PosicionFirma = PosicionFirma.CENTRADA,
    val firmante1Nombre: String = "",
    val firmante1Cargo: String = "",
    val firmante2Nombre: String = "",
    val firmante2Cargo: String = "",
    val datosBajoFirma: Boolean = true,
    val papel: Papel = Papel.CARTA,
    val fuente: Fuente = Fuente.SERIF
)

// ---- Estructura común de la hoja: la usan el PDF, la vista previa y el DOCX ----

data class Segmento(val texto: String, val negrita: Boolean = false, val subrayado: Boolean = false)

enum class Alineacion { IZQUIERDA, DERECHA, CENTRO, JUSTIFICADO }

data class FirmaHoja(val numero: Int, val lineas: List<Segmento>)

sealed interface Bloque {
    data class Parrafo(
        val segmentos: List<Segmento>,
        val alineacion: Alineacion = Alineacion.IZQUIERDA,
        val espacioAntesPt: Float = 0f,
        /** Sangría izquierda como fracción del ancho útil (0..1). */
        val sangria: Float = 0f
    ) : Bloque

    data class Firmas(
        val firmas: List<FirmaHoja>,
        val posicion: PosicionFirma,
        val espacioAntesPt: Float
    ) : Bloque
}
