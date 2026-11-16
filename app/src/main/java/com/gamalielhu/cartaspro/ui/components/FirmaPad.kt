package com.gamalielhu.cartaspro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gamalielhu.cartaspro.data.Trazo

private val Negro = Color(0xFF111111)
private val Azul = Color(0xFF0D2A8A)

/** Lienzo táctil para dibujar la firma con el dedo. Devuelve trazos en píxeles. */
@Composable
fun DialogoFirma(
    titulo: String,
    alCancelar: () -> Unit,
    alGuardar: (trazos: List<Trazo>, grosorPx: Float, colorArgb: Int) -> Unit
) {
    val trazos = remember { mutableStateListOf<List<Offset>>() }
    val actual = remember { mutableStateListOf<Offset>() }
    var color by remember { mutableStateOf(Azul) }
    val grosorPx = with(LocalDensity.current) { 3.dp.toPx() }

    Dialog(onDismissRequest = alCancelar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Text("Firma con el dedo dentro del recuadro", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Canvas(
                        Modifier
                            .matchParentSize()
                            .pointerInput(Unit) {
                                detectTapGestures { p -> trazos.add(listOf(p)) }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { p -> actual.clear(); actual.add(p) },
                                    onDrag = { cambio, _ -> actual.add(cambio.position) },
                                    onDragEnd = { trazos.add(actual.toList()); actual.clear() },
                                    onDragCancel = { trazos.add(actual.toList()); actual.clear() }
                                )
                            }
                    ) {
                        // Línea guía
                        val yGuia = size.height * 0.75f
                        drawLine(Color.LightGray, Offset(24f, yGuia), Offset(size.width - 24f, yGuia), strokeWidth = 2f)
                        val estilo = Stroke(width = grosorPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        (trazos + listOf(actual.toList())).forEach { t ->
                            if (t.isEmpty()) return@forEach
                            if (t.size == 1) {
                                drawCircle(color, grosorPx / 2f, t[0])
                            } else {
                                val path = Path().apply {
                                    moveTo(t[0].x, t[0].y)
                                    for (i in 1 until t.size) lineTo(t[i].x, t[i].y)
                                }
                                drawPath(path, color, style = estilo)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tinta:")
                    listOf(Azul, Negro).forEach { c ->
                        Surface(
                            shape = CircleShape,
                            color = c,
                            border = if (c == color) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(30.dp).clip(CircleShape).pointerInput(c) {
                                detectTapGestures { color = c }
                            }
                        ) {}
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { trazos.clear(); actual.clear() }) { Text("Limpiar") }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = alCancelar) { Text("Cancelar") }
                    TextButton(
                        enabled = trazos.isNotEmpty(),
                        onClick = {
                            alGuardar(trazos.map { t -> t.map { it.x to it.y } }, grosorPx, color.toArgb())
                        }
                    ) { Text("Guardar firma") }
                }
            }
        }
    }
}
