package com.manav.geaper.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.geaper.data.prefs.safUriToPath
import com.manav.geaper.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {

    val context = LocalContext.current

    val savePath       by viewModel.savePath.collectAsState()
    val segmentMinutes by viewModel.segmentMinutes.collectAsState()
    val themeMode      by viewModel.themeMode.collectAsState()
    val dynamicColor   by viewModel.dynamicColor.collectAsState()
    val cbWmToken      by viewModel.cbWmToken.collectAsState()

    var storageGranted by remember {
        mutableStateOf(com.manav.geaper.util.StoragePermission.isGranted())
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.setSavePath(uri.toString())
        }
    }

    var segmentInput by remember(segmentMinutes) { mutableStateOf(segmentMinutes.toString()) }
    var wmTokenInput    by remember(cbWmToken)      { mutableStateOf(cbWmToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {

        // ── Header ──────────────────────────────────────────────────────────
        Surface(
            color          = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    "SETTINGS",
                    fontSize      = 22.sp,
                    fontWeight    = FontWeight.Black,
                    color         = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 4.sp,
                )
                Text(
                    "Preferences & storage",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── APPEARANCE ─────────────────────────────────────────────────
            SectionLabel("APPEARANCE")

            // Dynamic color toggle
            SettingsToggleRow(
                title    = "Material You Dynamic Color",
                subtitle = "Adapts theme to your wallpaper (Android 12+)",
                checked  = dynamicColor,
                onToggle = { viewModel.setDynamicColor(it) },
            )

            // Theme mode — 3 tiles in ONE row
            SectionLabel("THEME MODE")
            SettingsCard {
                val themes = listOf(
                    Triple("system", "System",    Icons.Default.SettingsBrightness),
                    Triple("light",  "Light",     Icons.Default.WbSunny),
                    Triple("dark",   "Dark",      Icons.Default.DarkMode),
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    themes.forEach { (key, label, icon) ->
                        val selected = themeMode == key
                        Surface(
                            modifier  = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.setThemeMode(key) },
                            shape     = RoundedCornerShape(10.dp),
                            color     = if (selected)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceContainerHigh,
                            border    = if (selected)
                                androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.secondary,
                                )
                            else null,
                        ) {
                            Column(
                                modifier            = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector        = icon,
                                    contentDescription = label,
                                    tint               = if (selected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier           = Modifier.size(20.dp),
                                )
                                Text(
                                    label,
                                    fontSize   = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (selected)
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(4.dp))

            // ── RECORDING ──────────────────────────────────────────────────
            SectionLabel("RECORDING")

            // Storage permission warning
            if (!storageGranted) {
                Surface(
                    shape    = RoundedCornerShape(14.dp),
                    color    = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier              = Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Storage Permission Required",
                                fontSize   = 13.sp,
                                color      = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Grant \"All files access\" to record to custom paths.",
                                fontSize = 11.sp,
                                color    = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            )
                        }
                        TextButton(onClick = {
                            com.manav.geaper.util.StoragePermission.openSettings(context)
                            storageGranted = com.manav.geaper.util.StoragePermission.isGranted()
                        }) {
                            Text(
                                "Grant",
                                color      = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 12.sp,
                            )
                        }
                    }
                }
            }

            // Save path
            SettingsCard {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Save Location",
                            fontSize   = 14.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        val displayPath = if (savePath.isBlank()) {
                            "App internal storage (default)"
                        } else {
                            safUriToPath(context, savePath).ifBlank { savePath }
                        }
                        Text(displayPath, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick        = { folderPicker.launch(null) },
                        shape          = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Browse", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Segment duration
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text(
                            "Segment Duration",
                            fontSize   = 14.sp,
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (segmentMinutes == 0) "Disabled — one continuous file"
                            else "Split every $segmentMinutes min",
                            fontSize = 11.sp,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Input + Set button on one line
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value         = segmentInput,
                            onValueChange = { segmentInput = it.filter { c -> c.isDigit() } },
                            label         = { Text("Minutes (0 = off)", fontSize = 12.sp) },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(10.dp),
                        )
                        FilledTonalButton(
                            onClick        = {
                                viewModel.setSegmentMinutes(segmentInput.toIntOrNull() ?: 0)
                            },
                            shape          = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            Text("Set", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick preset chips — all on ONE line
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0, 30, 60, 120).forEach { mins ->
                            val label    = if (mins == 0) "Off" else "${mins}m"
                            val selected = segmentMinutes == mins
                            FilterChip(
                                selected = selected,
                                onClick  = {
                                    viewModel.setSegmentMinutes(mins)
                                    segmentInput = mins.toString()
                                },
                                label = { Text(label, fontSize = 11.sp) },
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(4.dp))

            // ── CHATURBATE ─────────────────────────────────────────────────
            SectionLabel("CHATURBATE")

            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        Text("Affiliate wm= Token", fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        Text(
                            "Used in the Chaturbate online-rooms API URL (?wm=…). " +
                                    "Get your token from chaturbate.com/affiliates.",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value         = wmTokenInput,
                            onValueChange = { wmTokenInput = it },
                            label         = { Text("wm token", fontSize = 12.sp) },
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            shape         = RoundedCornerShape(10.dp),
                        )
                        FilledTonalButton(
                            onClick        = { viewModel.setCbWmToken(wmTokenInput.trim()) },
                            shape          = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                        ) {
                            Text("Set", fontWeight = FontWeight.Bold)
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
        color         = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
        fontWeight    = FontWeight.Bold,
        modifier      = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape          = RoundedCornerShape(14.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier       = Modifier.fillMaxWidth(),
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
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        shape          = RoundedCornerShape(14.dp),
        color          = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier       = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title,    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked         = checked,
                onCheckedChange = onToggle,
            )
        }
    }
}