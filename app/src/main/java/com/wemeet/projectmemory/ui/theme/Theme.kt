package com.wemeet.projectmemory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    background = BgRoot,
    surface = BgSurface,
    primary = AccentBlueStart,
    secondary = AccentGreen,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onPrimary = TextHeading,
    error = AccentRedStart,
)

@Composable
fun ProjectMemoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The mockup is dark-only by design (voice/overlay-first utility app);
    // we still honor system light mode by falling back to the same palette
    // rather than maintaining a second scheme nobody designed for yet.
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
