package com.manav.geaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.data.repository.StreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StreamViewModel(private val repo: StreamRepository) : ViewModel() {

    val streamers: StateFlow<List<Streamer>> =
        repo.streamers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val presets: StateFlow<List<FfmpegPreset>> =
        repo.presets.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Monitoring ────────────────────────────────────────────────────────────

    private var monitoringJob: Job? = null
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        _isMonitoring.value = true
        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) { repo.updateStatuses(streamers.value); delay(15_000) }
        }.also { it.invokeOnCompletion { _isMonitoring.value = false } }
    }

    fun stopMonitoring() { monitoringJob?.cancel(); monitoringJob = null; _isMonitoring.value = false }
    fun toggleMonitoring() { if (_isMonitoring.value) stopMonitoring() else startMonitoring() }

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
        viewModelScope.launch { repo.updateStreamerSettings(id, autoRecord, ffmpegPresetId, formatSelector) }
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    fun isRecording(streamer: Streamer) = repo.isRecording(streamer.site, streamer.username)
    fun startRecording(streamer: Streamer) { repo.startRecording(streamer) }
    fun stopRecording(streamer: Streamer)  { repo.stopRecording(streamer.site, streamer.username) }

    // ── Presets (extra-flags only) ────────────────────────────────────────────

    fun addPreset(name: String, extraArgs: String = "") {
        viewModelScope.launch { repo.addPreset(FfmpegPreset(name = name, extraArgs = extraArgs)) }
    }

    fun deletePreset(preset: FfmpegPreset) { viewModelScope.launch { repo.deletePreset(preset) } }
    fun updatePreset(preset: FfmpegPreset) { viewModelScope.launch { repo.updatePreset(preset) } }
}