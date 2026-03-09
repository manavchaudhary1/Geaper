package com.manav.geaper.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.manav.geaper.R

@Composable
fun BottomBar(navController: NavHostController) {

    val currentRoute =
        navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {

        NavigationBarItem(
            selected = currentRoute == "Home",
            onClick = { navController.navigate("Home") },
            icon = { Icon(Icons.Default.Home, "Home") }
        )

        NavigationBarItem(
            selected = currentRoute == "Custom Script",
            onClick = { navController.navigate("Custom Script") },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.article_24px),
                    contentDescription = "Custom Script"
                )
            }
        )

        NavigationBarItem(
            selected = currentRoute == "Settings",
            onClick = { navController.navigate("Settings") },
            icon = { Icon(Icons.Default.Settings, "Settings") }
        )
    }
}