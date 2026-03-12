package com.manav.geaper.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.manav.geaper.data.prefs.safUriToPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.manav.geaper.viewmodel.SettingsViewModel

// ── Shared palette (mirrors HomeScreen) ──────────────────────────────────────
private val BackgroundDark = Color(0xFF0D0F14)
private val SurfaceDark    = Color(0xFF161920)
private val CardDark       = Color(0xFF1E2229)
private val AccentCyan     = Color(0xFF00E5C8)
private val TextPrimary    = Color(0xFFECEFF4)
private val TextSecondary  = Color(0xFF7A8499)
private val DividerColor   = Color(0xFF252A33)
private val InputBg        = Color(0xFF1A1E26)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {

    val context = LocalContext.current

    val savePath       by viewModel.savePath.collectAsState()
    val segmentMinutes by viewModel.segmentMinutes.collectAsState()
    val themeMode      by viewModel.themeMode.collectAsState()
    val dynamicColor   by viewModel.dynamicColor.collectAsState()

    // Track whether MANAGE_EXTERNAL_STORAGE is granted
    var storageGranted by remember {
        mutableStateOf(com.manav.geaper.util.StoragePermission.isGranted())
    }

    // SAF folder picker
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist permission
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setSavePath(uri.toString())
        }
    }

    var segmentInput by remember(segmentMinutes) { mutableStateOf(segmentMinutes.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("SETTINGS", fontSize = 22.sp, fontWeight = FontWeight.Black,
                color = TextPrimary, letterSpacing = 4.sp)
            Text("Preferences & storage", fontSize = 12.sp, color = TextSecondary)
        }

        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── APPEARANCE ─────────────────────────────────────────────────
            SectionLabel("APPEARANCE")

            // Dynamic color toggle (Android 12+)
            SettingsToggleRow(
                title       = "Material You Dynamic Color",
                subtitle    = "Adapts theme to your wallpaper (Android 12+)",
                checked     = dynamicColor,
                onToggle    = { viewModel.setDynamicColor(it) }
            )

            // Theme mode selector
            SectionLabel("THEME MODE")
            val themes = listOf("system" to "System Default", "light" to "Light", "dark" to "Dark")
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                themes.forEach { (key, label) ->
                    val selected = themeMode == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccentCyan.copy(alpha = 0.15f) else InputBg)
                            .border(1.dp, if (selected) AccentCyan else DividerColor, RoundedCornerShape(10.dp))
                            .clickable { viewModel.setThemeMode(key) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (key) {
                                    "light"  -> Icons.Default.WbSunny
                                    "dark"   -> Icons.Default.DarkMode
                                    else     -> Icons.Default.SettingsBrightness
                                },
                                contentDescription = label,
                                tint     = if (selected) AccentCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize   = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected) AccentCyan else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = DividerColor)
            Spacer(modifier = Modifier.height(4.dp))

            // ── RECORDING ──────────────────────────────────────────────────
            SectionLabel("RECORDING")

            // Storage permission warning
            if (!storageGranted) {
                Surface(
                    shape    = RoundedCornerShape(14.dp),
                    color    = Color(0xFFFF6D00).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, null,
                            tint = Color(0xFFFF6D00), modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Storage Permission Required",
                                fontSize = 13.sp, color = Color(0xFFFF6D00),
                                fontWeight = FontWeight.Bold)
                            Text("Grant \"All files access\" to record to custom paths.",
                                fontSize = 11.sp, color = TextSecondary)
                        }
                        TextButton(onClick = {
                            com.manav.geaper.util.StoragePermission.openSettings(context)
                            storageGranted = com.manav.geaper.util.StoragePermission.isGranted()
                        }) {
                            Text("Grant", color = Color(0xFFFF6D00),
                                fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Save path
            SettingsCard {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Save Location", fontSize = 14.sp, color = TextPrimary,
                            fontWeight = FontWeight.Medium)
                        val displayPath = if (savePath.isBlank()) {
                            "App internal storage (default)"
                        } else {
                            // Show the real path, not the raw content:// URI
                            com.manav.geaper.data.prefs.safUriToPath(context, savePath)
                                .ifBlank { savePath }
                        }
                        Text(
                            displayPath,
                            fontSize  = 11.sp,
                            color     = TextSecondary,
                            maxLines  = 2
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { folderPicker.launch(null) },
                        shape   = RoundedCornerShape(10.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null,
                            tint = BackgroundDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse", color = BackgroundDark,
                            fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Segment duration
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Segment Duration", fontSize = 14.sp, color = TextPrimary,
                                fontWeight = FontWeight.Medium)
                            Text(
                                if (segmentMinutes == 0) "Disabled — one continuous file"
                                else "Split every $segmentMinutes min",
                                fontSize = 11.sp, color = TextSecondary
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value         = segmentInput,
                            onValueChange = { segmentInput = it.filter { c -> c.isDigit() } },
                            label         = { Text("Minutes (0 = off)", fontSize = 12.sp) },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(10.dp),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor      = AccentCyan,
                                unfocusedBorderColor    = DividerColor,
                                focusedTextColor        = TextPrimary,
                                unfocusedTextColor      = TextPrimary,
                                cursorColor             = AccentCyan,
                                focusedContainerColor   = InputBg,
                                unfocusedContainerColor = InputBg
                            )
                        )
                        Button(
                            onClick = {
                                val mins = segmentInput.toIntOrNull() ?: 0
                                viewModel.setSegmentMinutes(mins)
                            },
                            shape  = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Text("Set", color = BackgroundDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Quick presets
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 30, 60, 120).forEach { mins ->
                            val label = if (mins == 0) "Off" else "${mins}m"
                            val selected = segmentMinutes == mins
                            FilterChip(
                                selected = selected,
                                onClick  = { viewModel.setSegmentMinutes(mins); segmentInput = mins.toString() },
                                label    = { Text(label, fontSize = 11.sp) },
                                colors   = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor    = AccentCyan.copy(alpha = 0.2f),
                                    selectedLabelColor        = AccentCyan,
                                    containerColor            = InputBg,
                                    labelColor                = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 10.sp,
        color         = TextSecondary,
        letterSpacing = 1.5.sp,
        fontWeight    = FontWeight.Bold,
        modifier      = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = CardDark,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape  = RoundedCornerShape(14.dp),
        color  = CardDark,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = BackgroundDark,
                    checkedTrackColor  = AccentCyan
                )
            )
        }
    }
}