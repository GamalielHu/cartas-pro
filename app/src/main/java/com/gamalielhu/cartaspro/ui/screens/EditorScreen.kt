package com.gamalielhu.cartaspro.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.gamalielhu.cartaspro.data.Exportador
import com.gamalielhu.cartaspro.data.Formato
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.Fuente
import com.gamalielhu.cartaspro.model.Papel
import com.gamalielhu.cartaspro.model.Plantillas
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.ui.CartasViewModel
import com.gamalielhu.cartaspro.ui.components.DialogoFirma
import android.graphics.BitmapFactory
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val TRATAMIENTOS = listOf("Sr.", "Sra.", "Ing.", "Lic.", "Dr.", "Dra.", "Arq.", "Abog.", "Prof.")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: CartasViewModel) {
    val carta by vm.carta.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var menuCompartir by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.mensajes.collect { snack.showSnackbar(it) } }

    val guardarPdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Formato.PDF.mime)) { uri ->
        uri?.let { vm.exportar(Formato.PDF, it) }
    }
    val guardarDocx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(Formato.DOCX.mime)) { uri ->
        uri?.let { vm.exportar(Formato.DOCX, it) }
    }
    fun compartir(f: Formato) {
        scope.launch {
            val archivo = vm.generar(f) ?: return@launch
            Exportador.compartir(context, archivo, f)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(carta.tipo.titulo, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = { vm.volverAlInicio() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                        }
                    }
                )
                TabRow(selectedTabIndex = pestana) {
                    Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Datos") })
                    Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Vista previa") })
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { guardarPdf.launch(vm.nombreArchivo(Formato.PDF)) }, modifier = Modifier.weight(1f)) {
                        Text("Exportar PDF")
                    }
                    OutlinedButton(onClick = { guardarDocx.launch(vm.nombreArchivo(Formato.DOCX)) }, modifier = Modifier.weight(1f)) {
                        Text("Word")
                    }
                    Box {
                        if (vm.ocupado) {
                            CircularProgressIndicator(Modifier.size(28.dp).padding(2.dp), strokeWidth = 3.dp)
                        } else {
                            IconButton(onClick = { menuCompartir = true }) { Icon(Icons.Filled.Share, "Compartir") }
                        }
                        DropdownMenu(expanded = menuCompartir, onDismissRequest = { menuCompartir = false }) {
                            DropdownMenuItem(text = { Text("Compartir PDF") }, onClick = { menuCompartir = false; compartir(Formato.PDF) })
                            DropdownMenuItem(text = { Text("Compartir Word (.docx)") }, onClick = { menuCompartir = false; compartir(Formato.DOCX) })
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            if (pestana == 0) FormularioCarta(vm, carta) else VistaPrevia(vm)
        }
    }
}

