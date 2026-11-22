package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// --- Curated Reading Theme Palettes (Paper, Sepia, Night, and AMOLED Pure Black) ---

// Paper Theme
val PaperBg = Color(0xFFF7F5F0)
val PaperSurface = Color(0xFFEEEAE1)
val PaperSurfaceGlass = Color(0xF5F5F1E9) // ~96% opaque HUD surface
val PaperText = Color(0xFF1F1E1C)
val PaperTextMuted = Color(0xFF6B665E)
val PaperAccent = Color(0xFF8C5A38)
val PaperDivider = Color(0x221F1E1C)
val PaperBackside = Color(0xFFECE8DF)

// Sepia Theme
val SepiaBg = Color(0xFFEFE6D5)
val SepiaSurface = Color(0xFFE4D8C1)
val SepiaSurfaceGlass = Color(0xF5EBE1CE) // ~96% opaque HUD surface
val SepiaText = Color(0xFF2B2118)
val SepiaTextMuted = Color(0xFF74614E)
val SepiaAccent = Color(0xFF7D4B28)
val SepiaDivider = Color(0x242B2118)
val SepiaBackside = Color(0xFFE3D7C2)

// Night Theme (Rich off-black #121214, soft warm gray #E0DDD6, crisp warm amber accent)
val NightBg = Color(0xFF121214)
val NightSurface = Color(0xFF1C1C20)
val NightSurfaceGlass = Color(0xF518181C) // ~96% opaque HUD surface
val NightText = Color(0xFFE0DDD6)
val NightTextMuted = Color(0xFF9A968E)
val NightAccent = Color(0xFFE5B86E)
val NightDivider = Color(0x28E0DDD6)
val NightBackside = Color(0xFF1A1A1E)

// AMOLED Pure Black Theme (#000000 for OLED battery savings, crisp warm amber / soft white)
val AmoledBg = Color(0xFF000000)
val AmoledSurface = Color(0xFF0E0E10)
val AmoledSurfaceGlass = Color(0xF50B0B0D) // ~96% opaque HUD surface
val AmoledText = Color(0xFFE4E1DA)
val AmoledTextMuted = Color(0xFF918E86)
val AmoledAccent = Color(0xFFE8BC74)
val AmoledDivider = Color(0x2AE4E1DA)
val AmoledBackside = Color(0xFF0B0B0D)

// --- 4 Muted, Eye-Friendly Highlight Colors ---
val HighlightWarmYellow = Color(0xFFEAD27B)
val HighlightSoftMint = Color(0xFF9ACFB5)
val HighlightDustyRose = Color(0xFFDEA2A8)
val HighlightPaleBlue = Color(0xFF9BBCE0)

val HighlightColors = listOf(
    HighlightWarmYellow,
    HighlightSoftMint,
    HighlightDustyRose,
    HighlightPaleBlue
)

val HighlightColorNames = listOf(
    "Warm Yellow",
    "Soft Mint",
    "Dusty Rose",
    "Pale Blue"
)

// --- Curated Literary Book Cover Palettes (Procedural Typographic Covers) ---
data class BookCoverStyle(
    val background: Color,
    val accent: Color,
    val text: Color,
    val spine: Color
)

val CuratedCoverStyles = listOf(
    BookCoverStyle(
        background = Color(0xFF283531), // Deep Botanical Pine
        accent = Color(0xFFD2A86A),
        text = Color(0xFFF3EFE6),
        spine = Color(0xFF1B2522)
    ),
    BookCoverStyle(
        background = Color(0xFF3E2924), // Oxblood Mahogany
        accent = Color(0xFFD8B07E),
        text = Color(0xFFF6EFE6),
        spine = Color(0xFF2B1B18)
    ),
    BookCoverStyle(
        background = Color(0xFF232C3D), // Midnight Indigo Cloth
        accent = Color(0xFFC7A876),
        text = Color(0xFFF2EFEA),
        spine = Color(0xFF171E2B)
    ),
    BookCoverStyle(
        background = Color(0xFF4A3D2E), // Burnished Umber
        accent = Color(0xFFE0C08D),
        text = Color(0xFFF7F2E8),
        spine = Color(0xFF352B20)
    ),
    BookCoverStyle(
        background = Color(0xFF2F2F33), // Graphite Linen
        accent = Color(0xFFC99B67),
        text = Color(0xFFEFECE6),
        spine = Color(0xFF202024)
    ),
    BookCoverStyle(
        background = Color(0xFF4C333B), // Dusty Plum Cloth
        accent = Color(0xFFDFB889),
        text = Color(0xFFF6F0EC),
        spine = Color(0xFF362228)
    )
)
