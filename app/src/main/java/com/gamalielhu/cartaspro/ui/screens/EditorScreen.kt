package com.gamalielhu.cartaspro.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gamalielhu.cartaspro.data.Exportador
import com.gamalielhu.cartaspro.data.Formato
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.ComposicionCarta
import com.gamalielhu.cartaspro.model.Estilo
import com.gamalielhu.cartaspro.model.Fuente
import com.gamalielhu.cartaspro.model.NumeroLetras
import com.gamalielhu.cartaspro.model.Papel
import com.gamalielhu.cartaspro.model.Plantillas
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.model.TipoCampo
import com.gamalielhu.cartaspro.ui.CartasViewModel
import com.gamalielhu.cartaspro.ui.components.DialogoFirma
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val TRATAMIENTOS = listOf("Sr.", "Sra.", "Ing.", "Lic.", "Arq.", "Dr.", "Dra.", "Abog.", "Prof.")
private const val CAMPO_LUGAR = "__lugar"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: CartasViewModel) {
    val carta by vm.carta.collectAsState()
    val version by vm.versionFirma.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    var pestana by rememberSaveable { mutableIntStateOf(0) }
    var menuCompartir by remember { mutableStateOf(false) }
    var dibujarFirma by remember { mutableIntStateOf(0) }

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
                    title = {
                        Column {
                            Text(carta.tipo.titulo, maxLines = 1)
                            vm.perfilActual?.let { Text(it.nombre, style = MaterialTheme.typography.labelSmall) }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { vm.volverAlInicio() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    }
                )
                TabRow(selectedTabIndex = pestana) {
                    Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Hoja") })
                    Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Datos") })
                    Tab(selected = pestana == 2, onClick = { pestana = 2 }, text = { Text("PDF") })
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
                        Text("Guardar PDF")
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
            when (pestana) {
                0 -> HojaEditable(vm, carta, version) { dibujarFirma = it }
                1 -> FormularioCarta(vm, carta, version) { dibujarFirma = it }
                else -> VistaPrevia(vm)
            }
        }
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
}

// =========================================================================== Formulario (Datos)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioCarta(vm: CartasViewModel, carta: Carta, version: Int, alDibujarFirma: (Int) -> Unit) {
    val perfiles by vm.perfiles.collectAsState()
    val recientes by vm.recientes.collectAsState()
    var fechaPara by remember { mutableStateOf<String?>(null) }
    var firmaGaleria by remember { mutableIntStateOf(1) }
    var nuevoPerfil by remember { mutableStateOf(false) }
    var borrarPerfil by remember { mutableStateOf(false) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.importarFirma(firmaGaleria, it) }
    }
    val galeriaLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { vm.importarLogo(it) }
    }
    val abrirWord = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.importarFormatoWord(it) }
    }

    Column(
        Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Seccion("Empresa / perfil") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                perfiles.forEach { p ->
                    FilterChip(selected = p.id == carta.perfilId, onClick = { vm.elegirPerfil(p.id) }, label = { Text(p.nombre) })
                }
                FilterChip(selected = false, onClick = { nuevoPerfil = true }, label = { Text("+ Nuevo") })
            }
            Text("Cada perfil recuerda sus datos y su formato (membrete, A/DE o carta, Cc/arch., letra).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (perfiles.size > 1 && carta.perfilId.isNotBlank()) {
                TextButton(onClick = { borrarPerfil = true }) { Text("Borrar este perfil") }
            }
        }

        val campos = Plantillas.campos(carta.tipo)
        if (campos.isNotEmpty()) {
            Seccion("Datos del trámite") {
                campos.forEach { cp ->
                    val valor = carta.campo(cp.clave)
                    when (cp.tipo) {
                        TipoCampo.FECHA -> Campo(cp.etiqueta, valor, { vm.actualizarCampo(cp.clave, it) }, ayuda = "10 de septiembre de 2026",
                            trailing = { IconButton(onClick = { fechaPara = cp.clave }) { Icon(Icons.Filled.DateRange, "Fecha") } })
                        TipoCampo.MONTO -> {
                            Campo(cp.etiqueta, valor, { vm.actualizarCampo(cp.clave, it) }, teclado = KeyboardType.Decimal, ayuda = cp.ayuda)
                            NumeroLetras.montoCompleto(valor)?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        TipoCampo.MULTILINEA -> Campo(cp.etiqueta, valor, { vm.actualizarCampo(cp.clave, it) }, unaLinea = false, ayuda = cp.ayuda)
                        TipoCampo.TEXTO -> Campo(cp.etiqueta, valor, { vm.actualizarCampo(cp.clave, it) }, ayuda = cp.ayuda,
                            mayusculas = cp.clave in setOf("obra", "beneficiario", "tipoPoliza", "cargoProf"))
                    }
                }
                if (!carta.cuerpoAuto) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Editaste el texto a mano: estos datos ya no lo cambian.", style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f))
                        TextButton(onClick = { vm.regenerarCuerpo() }) { Text("Rearmar") }
                    }
                }
            }
        }

        Seccion("Lugar y fecha") {
            Campo("Lugar y fecha", carta.lugarFecha, { v -> vm.actualizar { it.copy(lugarFecha = v) } },
                trailing = { IconButton(onClick = { fechaPara = CAMPO_LUGAR }) { Icon(Icons.Filled.DateRange, "Elegir fecha") } })
        }

        Seccion("Remitente") {
            Campo("Nombre completo", carta.remNombre, { v -> vm.actualizar { it.copy(remNombre = v) } }, palabras = true)
            Campo("Cargo", carta.remCargo, { v -> vm.actualizar { it.copy(remCargo = v) } }, mayusculas = true)
            Campo("Empresa (razón social)", carta.remEmpresa, { v -> vm.actualizar { it.copy(remEmpresa = v) } }, mayusculas = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo("C.I. / NIT", carta.remCi, { v -> vm.actualizar { it.copy(remCi = v) } }, modifier = Modifier.weight(1f))
                Campo("Teléfono", carta.remTelefono, { v -> vm.actualizar { it.copy(remTelefono = v) } },
                    teclado = KeyboardType.Phone, modifier = Modifier.weight(1f))
            }
        }

        Seccion("Encabezado y pie de página") {
            Text("Copia el logo, el membrete, la línea y el Cc/arch. de una carta tuya en Word de esta empresa.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = {
                abrirWord.launch(arrayOf(Formato.DOCX.mime, "application/msword", "application/octet-stream"))
            }, modifier = Modifier.fillMaxWidth()) { Text("Copiar formato de un Word (.docx)") }
            val logo = remember(carta.perfilId, version) {
                vm.logos.archivo(carta.perfilId)?.let { BitmapFactory.decodeFile(it.path) }?.asImageBitmap()
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(width = 120.dp, height = 56.dp).background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (logo != null) Image(logo, "Logo", Modifier.fillMaxSize().padding(4.dp), contentScale = ContentScale.Fit)
                    else Text("Sin logo", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    TextButton(onClick = { galeriaLogo.launch("image/*") }) { Text("Logo desde galería") }
                    if (logo != null) TextButton(onClick = { vm.quitarLogo() }) { Text("Quitar logo") }
                }
            }
            if (logo != null) {
                Text("Tamaño del logo: ${carta.logoAnchoPt.toInt()} pt", style = MaterialTheme.typography.labelLarge)
                Slider(value = carta.logoAnchoPt, onValueChange = { v -> vm.actualizar { it.copy(logoAnchoPt = v) } }, valueRange = 50f..300f)
            }
            Campo("Membrete (texto, una línea por renglón)", carta.membrete, { v -> vm.actualizar { it.copy(membrete = v) } },
                unaLinea = false, mayusculas = true)
            Casilla("Mostrar encabezado", carta.mostrarMembrete) { v -> vm.actualizar { it.copy(mostrarMembrete = v) } }
            Casilla("Línea bajo el encabezado", carta.lineaEncabezado) { v -> vm.actualizar { it.copy(lineaEncabezado = v) } }
            Casilla("\"Cc/arch.\" al pie de página", carta.copiaArchivo) { v -> vm.actualizar { it.copy(copiaArchivo = v) } }
        }

        if (carta.estilo != Estilo.CERTIFICADO) {
            Seccion("Destinatario") {
                if (recientes.isNotEmpty()) {
                    Text("Recientes", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        recientes.forEach { d ->
                            InputChip(
                                selected = d == carta.destinatario,
                                onClick = { vm.usarDestinatario(d) },
                                label = { Text(d.etiqueta, maxLines = 1) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, "Olvidar", Modifier.size(16.dp).clickable { vm.olvidarDestinatario(d) })
                                }
                            )
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TRATAMIENTOS.forEach { t ->
                        FilterChip(selected = carta.destTratamiento == t,
                            onClick = { vm.actualizar { it.copy(destTratamiento = if (it.destTratamiento == t) "" else t) } },
                            label = { Text(t) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Campo("Encabezado", carta.destEncabezado, { v -> vm.actualizar { it.copy(destEncabezado = v) } }, modifier = Modifier.weight(1f))
                    Campo("Tratamiento", carta.destTratamiento, { v -> vm.actualizar { it.copy(destTratamiento = v) } }, modifier = Modifier.weight(1f))
                }
                Campo("Nombre completo", carta.destNombre, { v -> vm.actualizar { it.copy(destNombre = v) } }, palabras = true)
                Campo("Cargo", carta.destCargo, { v -> vm.actualizar { it.copy(destCargo = v) } }, mayusculas = true)
                Campo("Institución / Empresa", carta.destInstitucion, { v -> vm.actualizar { it.copy(destInstitucion = v) } }, mayusculas = true)
                Text("Se guarda en Recientes al exportar o compartir.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Seccion(if (carta.estilo == Estilo.CERTIFICADO) "Título" else "Referencia") {
            if (carta.estilo == Estilo.CERTIFICADO) {
                Campo("Título del documento", carta.titulo, { v -> vm.actualizar { it.copy(titulo = v) } }, mayusculas = true)
            } else {
                Campo(if (carta.estilo == Estilo.MEMORANDO) "OBJETO" else "REF. (asunto)", carta.asunto,
                    { v -> vm.actualizar { it.copy(asunto = v) } }, mayusculas = true, unaLinea = false)
                Campo(if (carta.estilo == Estilo.MEMORANDO) "REF. (obra / proyecto)" else "Obra / proyecto (2.ª línea, opcional)",
                    carta.proyecto, { v -> vm.actualizar { it.copy(proyecto = v) } }, mayusculas = true, unaLinea = false)
            }
        }

        Seccion("Firma") {
            Opciones(PosicionFirma.entries, carta.posicionFirma, { it.titulo }) { p -> vm.actualizar { it.copy(posicionFirma = p) } }
            Campo("Firmante (vacío = remitente)", carta.firmante1Nombre, { v -> vm.actualizar { it.copy(firmante1Nombre = v) } }, palabras = true)
            Campo("Cargo del firmante (vacío = remitente)", carta.firmante1Cargo, { v -> vm.actualizar { it.copy(firmante1Cargo = v) } }, mayusculas = true)
            BloqueImagenFirma(vm, 1, version, alDibujar = { alDibujarFirma(1) }, alGaleria = { firmaGaleria = 1; galeria.launch("image/*") })
            if (carta.posicionFirma == PosicionFirma.DOBLE) {
                HorizontalDivider()
                Campo("Nombre firma 2", carta.firmante2Nombre, { v -> vm.actualizar { it.copy(firmante2Nombre = v) } }, palabras = true)
                Campo("Cargo firma 2", carta.firmante2Cargo, { v -> vm.actualizar { it.copy(firmante2Cargo = v) } }, mayusculas = true)
                BloqueImagenFirma(vm, 2, version, alDibujar = { alDibujarFirma(2) }, alGaleria = { firmaGaleria = 2; galeria.launch("image/*") })
            }
            Casilla("Línea sobre el nombre", carta.lineaFirma) { v -> vm.actualizar { it.copy(lineaFirma = v) } }
            Casilla("C.I. y celular bajo la firma", carta.datosBajoFirma) { v -> vm.actualizar { it.copy(datosBajoFirma = v) } }
        }

        Seccion("Formato") {
            Text("Diseño", style = MaterialTheme.typography.labelLarge)
            if (carta.tipo.estiloFijo != null) {
                Text("Este documento siempre usa el formato ${carta.tipo.estiloFijo.titulo}.", style = MaterialTheme.typography.bodySmall)
            } else {
                Opciones(listOf(Estilo.CLASICA, Estilo.MEMORANDO), carta.estilo, { it.titulo }) { e ->
                    vm.actualizar { it.copy(estilo = e) }
                }
            }
            Text("Papel", style = MaterialTheme.typography.labelLarge)
            Opciones(Papel.entries, carta.papel, { it.titulo }) { p -> vm.actualizar { it.copy(papel = p) } }
            Text("Letra", style = MaterialTheme.typography.labelLarge)
            Opciones(Fuente.entries, carta.fuente, { it.titulo }) { f -> vm.actualizar { it.copy(fuente = f) } }
        }
        Spacer(Modifier.height(24.dp))
    }

    fechaPara?.let { destino ->
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { fechaPara = null },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        val f = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (destino == CAMPO_LUGAR) vm.fijarFecha(f) else vm.actualizarCampo(destino, ComposicionCarta.fecha(f))
                    }
                    fechaPara = null
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { fechaPara = null }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }

    if (nuevoPerfil) {
        var nombre by remember { mutableStateOf("") }
        var personal by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { nuevoPerfil = false },
            title = { Text("Nuevo perfil") },
            text = {
                Column {
                    OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre corto (ej. empresa)") }, singleLine = true)
                    Casilla("Es personal (sin empresa)", personal) { personal = it }
                    Text("Se crea con los datos de remitente actuales; luego los cambias en Datos.",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { vm.nuevoPerfil(nombre, personal); nuevoPerfil = false }) { Text("Crear") } },
            dismissButton = { TextButton(onClick = { nuevoPerfil = false }) { Text("Cancelar") } }
        )
    }

    if (borrarPerfil) {
        AlertDialog(
            onDismissRequest = { borrarPerfil = false },
            title = { Text("Borrar perfil") },
            text = { Text("Se borra \"${vm.perfilActual?.nombre ?: ""}\" y sus datos guardados.") },
            confirmButton = { TextButton(onClick = { vm.borrarPerfil(carta.perfilId); borrarPerfil = false }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { borrarPerfil = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun BloqueImagenFirma(vm: CartasViewModel, numero: Int, version: Int, alDibujar: () -> Unit, alGaleria: () -> Unit) {
    val imagen = remember(numero, version) {
        vm.firmas.archivo(numero)?.let { BitmapFactory.decodeFile(it.path) }?.asImageBitmap()
    }
    Box(
        Modifier.fillMaxWidth().height(100.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (imagen != null) Image(imagen, "Firma $numero", Modifier.fillMaxSize().padding(10.dp), contentScale = ContentScale.Fit)
        else Text("Sin firma: queda el espacio para firmar y sellar a mano", color = Color.Gray,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = alDibujar, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Dibujar") }
        OutlinedButton(onClick = alGaleria, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Galería (PNG)") }
        if (imagen != null) TextButton(onClick = { vm.quitarFirma(numero) }) { Text("Quitar") }
    }
}

// =========================================================================== Vista previa (PDF)

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
                    modifier = Modifier.fillMaxWidth().aspectRatio(bmp.width.toFloat() / bmp.height).shadow(6.dp).background(Color.White)
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
            Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            contenido()
        }
    }
}

@Composable
private fun Casilla(texto: String, valor: Boolean, alCambiar: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { alCambiar(!valor) }) {
        Checkbox(checked = valor, onCheckedChange = alCambiar)
        Text(texto)
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
        placeholder = ayuda?.takeIf { it.isNotBlank() }?.let { { Text(it) } },
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
