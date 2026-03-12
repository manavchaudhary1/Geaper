package com.manav.geaper

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GeaperApplication : Application() {

    companion object {
        /** True once both YoutubeDL and FFmpeg have finished initialising. */
        @Volatile
        var ytDlpReady = false
            private set

        // Lets callers block until init is done (max 30 s)
        private val initLatch = CountDownLatch(1)

        fun awaitReady(timeoutSeconds: Long = 30): Boolean =
            initLatch.await(timeoutSeconds, TimeUnit.SECONDS) && ytDlpReady
    }

    override fun onCreate() {
        super.onCreate()

        // init() extracts ~60 MB of native binaries the first run —
        // must NOT run on the main thread or Android will ANR / throw.
        Thread({
            var ok = false
            try {
                YoutubeDL.getInstance().init(applicationContext)
                Log.d("GeaperApp", "YoutubeDL initialized")
                ok = true
            } catch (e: Exception) {
                Log.e("GeaperApp", "YoutubeDL init failed: ${e.message}", e)
            }

            try {
                FFmpeg.getInstance().init(applicationContext)
                Log.d("GeaperApp", "FFmpeg initialized")
            } catch (e: Exception) {
                Log.e("GeaperApp", "FFmpeg init failed: ${e.message}", e)
            }

            ytDlpReady = ok
            initLatch.countDown()
        }, "yt-dlp-init").also { it.isDaemon = true }.start()
    }
}