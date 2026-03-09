package com.manav.geaper.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.viewmodel.StreamViewModel

// ── Shared color palette ──────────────────────────────────────────────────────
private val BackgroundDark = Color(0xFF0D0F14)
private val SurfaceDark    = Color(0xFF161920)
private val CardDark       = Color(0xFF1E2229)
private val SheetDark      = Color(0xFF13161C)
private val AccentCyan     = Color(0xFF00E5C8)
private val TextPrimary    = Color(0xFFECEFF4)
private val TextSecondary  = Color(0xFF7A8499)
private val DividerColor   = Color(0xFF252A33)
private val InputBg        = Color(0xFF1A1E26)

private val StatusPublic  = Color(0xFF00E676)
private val StatusPrivate = Color(0xFFE040FB)
private val StatusHidden  = Color(0xFFFFD740)
private val StatusAway    = Color(0xFF78909C)
private val StatusOffline = Color(0xFFEF5350)

private fun statusColor(status: String) = when (status.lowercase()) {
    "public", "online" -> StatusPublic
    "private"          -> StatusPrivate
    "hidden"           -> StatusHidden
    "away"             -> StatusAway
    else               -> StatusOffline
}

private fun statusLabel(status: String) = when (status.lowercase()) {
    "public", "online" -> "LIVE"
    "private"          -> "PRIVATE"
    "hidden"           -> "HIDDEN"
    "away"             -> "AWAY"
    else               -> "OFFLINE"
}

