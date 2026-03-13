package com.manav.geaper.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streamers")
data class Streamer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val site: String,
    val username: String,
    val status: String,
    val flag: String?,
    /** If true, recording starts automatically when streamer goes live. */
    val autoRecord: Boolean = false,
    /** FK reference to FfmpegPreset.id for extra yt-dlp flags; null = none. */
    val ffmpegPresetId: Int? = null,
    /**
     * yt-dlp -f format selector stored per-streamer.
     * Empty string → use app default (bestvideo+bestaudio/best).
     * Examples: "bestvideo[height<=720]+bestaudio/best[height<=720]"
     */
    val formatSelector: String = "",
)