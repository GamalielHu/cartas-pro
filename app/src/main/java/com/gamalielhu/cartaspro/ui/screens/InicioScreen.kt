package com.gamalielhu.cartaspro.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gamalielhu.cartaspro.model.Grupo
import com.gamalielhu.cartaspro.model.TipoCarta
import com.gamalielhu.cartaspro.ui.CartasViewModel

private fun icono(t: TipoCarta): ImageVector = when (t) {
    TipoCarta.SOLICITUD_PAGO -> Icons.Filled.Star
    TipoCarta.ENTREGA_PROVISIONAL, TipoCarta.ENTREGA_DEFINITIVA -> Icons.Filled.Build
    TipoCarta.DEVOLUCION_GARANTIA -> Icons.Filled.Refresh
    TipoCarta.CERTIFICACION_PROYECTOS, TipoCarta.CERTIFICADO_TRABAJO -> Icons.Filled.Check
    TipoCarta.SOLICITUD_PERSONAL, TipoCarta.AUTORIZACION, TipoCarta.RECOMENDACION -> Icons.Filled.Person
    TipoCarta.SOLICITUD -> Icons.AutoMirrored.Filled.Send
    TipoCarta.NOTIFICACION -> Icons.Filled.Notifications
    else -> Icons.Filled.Edit
}

@Composable
fun InicioScreen(vm: CartasViewModel) {
    val carta by vm.carta.collectAsState()
    val perfiles by vm.perfiles.collectAsState()
    val personal = perfiles.firstOrNull { it.id == carta.perfilId }?.personal ?: false
    val grupos = if (personal) listOf(Grupo.PERSONAL, Grupo.GENERAL, Grupo.OBRAS) else listOf(Grupo.OBRAS, Grupo.GENERAL, Grupo.PERSONAL)
    Scaffold { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Email, null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column {
                    Text("CartasPro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Cartas formales en PDF y Word, con tu firma", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(4.dp))

            if (vm.hayBorrador) {
                Card(
                    onClick = { vm.continuarBorrador() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Continuar la última carta", fontWeight = FontWeight.SemiBold)
                            Text(
                                listOf(carta.tipo.titulo, carta.proyecto).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, maxLines = 1
                            )
                        }
                    }
                }
            }

            Text("¿Para quién es la carta?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                perfiles.forEach { p ->
                    FilterChip(selected = p.id == carta.perfilId, onClick = { vm.elegirPerfil(p.id) }, label = { Text(p.nombre) })
                }
            }

            grupos.forEach { g ->
                Text(g.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp))
                TipoCarta.entries.filter { it.grupo == g }.forEach { t ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { vm.elegirPlantilla(t) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Icon(icono(t), null) }
                            }
                            Spacer(Modifier.size(14.dp))
                            Column {
                                Text(t.titulo, style = MaterialTheme.typography.titleMedium)
                                Text(t.descripcion, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Cada empresa guarda sus datos y su formato. Lo que escribes se guarda solo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
