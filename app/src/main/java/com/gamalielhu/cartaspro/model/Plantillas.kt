package com.gamalielhu.cartaspro.model

data class Clausula(val titulo: String, val texto: String)

enum class TipoCampo { TEXTO, MULTILINEA, MONTO, FECHA }

data class CampoPlantilla(val clave: String, val etiqueta: String, val tipo: TipoCampo = TipoCampo.TEXTO, val ayuda: String = "")

/** Plantillas basadas en las cartas reales de HERHUAS, GRUPO JOSRAL y personales. */
object Plantillas {

    private const val FALTA = "______"
    private fun v(c: Carta, k: String) = c.campo(k).trim().ifEmpty { FALTA }
    private fun vMay(c: Carta, k: String) = c.campo(k).trim().uppercase().ifEmpty { FALTA }

    // --------------------------------------------------------------- campos del trámite

    private val obra = CampoPlantilla("obra", "Nombre del trabajo / obra", ayuda = "MANTENIMIENTO CENTRO DE ...")

    fun campos(tipo: TipoCarta): List<CampoPlantilla> = when (tipo) {
        TipoCarta.SOLICITUD_PAGO -> listOf(
            obra,
            CampoPlantilla("contrato", "Contrato N.º", ayuda = "222/2026"),
            CampoPlantilla("cuce", "CUCE (opcional)", ayuda = "25-0905-01-1562279-1-1"),
            CampoPlantilla("monto", "Monto Bs.", TipoCampo.MONTO, "18500,85")
        )
        TipoCarta.ENTREGA_PROVISIONAL, TipoCarta.ENTREGA_DEFINITIVA -> listOf(
            obra,
            CampoPlantilla("contrato", "Contrato N.º (opcional)"),
            CampoPlantilla("cuce", "CUCE (opcional)")
        )
        TipoCarta.DEVOLUCION_GARANTIA -> listOf(
            CampoPlantilla("monto", "Monto de la garantía Bs.", TipoCampo.MONTO, "6705,90"),
            CampoPlantilla("tipoPoliza", "Tipo de póliza", ayuda = "PÓLIZA DE GARANTÍA DE CUMPLIMIENTO DE CONTRATO"),
            CampoPlantilla("poliza", "N.º de póliza"),
            CampoPlantilla("beneficiario", "Institución beneficiaria", ayuda = "SERVICIO DEPARTAMENTAL DE GESTIÓN SOCIAL - SEDEGES POTOSÍ"),
            CampoPlantilla("vigencia", "Vigencia de la póliza hasta", TipoCampo.FECHA),
            CampoPlantilla("entrega", "Fecha de entrega definitiva", TipoCampo.FECHA),
            CampoPlantilla("banco", "Banco"),
            CampoPlantilla("moneda", "Moneda", ayuda = "Bolivianos"),
            CampoPlantilla("tipoCuenta", "Tipo de cuenta", ayuda = "Caja de ahorro"),
            CampoPlantilla("cuenta", "N.º de cuenta"),
            CampoPlantilla("titular", "Nombre de la cuenta")
        )
        TipoCarta.CERTIFICACION_PROYECTOS -> listOf(
            CampoPlantilla("proyectos", "Proyectos (uno por línea)", TipoCampo.MULTILINEA)
        )
        TipoCarta.CERTIFICADO_TRABAJO -> listOf(
            CampoPlantilla("profesional", "Nombre del profesional"),
            CampoPlantilla("ciProf", "C.I. del profesional", ayuda = "5495254 Ch."),
            CampoPlantilla("rni", "R.N.I. (opcional)", ayuda = "28-481"),
            CampoPlantilla("cargoProf", "Cargo desempeñado", ayuda = "RESIDENTE DE OBRA"),
            obra,
            CampoPlantilla("cuce", "CUCE (opcional)"),
            CampoPlantilla("desde", "Desde", TipoCampo.FECHA),
            CampoPlantilla("hasta", "Hasta", TipoCampo.FECHA)
        )
        TipoCarta.SOLICITUD_PERSONAL -> listOf(
            CampoPlantilla("detalle", "¿Qué solicitas?", TipoCampo.MULTILINEA, "se me otorgue ..."),
            CampoPlantilla("motivo", "Motivo (opcional)", TipoCampo.MULTILINEA)
        )
        TipoCarta.AUTORIZACION -> listOf(
            CampoPlantilla("autorizado", "Persona autorizada"),
            CampoPlantilla("ciAut", "C.I. de la persona autorizada"),
            CampoPlantilla("tramite", "¿Para qué la autorizas?", TipoCampo.MULTILINEA, "recoger mi documentación de ...")
        )
        TipoCarta.RECOMENDACION -> listOf(
            CampoPlantilla("persona", "Persona recomendada"),
            CampoPlantilla("ciPersona", "C.I. (opcional)"),
            CampoPlantilla("tiempo", "La conoces desde hace", ayuda = "5 años"),
            CampoPlantilla("cualidades", "Cualidades", ayuda = "responsable, honesta y trabajadora")
        )
        else -> emptyList()
    }

