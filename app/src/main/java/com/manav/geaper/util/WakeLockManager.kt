package com.manav.geaper.util

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * App-level manual wakelock — separate from the one held by RecordingService. The service wakelock
 * is tied to recording lifetime; this one is user-toggled from the top bar so they can keep
 * monitoring running with screen off even when not recording.
 */
object WakeLockManager {

  private const val TAG = "WakeLockManager"
  private var wakeLock: PowerManager.WakeLock? = null

  val isHeld: Boolean
    get() = wakeLock?.isHeld == true

  fun acquire(context: Context) {
    if (isHeld) return
    val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    wakeLock =
      pm
        .newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "Geaper::ManualWakeLock",
        )
        .also {
          it.acquire(6 * 60 * 60 * 1000L) // 6h safety timeout
          Log.d(TAG, "Manual wakelock acquired")
        }
  }

  fun release() {
    wakeLock?.takeIf { it.isHeld }?.release()
    wakeLock = null
    Log.d(TAG, "Manual wakelock released")
  }

  fun toggle(context: Context) {
    if (isHeld) release() else acquire(context)
  }
}
