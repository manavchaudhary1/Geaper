package com.manav.geaper.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manav.geaper.data.model.FfmpegPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface FfmpegPresetDao {

  @Query("SELECT * FROM ffmpeg_presets ORDER BY name ASC") fun getAll(): Flow<List<FfmpegPreset>>

  /** Returns the new row ID — needed by BackupManager to resolve preset IDs after import. */
  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(preset: FfmpegPreset): Long

  @Update suspend fun update(preset: FfmpegPreset)

  @Delete suspend fun delete(preset: FfmpegPreset)
}
