package com.manav.geaper.data.db

import androidx.room.*
import com.manav.geaper.data.model.Streamer
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamerDao {

    @Query("SELECT * FROM streamers")
    fun getAll(): Flow<List<Streamer>>

    @Insert
    suspend fun insert(streamer: Streamer)

    @Query("UPDATE streamers SET status = :status WHERE username = :username AND site = :site")
    suspend fun updateStatus(site: String, username: String, status: String)

    @Query("""
        UPDATE streamers
        SET autoRecord = :autoRecord,
            ffmpegPresetId = :ffmpegPresetId,
            formatSelector = :formatSelector
        WHERE id = :id
    """)
    suspend fun updateSettings(id: Int, autoRecord: Boolean, ffmpegPresetId: Int?, formatSelector: String)

    @Delete
    suspend fun delete(streamer: Streamer)
}