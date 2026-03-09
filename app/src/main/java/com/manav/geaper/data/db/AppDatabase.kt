package com.manav.geaper.data.db;

import androidx.room.Database
import androidx.room.RoomDatabase
import com.manav.geaper.data.model.Streamer

@Database(
    entities = [Streamer::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun streamerDao(): StreamerDao

}