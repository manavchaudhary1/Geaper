package com.manav.geaper.data.repository

import com.manav.geaper.data.db.StreamerDao
import com.manav.geaper.data.model.Streamer
import com.manav.geaper.network.CamsodaApi
import com.manav.geaper.network.ChaturbateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope


import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.forEach

class StreamRepository(
    private val dao: StreamerDao,
    private val cbApi: ChaturbateApi,
    private val csApi: CamsodaApi
) {

    val streamers = dao.getAll()

    suspend fun addStreamer(site: String, username: String) {

        dao.insert(
            Streamer(
                site = site,
                username = username,
                status = "offline",
                flag = null
            )
        )
    }

    private val TAG = "GeaperMonitor"
    suspend fun updateStatuses(streamers: List<Streamer>) {
        Log.d(TAG, "Monitoring cycle started. Streamers: ${streamers.size}")

        val cbStatus = HashMap<String, String>()
        Log.d(TAG, "Fetching Chaturbate rooms")
        val rooms = cbApi.getOnlineRooms()
        Log.d(TAG, "Chaturbate rooms received: ${rooms.size}")
        Log.d(TAG, "Monitoring cycle started. Streamers: ${streamers.size}")
        rooms.forEach {

            val username = it.username
            val show = it.current_show ?: "offline"

            cbStatus[username] = show
        }
        // Update chaturbate users
        streamers
            .filter { it.site == "chaturbate" }
            .forEach {

                val status =
                    cbStatus[it.username] ?: "offline"
                Log.d(TAG, "Chaturbate ${it.username} -> $status")
                dao.updateStatus(it.site,it.username,status)
            }
        Log.d(TAG, "Updating Camsoda users")
        // Update camsoda users
        updateCamsoda(streamers)
        Log.d(TAG, "Monitoring cycle finished")
        debugPrintDb()
    }

    suspend fun updateCamsoda(streamers: List<Streamer>) {
        Log.d(TAG, "Starting Camsoda checks")
        coroutineScope {
            streamers
                .filter { it.site == "camsoda" }
                .map { streamer ->
                    async(Dispatchers.IO) {
                        val status = csApi.getStatus(streamer.username)
                        Log.d(TAG, "Camsoda ${streamer.username} -> $status")
                        Log.d(TAG, "Updating DB ${streamer.username}: ${streamer.status} -> $status")
                        dao.updateStatus(streamer.site, streamer.username, status)
                    }
                }
                .awaitAll()
        }

        Log.d(TAG, "Finished Camsoda checks")
    }

    private suspend fun debugPrintDb() {
        val rows = dao.getAll().first()
        rows.forEach { streamer ->
            Log.d(
                TAG,
                "DB -> id=${streamer.id} site=${streamer.site}, username=${streamer.username}, status=${streamer.status}"
            )
        }
    }

    suspend fun removeStreamer(streamer: Streamer) {
        dao.delete(streamer)
    }
}