package com.wemeet.projectmemory.ui.theme

import androidx.compose.ui.graphics.Color

// Palette lifted directly from the UI mockup (dark, IBM Plex Mono accents).
val BgRoot = Color(0xFF05070A)
val BgSurface = Color(0xFF0B0D10)
val BgCard = Color(0xFF12151A)
val BgCardBorder = Color(0xFF1E2229)
val BgInputSurface = Color(0xFF0E1013)
val BgChip = Color(0xFF1A1E25)
val BgChipBorder = Color(0xFF2A2F38)
val BgTabActive = Color(0xFF1A1E25)
val BgTabActiveBorder = Color(0xFF333B47)

val TextPrimary = Color(0xFFE4E7EB)
val TextHeading = Color(0xFFEDEFF2)
val TextSecondary = Color(0xFF6B7480)
val TextMuted = Color(0xFF565F6B)
val TextFaint = Color(0xFF454C56)
val TextDim = Color(0xFF4B5563)

val AccentBlueStart = Color(0xFF4C7EF3)
val AccentBlueEnd = Color(0xFF3860D0)
val AccentGreen = Color(0xFF6FCF97)
val AccentRedStart = Color(0xFFE5484D)
val AccentRedEnd = Color(0xFFB23A3E)

// Doc-type accent colors (diff stripes / tab dots), keyed in DocType itself
// as hex strings — these are the same values, kept here too for direct use
// in previews/components that don't want to parse hex at runtime.
val DocReq = Color(0xFFF2B84B)
val DocTodo = Color(0xFF6FCF97)
val DocArch = Color(0xFF9B8CFF)
val DocIdea = Color(0xFFFF8F6B)
val DocDec = Color(0xFF4FD1C5)
val DocMeet = Color(0xFF63B3ED)
val DocRead = Color(0xFF8B95A5)
