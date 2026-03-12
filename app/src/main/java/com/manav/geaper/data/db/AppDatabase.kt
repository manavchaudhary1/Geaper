package com.manav.geaper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.manav.geaper.data.model.FfmpegPreset
import com.manav.geaper.data.model.Streamer

@Database(
    entities = [Streamer::class, FfmpegPreset::class],
    version  = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun streamerDao(): StreamerDao
    abstract fun ffmpegPresetDao(): FfmpegPresetDao

    companion object {
        /**
         * v1 → v2:
         *  - Add `autoRecord` (INTEGER, default 0) to streamers
         *  - Add `ffmpegPresetId` (INTEGER, nullable) to streamers
         *  - Create ffmpeg_presets table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // New columns on streamers
                db.execSQL("ALTER TABLE streamers ADD COLUMN autoRecord INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE streamers ADD COLUMN ffmpegPresetId INTEGER")

                // New table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ffmpeg_presets` (
                        `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name`        TEXT    NOT NULL,
                        `args`        TEXT    NOT NULL,
                        `description` TEXT    NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }
    }
}