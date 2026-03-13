package com.manav.geaper.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import androidx.room.Room
import com.manav.geaper.data.db.AppDatabase
import com.manav.geaper.data.prefs.AppPreferences
import com.manav.geaper.data.repository.StreamRepository
import com.manav.geaper.network.CamsodaApi
import com.manav.geaper.network.ChaturbateApi
import com.manav.geaper.ui.screens.*
import com.manav.geaper.ui.theme.GeaperTheme
import com.manav.geaper.viewmodel.*

@Composable
fun App() {

    val context = LocalContext.current

    val prefs = remember { AppPreferences(context) }

    val db = remember {
        Room.databaseBuilder(context, AppDatabase::class.java, "streamers.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository = remember {
        StreamRepository(
            context   = context,
            dao       = db.streamerDao(),
            presetDao = db.ffmpegPresetDao(),
            cbApi     = ChaturbateApi(),
            csApi     = CamsodaApi(),
            prefs     = prefs,
        )
    }

    val streamViewModel: StreamViewModel = viewModel(
        factory = StreamViewModelFactory(repository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(prefs)
    )

    val themeMode    by settingsViewModel.themeMode.collectAsState()
    val dynamicColor by settingsViewModel.dynamicColor.collectAsState()

    GeaperTheme(themeMode = themeMode, dynamicColor = dynamicColor) {

        val navController = rememberNavController()

        Scaffold(
            bottomBar = { BottomBar(navController) }
        ) { padding ->

            NavHost(
                navController    = navController,
                startDestination = "Home",
                modifier         = Modifier.padding(padding)
            ) {
                composable("Home") {
                    val streamers by streamViewModel.streamers.collectAsState()
                    HomeScreen(streamers = streamers, viewModel = streamViewModel)
                }
                composable("Custom Script") {
                    CustomScriptScreen(viewModel = streamViewModel)
                }
                composable("Settings") {
                    SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}