package com.manav.geaper.data.db;

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.manav.geaper.data.model.Streamer
import kotlinx.coroutines.flow.Flow

@Dao
interface StreamerDao {

    @Query("SELECT * FROM streamers")
    fun getAll(): Flow<List<Streamer>>

    @Insert
    suspend fun insert(streamer: Streamer)

    @Query("""
        UPDATE streamers
        SET status = :status
        WHERE username = :username AND site = :site
        """)
    suspend fun updateStatus(site: String, username: String, status: String)

    @Delete
    suspend fun delete(streamer: Streamer)
}