    // --------------------------------------------------------------- asunto, título, destinatario

    fun asunto(tipo: TipoCarta): String = when (tipo) {
        TipoCarta.SOLICITUD_PAGO -> "SOLICITUD DE PAGO DE PROYECTO"
        TipoCarta.ENTREGA_PROVISIONAL -> "SOLICITUD DE ENTREGA PROVISIONAL"
        TipoCarta.ENTREGA_DEFINITIVA -> "SOLICITUD DE ENTREGA DEFINITIVA"
        TipoCarta.DEVOLUCION_GARANTIA -> "SOLICITUD DE DEVOLUCIÓN DE GARANTÍA"
        TipoCarta.CERTIFICACION_PROYECTOS -> "SOLICITUD DE CERTIFICACIÓN DE EJECUCIÓN DE PROYECTOS DE MANTENIMIENTO Y REFACCIÓN"
        TipoCarta.SOLICITUD_PERSONAL, TipoCarta.SOLICITUD -> "SOLICITUD DE "
        TipoCarta.AUTORIZACION -> "AUTORIZACIÓN"
        TipoCarta.RECOMENDACION -> "CARTA DE RECOMENDACIÓN"
        TipoCarta.NOTIFICACION -> "NOTIFICACIÓN "
        TipoCarta.PRESENTACION -> "PRESENTACIÓN DE COTIZACIÓN"
        else -> ""
    }

    fun titulo(tipo: TipoCarta): String = when (tipo) {
        TipoCarta.CERTIFICADO_TRABAJO -> "CERTIFICADO DE TRABAJO"
        else -> ""
    }

    fun encabezado(tipo: TipoCarta): String = when (tipo) {
        TipoCarta.RECOMENDACION -> "A quien corresponda:"
        else -> "Señores:"
    }

    fun cuerpoEsAutomatico(tipo: TipoCarta) = campos(tipo).isNotEmpty()

    // --------------------------------------------------------------- cuerpo

    private fun montoTexto(c: Carta) =
        NumeroLetras.montoCompleto(c.campo("monto")) ?: "Bs. ${v(c, "monto")}"

    private fun obraCita(c: Carta) = "“${vMay(c, "obra")}”"

