package com.manav.geaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.manav.geaper.ui.App
import com.manav.geaper.ui.theme.GeaperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeaperTheme {
                Start()
                App()
            }
        }
    }
}

@Composable
fun Start() {
    Text("Hello Jetpack Compose")
}