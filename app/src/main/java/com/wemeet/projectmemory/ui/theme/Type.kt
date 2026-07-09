package com.wemeet.projectmemory.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The mockup uses IBM Plex Mono for headings/labels/mono UI chrome and Inter
// for body copy. Both are Google Fonts; rather than bundling font files in
// this scaffold we fall back to the platform monospace/sans-serif families,
// which read close enough. Swap in downloadable-fonts or bundled .ttf later
// if the exact typeface match matters.
val MonoFontFamily = FontFamily.Monospace
val SansFontFamily = FontFamily.SansSerif

val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SansFontFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = MonoFontFamily,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
    ),
)
