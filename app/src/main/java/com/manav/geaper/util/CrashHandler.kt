package com.manav.geaper.util

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.manav.geaper.BuildConfig

object CrashHandler : Thread.UncaughtExceptionHandler {

  private const val PREF_NAME = "geaper_crash"
  private const val KEY_DUMP = "last_crash_dump"

  private lateinit var defaultHandler: Thread.UncaughtExceptionHandler
  private lateinit var appContext: Context

  fun init(context: Context) {
    appContext = context.applicationContext
    defaultHandler = Thread.getDefaultUncaughtExceptionHandler()!!
    Thread.setDefaultUncaughtExceptionHandler(this)
  }

  override fun uncaughtException(thread: Thread, throwable: Throwable) {
    val dump = buildString {
      appendLine("=== Geaper Crash Report ===")
      appendLine("Time   : ${java.time.Instant.now()}")
      appendLine("Thread : ${thread.name}")
      appendLine("Device : ${Build.MANUFACTURER} ${Build.MODEL}")
      appendLine("OS     : Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
      appendLine("App    : ${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME}")
      appendLine()
      appendLine("=== Stack Trace ===")
      appendLine(throwable.stackTraceToString())
    }

    appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit(commit = true) {
      putString(KEY_DUMP, dump)
    } // commit() not apply() — process is dying

    defaultHandler.uncaughtException(thread, throwable)
  }

  fun getLastCrashDump(): String? =
    appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(KEY_DUMP, null)

  fun clearCrashDump() =
    appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit { remove(KEY_DUMP) }
}
