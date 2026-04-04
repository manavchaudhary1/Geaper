package com.manav.geaper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.manav.geaper.R

/**
 * Foreground service that keeps the process alive while recording or waiting for a streamer to come
 * online. Also holds a WakeLock so the CPU stays awake and yt-dlp keeps running when the screen is
 * off.
 */
class RecordingService : Service() {

  companion object {
    private const val TAG = "RecordingService"
    private const val CHANNEL_ID = "geaper_recording"
    private const val NOTIF_ID = 2001
    private const val EXTRA_NAME = "streamer_name"
    private const val EXTRA_ARMED = "armed_mode"

    /** Start foreground service for an active yt-dlp recording + acquire wakelock. */
    fun start(context: Context, streamerName: String) {
      context.startForegroundService(
        Intent(context, RecordingService::class.java)
          .putExtra(EXTRA_NAME, streamerName)
          .putExtra(EXTRA_ARMED, false),
      )
    }

    /**
     * Start foreground service in "armed" mode — yt-dlp not running yet, waiting for streamer to go
     * live. Still holds wakelock so monitoring keeps running with screen off.
     */
    fun startArmed(context: Context, streamerName: String) {
      context.startForegroundService(
        Intent(context, RecordingService::class.java)
          .putExtra(EXTRA_NAME, streamerName)
          .putExtra(EXTRA_ARMED, true),
      )
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, RecordingService::class.java))
    }
  }

  private var wakeLock: PowerManager.WakeLock? = null

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val name = intent?.getStringExtra(EXTRA_NAME) ?: "stream"
    val armed = intent?.getBooleanExtra(EXTRA_ARMED, false) ?: false
    startForeground(NOTIF_ID, buildNotification(name, armed))
    acquireWakeLock()
    Log.d(TAG, "Service started — name=$name armed=$armed")
    return START_STICKY
  }

  override fun onDestroy() {
    releaseWakeLock()
    Log.d(TAG, "Service destroyed — wakelock released")
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  // ── WakeLock ──────────────────────────────────────────────────────────────

  private fun acquireWakeLock() {
    if (wakeLock?.isHeld == true) return
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock =
      pm
        .newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "Geaper::RecordingWakeLock",
        )
        .also {
          it.acquire(12 * 60 * 60 * 1000L) // max 12h safety timeout
          Log.d(TAG, "WakeLock acquired")
        }
  }

  private fun releaseWakeLock() {
    wakeLock?.takeIf { it.isHeld }?.release()
    wakeLock = null
  }

  // ── Notification ──────────────────────────────────────────────────────────

  private fun buildNotification(streamerName: String, armed: Boolean): Notification {
    val title = if (armed) "Waiting for $streamerName" else "Recording $streamerName"
    val body = if (armed) "Will start recording when they go live" else "Tap to open Geaper"
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(title)
      .setContentText(body)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }

  private fun createChannel() {
    val channel =
      NotificationChannel(
          CHANNEL_ID,
          "Recording",
          NotificationManager.IMPORTANCE_LOW,
        )
        .apply { description = "Shown while recording or waiting for a streamer" }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }
}
