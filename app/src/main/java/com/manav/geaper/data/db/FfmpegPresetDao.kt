package com.manav.geaper.data.db

import androidx.room.*
import com.manav.geaper.data.model.FfmpegPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface FfmpegPresetDao {

    @Query("SELECT * FROM ffmpeg_presets ORDER BY name ASC")
    fun getAll(): Flow<List<FfmpegPreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: FfmpegPreset)

    @Delete
    suspend fun delete(preset: FfmpegPreset)

    @Update
    suspend fun update(preset: FfmpegPreset)
}