package com.manav.geaper.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.viewmodel.StreamViewModel

// ── Format options ────────────────────────────────────────────────────────────

private data class FormatOption(val label: String, val selector: String, val hint: String)

private val FORMAT_OPTIONS =
  listOf(
    FormatOption("Best", "", "Default"),
    FormatOption("≤1080p", "bestvideo[height<=1080]+bestaudio/best[height<=1080]", "Full HD"),
    FormatOption("≤720p", "bestvideo[height<=720]+bestaudio/best[height<=720]", "HD"),
    FormatOption("≤480p", "bestvideo[height<=480]+bestaudio/best[height<=480]", "SD"),
    FormatOption("Audio", "bestaudio/best", "No video"),
    FormatOption("Custom", "__custom__", "Manual -f"),
  )

// ── Colours ───────────────────────────────────────────────────────────────────

private val StatusPublic = Color(0xFF00E676)
private val StatusPrivate = Color(0xFFE040FB)
private val StatusHidden = Color(0xFFFFD740)
private val StatusAway = Color(0xFF78909C)
private val StatusOffline = Color(0xFFEF5350)
private val RecordRed = Color(0xFFFF1744)

private fun statusColor(s: String) =
  when (s.lowercase()) {
    "public",
    "online" -> StatusPublic
    "private" -> StatusPrivate
    "hidden" -> StatusHidden
    "away" -> StatusAway
    else -> StatusOffline
  }

private fun statusLabel(s: String) =
  when (s.lowercase()) {
    "public",
    "online" -> "LIVE"
    "private" -> "PRIVATE"
    "hidden" -> "HIDDEN"
    "away" -> "AWAY"
    else -> "OFFLINE"
  }

