package com.manav.geaper.recorder

import android.content.Context
import android.util.Log
import com.manav.geaper.GeaperApplication
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

object StreamRecorder {

  private const val TAG = "StreamRecorder"

  // Per-processId stop flag. Set by stopRecording(); polled by the record loops.
  // We deliberately do NOT cancel the coroutine from outside — cancellation
  // interrupts Thread.join() inside the salvage block and abandons the rename.
  private val stopFlags = ConcurrentHashMap<String, AtomicBoolean>()

  fun streamUrl(site: String, username: String): String =
    when (site) {
      "chaturbate" -> "https://chaturbate.com/$username/"
      "camsoda" -> "https://www.camsoda.com/$username"
      else -> throw IllegalArgumentException("Unknown site: $site")
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
  ) =
    withContext(Dispatchers.IO) {
      if (!GeaperApplication.awaitReady()) {
        Log.e(TAG, "yt-dlp not ready — aborting for $site/$username")
        return@withContext
      }

      val dir = File("$outputDir/$username").also { it.mkdirs() }
      val processId = "$site-$username"
      val resolvedFormat = buildFormatSelector(formatSelector)
      val url = streamUrl(site, username)
      val stopFlag = AtomicBoolean(false)
      stopFlags[processId] = stopFlag

      try {
        if (segmentMinutes > 0) {
          recordSegmented(
            url = url,
            dir = dir,
            processId = processId,
            stopFlag = stopFlag,
            username = username,
            segmentMinutes = segmentMinutes,
            resolvedFormat = resolvedFormat,
            extraArgs = extraArgs,
            site = site,
            onProgress = onProgress,
          )
        } else {
          recordContinuous(
            url = url,
            dir = dir,
            processId = processId,
            username = username,
            resolvedFormat = resolvedFormat,
            extraArgs = extraArgs,
            site = site,
            onProgress = onProgress,
          )
        }
      } finally {
        stopFlags.remove(processId)
        Log.d(TAG, "Recording fully finished for $processId")
      }
    }

  // ── Segmented ─────────────────────────────────────────────────────────────

