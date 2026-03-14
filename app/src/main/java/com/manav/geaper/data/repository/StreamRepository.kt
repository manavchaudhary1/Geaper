package com.manav.geaper.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.manav.geaper.data.backup.BackupManager
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

  val streamers: Flow<List<Streamer>> = dao.getAll()
  val presets: Flow<List<FfmpegPreset>> = presetDao.getAll()

  // ── Recording state ───────────────────────────────────────────────────────
  // Tracks all active recordings
  private val _recording = mutableSetOf<String>()
  // Tracks recordings started MANUALLY (not auto-record flag)
  // These will NOT auto-stop when streamer goes offline
  private val _manualRecording = mutableSetOf<String>()

  fun isRecording(site: String, username: String) = "$site-$username" in _recording

  // ── Streamer CRUD ─────────────────────────────────────────────────────────

  suspend fun addStreamer(
    site: String,
    username: String,
    autoRecord: Boolean = false,
    ffmpegPresetId: Int? = null,
    formatSelector: String = "",
  ) {
    dao.insert(
      Streamer(
        site = site,
        username = username,
        status = "offline",
        flag = null,
        autoRecord = autoRecord,
        ffmpegPresetId = ffmpegPresetId,
        formatSelector = formatSelector,
      ),
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

  // ── Preset CRUD ───────────────────────────────────────────────────────────

  suspend fun addPreset(preset: FfmpegPreset) = presetDao.insert(preset)

  suspend fun deletePreset(preset: FfmpegPreset) = presetDao.delete(preset)

  suspend fun updatePreset(preset: FfmpegPreset) = presetDao.update(preset)

  // ── Monitoring ────────────────────────────────────────────────────────────

  suspend fun updateStatuses(streamers: List<Streamer>) {
    Log.d(TAG, "Monitoring cycle — ${streamers.size} streamers")
    val wmToken = prefs.cbWmToken.first()

    val cbStatus = HashMap<String, String>()
    cbApi.getOnlineRooms(wmToken).forEach { cbStatus[it.username] = it.current_show ?: "offline" }

    streamers
      .filter { it.site == "chaturbate" }
      .forEach { streamer ->
        handleStatusChange(streamer, cbStatus[streamer.username] ?: "offline")
      }
    updateCamsoda(streamers)

    Log.d(TAG, "Monitoring cycle finished")
    debugPrintDb()
  }

  private suspend fun updateCamsoda(streamers: List<Streamer>) {
    coroutineScope {
      streamers
        .filter { it.site == "camsoda" }
        .map { streamer ->
          async(Dispatchers.IO) { handleStatusChange(streamer, csApi.getStatus(streamer.username)) }
        }
        .awaitAll()
    }
  }

  private suspend fun handleStatusChange(streamer: Streamer, newStatus: String) {
    val oldStatus = streamer.status
    if (oldStatus == newStatus) return

    Log.d(TAG, "${streamer.site}/${streamer.username}: $oldStatus → $newStatus")
    dao.updateStatus(streamer.site, streamer.username, newStatus)
    NotificationHelper.notifyStatusChange(
      context,
      streamer.username,
      streamer.site,
      oldStatus,
      newStatus
    )

    val key = "${streamer.site}-${streamer.username}"
    val goingLive = isLive(newStatus) && !isLive(oldStatus)
    val goingOffline = !isLive(newStatus) && isLive(oldStatus)

    // Auto-record: start when live
    if (goingLive && streamer.autoRecord && key !in _recording) {
      startRecording(streamer, manual = false)
    }

    // Auto-record going offline: only stop if this was NOT a manual recording
    if (goingOffline && key in _recording && key !in _manualRecording) {
      Log.d(TAG, "Auto-stopping recording for $key (went offline, was auto-started)")
      stopRecording(streamer.site, streamer.username)
    }

    // If manually recording and streamer goes live again — already recording, no-op needed
    // If manually recording and goes offline — keep going (intentional, user will stop manually)
    if (goingLive && key in _manualRecording) {
      Log.d(TAG, "Manual recording continuing through status change for $key")
    }
  }

  // ── Recording ─────────────────────────────────────────────────────────────

  /**
   * @param manual true = started by user tap (survives offline transitions) false = started by
   *   autoRecord flag (stops when streamer goes offline)
   */
  fun startRecording(streamer: Streamer, manual: Boolean = true) {
    val key = "${streamer.site}-${streamer.username}"
    if (key in _recording) return
    _recording += key
    if (manual) _manualRecording += key

    CoroutineScope(Dispatchers.IO).launch {
      val savePath = safUriToPath(context, prefs.savePath.first())
      val segmentMins = prefs.segmentMinutes.first()
      val extraArgs =
        streamer.ffmpegPresetId?.let { id ->
          presetDao.getAll().first().find { it.id == id }?.extraArgs
        } ?: ""

      Log.d(
        TAG,
        "Recording $key | manual=$manual | format='${streamer.formatSelector}' extra='$extraArgs'",
      )
      RecordingService.start(context, streamer.username)
      try {
        StreamRecorder.startRecording(
          context = context,
          outputDir = savePath,
          site = streamer.site,
          username = streamer.username,
          segmentMinutes = segmentMins,
          formatSelector = streamer.formatSelector,
          extraArgs = extraArgs,
        )
      } finally {
        RecordingService.stop(context)
        _recording -= key
        _manualRecording -= key
        Log.d(TAG, "Recording coroutine finished for $key")
      }
    }
  }

  fun stopRecording(site: String, username: String) {
    val key = "$site-$username"
    // StreamRecorder.stopRecording sets a flag + kills yt-dlp, then lets
    // the coroutine finish salvage naturally — do NOT cancel the job here
    StreamRecorder.stopRecording(site, username)
    _recording -= key
    _manualRecording -= key
  }

  // ── Backup / Restore ──────────────────────────────────────────────────────

  suspend fun exportBackup(uri: Uri): Result<Unit> {
    val allStreamers = dao.getAll().first()
    val allPresets = presetDao.getAll().first()
    return BackupManager.export(context, uri, allStreamers, allPresets)
  }

  suspend fun importBackup(uri: Uri): Result<ImportSummary> = runCatching {
    val backup = BackupManager.import(context, uri).getOrThrow()

    // Upsert presets first (so we can resolve preset IDs for streamers)
    val existingPresets = presetDao.getAll().first()
    val existingByName = existingPresets.associateBy { it.name }

    val nameToId = mutableMapOf<String, Int>()
    backup.presets.forEach { pb ->
      val existing = existingByName[pb.name]
      if (existing != null) {
        presetDao.update(existing.copy(extraArgs = pb.extraArgs))
        nameToId[pb.name] = existing.id
      } else {
        val newId = presetDao.insert(FfmpegPreset(name = pb.name, extraArgs = pb.extraArgs))
        nameToId[pb.name] = newId.toInt()
      }
    }

    // Upsert streamers
    val existingStreamers = dao.getAll().first()
    val existingKeys = existingStreamers.map { "${it.site}-${it.username}" }.toSet()
    var added = 0
    var updated = 0
    backup.streamers.forEach { sb ->
      val resolvedPresetId = sb.presetName?.let { nameToId[it] }
      val key = "${sb.site}-${sb.username}"
      if (key in existingKeys) {
        val existing = existingStreamers.first { "${it.site}-${it.username}" == key }
        dao.updateSettings(existing.id, sb.autoRecord, resolvedPresetId, sb.formatSelector)
        updated++
      } else {
        dao.insert(
          Streamer(
            site = sb.site,
            username = sb.username,
            status = "offline",
            flag = null,
            autoRecord = sb.autoRecord,
            ffmpegPresetId = resolvedPresetId,
            formatSelector = sb.formatSelector,
          ),
        )
        added++
      }
    }

    Log.d(
      TAG,
      "Import done: $added added, $updated updated streamers; ${backup.presets.size} presets processed",
    )
    ImportSummary(
      streamersAdded = added,
      streamersUpdated = updated,
      presetsProcessed = backup.presets.size,
    )
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private fun isLive(status: String) = status.lowercase() in listOf("public", "online")

  private suspend fun debugPrintDb() {
    dao.getAll().first().forEach {
      Log.d(TAG, "DB -> ${it.site}/${it.username} status=${it.status}")
    }
  }
}

data class ImportSummary(
  val streamersAdded: Int,
  val streamersUpdated: Int,
  val presetsProcessed: Int,
)
