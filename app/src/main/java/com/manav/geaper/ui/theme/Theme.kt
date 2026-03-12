package com.manav.geaper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Custom dark scheme that matches the app's dark palette
private val GeaperDarkScheme = darkColorScheme(
    primary        = Color(0xFF00E5C8),
    onPrimary      = Color(0xFF0D0F14),
    primaryContainer    = Color(0xFF00C4AD),
    onPrimaryContainer  = Color(0xFF0D0F14),
    background     = Color(0xFF0D0F14),
    onBackground   = Color(0xFFECEFF4),
    surface        = Color(0xFF161920),
    onSurface      = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF1E2229),
    onSurfaceVariant = Color(0xFF7A8499),
    secondary      = Color(0xFF7A8499),
    onSecondary    = Color(0xFF0D0F14),
)

private val GeaperLightScheme = lightColorScheme(
    primary        = Color(0xFF006B5E),
    onPrimary      = Color(0xFFFFFFFF),
    primaryContainer    = Color(0xFF9EF2E4),
    onPrimaryContainer  = Color(0xFF00201C),
    background     = Color(0xFFF5F5F5),
    onBackground   = Color(0xFF1A1C1E),
    surface        = Color(0xFFFFFFFF),
    onSurface      = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4947),
    secondary      = Color(0xFF4A6360),
    onSecondary    = Color(0xFFFFFFFF),
)

/**
 * @param themeMode "system" | "light" | "dark"
 * @param dynamicColor Use Material You dynamic colors (Android 12+)
 */
@Composable
fun GeaperTheme(
    themeMode:    String  = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
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
        useDark -> GeaperDarkScheme
        else    -> GeaperLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}