  private suspend fun recordSegmented(
    url: String,
    dir: File,
    processId: String,
    stopFlag: AtomicBoolean,
    username: String,
    segmentMinutes: Int,
    resolvedFormat: String,
    extraArgs: String,
    site: String,
    onProgress: (String) -> Unit,
  ) =
    withContext(Dispatchers.IO) {
      val segmentMs = segmentMinutes * 60 * 1000L
      Log.d(TAG, "Segmented: ${segmentMinutes}min segments for $processId")
      var segmentIndex = 1

      while (isActive && !stopFlag.get()) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = "${dir.absolutePath}/${username}_${timestamp}_seg${segmentIndex}"
        Log.d(TAG, "Starting segment $segmentIndex → $outputFile")

        Thread {
            try {
              YoutubeDL.getInstance().execute(
                buildRequest(url, outputFile, resolvedFormat, extraArgs, site),
                processId,
              ) { _, _, line ->
                if (line.isNotBlank()) {
                  onProgress(line)
                  Log.d(TAG, "[$processId] $line")
                }
              }
              Log.d(TAG, "Segment $segmentIndex finished naturally")
            } catch (e: Exception) {
              Log.d(TAG, "Segment $segmentIndex stopped: ${e.message}")
            }
          }
          .also {
            it.isDaemon = true
            it.start()
          }

        // Poll until segment time elapses OR stop requested
        val start = System.currentTimeMillis()
        while (isActive && !stopFlag.get() && (System.currentTimeMillis() - start) < segmentMs) {
          delay(500)
        }

        // Always kill yt-dlp before salvaging
        YoutubeDL.getInstance().destroyProcessById(processId)

        // Salvage on a plain Thread — must not be on a coroutine so that
        // Thread.sleep inside waitUntilFileSettles is never interrupted
        Log.d(TAG, "Salvaging segment $segmentIndex…")
        Thread { salvagePartFiles(dir, username) }
          .also {
            it.isDaemon = true
            it.start()
          }
          .join(12_000)
        Log.d(TAG, "Salvage done for segment $segmentIndex")

        if (stopFlag.get()) {
          Log.d(TAG, "Stop flag set — exiting after segment $segmentIndex")
          break
        }

        segmentIndex++
      }
    }

  // ── Continuous ────────────────────────────────────────────────────────────

  private suspend fun recordContinuous(
    url: String,
    dir: File,
    processId: String,
    username: String,
    resolvedFormat: String,
    extraArgs: String,
    site: String,
    onProgress: (String) -> Unit,
  ) =
    withContext(Dispatchers.IO) {
      val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val outputPath = "${dir.absolutePath}/${username}_${timestamp}.%(ext)s"
      Log.d(TAG, "Continuous recording → $outputPath")

      try {
        YoutubeDL.getInstance().execute(
          buildRequest(url, outputPath, resolvedFormat, extraArgs, site),
          processId,
        ) { _, _, line ->
          if (line.isNotBlank()) {
            onProgress(line)
            Log.d(TAG, "[$processId] $line")
          }
        }
        Log.d(TAG, "Continuous recording finished normally for $processId")
      } catch (e: Exception) {
        Log.d(TAG, "Continuous recording stopped for $processId: ${e.message}")
      } finally {
        Log.d(TAG, "Salvaging continuous recording for $processId…")
        // Plain thread — never on coroutine so sleep is uninterrupted
        Thread { salvagePartFiles(dir, username) }
          .also {
            it.isDaemon = true
            it.start()
          }
          .join(12_000)
        Log.d(TAG, "Salvage done for $processId")
      }
    }

  // ── Stop ──────────────────────────────────────────────────────────────────

  fun stopRecording(site: String, username: String) {
    val processId = "$site-$username"
    Log.d(TAG, "Stopping recording for $processId")
    // Set flag first so the poll loop exits on next tick
    stopFlags[processId]?.set(true)
    // Then kill yt-dlp so the gobbler unblocks
    YoutubeDL.getInstance().destroyProcessById(processId)
    // Do NOT cancel the coroutine — let it run through salvage naturally
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /**
   * Builds a yt-dlp format selector with automatic fallback.
   *
   * If the user picks e.g. "≤480p", we generate:
   * bestvideo[height<=480]+bestaudio/bestvideo[height<=720]+bestaudio/bestvideo+bestaudio/best
   *
   * This means yt-dlp tries 480p first, then 720p, then the best available. So if the streamer only
   * broadcasts in 720p, it won't fail — it falls back.
   *
   * For "bestaudio/best" (audio-only) we keep it as-is since there's no sensible video fallback to
   * chain.
   *
   * For a blank/default selector we just use "bestvideo+bestaudio/best".
   */
  private fun buildFormatSelector(userSelector: String): String {
    if (userSelector.isBlank()) return "bv*+ba/b"
    if (userSelector == "bestaudio/best") return "ba/b"
    if (!userSelector.contains("height<=")) return "$userSelector/best"

    val heightMatch =
      Regex("""height<=(\d+)""").find(userSelector)?.groupValues?.get(1)?.toIntOrNull()
        ?: return "$userSelector/best"

    val fallbacks = buildList {
      add("bv*[height<=$heightMatch]+ba/b[height<=$heightMatch]")
      listOf(480, 720, 1080)
        .filter { it > heightMatch }
        .forEach { h -> add("bv*[height<=$h]+ba/b[height<=$h]") }
      add("bv*+ba/b") // final catch-all, consistent with isBlank() default
    }
    return fallbacks.joinToString("/")
  }

  private fun buildRequest(
    url: String,
    outputPath: String,
    resolvedFormat: String,
    extraArgs: String,
    site: String = "",
  ): YoutubeDLRequest {
    val req =
      YoutubeDLRequest(url).apply {
        addOption("--no-update")
        addOption("--ignore-config")
        addOption("--retries", "10")
        addOption("--fragment-retries", "10")
        addOption("--retry-sleep", "5")
        addOption("--no-playlist")
        addOption("-f", resolvedFormat)
        addOption("--merge-output-format", "mp4")
        addOption("-o", outputPath)
        if (site == "camsoda") {
          Log.d(TAG, "Site is camsoda adding picky ext")
          addOption("--downloader-arg", "ffmpeg_i1:-extension_picky 0")
          addOption("--downloader-arg", "ffmpeg_i2:-extension_picky 0")
        }
        if (extraArgs.isNotBlank()) {
          extraArgs.trim().split(Regex("\\s+")).forEach { addOption(it) }
        }
      }
    Log.d(TAG, "yt-dlp command: ${req.buildCommand()}")
    return req
  }

  private fun salvagePartFiles(dir: File, username: String) {
    dir.listFiles()?.forEach { file ->
      val name = file.name
      if (!name.contains(username)) return@forEach
      when {
        name.endsWith(".mp4.part") -> {
          waitUntilFileSettles(file)
          val dest = File(dir, name.removeSuffix(".part"))
          if (dest.exists()) dest.delete()
          val ok =
            file.renameTo(dest) ||
              run {
                Log.d(TAG, "renameTo failed, copying ${file.name}")
                try {
                  file.inputStream().use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
                  file.delete()
                  true
                } catch (e: Exception) {
                  Log.e(TAG, "Copy also failed for ${file.name}: ${e.message}")
                  false
                }
              }
          Log.d(TAG, "Salvaged: ${file.name} → ${dest.name} ok=$ok size=${dest.length()}B")
        }
        name.endsWith(".ytdl") ||
          name.endsWith(".part-Frag0") ||
          (name.endsWith(".json") && name.contains(".info")) -> {
          file.delete()
          Log.d(TAG, "Cleaned temp: ${file.name}")
        }
      }
    }
  }

  private fun waitUntilFileSettles(file: File, timeoutMs: Long = 10_000) {
    val deadline = System.currentTimeMillis() + timeoutMs
    var prevSize = -1L
    var stableCount = 0
    while (System.currentTimeMillis() < deadline) {
      val size = file.length()
      if (size > 0 && size == prevSize) {
        stableCount++
        if (stableCount >= 2) {
          Log.d(TAG, "File settled at ${size}B: ${file.name}")
          return
        }
      } else {
        stableCount = 0
      }
      prevSize = size
      Thread.sleep(500)
    }
    Log.d(TAG, "Settle timeout for ${file.name}, renaming anyway (size=${file.length()}B)")
  }
}
