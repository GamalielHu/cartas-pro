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
import com.gamalielhu.cartaspro.model.TipoCarta
import com.gamalielhu.cartaspro.ui.CartasViewModel

private fun icono(t: TipoCarta): ImageVector = when (t) {
    TipoCarta.SOLICITUD -> Icons.AutoMirrored.Filled.Send
    TipoCarta.NOTIFICACION -> Icons.Filled.Notifications
    TipoCarta.PRESENTACION -> Icons.Filled.Star
    TipoCarta.LIBRE -> Icons.Filled.Edit
}

@Composable
fun InicioScreen(vm: CartasViewModel) {
    val carta by vm.carta.collectAsState()
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
                                carta.referencia.ifBlank { carta.tipo.titulo },
                                style = MaterialTheme.typography.bodySmall, maxLines = 1
                            )
                        }
                    }
                }
            }

            Text("Nueva carta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TipoCarta.entries.forEach { t ->
                ElevatedCard(Modifier.fillMaxWidth().clickable { vm.elegirPlantilla(t) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
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
            Spacer(Modifier.height(8.dp))
            Text(
                "Tus datos de remitente, ciudad y firma se guardan solos para la próxima carta.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
