package com.manav.geaper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(
    onSave: (String, String) -> Unit
) {

    var username by remember { mutableStateOf("") }

    val sites = listOf(
        "chaturbate",
        "camsoda"
    )

    var selectedSite by remember { mutableStateOf(sites[0]) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Add Streamer",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {

            TextField(
                value = selectedSite,
                onValueChange = {},
                readOnly = true,
                label = { Text("Site") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                },
                modifier = Modifier.menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {

                sites.forEach { site ->

                    DropdownMenuItem(
                        text = { Text(site) },
                        onClick = {
                            selectedSite = site
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Streamer Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(selectedSite, username)
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Save")
        }
    }
}