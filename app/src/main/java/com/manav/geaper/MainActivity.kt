package com.manav.geaper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.manav.geaper.notification.NotificationHelper
import com.manav.geaper.ui.App
import com.manav.geaper.util.StoragePermission

class MainActivity : ComponentActivity() {

  // Launcher for POST_NOTIFICATIONS permission (Android 13+)
  private val notifPermLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    NotificationHelper.createChannel(this)

    // Request POST_NOTIFICATIONS on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED
      ) {
        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    // Request MANAGE_EXTERNAL_STORAGE on Android 11+ so we can write
    // recordings to arbitrary paths the user picks in Settings.
    if (!StoragePermission.isGranted()) {
      StoragePermission.openSettings(this)
    }

    enableEdgeToEdge()
    setContent { App() }
  }
}