    fun cuerpo(c: Carta): String = when (c.tipo) {
        TipoCarta.SOLICITUD_PAGO -> {
            val cuce = c.campo("cuce").trim().let { if (it.isEmpty()) "" else ", con CUCE: $it" }
            listOf(
                "De nuestra mayor consideración:",
                "**Por medio de la presente solicito el pago correspondiente al trabajo realizado en el marco del Contrato N.º ${v(c, "contrato")}$cuce, del trabajo ${obraCita(c)}, conforme a lo convenido.**",
                "**El monto de ${montoTexto(c)}**",
                "Sin otro particular, agradezco de antemano la atención prestada y quedo a la espera de una respuesta favorable."
            )
        }
        TipoCarta.ENTREGA_PROVISIONAL, TipoCarta.ENTREGA_DEFINITIVA -> {
            val clase = if (c.tipo == TipoCarta.ENTREGA_PROVISIONAL) "provisional" else "definitiva"
            val contrato = c.campo("contrato").trim().let { if (it.isEmpty()) "" else ", en el marco del Contrato N.º $it" }
            val cuce = c.campo("cuce").trim().let { if (it.isEmpty()) "" else " con CUCE: $it" }
            listOf(
                "De mi mayor consideración, reciba un cordial saludo.",
                "**Por medio de la presente, solicito la entrega $clase del proyecto citado en referencia, ejecutado en el ${obraCita(c)}$contrato$cuce.**",
                "**Las tareas previstas en el alcance contractual fueron concluidas.**",
                "**En ese marco, solicito la programación de la entrega $clase del presente proyecto.**",
                "Sin otro particular, saludo a usted atentamente."
            )
        }
        TipoCarta.DEVOLUCION_GARANTIA -> {
            val monto = NumeroLetras.leer(c.campo("monto"))?.let { NumeroLetras.formatear(it) } ?: v(c, "monto")
            val tipoPoliza = c.campo("tipoPoliza").trim().uppercase().ifEmpty { "PÓLIZA DE GARANTÍA DE CUMPLIMIENTO DE CONTRATO" }
            listOf(
                "De nuestra mayor consideración:",
                "Por medio de esta, expreso mis sinceros saludos y deseos de éxito.",
                "**La presente tiene el objeto de SOLICITAR LA DEVOLUCIÓN DE GARANTÍA de Bs. $monto, suma que fue entregada para obtener la $tipoPoliza N.º ${v(c, "poliza")}, cuyo objeto de contrato con la institución ${vMay(c, "beneficiario")} (Beneficiario) fue concluido; para el mismo adjunto el Acta de Recepción Definitiva.**",
                "La vigencia de la póliza es hasta el ${v(c, "vigencia")}, por lo que, habiendo realizado la entrega definitiva el día ${v(c, "entrega")}, solicitamos abonar a la cuenta con los siguientes datos:",
                listOf(
                    "Banco: ${v(c, "banco")}",
                    "Moneda: ${c.campo("moneda").trim().ifEmpty { "Bolivianos" }}",
                    "Tipo de cuenta: ${c.campo("tipoCuenta").trim().ifEmpty { "Caja de ahorro" }}",
                    "Nro. de Cuenta: ${v(c, "cuenta")}",
                    "Nombre de la Cuenta: ${c.campo("titular").trim().ifEmpty { c.remNombre.ifBlank { FALTA } }}"
                ).joinToString("\n"),
                "Sin otro particular, reciba un cordial saludo."
            )
        }
        TipoCarta.CERTIFICACION_PROYECTOS -> {
            val proyectos = c.campo("proyectos").lines().map { it.trim() }.filter { it.isNotEmpty() }
            val lista = if (proyectos.isEmpty()) "**1. $FALTA**"
            else proyectos.mapIndexed { i, p -> "**${i + 1}. ${p.uppercase()}**" }.joinToString("\n")
            listOf(
                "De mi mayor consideración:",
                "**Por medio de la presente, me dirijo a ustedes con el debido respeto para solicitar se sirvan emitir la certificación correspondiente que acredite la ejecución y realización de los proyectos de mantenimiento, reparación y refacción desarrollados en diferentes centros de acogida e instituciones, los cuales se detallan a continuación:**",
                lista,
                "Dicha certificación será de gran importancia para respaldar formalmente la experiencia y cumplimiento de los trabajos ejecutados, por lo cual solicito que el documento incluya, de ser posible, el nombre de cada proyecto, lugar de ejecución, periodo de realización y conformidad del servicio realizado.",
                "Sin otro particular, y esperando una respuesta favorable a la presente solicitud, me despido agradeciendo de antemano su atención y colaboración."
            )
        }
        TipoCarta.CERTIFICADO_TRABAJO -> {
            val empresa = c.remEmpresa.trim().uppercase().ifEmpty { FALTA }
            val rep = c.remNombre.trim().uppercase().ifEmpty { FALTA }
            val ciRep = c.remCi.trim().let { if (it.isEmpty()) "" else ", con C.I. N.º $it" }
            val cargo = c.remCargo.trim().uppercase().ifEmpty { "REPRESENTANTE LEGAL" }
            val rni = c.campo("rni").trim().let { if (it.isEmpty()) "" else " y R.N.I. N.º $it" }
            val cuce = c.campo("cuce").trim().let { if (it.isEmpty()) "" else "\n**CUCE: $it**" }
            listOf(
                "**Por medio de la presente, la $empresa, legalmente representada por el Sr. $rep$ciRep, en calidad de $cargo, certifica que:**",
                "**${vMay(c, "profesional")}, con C.I. N.º ${v(c, "ciProf")}$rni, prestó sus servicios profesionales en el trabajo:**",
                "**${obraCita(c)}**$cuce",
                "**Desempeñando el cargo de ${vMay(c, "cargoProf")}, desde el ${v(c, "desde")} hasta el ${v(c, "hasta")}, cumpliendo las funciones y responsabilidades inherentes a su especialidad durante la ejecución del mencionado trabajo.**",
                "Es cuanto se certifica en honor a la verdad y para los fines que correspondan, a solicitud del interesado."
            )
        }
        TipoCarta.SOLICITUD_PERSONAL -> listOfNotNull(
            "De mi mayor consideración:",
            "Mediante la presente, me dirijo a usted con el debido respeto para hacerle llegar un cordial saludo y, a la vez, solicitarle de la manera más atenta ${v(c, "detalle").trimEnd('.')}.",
            c.campo("motivo").trim().takeIf { it.isNotEmpty() },
            "Sin otro particular, agradeciendo de antemano la atención que brinde a la presente, me despido con las consideraciones más distinguidas."
        )
        TipoCarta.AUTORIZACION -> {
            val ci = c.remCi.trim().let { if (it.isEmpty()) "" else ", con C.I. N.º $it" }
            listOf(
                "Por medio de la presente, yo, **${c.remNombre.trim().uppercase().ifEmpty { FALTA }}**$ci, autorizo a **${vMay(c, "autorizado")}**, con C.I. N.º ${v(c, "ciAut")}, para que en mi nombre y representación pueda ${v(c, "tramite").trimEnd('.')}.",
                "Se extiende la presente autorización para los fines que correspondan."
            )
        }
        TipoCarta.RECOMENDACION -> {
            val ci = c.campo("ciPersona").trim().let { if (it.isEmpty()) "" else ", con C.I. N.º $it" }
            listOf(
                "Por medio de la presente, tengo el agrado de recomendar ampliamente a **${vMay(c, "persona")}**$ci, a quien conozco desde hace ${v(c, "tiempo")}.",
                "Durante este tiempo ha demostrado ser una persona ${v(c, "cualidades")}, por lo que no dudo en recomendarla para el puesto o la actividad que considere conveniente.",
                "Se extiende la presente a solicitud del interesado, para los fines que estime pertinentes."
            )
        }
        TipoCarta.SOLICITUD -> listOf(
            "De mi mayor consideración:",
            "Mediante la presente, me dirijo a usted con el debido respeto para hacerle llegar un cordial saludo y, a la vez, solicitarle de la manera más atenta [detalle de lo que solicita].",
            "[Explique el motivo o la justificación de la solicitud].",
            "Sin otro particular, agradeciendo de antemano la atención que brinde a la presente, me despido con las consideraciones más distinguidas."
        )
        TipoCarta.NOTIFICACION -> listOf(
            "De mi consideración:",
            "Por medio de la presente, cumplo con notificarle formalmente que [hecho, decisión o requerimiento].",
            "[Detalle de antecedentes, plazos y acciones requeridas].",
            "Sin otro particular, saludo a usted atentamente."
        )
        TipoCarta.PRESENTACION -> listOf(
            "De nuestra mayor consideración:",
            "Tenemos el agrado de dirigirnos a usted para presentarle a [empresa], dedicada a [rubro], y poner a su disposición nuestros servicios.",
            "En atención a su requerimiento, adjuntamos la cotización correspondiente a [bien o servicio], con una validez de 30 días calendario.",
            "Agradeciendo la oportunidad, quedamos atentos a sus comentarios y nos despedimos con las consideraciones más distinguidas."
        )
        TipoCarta.PERSONAL_LIBRE, TipoCarta.LIBRE -> listOf(
            "De mi consideración:",
            "[Escriba aquí el contenido de la carta].",
            "Sin otro particular, me despido de usted atentamente."
        )
    }.joinToString("\n\n")

