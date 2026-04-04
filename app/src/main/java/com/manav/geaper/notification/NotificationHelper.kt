package com.manav.geaper.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.manav.geaper.R

object NotificationHelper {

  private const val CHANNEL_ID = "geaper_status"
  private const val CHANNEL_NAME = "Stream Status"

  fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
          description = "Notifications when a tracked streamer goes live or offline"
        }
      context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
  }

  fun notifyStatusChange(
    context: Context,
    username: String,
    site: String,
    oldStatus: String,
    newStatus: String,
  ) {
    val nm = context.getSystemService(NotificationManager::class.java)

    val (title, body) =
      when {
        isLive(newStatus) && !isLive(oldStatus) ->
          "🔴 $username is LIVE" to "$site · Stream just started"
        !isLive(newStatus) && isLive(oldStatus) ->
          "⚫ $username went offline" to "$site · ${newStatus.uppercase()}"
        newStatus == "private" -> "🔒 $username → PRIVATE" to "$site · Stream went private"
        newStatus == "hidden" -> "👁 $username → HIDDEN" to "$site"
        newStatus == "away" -> "💤 $username → AWAY" to "$site"
        else -> return
      }

    val notification =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    nm.notify("$site-$username".hashCode(), notification)
  }

  private fun isLive(status: String) = status.lowercase() in listOf("public", "online")
}
