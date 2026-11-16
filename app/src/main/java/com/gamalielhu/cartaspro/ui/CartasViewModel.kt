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

enum class Pantalla { INICIO, EDITOR }

@OptIn(FlowPreview::class)
class CartasViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = PreferenciasCarta(app)
    val firmas = FirmaStore(app)

    private val _carta = MutableStateFlow(Carta())
    val carta: StateFlow<Carta> = _carta.asStateFlow()

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
            var c = prefs.cargar()
            hayBorrador = c.cuerpo.isNotBlank()
            if (c.lugarFecha.isBlank()) c = c.copy(lugarFecha = ComposicionCarta.lugarFecha("Sucre", LocalDate.now()))
            _carta.value = c
            cargado = true
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

    fun actualizar(cambio: (Carta) -> Carta) = _carta.update(cambio)

    fun elegirPlantilla(tipo: TipoCarta) {
        _carta.update {
            it.copy(
                tipo = tipo,
                referencia = Plantillas.referencia(tipo),
                cuerpo = Plantillas.cuerpo(tipo),
                despedida = "Atentamente,",
                // Fecha de hoy conservando la ciudad elegida antes.
                lugarFecha = ComposicionCarta.lugarFecha(ComposicionCarta.ciudadDe(it.lugarFecha), LocalDate.now()),
                destNombre = "", destCargo = "", destInstitucion = ""
            )
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

    fun autocompletarEstructura() = _carta.update { it.copy(cuerpo = Plantillas.cuerpo(it.tipo)) }

    fun fijarFecha(fecha: LocalDate) = _carta.update {
        it.copy(lugarFecha = ComposicionCarta.lugarFecha(ComposicionCarta.ciudadDe(it.lugarFecha), fecha))
    }

    // ---- firmas
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

    // ---- exportación
    fun nombreArchivo(formato: Formato) = Exportador.nombreArchivo(_carta.value, formato)

    suspend fun generar(formato: Formato): File? {
        ocupado = true
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
