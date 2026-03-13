package com.manav.geaper.data.repository

import android.content.Context
import android.util.Log
import com.manav.geaper.data.db.FfmpegPresetDao
import com.manav.geaper.data.db.StreamerDao
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.data.prefs.AppPreferences
import com.manav.geaper.data.prefs.safUriToPath
import com.manav.geaper.network.CamsodaApi
import com.manav.geaper.network.ChaturbateApi
import com.manav.geaper.notification.NotificationHelper
import com.manav.geaper.recorder.StreamRecorder
import com.manav.geaper.service.RecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StreamRepository(
    private val context: Context,
    private val dao: StreamerDao,
    private val presetDao: FfmpegPresetDao,
    private val cbApi: ChaturbateApi,
    private val csApi: CamsodaApi,
    private val prefs: AppPreferences,
) {
    private val TAG = "GeaperMonitor"

    val streamers: Flow<List<Streamer>>     = dao.getAll()
    val presets:   Flow<List<FfmpegPreset>> = presetDao.getAll()

    // ---------- recording state ----------
    private val _recording = mutableSetOf<String>()
    fun isRecording(site: String, username: String) = "$site-$username" in _recording

    // ---------- streamer CRUD ----------

    suspend fun addStreamer(
        site: String,
        username: String,
        autoRecord: Boolean = false,
        ffmpegPresetId: Int? = null,
        formatSelector: String = "",
    ) {
        dao.insert(
            Streamer(
                site           = site,
                username       = username,
                status         = "offline",
                flag           = null,
                autoRecord     = autoRecord,
                ffmpegPresetId = ffmpegPresetId,
                formatSelector = formatSelector,
            )
        )
    }

    suspend fun updateStreamerSettings(
        id: Int,
        autoRecord: Boolean,
        ffmpegPresetId: Int?,
        formatSelector: String,
    ) {
        dao.updateSettings(id, autoRecord, ffmpegPresetId, formatSelector)
    }

    suspend fun removeStreamer(streamer: Streamer) {
        stopRecording(streamer.site, streamer.username)
        dao.delete(streamer)
    }

    // ---------- preset CRUD ----------

    suspend fun addPreset(preset: FfmpegPreset)    = presetDao.insert(preset)
    suspend fun deletePreset(preset: FfmpegPreset) = presetDao.delete(preset)
    suspend fun updatePreset(preset: FfmpegPreset) = presetDao.update(preset)

    // ---------- monitoring ----------

    suspend fun updateStatuses(streamers: List<Streamer>) {
        Log.d(TAG, "Monitoring cycle — ${streamers.size} streamers")
        val wmToken = prefs.cbWmToken.first()

        val cbStatus = HashMap<String, String>()
        cbApi.getOnlineRooms(wmToken).forEach { cbStatus[it.username] = it.current_show ?: "offline" }

        streamers.filter { it.site == "chaturbate" }.forEach { streamer ->
            handleStatusChange(streamer, cbStatus[streamer.username] ?: "offline")
        }
        updateCamsoda(streamers)

        Log.d(TAG, "Monitoring cycle finished")
        debugPrintDb()
    }

    private suspend fun updateCamsoda(streamers: List<Streamer>) {
        coroutineScope {
            streamers.filter { it.site == "camsoda" }.map { streamer ->
                async(Dispatchers.IO) {
                    handleStatusChange(streamer, csApi.getStatus(streamer.username))
                }
            }.awaitAll()
        }
    }

    private suspend fun handleStatusChange(streamer: Streamer, newStatus: String) {
        val oldStatus = streamer.status
        if (oldStatus == newStatus) return

        Log.d(TAG, "${streamer.site}/${streamer.username}: $oldStatus → $newStatus")
        dao.updateStatus(streamer.site, streamer.username, newStatus)
        NotificationHelper.notifyStatusChange(context, streamer.username, streamer.site, oldStatus, newStatus)

        if (isLive(newStatus) && !isLive(oldStatus) && streamer.autoRecord) startRecording(streamer)
        if (!isLive(newStatus) && isLive(oldStatus))                         stopRecording(streamer.site, streamer.username)
    }

    // ---------- recording ----------

    fun startRecording(streamer: Streamer) {
        val key = "${streamer.site}-${streamer.username}"
        if (key in _recording) return
        _recording += key

        CoroutineScope(Dispatchers.IO).launch {
            val savePath    = safUriToPath(context, prefs.savePath.first())
            val segmentMins = prefs.segmentMinutes.first()
            // Format comes from the streamer; extra flags come from the preset
            val extraArgs   = streamer.ffmpegPresetId
                ?.let { id -> presetDao.getAll().first().find { it.id == id }?.extraArgs }
                ?: ""

            Log.d(TAG, "Recording $key | format='${streamer.formatSelector}' extra='$extraArgs'")
            RecordingService.start(context, streamer.username)
            try {
                StreamRecorder.startRecording(
                    context        = context,
                    outputDir      = savePath,
                    site           = streamer.site,
                    username       = streamer.username,
                    segmentMinutes = segmentMins,
                    formatSelector = streamer.formatSelector,
                    extraArgs      = extraArgs,
                )
            } finally {
                RecordingService.stop(context)
                _recording -= key
                Log.d(TAG, "Recording finished for $key")
            }
        }
    }

    fun stopRecording(site: String, username: String) {
        StreamRecorder.stopRecording(site, username)
        _recording -= "$site-$username"
    }

    // ---------- helpers ----------

    private fun isLive(status: String) = status.lowercase() in listOf("public", "online")

    private suspend fun debugPrintDb() {
        dao.getAll().first().forEach {
            Log.d(TAG, "DB -> ${it.site}/${it.username} status=${it.status} format='${it.formatSelector}'")
        }
    }
}