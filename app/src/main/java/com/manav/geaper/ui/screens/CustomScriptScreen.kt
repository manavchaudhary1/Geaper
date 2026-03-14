package com.manav.geaper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.viewmodel.StreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScriptScreen(viewModel: StreamViewModel) {

  val presets by viewModel.presets.collectAsState()
  var showDialog by remember { mutableStateOf(false) }
  var editTarget by remember { mutableStateOf<FfmpegPreset?>(null) }

  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
  ) {
    // ── Header ─────────────────────────────────────────────────────────
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
      ) {
        Text(
          "FLAG PRESETS",
          fontSize = 20.sp,
          fontWeight = FontWeight.Black,
          color = MaterialTheme.colorScheme.onSurface,
          letterSpacing = 4.sp,
        )
        Text(
          "Named sets of extra yt-dlp flags — applied per streamer",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (presets.isEmpty()) {
      Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("🚩", fontSize = 36.sp)
          Text(
            "No presets yet",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            "Tap + to create an extra-flags preset",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              "These flags are appended verbatim after the standard yt-dlp options. " +
                "Assign a preset to a streamer when adding or editing it.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
        items(presets, key = { it.id }) { preset ->
          PresetCard(
            preset = preset,
            onEdit = {
              editTarget = preset
              showDialog = true
            },
            onDelete = { viewModel.deletePreset(preset) },
          )
        }
        item { Spacer(Modifier.height(80.dp)) }
      }
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
      FloatingActionButton(
        onClick = {
          editTarget = null
          showDialog = true
        },
        modifier = Modifier.padding(20.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Default.Add, "New preset")
      }
    }
  }

  if (showDialog) {
    PresetDialog(
      initial = editTarget,
      onSave = { name, extraArgs ->
        if (editTarget == null) viewModel.addPreset(name, extraArgs)
        else viewModel.updatePreset(editTarget!!.copy(name = name, extraArgs = extraArgs))
        showDialog = false
        editTarget = null
      },
      onDismiss = {
        showDialog = false
        editTarget = null
      },
    )
  }
}

// ── Preset card ───────────────────────────────────────────────────────────────

@Composable
private fun PresetCard(
  preset: FfmpegPreset,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          preset.name,
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.SemiBold,
          fontSize = 15.sp,
        )
        Row {
          IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
            Icon(
              Icons.Default.Edit,
              "Edit",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(17.dp)
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
              Icons.Default.Delete,
              "Delete",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(17.dp)
            )
          }
        }
      }

      // Extra flags value
      if (preset.extraArgs.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            preset.extraArgs,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(10.dp),
          )
        }
      } else {
        Text("No flags set", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDialog(
  initial: FfmpegPreset?,
  onSave: (name: String, extraArgs: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var extraArgs by remember { mutableStateOf(initial?.extraArgs ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initial == null) "New Flag Preset" else "Edit Preset", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Preset Name") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            "EXTRA YT-DLP FLAGS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
          )
          Text(
            "Appended verbatim after all standard yt-dlp options.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = extraArgs,
            onValueChange = { extraArgs = it },
            label = { Text("Flags") },
            placeholder = {
              Text(
                "--concurrent-fragments 4 --throttled-rate 100K",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            },
            minLines = 3,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            textStyle =
              LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
              ),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onSave(name.trim(), extraArgs.trim()) },
        enabled = name.isNotBlank(),
        shape = RoundedCornerShape(10.dp),
      ) {
        Text("Save", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
