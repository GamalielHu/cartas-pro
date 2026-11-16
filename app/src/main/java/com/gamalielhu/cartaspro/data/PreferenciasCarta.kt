package com.gamalielhu.cartaspro.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gamalielhu.cartaspro.model.Carta
import com.gamalielhu.cartaspro.model.Fuente
import com.gamalielhu.cartaspro.model.Papel
import com.gamalielhu.cartaspro.model.PosicionFirma
import com.gamalielhu.cartaspro.model.TipoCarta
import kotlinx.coroutines.flow.first

private val Context.almacen: DataStore<Preferences> by preferencesDataStore(name = "carta")

/** Autoguardado de la carta en curso (datos frecuentes + borrador). */
class PreferenciasCarta(private val context: Context) {

    private object K {
        val tipo = stringPreferencesKey("tipo")
        val lugarFecha = stringPreferencesKey("lugar_fecha")
        val remNombre = stringPreferencesKey("rem_nombre")
        val remCargo = stringPreferencesKey("rem_cargo")
        val remCi = stringPreferencesKey("rem_ci")
        val remTelefono = stringPreferencesKey("rem_telefono")
        val destEncabezado = stringPreferencesKey("dest_encabezado")
        val destTratamiento = stringPreferencesKey("dest_tratamiento")
        val destNombre = stringPreferencesKey("dest_nombre")
        val destCargo = stringPreferencesKey("dest_cargo")
        val destInstitucion = stringPreferencesKey("dest_institucion")
        val referencia = stringPreferencesKey("referencia")
        val cuerpo = stringPreferencesKey("cuerpo")
        val despedida = stringPreferencesKey("despedida")
        val posicion = stringPreferencesKey("posicion_firma")
        val f1Nombre = stringPreferencesKey("f1_nombre")
        val f1Cargo = stringPreferencesKey("f1_cargo")
        val f2Nombre = stringPreferencesKey("f2_nombre")
        val f2Cargo = stringPreferencesKey("f2_cargo")
        val datosBajoFirma = booleanPreferencesKey("datos_bajo_firma")
        val papel = stringPreferencesKey("papel")
        val fuente = stringPreferencesKey("fuente")
    }

    suspend fun cargar(): Carta {
        val p = context.almacen.data.first()
        val d = Carta()
        return Carta(
            tipo = p[K.tipo]?.let { runCatching { TipoCarta.valueOf(it) }.getOrNull() } ?: d.tipo,
            lugarFecha = p[K.lugarFecha] ?: d.lugarFecha,
            remNombre = p[K.remNombre] ?: d.remNombre,
            remCargo = p[K.remCargo] ?: d.remCargo,
            remCi = p[K.remCi] ?: d.remCi,
            remTelefono = p[K.remTelefono] ?: d.remTelefono,
            destEncabezado = p[K.destEncabezado] ?: d.destEncabezado,
            destTratamiento = p[K.destTratamiento] ?: d.destTratamiento,
            destNombre = p[K.destNombre] ?: d.destNombre,
            destCargo = p[K.destCargo] ?: d.destCargo,
            destInstitucion = p[K.destInstitucion] ?: d.destInstitucion,
            referencia = p[K.referencia] ?: d.referencia,
            cuerpo = p[K.cuerpo] ?: d.cuerpo,
            despedida = p[K.despedida] ?: d.despedida,
            posicionFirma = p[K.posicion]?.let { runCatching { PosicionFirma.valueOf(it) }.getOrNull() } ?: d.posicionFirma,
            firmante1Nombre = p[K.f1Nombre] ?: d.firmante1Nombre,
            firmante1Cargo = p[K.f1Cargo] ?: d.firmante1Cargo,
            firmante2Nombre = p[K.f2Nombre] ?: d.firmante2Nombre,
            firmante2Cargo = p[K.f2Cargo] ?: d.firmante2Cargo,
            datosBajoFirma = p[K.datosBajoFirma] ?: d.datosBajoFirma,
            papel = p[K.papel]?.let { runCatching { Papel.valueOf(it) }.getOrNull() } ?: d.papel,
            fuente = p[K.fuente]?.let { runCatching { Fuente.valueOf(it) }.getOrNull() } ?: d.fuente
        )
    }

    suspend fun guardar(c: Carta) {
        context.almacen.edit { p ->
            p[K.tipo] = c.tipo.name
            p[K.lugarFecha] = c.lugarFecha
            p[K.remNombre] = c.remNombre
            p[K.remCargo] = c.remCargo
            p[K.remCi] = c.remCi
            p[K.remTelefono] = c.remTelefono
            p[K.destEncabezado] = c.destEncabezado
            p[K.destTratamiento] = c.destTratamiento
            p[K.destNombre] = c.destNombre
            p[K.destCargo] = c.destCargo
            p[K.destInstitucion] = c.destInstitucion
            p[K.referencia] = c.referencia
            p[K.cuerpo] = c.cuerpo
            p[K.despedida] = c.despedida
            p[K.posicion] = c.posicionFirma.name
            p[K.f1Nombre] = c.firmante1Nombre
            p[K.f1Cargo] = c.firmante1Cargo
            p[K.f2Nombre] = c.firmante2Nombre
            p[K.f2Cargo] = c.firmante2Cargo
            p[K.datosBajoFirma] = c.datosBajoFirma
            p[K.papel] = c.papel.name
            p[K.fuente] = c.fuente.name
        }
    }
}
