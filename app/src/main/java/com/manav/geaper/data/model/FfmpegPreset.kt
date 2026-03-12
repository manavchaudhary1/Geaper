package com.manav.geaper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named FFmpeg post-processor argument preset that users create on the
 * Custom Script page and then select when adding/editing a streamer.
 *
 * Example:
 *   name = "720p re-encode"
 *   args = "-vf scale=-2:720 -c:v libx264 -crf 23"
 */
@Entity(tableName = "ffmpeg_presets")
data class FfmpegPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val args: String,
    val description: String = ""
)