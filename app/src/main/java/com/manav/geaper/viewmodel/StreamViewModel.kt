package com.manav.geaper.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.data.repository.StreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StreamViewModel(private val repo: StreamRepository) : ViewModel() {

  val streamers: StateFlow<List<Streamer>> =
    repo.streamers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val presets: StateFlow<List<FfmpegPreset>> =
    repo.presets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // ── Search / filter ───────────────────────────────────────────────────────

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _filterSite = MutableStateFlow<String?>(null) // null = all
  val filterSite: StateFlow<String?> = _filterSite.asStateFlow()

  private val _filterStatus = MutableStateFlow<String?>(null) // null = all
  val filterStatus: StateFlow<String?> = _filterStatus.asStateFlow()

  fun setSearchQuery(q: String) {
    _searchQuery.value = q
  }

  fun setFilterSite(site: String?) {
    _filterSite.value = site
  }

  fun setFilterStatus(status: String?) {
    _filterStatus.value = status
  }

  /**
   * Reactive filtered list — recomputes whenever streamers, query, site filter, or status filter
   * changes. Collect this in the UI directly.
   */
  val filteredStreamers: StateFlow<List<Streamer>> =
    combine(streamers, _searchQuery, _filterSite, _filterStatus) { all, q, site, status ->
        val query = q.trim().lowercase()
        all.filter { s ->
          (query.isEmpty() || s.username.lowercase().contains(query)) &&
            (site == null || s.site == site) &&
            (status == null || s.status.lowercase() == status)
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // ── Monitoring ────────────────────────────────────────────────────────────

  private var monitoringJob: Job? = null
  private val _isMonitoring = MutableStateFlow(false)
  val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

  fun startMonitoring() {
    if (monitoringJob?.isActive == true) return
    _isMonitoring.value = true
    monitoringJob =
      viewModelScope
        .launch(Dispatchers.IO) {
          while (isActive) {
            repo.updateStatuses(streamers.value)
            delay(15_000)
          }
        }
        .also { it.invokeOnCompletion { _isMonitoring.value = false } }
  }

  fun stopMonitoring() {
    monitoringJob?.cancel()
    monitoringJob = null
    _isMonitoring.value = false
  }

  fun toggleMonitoring() {
    if (_isMonitoring.value) stopMonitoring() else startMonitoring()
  }

  // ── Streamers ─────────────────────────────────────────────────────────────

  fun addStreamer(
    site: String,
    username: String,
    autoRecord: Boolean = false,
    ffmpegPresetId: Int? = null,
    formatSelector: String = "",
  ) {
    viewModelScope.launch {
      repo.addStreamer(site, username, autoRecord, ffmpegPresetId, formatSelector)
    }
  }

  fun removeStreamer(streamer: Streamer) {
    viewModelScope.launch { repo.removeStreamer(streamer) }
  }

  fun updateStreamerSettings(
    id: Int,
    autoRecord: Boolean,
    ffmpegPresetId: Int?,
    formatSelector: String,
  ) {
    viewModelScope.launch {
      repo.updateStreamerSettings(id, autoRecord, ffmpegPresetId, formatSelector)
    }
  }

  // ── Recording ─────────────────────────────────────────────────────────────

  fun isRecording(streamer: Streamer) = repo.isRecording(streamer.site, streamer.username)

  /** Manual start — survives the streamer going offline */
  fun startRecording(streamer: Streamer) {
    repo.startRecording(streamer, manual = true)
  }

  fun stopRecording(streamer: Streamer) {
    repo.stopRecording(streamer.site, streamer.username)
  }

  // ── Presets ───────────────────────────────────────────────────────────────

  fun addPreset(name: String, extraArgs: String = "") {
    viewModelScope.launch { repo.addPreset(FfmpegPreset(name = name, extraArgs = extraArgs)) }
  }

  fun deletePreset(preset: FfmpegPreset) {
    viewModelScope.launch { repo.deletePreset(preset) }
  }

  fun updatePreset(preset: FfmpegPreset) {
    viewModelScope.launch { repo.updatePreset(preset) }
  }

  // ── Backup ────────────────────────────────────────────────────────────────

  private val _backupResult = MutableStateFlow<String?>(null)
  val backupResult: StateFlow<String?> = _backupResult.asStateFlow()

  fun clearBackupResult() {
    _backupResult.value = null
  }

  fun exportBackup(uri: Uri) {
    viewModelScope.launch {
      repo
        .exportBackup(uri)
        .onSuccess { _backupResult.value = "✅ Backup exported successfully" }
        .onFailure { _backupResult.value = "❌ Export failed: ${it.message}" }
    }
  }

  fun importBackup(uri: Uri) {
    viewModelScope.launch {
      repo
        .importBackup(uri)
        .onSuccess { summary ->
          _backupResult.value =
            "✅ Import complete\n" +
              "${summary.streamersAdded} added · ${summary.streamersUpdated} updated · " +
              "${summary.presetsProcessed} presets"
        }
        .onFailure { _backupResult.value = "❌ Import failed: ${it.message}" }
    }
  }
}
