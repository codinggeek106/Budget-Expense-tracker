package com.spendtrack.app.ui.theme

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendtrack.app.appContainer

// Full Material 3 palettes for when wallpaper (dynamic) colors are off or unavailable (Android 10–11).
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B3A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA4F4B4),
    onPrimaryContainer = Color(0xFF00210C),
    secondary = Color(0xFF4F6353),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0D1F13),
    tertiary = Color(0xFF3A646F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEAF6),
    onTertiaryContainer = Color(0xFF001F26),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6FBF3),
    onBackground = Color(0xFF181D18),
    surface = Color(0xFFF6FBF3),
    onSurface = Color(0xFF181D18),
    surfaceVariant = Color(0xFFDDE5DA),
    onSurfaceVariant = Color(0xFF414941),
    outline = Color(0xFF717970),
    outlineVariant = Color(0xFFC1C9BF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE5EAE2),
    surfaceContainerHighest = Color(0xFFDFE4DD),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89D89A),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005228),
    onPrimaryContainer = Color(0xFFA4F4B4),
    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF223527),
    secondaryContainer = Color(0xFF384B3C),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = Color(0xFFA2CED9),
    onTertiary = Color(0xFF02363F),
    tertiaryContainer = Color(0xFF214C57),
    onTertiaryContainer = Color(0xFFBEEAF6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101510),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF101510),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B9389),
    outlineVariant = Color(0xFF414941),
    surfaceContainerLowest = Color(0xFF0B0F0B),
    surfaceContainerLow = Color(0xFF181D18),
    surfaceContainer = Color(0xFF1C211C),
    surfaceContainerHigh = Color(0xFF262B26),
    surfaceContainerHighest = Color(0xFF313631),
)

@Composable
fun SpendTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** The user's chosen appearance, resolved against the system dark-mode setting. */
data class ResolvedAppearance(val darkTheme: Boolean, val dynamicColor: Boolean)

@Composable
fun rememberAppearance(): ResolvedAppearance {
    val prefs = LocalContext.current.appContainer.themePreferences
    val appearance by prefs.appearance.collectAsStateWithLifecycle()
    return ResolvedAppearance(appearance.mode.isDark(isSystemInDarkTheme()), appearance.dynamicColor)
}

/** App theme from the user's appearance settings. Use at the root of every activity. */
@Composable
fun SpendTrackAppTheme(content: @Composable () -> Unit) {
    val appearance = rememberAppearance()
    SpendTrackTheme(darkTheme = appearance.darkTheme, dynamicColor = appearance.dynamicColor, content = content)
}

/**
 * Edge-to-edge with system-bar icons that follow the app theme rather than the system one,
 * so a forced dark theme on a light system still gets light status-bar icons.
 */
@Composable
fun ComponentActivity.SystemBarsForTheme(darkTheme: Boolean) {
    DisposableEffect(darkTheme) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { darkTheme },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
        )
        onDispose { }
    }
}

private val LIGHT_SCRIM = AndroidColor.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = AndroidColor.argb(0x80, 0x1B, 0x1B, 0x1B)
