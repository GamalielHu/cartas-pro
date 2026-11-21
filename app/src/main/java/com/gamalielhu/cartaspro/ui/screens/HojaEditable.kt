package com.gamalielhu.cartaspro.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.Estilo
import com.gamalielhu.cartaspro.model.Fuente
import com.gamalielhu.cartaspro.model.Plantillas
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.ui.CartasViewModel

/** Muestra en mayúsculas sin cambiar lo escrito (como saldrá en el PDF). */
private object Mayusculas : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val m = text.text.uppercase()
        return if (m.length == text.text.length) TransformedText(AnnotatedString(m), OffsetMapping.Identity)
        else TransformedText(text, OffsetMapping.Identity)
    }
}

/** Pinta **texto** en negrita y deja los asteriscos tenues (mismas posiciones). */
private object NegritaMarcada : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        val out = buildAnnotatedString {
            append(s)
            var i = 0
            var abierto = -1
            while (i < s.length - 1) {
                if (s[i] == '*' && s[i + 1] == '*') {
                    addStyle(SpanStyle(color = Color(0xFFB0B6C3), fontSize = 9.sp), i, i + 2)
                    if (abierto < 0) abierto = i + 2 else {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), abierto, i)
                        abierto = -1
                    }
                    i += 2
                } else i++
            }
        }
        return TransformedText(out, OffsetMapping.Identity)
    }
}

private val fondoCampo = Color(0x141F3A5F)
private val gris = Color(0xFF9AA3B2)

@Composable
private fun CampoHoja(
    valor: String,
    alCambiar: (String) -> Unit,
    estilo: TextStyle,
    pista: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    alineacion: TextAlign = TextAlign.Start,
    mayusculas: Boolean = false,
    unaLinea: Boolean = false
) {
    val st = estilo.copy(textAlign = alineacion)
    BasicTextField(
        value = valor,
        onValueChange = alCambiar,
        textStyle = st,
        singleLine = unaLinea,
        visualTransformation = if (mayusculas) Mayusculas else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            capitalization = if (mayusculas) KeyboardCapitalization.Characters else KeyboardCapitalization.Sentences
        ),
        modifier = modifier.background(fondoCampo, RoundedCornerShape(3.dp)).padding(horizontal = 3.dp, vertical = 1.dp),
        decorationBox = { campo ->
            Box {
                if (valor.isEmpty()) Text(pista, style = st.copy(color = gris, fontWeight = FontWeight.Normal), modifier = Modifier.fillMaxWidth())
                campo()
            }
        }
    )
}

@Composable
private fun FilaHoja(etiqueta: String, estilo: TextStyle, contenido: @Composable ColumnScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(etiqueta, style = estilo.copy(fontWeight = FontWeight.Bold), modifier = Modifier.width(64.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp), content = contenido)
    }
}