// ── Root screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    streamers: List<Streamer>,
    viewModel: StreamViewModel
) {
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet    by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {

        Column(modifier = Modifier.fillMaxSize()) {

            val isMonitoring by viewModel.isMonitoring.collectAsState()
            TopBar(
                streamerCount  = streamers.size,
                liveCount      = streamers.count { it.status.lowercase() == "public" || it.status.lowercase() == "online" },
                isMonitoring   = isMonitoring,
                onMonitorClick = { viewModel.toggleMonitoring() }
            )

            if (streamers.isEmpty()) {
                EmptyState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier            = Modifier.weight(1f),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(streamers, key = { it.username }) { streamer ->
                        StreamerCard(
                            streamer = streamer,
                            onDelete = { viewModel.removeStreamer(streamer) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // FAB – bottom right
        FloatingActionButton(
            onClick        = { showSheet = true },
            modifier       = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = AccentCyan,
            contentColor   = BackgroundDark,
            shape          = RoundedCornerShape(16.dp),
            elevation      = FloatingActionButtonDefaults.elevation(6.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add streamer")
        }
    }

    // ── Slide-up add panel ────────────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; focusManager.clearFocus() },
            sheetState       = sheetState,
            containerColor   = SheetDark,
            tonalElevation   = 0.dp,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle       = {
                // Custom header row acts as drag handle
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text          = "ADD STREAMER",
                            fontSize      = 13.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = AccentCyan,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text     = "Track a new channel",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                    IconButton(
                        onClick  = { showSheet = false; focusManager.clearFocus() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF252A33))
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close panel",
                            tint               = TextSecondary,
                            modifier           = Modifier.size(16.dp)
                        )
                    }
                }
            }
        ) {
            AddPanel(
                onSave = { site, username ->
                    viewModel.addStreamer(site, username)
                    showSheet = false
                    focusManager.clearFocus()
                }
            )
        }
    }
}

// ── Add panel content ─────────────────────────────────────────────────────────

@Composable
private fun AddPanel(onSave: (String, String) -> Unit) {
    val sites          = listOf("chaturbate", "camsoda")
    var username       by remember { mutableStateOf("") }
    var selectedSite   by remember { mutableStateOf(sites[0]) }
    val focusRequester = remember { FocusRequester() }
    val canSave        = username.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(modifier = Modifier.height(4.dp))

        // Platform label + tiles
        Text("PLATFORM", fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.5.sp)

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            sites.forEach { site ->
                val selected = site == selectedSite
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) AccentCyan.copy(alpha = 0.12f) else InputBg)
                        .border(1.dp, if (selected) AccentCyan else DividerColor, RoundedCornerShape(12.dp))
                        .clickable { selectedSite = site }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text       = if (site == "chaturbate") "CB" else "CS",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Black,
                            color      = if (selected) AccentCyan else TextSecondary
                        )
                        Text(
                            text       = site.replaceFirstChar { it.uppercase() },
                            fontSize   = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (selected) AccentCyan else TextSecondary
                        )
                    }
                }
            }
        }

        // Username label + field
        Text("USERNAME", fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.5.sp)

        OutlinedTextField(
            value           = username,
            onValueChange   = { username = it },
            placeholder     = { Text("e.g. streamer_name", color = TextSecondary, fontSize = 14.sp) },
            singleLine      = true,
            modifier        = Modifier.fillMaxWidth().focusRequester(focusRequester),
            shape           = RoundedCornerShape(12.dp),
            colors          = outlinedFieldColors(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (canSave) onSave(selectedSite, username.trim()) }
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick   = { if (canSave) onSave(selectedSite, username.trim()) },
            enabled   = canSave,
            modifier  = Modifier.fillMaxWidth().height(52.dp),
            shape     = RoundedCornerShape(14.dp),
            colors    = ButtonDefaults.buttonColors(
                containerColor         = AccentCyan,
                contentColor           = BackgroundDark,
                disabledContainerColor = AccentCyan.copy(alpha = 0.25f),
                disabledContentColor   = BackgroundDark.copy(alpha = 0.4f)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text("Add Streamer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = AccentCyan,
    unfocusedBorderColor    = DividerColor,
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
    cursorColor             = AccentCyan,
    focusedContainerColor   = InputBg,
    unfocusedContainerColor = InputBg
)

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(
    streamerCount: Int,
    liveCount: Int,
    isMonitoring: Boolean,
    onMonitorClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("GEAPER", fontSize = 22.sp, fontWeight = FontWeight.Black,
                    color = TextPrimary, letterSpacing = 4.sp)
                Text("Stream Monitor", fontSize = 12.sp, color = TextSecondary)
            }
            Button(
                onClick        = onMonitorClick,
                shape          = RoundedCornerShape(12.dp),
                colors         = ButtonDefaults.buttonColors(
                    containerColor = if (isMonitoring) StatusOffline else AccentCyan
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                elevation      = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    if (isMonitoring) Icons.Default.Close else Icons.Default.PlayArrow,
                    null, tint = BackgroundDark, modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isMonitoring) "Stop" else "Monitor",
                    color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = DividerColor, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatChip("TRACKED", streamerCount.toString())
            StatChip("LIVE NOW", liveCount.toString(), highlight = liveCount > 0)
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, highlight: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = if (highlight) AccentCyan else TextPrimary)
        Text(label, fontSize = 10.sp, color = TextSecondary, letterSpacing = 1.sp)
    }
}

// ── Streamer card ─────────────────────────────────────────────────────────────

@Composable
private fun StreamerCard(streamer: Streamer, onDelete: () -> Unit) {
    val color = statusColor(streamer.status)
    val label = statusLabel(streamer.status)

    Card(
        modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(14.dp)),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier              = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.08f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(streamer.username.first().uppercaseChar().toString(),
                        color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(streamer.username, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        PulseDot(color)
                        Text(label, color = color, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            IconButton(
                onClick  = onDelete,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x22EF5350))
            ) {
                Icon(Icons.Default.Delete, "Remove ${streamer.username}",
                    tint = StatusOffline, modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ── Pulse dot ─────────────────────────────────────────────────────────────────

@Composable
private fun PulseDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue  = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "alpha"
    )
    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = alpha)))
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("📡", fontSize = 40.sp)
            Text("No streamers tracked", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Tap + to add one", color = TextSecondary, fontSize = 13.sp)
        }
    }
}