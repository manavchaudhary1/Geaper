package com.manav.geaper.data.model;

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streamers")
data class Streamer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val site: String,
    val username: String,
    val status: String,
    val flag: String?
)