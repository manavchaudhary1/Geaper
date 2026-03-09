package com.manav.geaper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manav.geaper.viewmodel.StreamViewModel
import androidx.navigation.compose.*
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.manav.geaper.data.db.AppDatabase
import com.manav.geaper.data.repository.StreamRepository
import com.manav.geaper.network.CamsodaApi
import com.manav.geaper.network.ChaturbateApi
import com.manav.geaper.ui.screens.*
import com.manav.geaper.viewmodel.StreamViewModelFactory

@Composable
fun App() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val db = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "streamers.db"
    ).build()

    val repository = StreamRepository(
        db.streamerDao(),
        ChaturbateApi(),
        CamsodaApi()
    )

    val viewModel: StreamViewModel = viewModel(
        factory = StreamViewModelFactory(repository)
    )

    Scaffold(
        bottomBar = {
            BottomBar(navController)
        }
    ) { padding ->

        NavHost(
            navController    = navController,
            startDestination = "Home",
            modifier         = Modifier.padding(padding)
        ) {

            composable("Home") {
                val streamers by viewModel.streamers.collectAsState(initial = emptyList())
                HomeScreen(
                    streamers = streamers,
                    viewModel = viewModel
                )
            }

            composable("Custom Script") { Screen("Custom Script") }
            composable("Settings") { Screen("Settings") }
        }
    }
}