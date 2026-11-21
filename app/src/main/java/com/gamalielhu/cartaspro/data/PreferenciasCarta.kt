package com.gamalielhu.cartaspro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.Destinatario
import com.gamalielhu.cartaspro.model.Estilo
import com.gamalielhu.cartaspro.model.Fuente
import com.gamalielhu.cartaspro.model.Papel
import com.gamalielhu.cartaspro.model.Perfil
import com.gamalielhu.cartaspro.model.Plantillas
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.model.TipoCarta
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.almacen: DataStore<Preferences> by preferencesDataStore(name = "carta")

private inline fun <reified T : Enum<T>> enumDe(s: String?, defecto: T): T =
    s?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: defecto

/** Autoguardado de la carta en curso, perfiles (empresas/personal) y destinatarios recientes. */
class PreferenciasCarta(private val context: Context) {

    private object K {
        val carta = stringPreferencesKey("carta_v2")
        val perfiles = stringPreferencesKey("perfiles")
        val recientes = stringPreferencesKey("destinatarios")
    }

    // ------------------------------------------------------------ carta

    suspend fun cargar(): Carta? {
        val json = context.almacen.data.first()[K.carta] ?: return null
        return runCatching { cartaDe(JSONObject(json)) }.getOrNull()
    }

    suspend fun guardar(c: Carta) {
        context.almacen.edit { it[K.carta] = cartaAJson(c).toString() }
    }

    private fun cartaAJson(c: Carta) = JSONObject().apply {
        put("tipo", c.tipo.name); put("estilo", c.estilo.name); put("perfilId", c.perfilId)
        put("lugarFecha", c.lugarFecha)
        put("remNombre", c.remNombre); put("remCargo", c.remCargo); put("remEmpresa", c.remEmpresa)
        put("remCi", c.remCi); put("remTelefono", c.remTelefono)
        put("membrete", c.membrete); put("mostrarMembrete", c.mostrarMembrete)
        put("destEncabezado", c.destEncabezado); put("destTratamiento", c.destTratamiento)
        put("destNombre", c.destNombre); put("destCargo", c.destCargo); put("destInstitucion", c.destInstitucion)
        put("asunto", c.asunto); put("proyecto", c.proyecto); put("titulo", c.titulo)
        put("cuerpo", c.cuerpo); put("cuerpoAuto", c.cuerpoAuto); put("despedida", c.despedida)
        put("campos", JSONObject(c.campos as Map<*, *>))
        put("posicionFirma", c.posicionFirma.name)
        put("f1Nombre", c.firmante1Nombre); put("f1Cargo", c.firmante1Cargo)
        put("f2Nombre", c.firmante2Nombre); put("f2Cargo", c.firmante2Cargo)
        put("datosBajoFirma", c.datosBajoFirma); put("lineaFirma", c.lineaFirma); put("copiaArchivo", c.copiaArchivo)
        put("papel", c.papel.name); put("fuente", c.fuente.name)
        put("logoAnchoPt", c.logoAnchoPt.toDouble()); put("lineaEncabezado", c.lineaEncabezado)
    }

    private fun cartaDe(o: JSONObject): Carta {
        val d = Carta()
        val campos = o.optJSONObject("campos")?.let { cj -> cj.keys().asSequence().associateWith { cj.optString(it) } } ?: emptyMap()
        return Carta(
            tipo = enumDe(o.optString("tipo"), d.tipo),
            estilo = enumDe(o.optString("estilo"), d.estilo),
            perfilId = o.optString("perfilId"),
            lugarFecha = o.optString("lugarFecha"),
            remNombre = o.optString("remNombre"), remCargo = o.optString("remCargo"),
            remEmpresa = o.optString("remEmpresa"), remCi = o.optString("remCi"), remTelefono = o.optString("remTelefono"),
            membrete = o.optString("membrete"), mostrarMembrete = o.optBoolean("mostrarMembrete", true),
            logoAnchoPt = o.optDouble("logoAnchoPt", 117.0).toFloat(), lineaEncabezado = o.optBoolean("lineaEncabezado"),
            destEncabezado = o.optString("destEncabezado", d.destEncabezado),
            destTratamiento = o.optString("destTratamiento"), destNombre = o.optString("destNombre"),
            destCargo = o.optString("destCargo"), destInstitucion = o.optString("destInstitucion"),
            asunto = o.optString("asunto"), proyecto = o.optString("proyecto"), titulo = o.optString("titulo"),
            cuerpo = o.optString("cuerpo"), cuerpoAuto = o.optBoolean("cuerpoAuto"), despedida = o.optString("despedida"),
            campos = campos,
            posicionFirma = enumDe(o.optString("posicionFirma"), PosicionFirma.CENTRADA),
            firmante1Nombre = o.optString("f1Nombre"), firmante1Cargo = o.optString("f1Cargo"),
            firmante2Nombre = o.optString("f2Nombre"), firmante2Cargo = o.optString("f2Cargo"),
            datosBajoFirma = o.optBoolean("datosBajoFirma"), lineaFirma = o.optBoolean("lineaFirma"),
            copiaArchivo = o.optBoolean("copiaArchivo"),
            papel = enumDe(o.optString("papel"), Papel.CARTA), fuente = enumDe(o.optString("fuente"), Fuente.SERIF)
        )
    }

