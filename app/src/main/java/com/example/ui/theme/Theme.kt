package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ReaderThemeMode(val label: String) {
    PAPER("Paper"),
    SEPIA("Sepia"),
    NIGHT("Night")
}

enum class ReaderFontStyle(val label: String) {
    SERIF("Serif"),
    SANS_SERIF("Sans-Serif"),
    CLASSIC_BOOK("Classic Book")
}

@Immutable
data class FolioPalette(
    val mode: ReaderThemeMode,
    val background: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val divider: Color,
    val pageBackside: Color,
    val isDark: Boolean
)

val PaperPalette = FolioPalette(
    mode = ReaderThemeMode.PAPER,
    background = PaperBg,
    surface = PaperSurface,
    surfaceGlass = PaperSurfaceGlass,
    text = PaperText,
    textMuted = PaperTextMuted,
    accent = PaperAccent,
    divider = PaperDivider,
    pageBackside = PaperBackside,
    isDark = false
)

val SepiaPalette = FolioPalette(
    mode = ReaderThemeMode.SEPIA,
    background = SepiaBg,
    surface = SepiaSurface,
    surfaceGlass = SepiaSurfaceGlass,
    text = SepiaText,
    textMuted = SepiaTextMuted,
    accent = SepiaAccent,
    divider = SepiaDivider,
    pageBackside = SepiaBackside,
    isDark = false
)

val NightPalette = FolioPalette(
    mode = ReaderThemeMode.NIGHT,
    background = NightBg,
    surface = NightSurface,
    surfaceGlass = NightSurfaceGlass,
    text = NightText,
    textMuted = NightTextMuted,
    accent = NightAccent,
    divider = NightDivider,
    pageBackside = NightBackside,
    isDark = true
)

fun paletteFor(mode: ReaderThemeMode): FolioPalette = when (mode) {
    ReaderThemeMode.PAPER -> PaperPalette
    ReaderThemeMode.SEPIA -> SepiaPalette
    ReaderThemeMode.NIGHT -> NightPalette
}

val LocalFolioPalette = staticCompositionLocalOf { PaperPalette }

@Composable
fun FolioTheme(
    themeMode: ReaderThemeMode = ReaderThemeMode.PAPER,
    content: @Composable () -> Unit
) {
    val palette = paletteFor(themeMode)
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.textMuted,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.textMuted,
            outline = palette.divider
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.background,
            secondary = palette.textMuted,
            background = palette.background,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.textMuted,
            outline = palette.divider
        )
    }

    CompositionLocalProvider(LocalFolioPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FolioTypography,
            content = content
        )
    }
}
