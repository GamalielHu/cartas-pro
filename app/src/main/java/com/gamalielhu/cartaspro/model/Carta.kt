package com.gamalielhu.cartaspro.model

enum class Grupo(val titulo: String) {
    OBRAS("Obras y contratos"), PERSONAL("Cartas personales"), GENERAL("Cartas generales")
}

/** Cómo se arma la hoja (según los modelos de Gamaliel). */
enum class Estilo(val titulo: String) {
    CLASICA("Carta"),          // Lugar y fecha, Señores:, Presente., REF.:, cuerpo, firma
    MEMORANDO("A / DE / REF"), // Membrete, A:, DE:, REF.:, OBJETO:, FECHA:, cuerpo, firma
    CERTIFICADO("Certificado") // Título, cuerpo, lugar y fecha al final, firma
}

enum class TipoCarta(
    val titulo: String,
    val descripcion: String,
    val grupo: Grupo,
    val estiloFijo: Estilo? = null
) {
    SOLICITUD_PAGO("Solicitud de pago", "Pago de un trabajo por contrato (monto en letras automático)", Grupo.OBRAS),
    ENTREGA_PROVISIONAL("Solicitud de entrega provisional", "Programar la recepción provisional de la obra", Grupo.OBRAS),
    ENTREGA_DEFINITIVA("Solicitud de entrega definitiva", "Programar la recepción definitiva de la obra", Grupo.OBRAS),
    DEVOLUCION_GARANTIA("Devolución de póliza / garantía", "Pedir a la aseguradora la devolución de la garantía", Grupo.OBRAS),
    CERTIFICACION_PROYECTOS("Solicitud de certificación", "Pedir certificado de ejecución de varios proyectos", Grupo.OBRAS),
    CERTIFICADO_TRABAJO("Certificado de trabajo", "Certificar a un profesional que trabajó en una obra", Grupo.OBRAS, Estilo.CERTIFICADO),

    SOLICITUD_PERSONAL("Solicitud personal", "Pedir algo a una institución a título personal", Grupo.PERSONAL),
    AUTORIZACION("Autorización", "Autorizar a otra persona a hacer un trámite", Grupo.PERSONAL),
    RECOMENDACION("Carta de recomendación", "Recomendar a una persona", Grupo.PERSONAL),
    PERSONAL_LIBRE("Carta personal libre", "Carta personal con formato formal", Grupo.PERSONAL),

    SOLICITUD("Solicitud formal", "Pedir algo a una autoridad, institución o empresa", Grupo.GENERAL),
    NOTIFICACION("Notificación / Comunicado", "Comunicar o notificar algo de forma oficial", Grupo.GENERAL),
    PRESENTACION("Presentación / Cotización", "Presentar la empresa o enviar una cotización", Grupo.GENERAL),
    LIBRE("Carta libre", "Estructura formal con el contenido que quieras", Grupo.GENERAL)
}

enum class PosicionFirma(val titulo: String) {
    CENTRADA("Centrada"), IZQUIERDA("Izquierda"), DOBLE("Doble firma")
}

/** Medidas en puntos (1/72") para el PDF y en twips (1/1440") para el DOCX. */
enum class Papel(val titulo: String, val anchoPt: Int, val altoPt: Int, val anchoTwip: Int, val altoTwip: Int) {
    CARTA("Carta", 612, 792, 12240, 15840),
    A4("A4", 595, 842, 11906, 16838),
    OFICIO("Oficio", 612, 936, 12240, 18720)
}

enum class Fuente(val titulo: String, val familiaDocx: String) {
    SERIF("Times", "Times New Roman"),
    SANS("Arial", "Arial")
}