// ── Root screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
  streamers: List<Streamer>,
  viewModel: StreamViewModel,
) {
  val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showAddSheet by remember { mutableStateOf(false) }
  var editTarget by remember { mutableStateOf<Streamer?>(null) }
  val focusManager = LocalFocusManager.current

  val presets by viewModel.presets.collectAsState()
  val isMonitoring by viewModel.isMonitoring.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val filterSite by viewModel.filterSite.collectAsState()
  val filterStatus by viewModel.filterStatus.collectAsState()
  val backupResult by viewModel.backupResult.collectAsState()

  var selectedIds by remember { mutableStateOf(setOf<Int>()) }
  val inSelectMode = selectedIds.isNotEmpty()
  var showFilterBar by remember { mutableStateOf(false) }

  // Backup launchers
  val exportLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
      uri: Uri? ->
      uri?.let { viewModel.exportBackup(it) }
    }
  val importLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
      uri?.let { viewModel.importBackup(it) }
    }

  var tick by remember { mutableStateOf(0) }
  LaunchedEffect(Unit) {
    while (true) {
      kotlinx.coroutines.delay(1_000)
      tick++
    }
  }

  LaunchedEffect(streamers) { selectedIds = selectedIds.intersect(streamers.map { it.id }.toSet()) }

  val displayed by viewModel.filteredStreamers.collectAsState()

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Column(modifier = Modifier.fillMaxSize()) {

      // ── Top bar ───────────────────────────────────────────────────
      AnimatedContent(
        targetState = inSelectMode,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        label = "topbar",
      ) { selecting ->
        if (selecting) {
          SelectionTopBar(
            count = selectedIds.size,
            onSelectAll = { selectedIds = streamers.map { it.id }.toSet() },
            onCancel = { selectedIds = emptySet() },
            onDelete = {
              val toDelete = streamers.filter { it.id in selectedIds }
              selectedIds = emptySet()
              toDelete.forEach { viewModel.removeStreamer(it) }
            },
          )
        } else {
          NormalTopBar(
            streamerCount = streamers.size,
            liveCount = streamers.count { it.status.lowercase() in listOf("public", "online") },
            recordingCount = streamers.count { viewModel.isRecording(it).also { _ -> tick } },
            isMonitoring = isMonitoring,
            showFilterBar = showFilterBar,
            onMonitorClick = { viewModel.toggleMonitoring() },
            onFilterClick = { showFilterBar = !showFilterBar },
            onExport = { exportLauncher.launch("geaper_backup.json") },
            onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) },
          )
        }
      }

      // ── Search + filter bar ───────────────────────────────────────
      AnimatedVisibility(visible = showFilterBar) {
        SearchFilterBar(
          query = searchQuery,
          onQueryChange = { viewModel.setSearchQuery(it) },
          filterSite = filterSite,
          onSiteFilter = { viewModel.setFilterSite(it) },
          filterStatus = filterStatus,
          onStatusFilter = { viewModel.setFilterStatus(it) },
        )
      }

      if (displayed.isEmpty()) {
        EmptyState(
          hasStreamers = streamers.isNotEmpty(),
          modifier = Modifier.weight(1f),
        )
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(displayed, key = { it.id }) { streamer ->
            val recording = viewModel.isRecording(streamer).also { _ -> tick }
            val isSelected = streamer.id in selectedIds
            StreamerCard(
              streamer = streamer,
              isRecording = recording,
              isSelected = isSelected,
              inSelectMode = inSelectMode,
              onRecord = {
                if (recording) viewModel.stopRecording(streamer)
                else viewModel.startRecording(streamer)
              },
              onEdit = { editTarget = streamer },
              onClick = {
                if (inSelectMode) {
                  selectedIds =
                    if (isSelected) selectedIds - streamer.id else selectedIds + streamer.id
                }
              },
              onLongClick = { selectedIds = selectedIds + streamer.id },
            )
          }
          item { Spacer(Modifier.height(80.dp)) }
        }
      }
    }

    // ── FAB ───────────────────────────────────────────────────────────
    AnimatedVisibility(
      visible = !inSelectMode,
      enter = scaleIn(),
      exit = scaleOut(),
      modifier = Modifier.align(Alignment.BottomEnd),
    ) {
      Button(
        onClick = { showAddSheet = true },
        modifier = Modifier.padding(20.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
      ) {
        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Add Streamer", fontWeight = FontWeight.Bold)
      }
    }

    // ── Snackbar for backup results ───────────────────────────────────
    backupResult?.let { msg ->
      Snackbar(
        modifier =
          Modifier.align(Alignment.BottomCenter)
            .padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
        action = { TextButton(onClick = { viewModel.clearBackupResult() }) { Text("OK") } },
      ) {
        Text(msg)
      }
    }
  }

  // ── Add sheet ─────────────────────────────────────────────────────────────
  if (showAddSheet) {
    ModalBottomSheet(
      onDismissRequest = {
        showAddSheet = false
        focusManager.clearFocus()
      },
      sheetState = addSheetState,
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      dragHandle = {
        SheetHeader("ADD STREAMER", "Track a new channel") {
          showAddSheet = false
          focusManager.clearFocus()
        }
      },
    ) {
      AddPanel(
        presets = presets,
        onSave = { site, username, autoRecord, presetId, formatSelector ->
          viewModel.addStreamer(site, username, autoRecord, presetId, formatSelector)
          showAddSheet = false
          focusManager.clearFocus()
        },
      )
    }
  }

  // ── Edit sheet ────────────────────────────────────────────────────────────
  editTarget?.let { streamer ->
    ModalBottomSheet(
      onDismissRequest = {
        editTarget = null
        focusManager.clearFocus()
      },
      sheetState = editSheetState,
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      dragHandle = {
        SheetHeader(
          "EDIT  ·  ${streamer.username}",
          streamer.site.replaceFirstChar { it.uppercase() },
        ) {
          editTarget = null
          focusManager.clearFocus()
        }
      },
    ) {
      EditPanel(
        streamer = streamer,
        presets = presets,
        onSave = { autoRecord, presetId, formatSelector ->
          viewModel.updateStreamerSettings(streamer.id, autoRecord, presetId, formatSelector)
          editTarget = null
          focusManager.clearFocus()
        },
      )
    }
  }
}

