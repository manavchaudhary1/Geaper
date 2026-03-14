package com.manav.geaper.data.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
  prettyPrint = true
  ignoreUnknownKeys = true
}

@Serializable
data class GeaperBackup(
  val version: Int = 1,
  val exportedAt: String = "",
  val streamers: List<StreamerBackup> = emptyList(),
  val presets: List<PresetBackup> = emptyList(),
)

@Serializable
data class StreamerBackup(
  val site: String,
  val username: String,
  val autoRecord: Boolean,
  val formatSelector: String,
  // preset resolved by name so import works across devices with different DB ids
  val presetName: String?,
)

@Serializable
data class PresetBackup(
  val name: String,
  val extraArgs: String,
)

object BackupManager {

  private const val TAG = "GeaperBackup"

  // ── Export ────────────────────────────────────────────────────────────────

  fun export(
    context: Context,
    uri: Uri,
    streamers: List<Streamer>,
    presets: List<FfmpegPreset>,
  ): Result<Unit> = runCatching {
    val presetById = presets.associateBy { it.id }

    val backup =
      GeaperBackup(
        version = 1,
        exportedAt = Instant.now().toString(),
        streamers =
          streamers.map { s ->
            StreamerBackup(
              site = s.site,
              username = s.username,
              autoRecord = s.autoRecord,
              formatSelector = s.formatSelector,
              presetName = s.ffmpegPresetId?.let { presetById[it]?.name },
            )
          },
        presets = presets.map { p -> PresetBackup(p.name, p.extraArgs) },
      )

    context.contentResolver.openOutputStream(uri)?.use { out ->
      out.write(json.encodeToString(backup).toByteArray())
    } ?: error("Could not open output stream for $uri")

    Log.d(TAG, "Exported ${streamers.size} streamers + ${presets.size} presets to $uri")
  }

  // ── Import ────────────────────────────────────────────────────────────────

  fun import(context: Context, uri: Uri): Result<GeaperBackup> = runCatching {
    val raw =
      context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: error("Could not open input stream for $uri")
    val backup = json.decodeFromString<GeaperBackup>(raw)
    Log.d(
      TAG,
      "Parsed backup v${backup.version}: ${backup.streamers.size} streamers, ${backup.presets.size} presets"
    )
    backup
  }
}
