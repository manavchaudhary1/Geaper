package com.manav.geaper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Geaper theme — pure Material You.
 *
 * On Android 12+ the system wallpaper palette is used (dynamic color).
 * On older devices a neutral teal-tinted scheme is used as fallback.
 *
 * @param themeMode "system" | "light" | "dark"
 * @param dynamicColor Use Material You dynamic colors (Android 12+)
 */
@Composable
fun GeaperTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        "dark"  -> true
        "light" -> false
        else    -> systemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (useDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        useDark -> darkColorScheme()   // M3 baseline dark  (no hardcoded colors)
        else    -> lightColorScheme()  // M3 baseline light
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content,
    )
}