package com.manav.geaper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named collection of extra raw yt-dlp flags appended verbatim to every recording that uses this
 * preset.
 *
 * Format selection (-f) is stored on the Streamer itself, not here, so one preset can be reused
 * across streamers with different quality targets.
 *
 * Example: name = "Fast fragments" extraArgs = "--concurrent-fragments 4 --throttled-rate 100K"
 */
@Entity(tableName = "ffmpeg_presets")
data class FfmpegPreset(
  @PrimaryKey(autoGenerate = true) val id: Int = 0,
  val name: String,
  /** Raw yt-dlp flags appended verbatim after the standard recording options. */
  val extraArgs: String = "",
)
