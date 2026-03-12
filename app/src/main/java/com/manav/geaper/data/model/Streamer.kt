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
    /** FK reference to FfmpegPreset.id; null = no preset / plain recording. */
    val ffmpegPresetId: Int? = null
)