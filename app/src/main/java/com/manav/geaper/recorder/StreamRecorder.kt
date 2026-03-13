package com.manav.geaper.recorder

import android.content.Context
import android.util.Log
import com.manav.geaper.GeaperApplication
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StreamRecorder {

    private const val TAG = "StreamRecorder"

    fun streamUrl(site: String, username: String): String = when (site) {
        "chaturbate" -> "https://chaturbate.com/$username/"
        "camsoda"    -> "https://www.camsoda.com/$username"
        else         -> throw IllegalArgumentException("Unknown site: $site")
    }

    suspend fun startRecording(
        context: Context,
        outputDir: String,
        site: String,
        username: String,
        segmentMinutes: Int = 0,
        formatSelector: String = "",
        extraArgs: String = "",
        onProgress: (String) -> Unit = {},
    ) = withContext(Dispatchers.IO) {

        if (!GeaperApplication.awaitReady()) {
            Log.e(TAG, "yt-dlp not ready — aborting for $site/$username")
            return@withContext
        }

        val dir       = File("$outputDir/$username").also { it.mkdirs() }
        val processId = "$site-$username"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val resolvedFormat = formatSelector.takeIf { it.isNotBlank() } ?: "bestvideo+bestaudio/best"

        val request = YoutubeDLRequest(streamUrl(site, username)).apply {

            addOption("--no-update")
            addOption("--retries", "10")
            addOption("--fragment-retries", "10")
            addOption("--retry-sleep", "5")
            addOption("--no-playlist")
            addOption("-f", resolvedFormat)
            addOption("--merge-output-format", "mp4")

            if (segmentMinutes > 0) {
                addOption("-o", "${dir.absolutePath}/${username}_${timestamp}.mp4")
                addOption(
                    "--postprocessor-args",
                    "ffmpeg:-f segment -segment_time ${segmentMinutes * 60} -reset_timestamps 1 -c copy"
                )
            } else {
                addOption("-o", "${dir.absolutePath}/${username}_${timestamp}_%(ext)s")
            }

            if (extraArgs.isNotBlank()) {
                extraArgs.trim().split(Regex("\\s+")).forEach { token -> addOption(token) }
            }
        }

        Log.d(TAG, "Starting recording for $processId → ${dir.absolutePath}")
        Log.d(TAG, "Format: $resolvedFormat | Extra: '$extraArgs'")

        try {
            YoutubeDL.getInstance().execute(request, processId) { _, _, line ->
                if (line.isNotBlank()) { onProgress(line); Log.d(TAG, "[$processId] $line") }
            }
            Log.d(TAG, "Recording finished normally for $processId")
        } catch (e: Exception) {
            // InterruptedIOException is expected on manual stop — not an error
            Log.d(TAG, "Recording stopped for $processId: ${e.message}")
        } finally {
            // Clean up any leftover .part files yt-dlp leaves when interrupted
            cleanPartFiles(dir, username)
        }
    }

    fun stopRecording(site: String, username: String) {
        val processId = "$site-$username"
        Log.d(TAG, "Stopping recording for $processId")
        YoutubeDL.getInstance().destroyProcessById(processId)
    }

    /**
     * Deletes *.part and *.ytdl temp files left in [dir] that match [username].
     * Called in the finally block so it runs on both normal finish and manual stop.
     */
    private fun cleanPartFiles(dir: File, username: String) {
        val partExtensions = listOf(".part", ".ytdl", ".part-Frag0")
        dir.listFiles()?.forEach { file ->
            val name = file.name
            if (partExtensions.any { name.endsWith(it) } && name.contains(username)) {
                val deleted = file.delete()
                Log.d(TAG, "Cleaned up temp file ${file.name}: deleted=$deleted")
            }
        }
    }
}