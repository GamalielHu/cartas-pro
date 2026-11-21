package com.gamalielhu.cartaspro.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamalielhu.cartaspro.data.Exportador
import com.gamalielhu.cartaspro.data.FirmaStore
import com.gamalielhu.cartaspro.data.Formato
import com.gamalielhu.cartaspro.data.PreferenciasCarta
import com.gamalielhu.cartaspro.data.Trazo
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.ComposicionCarta
import com.gamalielhu.cartaspro.model.Destinatario
import com.gamalielhu.cartaspro.model.Estilo
import com.gamalielhu.cartaspro.model.Perfil
import com.gamalielhu.cartaspro.model.Plantillas
import com.gamalielhu.cartaspro.model.TipoCarta
import com.gamalielhu.cartaspro.pdf.RenderizadorCarta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.UUID

enum class Pantalla { INICIO, EDITOR }

@OptIn(FlowPreview::class)
class CartasViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferenciasCarta(app)
    val firmas = FirmaStore(app)

    private val _carta = MutableStateFlow(Carta())
    val carta: StateFlow<Carta> = _carta.asStateFlow()

    private val _perfiles = MutableStateFlow<List<Perfil>>(emptyList())
    val perfiles: StateFlow<List<Perfil>> = _perfiles.asStateFlow()

    private val _recientes = MutableStateFlow<List<Destinatario>>(emptyList())
    val recientes: StateFlow<List<Destinatario>> = _recientes.asStateFlow()

    private val _versionFirma = MutableStateFlow(0)
    val versionFirma: StateFlow<Int> = _versionFirma.asStateFlow()

    private val _paginas = MutableStateFlow<List<Bitmap>>(emptyList())
    val paginas: StateFlow<List<Bitmap>> = _paginas.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes = _mensajes.asSharedFlow()

    var pantalla by mutableStateOf(Pantalla.INICIO)
        private set
    var cargado by mutableStateOf(false)
        private set
    var hayBorrador by mutableStateOf(false)
        private set
    var ocupado by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            _perfiles.value = prefs.perfiles()
            _recientes.value = prefs.recientes()
            val guardada = prefs.cargar()
            hayBorrador = guardada != null && guardada.cuerpo.isNotBlank()
            _carta.value = guardada ?: aplicarPerfil(Carta(), _perfiles.value.first())
            cargado = true
            launch { _perfiles.drop(1).debounce(500).collect { prefs.guardarPerfiles(it) } }
            _carta.drop(1).debounce(600).collect { prefs.guardar(it) }
        }
        viewModelScope.launch {
            combine(_carta, _versionFirma) { c, _ -> c }
                .debounce(250)
                .collectLatest { c ->
                    _paginas.value = withContext(Dispatchers.Default) {
                        runCatching {
                            val r = RenderizadorCarta(c, firmas)
                            (0 until r.totalPaginas).map { r.paginaBitmap(it, 2f) }
                        }.getOrDefault(emptyList())
                    }
                }
        }
    }

    val perfilActual: Perfil? get() = _perfiles.value.firstOrNull { it.id == _carta.value.perfilId }

    // ------------------------------------------------------------ edición

    private fun conCuerpo(c: Carta) = if (c.cuerpoAuto) c.copy(cuerpo = Plantillas.cuerpo(c)) else c

    fun actualizar(cambio: (Carta) -> Carta) {
        _carta.update { conCuerpo(cambio(it)) }
        sincronizarPerfil()
    }

    fun actualizarCampo(clave: String, valor: String) = actualizar {
        val c = it.copy(campos = it.campos + (clave to valor))
        if (clave == "obra") c.copy(proyecto = valor.uppercase()) else c
    }

    /** El usuario escribió en el cuerpo: deja de rearmarse solo. */
    fun editarCuerpo(texto: String) = _carta.update { it.copy(cuerpo = texto, cuerpoAuto = false) }

    fun regenerarCuerpo() = _carta.update { conCuerpo(it.copy(cuerpoAuto = true)) }

    fun fijarFecha(fecha: LocalDate) = actualizar {
        it.copy(lugarFecha = ComposicionCarta.lugarFecha(ComposicionCarta.ciudadDe(it.lugarFecha), fecha))
    }

    fun usarDestinatario(d: Destinatario) = actualizar {
        it.copy(
            destEncabezado = d.encabezado, destTratamiento = d.tratamiento, destNombre = d.nombre,
            destCargo = d.cargo, destInstitucion = d.institucion
        )
    }

    fun olvidarDestinatario(d: Destinatario) {
        _recientes.update { l -> l.filterNot { it == d } }
        viewModelScope.launch { prefs.guardarRecientes(_recientes.value) }
    }

    private fun recordarDestinatario() {
        val d = _carta.value.destinatario
        if (d.nombre.isBlank() && d.institucion.isBlank()) return
        _recientes.update { l -> (listOf(d) + l.filterNot { it.etiqueta == d.etiqueta }).take(12) }
        viewModelScope.launch { prefs.guardarRecientes(_recientes.value) }
    }

    // ------------------------------------------------------------ perfiles (empresas / personal)

    private fun aplicarPerfil(c: Carta, p: Perfil): Carta {
        val estilo = c.tipo.estiloFijo ?: p.estilo
        return c.copy(
            perfilId = p.id,
            remNombre = p.representante, remCargo = p.cargo, remEmpresa = p.empresa,
            remCi = p.ci, remTelefono = p.telefono, membrete = p.membrete,
            estilo = estilo,
            mostrarMembrete = p.membrete.isNotBlank() && estilo == Estilo.MEMORANDO,
            copiaArchivo = p.copiaArchivo, datosBajoFirma = p.datosBajoFirma,
            fuente = p.fuente, papel = p.papel,
            firmante1Nombre = "", firmante1Cargo = "",
            lugarFecha = ComposicionCarta.lugarFecha(p.ciudad, LocalDate.now())
        )
    }

    fun elegirPerfil(id: String) {
        val p = _perfiles.value.firstOrNull { it.id == id } ?: return
        _carta.update { conCuerpo(aplicarPerfil(it, p)) }
    }

    /** Guarda en el perfil activo lo que se cambia del remitente y del formato. */
    private fun sincronizarPerfil() {
        val c = _carta.value
        if (c.perfilId.isBlank()) return
        _perfiles.update { lista ->
            lista.map { p ->
                if (p.id != c.perfilId) p else p.copy(
                    representante = c.remNombre, cargo = c.remCargo, empresa = c.remEmpresa,
                    ci = c.remCi, telefono = c.remTelefono, membrete = c.membrete,
                    ciudad = ComposicionCarta.ciudadDe(c.lugarFecha).ifBlank { p.ciudad },
                    estilo = if (c.tipo.estiloFijo == null) c.estilo else p.estilo,
                    copiaArchivo = c.copiaArchivo, datosBajoFirma = c.datosBajoFirma,
                    fuente = c.fuente, papel = c.papel
                )
            }
        }
    }

    fun nuevoPerfil(nombre: String, personal: Boolean) {
        val c = _carta.value
        val p = Perfil(
            id = UUID.randomUUID().toString(), nombre = nombre.trim().ifBlank { "Nueva" }, personal = personal,
            representante = c.remNombre, cargo = if (personal) "" else c.remCargo, empresa = if (personal) "" else c.remEmpresa,
            ci = c.remCi, telefono = c.remTelefono, ciudad = ComposicionCarta.ciudadDe(c.lugarFecha),
            estilo = Estilo.CLASICA, datosBajoFirma = personal
        )
        _perfiles.update { it + p }
        elegirPerfil(p.id)
    }

    fun borrarPerfil(id: String) {
        if (_perfiles.value.size <= 1) return
        _perfiles.update { l -> l.filterNot { it.id == id } }
        if (_carta.value.perfilId == id) elegirPerfil(_perfiles.value.first().id)
    }

    // ------------------------------------------------------------ navegación

    fun elegirPlantilla(tipo: TipoCarta) {
        val perfil = perfilActual ?: _perfiles.value.first()
        _carta.update { anterior ->
            val base = aplicarPerfil(anterior.copy(tipo = tipo), perfil)
            val c = base.copy(
                asunto = Plantillas.asunto(tipo),
                titulo = Plantillas.titulo(tipo),
                proyecto = if (Plantillas.campos(tipo).any { it.clave == "obra" }) anterior.campo("obra").uppercase() else "",
                destEncabezado = if (tipo == TipoCarta.RECOMENDACION) Plantillas.encabezado(tipo)
                    else anterior.destEncabezado.takeUnless { it == "A quien corresponda:" } ?: "Señores:",
                despedida = "",
                cuerpoAuto = Plantillas.cuerpoEsAutomatico(tipo),
                cuerpo = ""
            )
            if (c.cuerpoAuto) conCuerpo(c) else c.copy(cuerpo = Plantillas.cuerpo(c))
        }
        pantalla = Pantalla.EDITOR
    }

    fun continuarBorrador() {
        pantalla = Pantalla.EDITOR
    }

    fun volverAlInicio() {
        hayBorrador = _carta.value.cuerpo.isNotBlank()
        pantalla = Pantalla.INICIO
    }

    // ------------------------------------------------------------ firmas

    fun guardarFirmaDibujada(numero: Int, trazos: List<Trazo>, grosorPx: Float, color: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            firmas.guardarTrazos(numero, trazos, grosorPx, color)
            _versionFirma.update { it + 1 }
        }
    }

    fun importarFirma(numero: Int, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { firmas.importar(numero, uri) }.getOrDefault(false)
            if (ok) _versionFirma.update { it + 1 } else _mensajes.tryEmit("No se pudo leer la imagen")
        }
    }

    fun quitarFirma(numero: Int) {
        firmas.borrar(numero)
        _versionFirma.update { it + 1 }
    }

    // ------------------------------------------------------------ exportación

    fun nombreArchivo(formato: Formato) = Exportador.nombreArchivo(_carta.value, formato)

    suspend fun generar(formato: Formato): File? {
        ocupado = true
        recordarDestinatario()
        return try {
            withContext(Dispatchers.IO) { Exportador.generar(getApplication(), _carta.value, formato) }
        } catch (e: Exception) {
            _mensajes.tryEmit("Error al generar: ${e.message}")
            null
        } finally {
            ocupado = false
        }
    }

    fun exportar(formato: Formato, destino: Uri) {
        viewModelScope.launch {
            val f = generar(formato) ?: return@launch
            val ok = withContext(Dispatchers.IO) {
                runCatching { Exportador.copiarA(getApplication(), f, destino) }.isSuccess
            }
            _mensajes.tryEmit(if (ok) "${formato.titulo} guardado" else "No se pudo guardar el archivo")
        }
    }
}