    // ------------------------------------------------------------ perfiles

    suspend fun perfiles(): List<Perfil> {
        val json = context.almacen.data.first()[K.perfiles] ?: return Plantillas.perfilesIniciales
        return runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Perfil(
                    id = o.getString("id"), nombre = o.optString("nombre"), personal = o.optBoolean("personal"),
                    representante = o.optString("representante"), cargo = o.optString("cargo"),
                    empresa = o.optString("empresa"), ci = o.optString("ci"), telefono = o.optString("telefono"),
                    ciudad = o.optString("ciudad", "Potosí"), membrete = o.optString("membrete"),
                    estilo = enumDe(o.optString("estilo"), Estilo.CLASICA),
                    copiaArchivo = o.optBoolean("copiaArchivo"), datosBajoFirma = o.optBoolean("datosBajoFirma"),
                    fuente = enumDe(o.optString("fuente"), Fuente.SERIF), papel = enumDe(o.optString("papel"), Papel.CARTA),
                    logoAnchoPt = o.optDouble("logoAnchoPt", 117.0).toFloat(),
                    lineaEncabezado = o.optBoolean("lineaEncabezado", p0(o.getString("id")))
                )
            }.ifEmpty { Plantillas.perfilesIniciales }
        }.getOrDefault(Plantillas.perfilesIniciales)
    }

    suspend fun guardarPerfiles(lista: List<Perfil>) {
        val a = JSONArray()
        lista.forEach { p ->
            a.put(JSONObject().apply {
                put("id", p.id); put("nombre", p.nombre); put("personal", p.personal)
                put("representante", p.representante); put("cargo", p.cargo); put("empresa", p.empresa)
                put("ci", p.ci); put("telefono", p.telefono); put("ciudad", p.ciudad); put("membrete", p.membrete)
                put("estilo", p.estilo.name); put("copiaArchivo", p.copiaArchivo); put("datosBajoFirma", p.datosBajoFirma)
                put("fuente", p.fuente.name); put("papel", p.papel.name)
                put("logoAnchoPt", p.logoAnchoPt.toDouble()); put("lineaEncabezado", p.lineaEncabezado)
            })
        }
        context.almacen.edit { it[K.perfiles] = a.toString() }
    }

    /** Valor de fábrica de la línea de encabezado para perfiles guardados por la v1.2. */
    private fun p0(id: String) = Plantillas.perfilesIniciales.firstOrNull { it.id == id }?.lineaEncabezado ?: false

    // ------------------------------------------------------------ destinatarios recientes

    suspend fun recientes(): List<Destinatario> {
        val json = context.almacen.data.first()[K.recientes] ?: return emptyList()
        return runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Destinatario(
                    o.optString("encabezado", "Señores:"), o.optString("tratamiento"), o.optString("nombre"),
                    o.optString("cargo"), o.optString("institucion")
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun guardarRecientes(lista: List<Destinatario>) {
        val a = JSONArray()
        lista.forEach { d ->
            a.put(JSONObject().apply {
                put("encabezado", d.encabezado); put("tratamiento", d.tratamiento); put("nombre", d.nombre)
                put("cargo", d.cargo); put("institucion", d.institucion)
            })
        }
        context.almacen.edit { it[K.recientes] = a.toString() }
    }
}