// ── Search + filter bar ───────────────────────────────────────────────────────

@Composable
private fun SearchFilterBar(
  query: String,
  onQueryChange: (String) -> Unit,
  filterSite: String?,
  onSiteFilter: (String?) -> Unit,
  filterStatus: String?,
  onStatusFilter: (String?) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search username…", fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
          if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
              Icon(Icons.Default.Close, "Clear", modifier = Modifier.size(16.dp))
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      // Site chips
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Site:",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.CenterVertically),
        )
        listOf(null to "All", "chaturbate" to "CB", "camsoda" to "CS").forEach { (value, label) ->
          FilterChip(
            selected = filterSite == value,
            onClick = { onSiteFilter(value) },
            label = { Text(label, fontSize = 12.sp) },
          )
        }

        Spacer(Modifier.weight(1f))

        // Status chips
        Text(
          "Status:",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.CenterVertically),
        )
        listOf(null to "All", "public" to "Live", "offline" to "Offline").forEach { (value, label)
          ->
          FilterChip(
            selected = filterStatus == value,
            onClick = { onStatusFilter(value) },
            label = { Text(label, fontSize = 12.sp) },
          )
        }
      }
    }
  }
}

// ── Top bars ──────────────────────────────────────────────────────────────────

@Composable
private fun SelectionTopBar(
  count: Int,
  onSelectAll: () -> Unit,
  onCancel: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(color = MaterialTheme.colorScheme.errorContainer, tonalElevation = 2.dp) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(onClick = onCancel) {
        Icon(Icons.Default.Close, "Cancel", tint = MaterialTheme.colorScheme.onErrorContainer)
      }
      Text(
        "$count selected",
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = onSelectAll,
        colors =
          ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onErrorContainer
          ),
      ) {
        Text("All")
      }
      Button(
        onClick = onDelete,
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      ) {
        Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("Delete", fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun NormalTopBar(
  streamerCount: Int,
  liveCount: Int,
  recordingCount: Int,
  isMonitoring: Boolean,
  showFilterBar: Boolean,
  onMonitorClick: () -> Unit,
  onFilterClick: () -> Unit,
  onExport: () -> Unit,
  onImport: () -> Unit,
) {
  var showMenu by remember { mutableStateOf(false) }

  Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 2.dp) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            "GEAPER",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 4.sp,
          )
          Text(
            "Stream Monitor",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Filter toggle
          IconButton(onClick = onFilterClick) {
            Icon(
              Icons.Default.FilterList,
              "Filter",
              tint =
                if (showFilterBar) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Overflow menu (backup)
          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(
                Icons.Default.MoreVert,
                "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
              DropdownMenuItem(
                text = { Text("Export backup") },
                onClick = {
                  showMenu = false
                  onExport()
                },
              )
              DropdownMenuItem(
                text = { Text("Import backup") },
                onClick = {
                  showMenu = false
                  onImport()
                },
              )
            }
          }

          FilledTonalButton(
            onClick = onMonitorClick,
            shape = RoundedCornerShape(12.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor =
                  if (isMonitoring) MaterialTheme.colorScheme.errorContainer
                  else MaterialTheme.colorScheme.primaryContainer,
                contentColor =
                  if (isMonitoring) MaterialTheme.colorScheme.onErrorContainer
                  else MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          ) {
            Icon(
              if (isMonitoring) Icons.Default.Close else Icons.Default.PlayArrow,
              null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              if (isMonitoring) "Stop" else "Monitor",
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      Spacer(Modifier.height(14.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(Modifier.height(12.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        StatChip("TRACKED", streamerCount.toString())
        StatChip("LIVE NOW", liveCount.toString(), highlight = liveCount > 0)
        if (recordingCount > 0) {
          StatChip("REC", recordingCount.toString(), highlight = true, highlightColor = RecordRed)
        }
      }
    }
  }
}

@Composable
private fun StatChip(
  label: String,
  value: String,
  highlight: Boolean = false,
  highlightColor: Color = MaterialTheme.colorScheme.primary,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(
      value,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = if (highlight) highlightColor else MaterialTheme.colorScheme.onSurface,
    )
    Text(
      label,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      letterSpacing = 1.sp
    )
  }
}

// ── Sheet header ──────────────────────────────────────────────────────────────

@Composable
private fun SheetHeader(
  title: String,
  subtitle: String,
  onClose: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column {
      Text(
        title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
      )
      Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    IconButton(
      onClick = onClose,
      modifier =
        Modifier.size(34.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
      Icon(
        Icons.Default.Close,
        "Close",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp),
      )
    }
  }
}

// ── Add panel ─────────────────────────────────────────────────────────────────

@Composable
private fun AddPanel(
  presets: List<FfmpegPreset>,
  onSave:
    (
      site: String, username: String, autoRecord: Boolean, presetId: Int?, formatSelector: String
    ) -> Unit,
) {
  val sites = listOf("chaturbate", "camsoda")
  var username by remember { mutableStateOf("") }
  var selectedSite by remember { mutableStateOf(sites[0]) }
  var autoRecord by remember { mutableStateOf(false) }
  var selectedPreset by remember { mutableStateOf<FfmpegPreset?>(null) }
  var selectedFormat by remember { mutableStateOf(FORMAT_OPTIONS[0]) }
  var customSelector by remember { mutableStateOf("") }

  val resolvedFormat =
    if (selectedFormat.selector == "__custom__") customSelector.trim() else selectedFormat.selector

  FormContent(
    sites = sites,
    selectedSite = selectedSite,
    onSiteChange = { selectedSite = it },
    showUsernameField = true,
    username = username,
    onUsernameChange = { username = it },
    autoRecord = autoRecord,
    onAutoRecordChange = { autoRecord = it },
    presets = presets,
    selectedPreset = selectedPreset,
    onPresetChange = { selectedPreset = it },
    selectedFormat = selectedFormat,
    onFormatChange = { selectedFormat = it },
    customSelector = customSelector,
    onCustomSelectorChange = { customSelector = it },
    canSave = username.isNotBlank(),
    saveLabel = "Add Streamer",
    onSave = {
      onSave(selectedSite, username.trim(), autoRecord, selectedPreset?.id, resolvedFormat)
    },
  )
}

// ── Edit panel ────────────────────────────────────────────────────────────────

@Composable
private fun EditPanel(
  streamer: Streamer,
  presets: List<FfmpegPreset>,
  onSave: (autoRecord: Boolean, presetId: Int?, formatSelector: String) -> Unit,
) {
  var autoRecord by remember { mutableStateOf(streamer.autoRecord) }
  var selectedPreset by remember { mutableStateOf<FfmpegPreset?>(null) }

  val initialFormat =
    FORMAT_OPTIONS.firstOrNull { it.selector == streamer.formatSelector }
      ?: if (streamer.formatSelector.isBlank()) FORMAT_OPTIONS[0] else FORMAT_OPTIONS.last()
  var selectedFormat by remember { mutableStateOf(initialFormat) }
  var customSelector by remember {
    mutableStateOf(if (initialFormat.selector == "__custom__") streamer.formatSelector else "")
  }

  LaunchedEffect(presets) {
    if (selectedPreset == null && streamer.ffmpegPresetId != null) {
      selectedPreset = presets.find { it.id == streamer.ffmpegPresetId }
    }
  }

  val resolvedFormat =
    if (selectedFormat.selector == "__custom__") customSelector.trim() else selectedFormat.selector

  FormContent(
    sites = listOf(streamer.site),
    selectedSite = streamer.site,
    onSiteChange = {},
    showUsernameField = false,
    username = streamer.username,
    onUsernameChange = {},
    autoRecord = autoRecord,
    onAutoRecordChange = { autoRecord = it },
    presets = presets,
    selectedPreset = selectedPreset,
    onPresetChange = { selectedPreset = it },
    selectedFormat = selectedFormat,
    onFormatChange = { selectedFormat = it },
    customSelector = customSelector,
    onCustomSelectorChange = { customSelector = it },
    canSave = true,
    saveLabel = "Save Changes",
    onSave = { onSave(autoRecord, selectedPreset?.id, resolvedFormat) },
  )
}

// ── Shared form ───────────────────────────────────────────────────────────────

@Composable
private fun FormContent(
  sites: List<String>,
  selectedSite: String,
  onSiteChange: (String) -> Unit,
  showUsernameField: Boolean,
  username: String,
  onUsernameChange: (String) -> Unit,
  autoRecord: Boolean,
  onAutoRecordChange: (Boolean) -> Unit,
  presets: List<FfmpegPreset>,
  selectedPreset: FfmpegPreset?,
  onPresetChange: (FfmpegPreset?) -> Unit,
  selectedFormat: FormatOption,
  onFormatChange: (FormatOption) -> Unit,
  customSelector: String,
  onCustomSelectorChange: (String) -> Unit,
  canSave: Boolean,
  saveLabel: String,
  onSave: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(2.dp))

    // Platform tiles
    Text(
      "PLATFORM",
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      letterSpacing = 1.5.sp
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      sites.forEach { site ->
        val sel = site == selectedSite
        Surface(
          onClick = { if (sites.size > 1) onSiteChange(site) },
          enabled = sites.size > 1,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp),
          color =
            if (sel) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
          border = if (sel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
        ) {
          Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              if (site == "chaturbate") "CB" else "CS",
              fontSize = 20.sp,
              fontWeight = FontWeight.Black,
              color =
                if (sel) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              site.replaceFirstChar { it.uppercase() },
              fontSize = 11.sp,
              fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
              color =
                if (sel) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // Username
    if (showUsernameField) {
      Text(
        "USERNAME",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp
      )
      OutlinedTextField(
        value = username,
        onValueChange = onUsernameChange,
        placeholder = {
          Text(
            "e.g. streamer_name",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
          )
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (canSave) onSave() }),
      )
    }

    // Format tiles
    Text(
      "FORMAT  (-f)",
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      letterSpacing = 1.5.sp
    )
    FORMAT_OPTIONS.chunked(3).forEach { row ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { option ->
          val sel = selectedFormat == option
          Surface(
            onClick = { onFormatChange(option) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            color =
              if (sel) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.surfaceContainer,
            border = if (sel) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Text(
                option.label,
                fontSize = 12.sp,
                fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                color =
                  if (sel) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                softWrap = false,
                maxLines = 1,
              )
              Text(
                option.hint,
                fontSize = 9.sp,
                color =
                  if (sel) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
      }
    }

    if (selectedFormat.selector == "__custom__") {
      OutlinedTextField(
        value = customSelector,
        onValueChange = onCustomSelectorChange,
        label = { Text("Custom -f value") },
        placeholder = {
          Text(
            "bestvideo[height<=1080]+bestaudio/best",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
      )
    }

    // Auto-record toggle
    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            "Auto Record",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            "Start recording when streamer goes live.\nAuto-stops when they go offline.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(checked = autoRecord, onCheckedChange = onAutoRecordChange)
      }
    }

    // Extra-flags preset tiles
    if (presets.isNotEmpty()) {
      Text(
        "EXTRA FLAGS PRESET",
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp
      )
      val allOptions: List<FfmpegPreset?> = listOf(null) + presets
      allOptions.chunked(3).forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          row.forEach { preset ->
            val sel = selectedPreset == preset
            Surface(
              onClick = { onPresetChange(preset) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              color =
                if (sel) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
              border = if (sel) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  preset?.name ?: "None",
                  fontSize = 12.sp,
                  fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                  color =
                    if (sel) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  if (preset == null) "No extra flags" else preset.extraArgs.ifBlank { "No flags" },
                  fontSize = 9.sp,
                  color =
                    if (sel) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
          repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
      }
    }

    Spacer(Modifier.height(4.dp))

    Button(
      onClick = { if (canSave) onSave() },
      enabled = canSave,
      modifier = Modifier.fillMaxWidth().height(52.dp),
      shape = RoundedCornerShape(14.dp),
    ) {
      Text(saveLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
  }
}

// ── Streamer card ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StreamerCard(
  streamer: Streamer,
  isRecording: Boolean,
  isSelected: Boolean,
  inSelectMode: Boolean,
  onRecord: () -> Unit,
  onEdit: () -> Unit,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val sc = statusColor(streamer.status)
  val sl = statusLabel(streamer.status)

  Card(
    modifier =
      Modifier.fillMaxWidth()
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .then(
          when {
            isSelected ->
              Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            isRecording ->
              Modifier.border(1.5.dp, RecordRed.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            else -> Modifier
          },
        ),
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          else MaterialTheme.colorScheme.surfaceContainerHigh,
      ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      AnimatedVisibility(visible = inSelectMode) {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { onClick() },
          modifier = Modifier.padding(end = 8.dp),
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.weight(1f),
      ) {
        // Avatar
        Box(
          modifier =
            Modifier.size(42.dp)
              .clip(CircleShape)
              .background(
                Brush.radialGradient(listOf(sc.copy(alpha = 0.35f), sc.copy(alpha = 0.08f)))
              ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            streamer.username.first().uppercaseChar().toString(),
            color = sc,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
          // Name row with badges
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              streamer.username,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (isRecording) RecordingBadge()
            if (streamer.autoRecord) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
              ) {
                Text(
                  "AUTO",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  letterSpacing = 0.5.sp,
                  softWrap = false,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
              }
            }
            if (streamer.formatSelector.isNotBlank()) {
              val fmtLabel =
                FORMAT_OPTIONS.firstOrNull { it.selector == streamer.formatSelector }?.label
                  ?: "Custom"
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
              ) {
                Text(
                  fmtLabel,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  letterSpacing = 0.5.sp,
                  softWrap = false,
                  maxLines = 1,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                )
              }
            }
          }
          // Status row
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
          ) {
            PulseDot(sc)
            Text(
              sl,
              color = sc,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Text(
              streamer.site.replaceFirstChar { it.uppercase() },
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 10.sp,
            )
          }
        }
      }

      AnimatedVisibility(visible = !inSelectMode) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          IconButton(
            onClick = onRecord,
            modifier = Modifier.size(36.dp),
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor =
                  if (isRecording) MaterialTheme.colorScheme.errorContainer
                  else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor =
                  if (isRecording) MaterialTheme.colorScheme.onErrorContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              ),
          ) {
            Icon(
              if (isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
              if (isRecording) "Stop" else "Record",
              modifier = Modifier.size(17.dp),
            )
          }
          IconButton(
            onClick = onEdit,
            modifier = Modifier.size(36.dp),
            colors =
              IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              ),
          ) {
            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(17.dp))
          }
        }
      }
    }
  }
}

// ── Misc composables ──────────────────────────────────────────────────────────

@Composable
private fun RecordingBadge() {
  val t = rememberInfiniteTransition(label = "rec")
  val a by t.animateFloat(1f, 0.3f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
  Surface(shape = RoundedCornerShape(4.dp), color = RecordRed.copy(alpha = 0.15f)) {
    Row(
      modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
      Box(Modifier.size(5.dp).clip(CircleShape).background(RecordRed.copy(alpha = a)))
      Text(
        "REC",
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = RecordRed,
        letterSpacing = 0.5.sp,
        softWrap = false,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun PulseDot(color: Color) {
  val t = rememberInfiniteTransition(label = "pulse")
  val a by
    t.animateFloat(
      1f,
      0.3f,
      infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
      label = "a",
    )
  Box(Modifier.size(7.dp).clip(CircleShape).background(color.copy(alpha = a)))
}

@Composable
private fun EmptyState(
  hasStreamers: Boolean,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(if (hasStreamers) "🔍" else "📡", fontSize = 40.sp)
      Text(
        if (hasStreamers) "No results" else "No streamers tracked",
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
      )
      Text(
        if (hasStreamers) "Try changing your search or filters" else "Tap + to add one",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
      )
    }
  }
}
