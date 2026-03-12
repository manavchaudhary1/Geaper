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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.viewmodel.StreamViewModel

// ── Palette (mirrors HomeScreen/SettingsScreen) ───────────────────────────────
private val BackgroundDark = Color(0xFF0D0F14)
private val SurfaceDark    = Color(0xFF161920)
private val CardDark       = Color(0xFF1E2229)
private val AccentCyan     = Color(0xFF00E5C8)
private val TextPrimary    = Color(0xFFECEFF4)
private val TextSecondary  = Color(0xFF7A8499)
private val DividerColor   = Color(0xFF252A33)
private val InputBg        = Color(0xFF1A1E26)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomScriptScreen(viewModel: StreamViewModel) {

    val presets by viewModel.presets.collectAsState()
    var showDialog  by remember { mutableStateOf(false) }
    var editTarget  by remember { mutableStateOf<FfmpegPreset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("FFMPEG PRESETS", fontSize = 20.sp, fontWeight = FontWeight.Black,
                color = TextPrimary, letterSpacing = 4.sp)
            Text("Custom yt-dlp post-processor arguments", fontSize = 12.sp, color = TextSecondary)
        }

        if (presets.isEmpty()) {
            Box(
                modifier         = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎬", fontSize = 36.sp)
                    Text("No presets yet", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Tap + to create a custom FFmpeg preset", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "These presets are passed as --postprocessor-args ffmpeg:<args> to yt-dlp.",
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(presets, key = { it.id }) { preset ->
                    PresetCard(
                        preset   = preset,
                        onEdit   = { editTarget = preset; showDialog = true },
                        onDelete = { viewModel.deletePreset(preset) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // FAB
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
            FloatingActionButton(
                onClick        = { editTarget = null; showDialog = true },
                modifier       = Modifier.padding(20.dp),
                containerColor = AccentCyan,
                contentColor   = BackgroundDark,
                shape          = RoundedCornerShape(16.dp),
                elevation      = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Add, "New preset")
            }
        }
    }

    // ── Add / Edit dialog ──────────────────────────────────────────────────
    if (showDialog) {
        PresetDialog(
            initial  = editTarget,
            onSave   = { name, args, desc ->
                if (editTarget == null) {
                    viewModel.addPreset(name, args, desc)
                } else {
                    viewModel.updatePreset(editTarget!!.copy(name = name, args = args, description = desc))
                }
                showDialog = false
                editTarget = null
            },
            onDismiss = { showDialog = false; editTarget = null }
        )
    }
}

// ── Preset card ───────────────────────────────────────────────────────────────

@Composable
private fun PresetCard(
    preset: FfmpegPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = CardDark,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(preset.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    if (preset.description.isNotBlank())
                        Text(preset.description, color = TextSecondary, fontSize = 12.sp)
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = AccentCyan, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(17.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Args block
            Surface(
                shape  = RoundedCornerShape(8.dp),
                color  = Color(0xFF0D0F14),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text     = preset.args,
                    color    = AccentCyan,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

// ── Add / Edit dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDialog(
    initial: FfmpegPreset?,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var args by remember { mutableStateOf(initial?.args ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = AccentCyan,
        unfocusedBorderColor    = DividerColor,
        focusedTextColor        = TextPrimary,
        unfocusedTextColor      = TextPrimary,
        cursorColor             = AccentCyan,
        focusedContainerColor   = InputBg,
        unfocusedContainerColor = InputBg
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF161920),
        title = {
            Text(
                if (initial == null) "New FFmpeg Preset" else "Edit Preset",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Help text
                Surface(shape = RoundedCornerShape(8.dp), color = AccentCyan.copy(alpha = 0.08f)) {
                    Text(
                        "Args are appended to: --postprocessor-args ffmpeg:<your args>",
                        fontSize = 11.sp,
                        color    = AccentCyan,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Preset Name") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = textFieldColors,
                    modifier      = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value         = args,
                    onValueChange = { args = it },
                    label         = { Text("FFmpeg Arguments") },
                    placeholder   = { Text("-vf scale=-2:720 -c:v libx264 -crf 23", color = TextSecondary, fontSize = 12.sp) },
                    minLines      = 3,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = textFieldColors,
                    modifier      = Modifier.fillMaxWidth(),
                    textStyle     = LocalTextStyle.current.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize   = 13.sp
                    )
                )
                OutlinedTextField(
                    value         = desc,
                    onValueChange = { desc = it },
                    label         = { Text("Description (optional)") },
                    singleLine    = true,
                    shape         = RoundedCornerShape(10.dp),
                    colors        = textFieldColors,
                    modifier      = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick   = { if (name.isNotBlank() && args.isNotBlank()) onSave(name.trim(), args.trim(), desc.trim()) },
                enabled   = name.isNotBlank() && args.isNotBlank(),
                shape     = RoundedCornerShape(10.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = AccentCyan)
            ) {
                Text("Save", color = BackgroundDark, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}