// =========================================================================== Formulario

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioCarta(vm: CartasViewModel, carta: Carta) {
    var verFecha by remember { mutableStateOf(false) }
    var dibujarFirma by remember { mutableIntStateOf(0) }
    var firmaGaleria by remember { mutableIntStateOf(1) }
    var confirmarEstructura by remember { mutableStateOf(false) }
    val version by vm.versionFirma.collectAsState()

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.importarFirma(firmaGaleria, it) }
    }

    // Cuerpo con cursor propio para insertar cláusulas donde está el cursor.
    var cuerpo by remember { mutableStateOf(TextFieldValue(carta.cuerpo, TextRange(carta.cuerpo.length))) }
    LaunchedEffect(carta.cuerpo) {
        if (carta.cuerpo != cuerpo.text) cuerpo = TextFieldValue(carta.cuerpo, TextRange(carta.cuerpo.length))
    }
    fun insertar(texto: String) {
        val t = cuerpo.text
        val ini = cuerpo.selection.min.coerceIn(0, t.length)
        val fin = cuerpo.selection.max.coerceIn(0, t.length)
        val antes = t.substring(0, ini)
        val sep = when {
            antes.isEmpty() || antes.endsWith("\n\n") -> ""
            antes.endsWith("\n") -> "\n"
            else -> "\n\n"
        }
        val nuevo = antes + sep + texto + t.substring(fin)
        val cursor = (antes + sep + texto).length
        cuerpo = TextFieldValue(nuevo, TextRange(cursor))
        vm.actualizar { it.copy(cuerpo = nuevo) }
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Seccion("Lugar y fecha") {
            Campo("Lugar y fecha", carta.lugarFecha, { v -> vm.actualizar { it.copy(lugarFecha = v) } },
                trailing = {
                    IconButton(onClick = { verFecha = true }) { Icon(Icons.Filled.DateRange, "Elegir fecha") }
                })
            Text("Ej.: Sucre, 16 de noviembre de 2026. El calendario conserva la ciudad.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Seccion("Remitente") {
            Campo("Nombre completo", carta.remNombre, { v -> vm.actualizar { it.copy(remNombre = v) } }, palabras = true)
            Campo("Cargo / Profesión", carta.remCargo, { v -> vm.actualizar { it.copy(remCargo = v) } })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo("C.I. / NIT", carta.remCi, { v -> vm.actualizar { it.copy(remCi = v) } }, modifier = Modifier.weight(1f))
                Campo("Teléfono", carta.remTelefono, { v -> vm.actualizar { it.copy(remTelefono = v) } },
                    teclado = KeyboardType.Phone, modifier = Modifier.weight(1f))
            }
        }

        Seccion("Destinatario") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TRATAMIENTOS.forEach { t ->
                    FilterChip(
                        selected = carta.destTratamiento == t,
                        onClick = {
                            vm.actualizar {
                                val fem = t == "Sra." || t == "Dra."
                                val enc = when (it.destEncabezado) {
                                    "Señor:", "Señora:", "" -> if (fem) "Señora:" else "Señor:"
                                    else -> it.destEncabezado
                                }
                                it.copy(destTratamiento = t, destEncabezado = enc)
                            }
                        },
                        label = { Text(t) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo("Encabezado", carta.destEncabezado, { v -> vm.actualizar { it.copy(destEncabezado = v) } }, modifier = Modifier.weight(1f))
                Campo("Tratamiento", carta.destTratamiento, { v -> vm.actualizar { it.copy(destTratamiento = v) } }, modifier = Modifier.weight(1f))
            }
            Campo("Nombre completo", carta.destNombre, { v -> vm.actualizar { it.copy(destNombre = v) } }, palabras = true)
            Campo("Cargo", carta.destCargo, { v -> vm.actualizar { it.copy(destCargo = v) } })
            Campo("Institución / Empresa", carta.destInstitucion, { v -> vm.actualizar { it.copy(destInstitucion = v) } })
        }

        Seccion("Referencia") {
            Campo("REF.:", carta.referencia, { v -> vm.actualizar { it.copy(referencia = v) } }, mayusculas = true, unaLinea = false)
            Text("Sale en negrita y subrayada, alineada a la derecha.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Seccion("Cuerpo de la carta") {
            Text("Cláusulas rápidas (se insertan donde está el cursor):", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Plantillas.clausulas(carta.tipo).forEach { c ->
                    AssistChip(onClick = { insertar(c.texto) }, label = { Text(c.titulo) })
                }
            }
            OutlinedTextField(
                value = cuerpo,
                onValueChange = { v -> cuerpo = v; if (v.text != carta.cuerpo) vm.actualizar { it.copy(cuerpo = v.text) } },
                label = { Text("Texto") },
                minLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cada línea es un párrafo. **texto** = negrita.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    if (carta.cuerpo.isBlank()) vm.autocompletarEstructura() else confirmarEstructura = true
                }) { Text("Estructura formal") }
            }
            Campo("Despedida", carta.despedida, { v -> vm.actualizar { it.copy(despedida = v) } })
        }

        Seccion("Firma") {
            Opciones(PosicionFirma.entries, carta.posicionFirma, { it.titulo }) { p -> vm.actualizar { it.copy(posicionFirma = p) } }
            Text(if (carta.posicionFirma == PosicionFirma.DOBLE) "Firma 1 (izquierda)" else "Firmante",
                fontWeight = FontWeight.SemiBold)
            Campo("Nombre del firmante", carta.firmante1Nombre, { v -> vm.actualizar { it.copy(firmante1Nombre = v) } },
                ayuda = carta.remNombre.takeIf { it.isNotBlank() }?.let { "Vacío = $it" }, palabras = true)
            Campo("Cargo del firmante", carta.firmante1Cargo, { v -> vm.actualizar { it.copy(firmante1Cargo = v) } },
                ayuda = carta.remCargo.takeIf { it.isNotBlank() }?.let { "Vacío = $it" })
            BloqueImagenFirma(vm, 1, version,
                alDibujar = { dibujarFirma = 1 },
                alGaleria = { firmaGaleria = 1; galeria.launch("image/*") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = carta.datosBajoFirma, onCheckedChange = { v -> vm.actualizar { it.copy(datosBajoFirma = v) } })
                Text("Mostrar C.I. y teléfono bajo la firma")
            }
            if (carta.posicionFirma == PosicionFirma.DOBLE) {
                HorizontalDivider()
                Text("Firma 2 (derecha)", fontWeight = FontWeight.SemiBold)
                Campo("Nombre del firmante 2", carta.firmante2Nombre, { v -> vm.actualizar { it.copy(firmante2Nombre = v) } }, palabras = true)
                Campo("Cargo del firmante 2", carta.firmante2Cargo, { v -> vm.actualizar { it.copy(firmante2Cargo = v) } })
                BloqueImagenFirma(vm, 2, version,
                    alDibujar = { dibujarFirma = 2 },
                    alGaleria = { firmaGaleria = 2; galeria.launch("image/*") })
            }
        }

        Seccion("Formato de hoja") {
            Text("Papel", style = MaterialTheme.typography.labelLarge)
            Opciones(Papel.entries, carta.papel, { it.titulo }) { p -> vm.actualizar { it.copy(papel = p) } }
            Text("Tipografía", style = MaterialTheme.typography.labelLarge)
            Opciones(Fuente.entries, carta.fuente, { it.titulo }) { f -> vm.actualizar { it.copy(fuente = f) } }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (verFecha) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { verFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        vm.fijarFecha(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    verFecha = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { verFecha = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }

    if (dibujarFirma != 0) {
        val n = dibujarFirma
        DialogoFirma(
            titulo = if (n == 1) "Dibujar firma" else "Dibujar firma 2",
            alCancelar = { dibujarFirma = 0 },
            alGuardar = { trazos, grosor, color ->
                vm.guardarFirmaDibujada(n, trazos, grosor, color)
                dibujarFirma = 0
            }
        )
    }

    if (confirmarEstructura) {
        AlertDialog(
            onDismissRequest = { confirmarEstructura = false },
            title = { Text("Reemplazar el texto") },
            text = { Text("Se cambiará el cuerpo actual por la estructura formal de \"${carta.tipo.titulo}\".") },
            confirmButton = {
                TextButton(onClick = { vm.autocompletarEstructura(); confirmarEstructura = false }) { Text("Reemplazar") }
            },
            dismissButton = { TextButton(onClick = { confirmarEstructura = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun BloqueImagenFirma(
    vm: CartasViewModel, numero: Int, version: Int,
    alDibujar: () -> Unit, alGaleria: () -> Unit
) {
    val imagen = remember(numero, version) {
        vm.firmas.archivo(numero)?.let { BitmapFactory.decodeFile(it.path) }?.asImageBitmap()
    }
    Box(
        Modifier.fillMaxWidth().height(110.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imagen != null) {
            Image(imagen, "Firma $numero", Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
        } else {
            Text("Sin firma (queda el espacio para firmar a mano)", color = Color.Gray,
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = alDibujar, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Dibujar") }
        OutlinedButton(onClick = alGaleria, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Galería (PNG)") }
        if (imagen != null) TextButton(onClick = { vm.quitarFirma(numero) }) { Text("Quitar") }
    }
}

// =========================================================================== Vista previa

@Composable
private fun VistaPrevia(vm: CartasViewModel) {
    val paginas by vm.paginas.collectAsState()
    if (paginas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().background(Color(0xFFD9DCE3)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(paginas) { i, bmp ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Página ${i + 1}",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth()
                        .aspectRatio(bmp.width.toFloat() / bmp.height)
                        .shadow(6.dp)
                        .background(Color.White)
                )
                Spacer(Modifier.height(4.dp))
                Text("Página ${i + 1} de ${paginas.size}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// =========================================================================== Piezas

@Composable
private fun Seccion(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold)
            contenido()
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    alCambiar: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    teclado: KeyboardType = KeyboardType.Text,
    palabras: Boolean = false,
    mayusculas: Boolean = false,
    unaLinea: Boolean = true,
    ayuda: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        singleLine = unaLinea,
        placeholder = ayuda?.let { { Text(it) } },
        trailingIcon = trailing,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = when {
                mayusculas -> KeyboardCapitalization.Characters
                palabras -> KeyboardCapitalization.Words
                else -> KeyboardCapitalization.Sentences
            }
        ),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Opciones(opciones: List<T>, seleccion: T, etiqueta: (T) -> String, alElegir: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, o ->
            SegmentedButton(
                selected = o == seleccion,
                onClick = { alElegir(o) },
                shape = SegmentedButtonDefaults.itemShape(i, opciones.size)
            ) { Text(etiqueta(o), maxLines = 1) }
        }
    }
}
