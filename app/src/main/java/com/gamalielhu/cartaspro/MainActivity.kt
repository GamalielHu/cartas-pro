package com.gamalielhu.cartaspro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamalielhu.cartaspro.ui.CartasViewModel
import com.gamalielhu.cartaspro.ui.screens.AppCartas
import com.gamalielhu.cartaspro.ui.theme.CartasTema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CartasTema {
                val vm: CartasViewModel = viewModel()
                AppCartas(vm)
            }
        }
    }
}
