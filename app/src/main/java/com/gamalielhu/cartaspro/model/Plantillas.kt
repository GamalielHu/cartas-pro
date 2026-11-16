package com.gamalielhu.cartaspro.model

data class Clausula(val titulo: String, val texto: String)

object Plantillas {

    private val comunes = listOf(
        Clausula("Saludo", "De mi mayor consideración:"),
        Clausula("Cordial saludo", "Mediante la presente, me dirijo a usted para hacerle llegar un cordial saludo y desearle éxitos en las funciones que desempeña."),
        Clausula("Adjuntos", "Para tal efecto, adjunto a la presente la documentación de respaldo correspondiente."),
        Clausula("Agradecimiento", "Agradezco de antemano la atención que se sirva prestar a la presente."),
        Clausula("Cierre", "Sin otro particular, me despido de usted con las consideraciones más distinguidas.")
    )

    fun clausulas(tipo: TipoCarta): List<Clausula> = when (tipo) {
        TipoCarta.SOLICITUD -> listOf(
            Clausula("Solicitud", "Por medio de la presente, solicito a su autoridad de la manera más atenta se sirva [detalle de lo solicitado]."),
            Clausula("Fundamento", "La presente solicitud se fundamenta en [norma, contrato o motivo], por lo que considero procedente lo requerido."),
            Clausula("Pronta respuesta", "En espera de una respuesta favorable a la brevedad posible, quedo a su disposición para cualquier aclaración.")
        ) + comunes
        TipoCarta.NOTIFICACION -> listOf(
            Clausula("Notificar", "Por medio de la presente, cumplo con notificarle formalmente que [hecho o decisión que se comunica]."),
            Clausula("Plazo", "Para tal efecto, se otorga un plazo de [N] días hábiles computables a partir de la recepción de la presente."),
            Clausula("Advertencia", "En caso de incumplimiento, se procederá conforme a las disposiciones legales y contractuales vigentes."),
            Clausula("Constancia", "La presente se emite para fines de constancia y los efectos que correspondan.")
        ) + comunes
        TipoCarta.PRESENTACION -> listOf(
            Clausula("Presentación", "Tenemos el agrado de presentarle a [empresa], dedicada a [rubro], con amplia experiencia en el sector."),
            Clausula("Cotización", "En atención a su requerimiento, adjuntamos nuestra cotización por [bien o servicio], por un monto total de Bs [monto]."),
            Clausula("Validez", "La presente oferta tiene una validez de 30 días calendario a partir de la fecha."),
            Clausula("Forma de pago", "La forma de pago propuesta es [anticipo / contra entrega / crédito], según lo que convenga a su institución."),
            Clausula("Disponibilidad", "Quedamos a su entera disposición para ampliar cualquier información o coordinar una reunión.")
        ) + comunes
        TipoCarta.LIBRE -> comunes
    }

    fun referencia(tipo: TipoCarta): String = when (tipo) {
        TipoCarta.SOLICITUD -> "SOLICITUD DE "
        TipoCarta.NOTIFICACION -> "NOTIFICACIÓN "
        TipoCarta.PRESENTACION -> "PRESENTACIÓN DE COTIZACIÓN"
        TipoCarta.LIBRE -> ""
    }

    /** Estructura formal completa para autocompletar el cuerpo. */
    fun cuerpo(tipo: TipoCarta): String = when (tipo) {
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
        TipoCarta.LIBRE -> listOf(
            "De mi consideración:",
            "[Escriba aquí el contenido de la carta].",
            "Sin otro particular, me despido de usted atentamente."
        )
    }.joinToString("\n\n")
}
