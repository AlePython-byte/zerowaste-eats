package com.zerowasteeats.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zerowasteeats.app.presentation.screens.playground.GlassPlaygroundScreen
import com.zerowasteeats.app.presentation.theme.ZeroWasteEatsTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            ZeroWasteEatsTheme {
                GlassPlaygroundScreen()
            }
        }
    }
}