/** Empresa o perfil personal: cada uno con sus datos y su propio formato de carta. */
data class Perfil(
    val id: String,
    val nombre: String,
    val personal: Boolean = false,
    val representante: String = "",
    val cargo: String = "",
    val empresa: String = "",
    val ci: String = "",
    val telefono: String = "",
    val ciudad: String = "Potosí",
    val membrete: String = "",
    val estilo: Estilo = Estilo.CLASICA,
    val copiaArchivo: Boolean = false,
    val datosBajoFirma: Boolean = false,
    val fuente: Fuente = Fuente.SERIF,
    val papel: Papel = Papel.CARTA,
    /** Ancho del logo del encabezado en puntos (el logo es filesDir/logo_<id>.png). */
    val logoAnchoPt: Float = 117f,
    /** Línea horizontal bajo el encabezado de página. */
    val lineaEncabezado: Boolean = false
)

data class Destinatario(
    val encabezado: String,
    val tratamiento: String,
    val nombre: String,
    val cargo: String,
    val institucion: String
) {
    val etiqueta: String
        get() = listOf(listOf(tratamiento, nombre).filter { it.isNotBlank() }.joinToString(" "), institucion)
            .filter { it.isNotBlank() }.joinToString(" · ").ifBlank { encabezado }
}

data class Carta(
    val tipo: TipoCarta = TipoCarta.SOLICITUD_PAGO,
    val estilo: Estilo = Estilo.CLASICA,
    val perfilId: String = "",
    val lugarFecha: String = "",
    val remNombre: String = "",
    val remCargo: String = "",
    val remEmpresa: String = "",
    val remCi: String = "",
    val remTelefono: String = "",
    /** Texto del encabezado de página (una línea por renglón). */
    val membrete: String = "",
    /** Mostrar encabezado de página (logo + membrete + línea). */
    val mostrarMembrete: Boolean = true,
    val logoAnchoPt: Float = 117f,
    val lineaEncabezado: Boolean = false,
    val destEncabezado: String = "Señores:",
    val destTratamiento: String = "",
    val destNombre: String = "",
    val destCargo: String = "",
    val destInstitucion: String = "",
    /** REF.: en carta clásica; OBJETO: en A/DE. */
    val asunto: String = "",
    /** Obra o proyecto: REF.: en A/DE; segunda línea de la REF. en carta clásica. */
    val proyecto: String = "",
    /** Título del certificado. */
    val titulo: String = "",
    val cuerpo: String = "",
    /** true = el cuerpo se rearma solo con los datos del trámite. */
    val cuerpoAuto: Boolean = false,
    val despedida: String = "",
    val campos: Map<String, String> = emptyMap(),
    val posicionFirma: PosicionFirma = PosicionFirma.CENTRADA,
    val firmante1Nombre: String = "",
    val firmante1Cargo: String = "",
    val firmante2Nombre: String = "",
    val firmante2Cargo: String = "",
    val datosBajoFirma: Boolean = false,
    val lineaFirma: Boolean = false,
    val copiaArchivo: Boolean = false,
    val papel: Papel = Papel.CARTA,
    val fuente: Fuente = Fuente.SERIF
) {
    fun campo(clave: String) = campos[clave].orEmpty()
    val destinatario get() = Destinatario(destEncabezado, destTratamiento, destNombre, destCargo, destInstitucion)
}

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
        val sangria: Float = 0f,
        val tamano: Float = 11.5f,
        /** Peso para repartir el espacio libre de la hoja antes de este bloque. */
        val elastico: Float = 0f
    ) : Bloque

    /** Fila "ETIQUETA:  valor" del formato A / DE / REF (valor en una o varias líneas). */
    data class Fila(
        val etiqueta: String,
        val lineas: List<List<Segmento>>,
        val espacioAntesPt: Float = 0f,
        val elastico: Float = 0f
    ) : Bloque

    data class Firmas(
        val firmas: List<FirmaHoja>,
        val posicion: PosicionFirma,
        val espacioAntesPt: Float,
        val linea: Boolean,
        val elastico: Float = 0f
    ) : Bloque
}

val Bloque.elasticidad: Float
    get() = when (this) {
        is Bloque.Parrafo -> elastico
        is Bloque.Fila -> elastico
        is Bloque.Firmas -> elastico
    }
