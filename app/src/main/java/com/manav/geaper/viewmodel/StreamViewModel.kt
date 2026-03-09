package com.manav.geaper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.data.repository.StreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StreamViewModel(
    private val repo: StreamRepository,
) : ViewModel() {

    val streamers = repo.streamers

    private var monitoringJob: Job? = null

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    fun addStreamer(site: String, username: String) {
        viewModelScope.launch {
            repo.addStreamer(site, username)
        }
    }

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        monitoringJob = viewModelScope.launch(Dispatchers.IO) {
            _isMonitoring.value = true
            while (isActive) {
                val list = repo.streamers.first()
                repo.updateStatuses(list)
                delay(10_000)
            }
        }.also { job ->
            job.invokeOnCompletion { _isMonitoring.value = false }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoring.value = false
    }

    fun toggleMonitoring() {
        if (_isMonitoring.value) stopMonitoring() else startMonitoring()
    }

    fun removeStreamer(streamer: Streamer) {
        viewModelScope.launch {
            repo.removeStreamer(streamer)
        }
    }
}