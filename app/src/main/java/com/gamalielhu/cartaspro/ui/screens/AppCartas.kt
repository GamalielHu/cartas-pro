package com.gamalielhu.cartaspro.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gamalielhu.cartaspro.ui.CartasViewModel
import com.gamalielhu.cartaspro.ui.Pantalla

@Composable
fun AppCartas(vm: CartasViewModel) {
    if (!vm.cargado) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    when (vm.pantalla) {
        Pantalla.INICIO -> InicioScreen(vm)
        Pantalla.EDITOR -> {
            BackHandler { vm.volverAlInicio() }
            EditorScreen(vm)
        }
    }
}