    // --------------------------------------------------------------- cláusulas rápidas

    private val comunes = listOf(
        Clausula("Saludo", "De mi mayor consideración:"),
        Clausula("Cordial saludo", "Mediante la presente, me dirijo a usted para hacerle llegar un cordial saludo y desearle éxitos en las funciones que desempeña."),
        Clausula("Adjuntos", "Para tal efecto, adjunto a la presente la documentación de respaldo correspondiente."),
        Clausula("Agradecimiento", "Agradezco de antemano la atención que se sirva prestar a la presente."),
        Clausula("Cierre", "Sin otro particular, me despido de usted con las consideraciones más distinguidas.")
    )

    private val obras = listOf(
        Clausula("Adjunto planilla", "Para tal efecto, adjunto a la presente la planilla de avance y la documentación de respaldo correspondiente."),
        Clausula("Adjunto acta", "Adjunto a la presente el Acta de Recepción correspondiente."),
        Clausula("Plazo cumplido", "Cabe señalar que los trabajos fueron concluidos dentro del plazo contractual establecido."),
        Clausula("Respuesta favorable", "Quedo a la espera de una respuesta favorable a la brevedad posible.")
    )

    fun clausulas(tipo: TipoCarta): List<Clausula> = when (tipo.grupo) {
        Grupo.OBRAS -> obras + comunes
        else -> when (tipo) {
            TipoCarta.NOTIFICACION -> listOf(
                Clausula("Plazo", "Para tal efecto, se otorga un plazo de [N] días hábiles computables a partir de la recepción de la presente."),
                Clausula("Advertencia", "En caso de incumplimiento, se procederá conforme a las disposiciones legales y contractuales vigentes.")
            ) + comunes
            TipoCarta.PRESENTACION -> listOf(
                Clausula("Validez", "La presente oferta tiene una validez de 30 días calendario a partir de la fecha."),
                Clausula("Forma de pago", "La forma de pago propuesta es [anticipo / contra entrega / crédito].")
            ) + comunes
            else -> comunes
        }
    }

    // --------------------------------------------------------------- perfiles de fábrica

    val perfilesIniciales = listOf(
        Perfil(
            id = "herhuas", nombre = "HERHUAS",
            cargo = "REPRESENTANTE LEGAL",
            empresa = "EMPRESA CONSTRUCTORA Y CONSULTORA HERHUAS S.R.L.", ciudad = "Potosí",
            membrete = "EMPRESA CONSTRUCTORA Y CONSULTORA\nHERHUAS S.R.L.\nPOTOSÍ – BOLIVIA",
            estilo = Estilo.MEMORANDO, copiaArchivo = true, lineaEncabezado = true
        ),
        Perfil(
            id = "josral", nombre = "GRUPO JOSRAL",
            cargo = "REPRESENTANTE LEGAL",
            empresa = "EMPRESA CONSTRUCTORA Y CONSULTORA GRUPO JOSRAL (ECYCGJ) S.R.L.", ciudad = "Potosí",
            estilo = Estilo.CLASICA, copiaArchivo = true
        ),
        Perfil(
            id = "personal", nombre = "Personal", personal = true,
            ciudad = "Potosí",
            estilo = Estilo.CLASICA, datosBajoFirma = true
        )
    )
}
