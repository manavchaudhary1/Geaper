package com.manav.geaper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer

@Database(
    entities = [Streamer::class, FfmpegPreset::class],
    version  = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streamerDao(): StreamerDao
    abstract fun ffmpegPresetDao(): FfmpegPresetDao
}