package com.manav.geaper.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri

object StoragePermission {

    /** Returns true if the app can write anywhere on external storage. */
    fun isGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // WRITE_EXTERNAL_STORAGE covers it on Android 9 and below
        }

    /** Opens the system "All files access" settings page for this app. */
    fun openSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            context.startActivity(intent)
        }
    }
}