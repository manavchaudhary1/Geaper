package com.manav.geaper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.manav.geaper.R

/**
 * Foreground service that keeps the app process alive while recording. Without this, Android kills
 * the network when the app is backgrounded.
 */
class RecordingService : Service() {

  companion object {
    private const val CHANNEL_ID = "geaper_recording"
    private const val NOTIF_ID = 2001
    private const val EXTRA_NAME = "streamer_name"

    fun start(context: Context, streamerName: String) {
      context.startForegroundService(
        Intent(context, RecordingService::class.java).putExtra(EXTRA_NAME, streamerName)
      )
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, RecordingService::class.java))
    }
  }

  override fun onCreate() {
    super.onCreate()
    createChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val name = intent?.getStringExtra(EXTRA_NAME) ?: "stream"
    startForeground(NOTIF_ID, buildNotification(name))
    return START_STICKY
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun buildNotification(streamerName: String): Notification =
    NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher) // ← app icon
      .setContentTitle("Recording $streamerName")
      .setContentText("Tap to open Geaper")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()

  private fun createChannel() {
    val channel =
      NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_LOW).apply {
        description = "Shown while a stream is being recorded"
      }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
  }
}
