package com.manav.geaper.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun AddStreamerSheet(onSave: (String, String) -> Unit) {

  var username by remember { mutableStateOf("") }
  var site by remember { mutableStateOf("chaturbate") }

  Column {
    TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })

    Button(onClick = { onSave(site, username) }) { Text("Save") }
  }
}
