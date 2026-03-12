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

    /**
     * Blocks until the stream ends or [stopRecording] is called.
     * Waits up to 30 s for yt-dlp to finish initialising before starting.
     */
    suspend fun startRecording(
        context: Context,
        outputDir: String,
        site: String,
        username: String,
        segmentMinutes: Int = 0,
        ffmpegArgs: String = "",
        onProgress: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {

        if (!GeaperApplication.awaitReady()) {
            Log.e(TAG, "yt-dlp not ready — aborting for $site/$username")
            return@withContext
        }

        val dir       = File("$outputDir/$username").also { it.mkdirs() }
        val processId = "$site-$username"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val request = YoutubeDLRequest(streamUrl(site, username)).apply {

            // ── Suppress the "version is old" noise ──
            addOption("--no-update")

            // ── Resilience: retry on network hiccup, keep retrying fragments ──
            addOption("--retries", "10")
            addOption("--fragment-retries", "10")
            addOption("--retry-sleep", "5")

            // ── Format ──
            addOption("--no-playlist")
            addOption("-f", "bestvideo+bestaudio/best")
            addOption("--merge-output-format", "mp4")
//            addOption("--live-from-start")

            if (segmentMinutes > 0) {
                // Segment mode: ffmpeg splits into N-minute chunks.
                // The %03d in the template is replaced by ffmpeg (000, 001, …).
                // We must NOT add --merge-output-format when segmenting or
                // yt-dlp will try to rename a file with a literal %03d in its name.
                addOption("-o", "${dir.absolutePath}/${timestamp}_${username}_%03d.mp4")
                addOption(
                    "--postprocessor-args",
                    "ffmpeg:-f segment -segment_time ${segmentMinutes * 60} " +
                            "-reset_timestamps 1 -c copy"
                )
            } else {
                // Normal mode: single continuous file.
                addOption("-o", "${dir.absolutePath}/${timestamp}_${username}.%(ext)s")
            }

            // Optional extra ffmpeg args from preset (only in non-segment mode to avoid conflict)
            if (ffmpegArgs.isNotBlank() && segmentMinutes == 0) {
                addOption("--postprocessor-args", "ffmpeg:$ffmpegArgs")
            }
        }

        Log.d(TAG, "Starting recording for $processId → ${dir.absolutePath}")

        try {
            YoutubeDL.getInstance().execute(request, processId) { _, _, line ->
                if (line.isNotBlank()) {
                    onProgress(line)
                    Log.d(TAG, "[$processId] $line")
                }
            }
            Log.d(TAG, "Recording finished normally for $processId")
        } catch (e: Exception) {
            Log.d(TAG, "Recording stopped for $processId: ${e.message}")
        }
    }

    fun stopRecording(site: String, username: String) {
        val processId = "$site-$username"
        Log.d(TAG, "Stopping recording for $processId")
        YoutubeDL.getInstance().destroyProcessById(processId)
    }
}