/** Editor tipo Word: se escribe directo sobre la hoja. */
@Composable
fun HojaEditable(vm: CartasViewModel, carta: Carta, version: Int, alDibujarFirma: (Int) -> Unit) {
    val familia = if (carta.fuente == Fuente.SERIF) FontFamily.Serif else FontFamily.SansSerif
    val base = TextStyle(fontFamily = familia, fontSize = 12.5.sp, lineHeight = 17.sp, color = Color.Black)
    val negrita = base.copy(fontWeight = FontWeight.Bold)
    val esp = 14.dp

    var cuerpo by remember { mutableStateOf(TextFieldValue(carta.cuerpo, TextRange(carta.cuerpo.length))) }
    LaunchedEffect(carta.cuerpo) {
        if (carta.cuerpo != cuerpo.text) cuerpo = TextFieldValue(carta.cuerpo, TextRange(carta.cuerpo.length))
    }
    fun ponerCuerpo(v: TextFieldValue) {
        cuerpo = v
        if (v.text != carta.cuerpo) vm.editarCuerpo(v.text)
    }
    fun negritaSeleccion() {
        val t = cuerpo.text
        val a = cuerpo.selection.min.coerceIn(0, t.length)
        val b = cuerpo.selection.max.coerceIn(0, t.length)
        val nuevo = t.substring(0, a) + "**" + t.substring(a, b) + "**" + t.substring(b)
        ponerCuerpo(TextFieldValue(nuevo, if (a == b) TextRange(a + 2) else TextRange(b + 4)))
    }
    fun insertar(texto: String) {
        val t = cuerpo.text
        val a = cuerpo.selection.min.coerceIn(0, t.length)
        val b = cuerpo.selection.max.coerceIn(0, t.length)
        val antes = t.substring(0, a)
        val sep = when {
            antes.isEmpty() || antes.endsWith("\n\n") -> ""
            antes.endsWith("\n") -> "\n"
            else -> "\n\n"
        }
        val nuevo = antes + sep + texto + t.substring(b)
        ponerCuerpo(TextFieldValue(nuevo, TextRange((antes + sep + texto).length)))
    }
    var menuClausulas by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color(0xFFD9DCE3))) {
        // Barra de herramientas
        Surface(tonalElevation = 2.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(onClick = { negritaSeleccion() }) { Text("N", fontWeight = FontWeight.Black) }
                Box {
                    TextButton(onClick = { menuClausulas = true }) { Text("Cláusulas ▾") }
                    DropdownMenu(expanded = menuClausulas, onDismissRequest = { menuClausulas = false }) {
                        Plantillas.clausulas(carta.tipo).forEach { c ->
                            DropdownMenuItem(text = { Text(c.titulo) }, onClick = { menuClausulas = false; insertar(c.texto) })
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (Plantillas.cuerpoEsAutomatico(carta.tipo)) {
                    if (carta.cuerpoAuto) {
                        Text("Texto automático", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        TextButton(onClick = { vm.regenerarCuerpo() }) { Text("Rearmar con datos") }
                    }
                }
            }
        }

        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(10.dp)) {
            Surface(color = Color.White, modifier = Modifier.fillMaxWidth().shadow(4.dp)) {
                Column(Modifier.padding(start = 26.dp, end = 20.dp, top = 24.dp, bottom = 30.dp)) {

                    if (carta.mostrarMembrete) EncabezadoHoja(vm, carta, version, negrita)

                    when (carta.estilo) {
                        Estilo.CLASICA -> {
                            CampoHoja(carta.lugarFecha, { v -> vm.actualizar { it.copy(lugarFecha = v) } }, base, "Ciudad, fecha", unaLinea = true)
                            Spacer(Modifier.height(esp))
                            CampoHoja(carta.destEncabezado, { v -> vm.actualizar { it.copy(destEncabezado = v) } }, base, "Señores:", unaLinea = true)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                CampoHoja(carta.destTratamiento, { v -> vm.actualizar { it.copy(destTratamiento = v) } }, negrita, "Lic.",
                                    Modifier.width(52.dp), unaLinea = true)
                                CampoHoja(carta.destNombre, { v -> vm.actualizar { it.copy(destNombre = v) } }, negrita, "Nombre del destinatario",
                                    Modifier.weight(1f))
                            }
                            CampoHoja(carta.destCargo, { v -> vm.actualizar { it.copy(destCargo = v) } }, negrita, "CARGO", mayusculas = true)
                            CampoHoja(carta.destInstitucion, { v -> vm.actualizar { it.copy(destInstitucion = v) } }, negrita, "INSTITUCIÓN", mayusculas = true)
                            Text("Presente.", style = base, modifier = Modifier.padding(top = 4.dp))
                            Spacer(Modifier.height(esp))
                            Row {
                                Text("REF.: ", style = negrita)
                                Column(Modifier.weight(1f)) {
                                    CampoHoja(carta.asunto, { v -> vm.actualizar { it.copy(asunto = v) } }, negrita, "ASUNTO", mayusculas = true)
                                    CampoHoja(carta.proyecto, { v -> vm.actualizar { it.copy(proyecto = v) } }, negrita,
                                        "“OBRA O PROYECTO” (opcional)", mayusculas = true)
                                }
                            }
                        }
                        Estilo.MEMORANDO -> {
                            FilaHoja("A:", base) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    CampoHoja(carta.destTratamiento, { v -> vm.actualizar { it.copy(destTratamiento = v) } }, base, "Lic.",
                                        Modifier.width(48.dp), unaLinea = true)
                                    CampoHoja(carta.destNombre, { v -> vm.actualizar { it.copy(destNombre = v) } }, base, "Nombre", Modifier.weight(1f))
                                }
                                CampoHoja(carta.destCargo, { v -> vm.actualizar { it.copy(destCargo = v) } }, base, "CARGO", mayusculas = true)
                                CampoHoja(carta.destInstitucion, { v -> vm.actualizar { it.copy(destInstitucion = v) } }, base, "INSTITUCIÓN", mayusculas = true)
                            }
                            FilaHoja("DE:", base) {
                                CampoHoja(carta.remNombre, { v -> vm.actualizar { it.copy(remNombre = v) } }, base, "Tu nombre")
                                CampoHoja(carta.remCargo, { v -> vm.actualizar { it.copy(remCargo = v) } }, base, "CARGO", mayusculas = true)
                                CampoHoja(carta.remEmpresa, { v -> vm.actualizar { it.copy(remEmpresa = v) } }, base, "EMPRESA", mayusculas = true)
                            }
                            FilaHoja("REF.:", base) {
                                CampoHoja(carta.proyecto, { v -> vm.actualizar { it.copy(proyecto = v) } }, negrita, "OBRA / PROYECTO", mayusculas = true)
                            }
                            FilaHoja("OBJETO:", base) {
                                CampoHoja(carta.asunto, { v -> vm.actualizar { it.copy(asunto = v) } }, negrita, "ASUNTO", mayusculas = true)
                            }
                            FilaHoja("FECHA:", base) {
                                CampoHoja(carta.lugarFecha, { v -> vm.actualizar { it.copy(lugarFecha = v) } }, base, "Ciudad, fecha", unaLinea = true)
                            }
                        }
                        Estilo.CERTIFICADO -> {
                            Spacer(Modifier.height(esp))
                            CampoHoja(carta.titulo, { v -> vm.actualizar { it.copy(titulo = v) } },
                                negrita.copy(fontSize = 15.sp, textDecoration = TextDecoration.Underline),
                                "TÍTULO", alineacion = TextAlign.Center, mayusculas = true)
                        }
                    }

                    Spacer(Modifier.height(esp))
                    BasicTextField(
                        value = cuerpo,
                        onValueChange = { ponerCuerpo(it) },
                        textStyle = base.copy(textAlign = TextAlign.Justify),
                        visualTransformation = NegritaMarcada,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp)
                            .background(fondoCampo, RoundedCornerShape(3.dp)).padding(3.dp),
                        decorationBox = { campo ->
                            Box {
                                if (cuerpo.text.isEmpty()) Text("Escribe aquí la carta…", style = base.copy(color = gris))
                                campo()
                            }
                        }
                    )
                    CampoHoja(carta.despedida, { v -> vm.actualizar { it.copy(despedida = v) } }, base, "Despedida (opcional)",
                        Modifier.fillMaxWidth().padding(top = 8.dp))

                    if (carta.estilo == Estilo.CERTIFICADO) {
                        Spacer(Modifier.height(esp))
                        CampoHoja(carta.lugarFecha, { v -> vm.actualizar { it.copy(lugarFecha = v) } }, negrita, "Ciudad, fecha",
                            alineacion = TextAlign.End, unaLinea = true)
                    }

                    Spacer(Modifier.height(24.dp))
                    when (carta.posicionFirma) {
                        PosicionFirma.DOBLE -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.weight(1f)) { FirmaHoja1(vm, carta, version, negrita, alDibujarFirma) }
                            Column(Modifier.weight(1f)) { FirmaHoja2(vm, carta, version, negrita, alDibujarFirma) }
                        }
                        PosicionFirma.IZQUIERDA -> Column(Modifier.fillMaxWidth(0.62f)) { FirmaHoja1(vm, carta, version, negrita, alDibujarFirma) }
                        PosicionFirma.CENTRADA -> Column(Modifier.fillMaxWidth()) { FirmaHoja1(vm, carta, version, negrita, alDibujarFirma) }
                    }
                    if (carta.copiaArchivo) {
                        Spacer(Modifier.height(40.dp))
                        Text("Cc/arch.", style = base.copy(fontSize = 10.sp))
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EncabezadoHoja(vm: CartasViewModel, carta: Carta, version: Int, negrita: TextStyle) {
    val logo = remember(carta.perfilId, version) {
        vm.logos.archivo(carta.perfilId)?.let { BitmapFactory.decodeFile(it.path) }
    }
    val lineas = carta.membrete.lines().filter { it.isNotBlank() }
    if (logo == null && lineas.isEmpty() && !carta.lineaEncabezado) return
    // En el celular la hoja útil mide ~295 dp para ~456 pt: ~0.64 dp por punto.
    val escala = 0.64f
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        if (logo != null) {
            val w = (carta.logoAnchoPt * escala).dp
            Image(logo.asImageBitmap(), "Logo", Modifier.width(w).height(w * logo.height / logo.width), contentScale = ContentScale.Fit)
            Spacer(Modifier.width(8.dp))
        }
        if (lineas.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                lineas.forEach { Text(it.trim(), style = negrita.copy(fontSize = 7.5.sp, lineHeight = 9.sp)) }
            }
        }
    }
    if (carta.lineaEncabezado) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.Black))
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ImagenFirmaHoja(vm: CartasViewModel, numero: Int, version: Int, alDibujar: (Int) -> Unit) {
    val imagen = remember(numero, version) {
        vm.firmas.archivo(numero)?.let { BitmapFactory.decodeFile(it.path) }?.asImageBitmap()
    }
    Box(
        Modifier.fillMaxWidth().height(64.dp).clickable { alDibujar(numero) }
            .border(1.dp, if (imagen == null) Color(0x331F3A5F) else Color.Transparent, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imagen != null) Image(imagen, "Firma", Modifier.fillMaxSize().padding(2.dp), contentScale = ContentScale.Fit)
        else Text("Toca para firmar (o deja el espacio)", color = gris, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun FirmaHoja1(vm: CartasViewModel, c: Carta, version: Int, estilo: TextStyle, alDibujar: (Int) -> Unit) {
    val est = estilo.copy(fontSize = 12.sp)
    ImagenFirmaHoja(vm, 1, version, alDibujar)
    if (c.firmante1Nombre.isNotBlank()) {
        CampoHoja(c.firmante1Nombre, { v -> vm.actualizar { it.copy(firmante1Nombre = v) } }, est, "Nombre", alineacion = TextAlign.Center)
    } else {
        CampoHoja(c.remNombre, { v -> vm.actualizar { it.copy(remNombre = v) } }, est, "Nombre", alineacion = TextAlign.Center)
    }
    if (c.firmante1Cargo.isNotBlank()) {
        CampoHoja(c.firmante1Cargo, { v -> vm.actualizar { it.copy(firmante1Cargo = v) } }, est, "Cargo", alineacion = TextAlign.Center)
    } else {
        CampoHoja(c.remCargo, { v -> vm.actualizar { it.copy(remCargo = v) } }, est, "Cargo (opcional)", alineacion = TextAlign.Center)
    }
    if (c.remEmpresa.isNotBlank() || !(vm.perfilActual?.personal ?: false)) {
        CampoHoja(c.remEmpresa, { v -> vm.actualizar { it.copy(remEmpresa = v) } }, est, "Empresa (opcional)", alineacion = TextAlign.Center)
    }
    if (c.datosBajoFirma) {
        val datos = listOfNotNull(
            c.remCi.takeIf { it.isNotBlank() }?.let { "C.I. $it" },
            c.remTelefono.takeIf { it.isNotBlank() }?.let { "Cel. $it" }
        ).joinToString("   ")
        if (datos.isNotEmpty()) Text(datos, style = est.copy(fontWeight = FontWeight.Normal, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FirmaHoja2(vm: CartasViewModel, c: Carta, version: Int, estilo: TextStyle, alDibujar: (Int) -> Unit) {
    val est = estilo.copy(fontSize = 12.sp)
    ImagenFirmaHoja(vm, 2, version, alDibujar)
    CampoHoja(c.firmante2Nombre, { v -> vm.actualizar { it.copy(firmante2Nombre = v) } }, est, "Nombre firma 2", alineacion = TextAlign.Center)
    CampoHoja(c.firmante2Cargo, { v -> vm.actualizar { it.copy(firmante2Cargo = v) } }, est, "Cargo firma 2", alineacion = TextAlign.Center)
}
