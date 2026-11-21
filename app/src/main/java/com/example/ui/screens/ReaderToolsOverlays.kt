package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.TranslationLanguage
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.viewmodel.ActiveTranslationCard
import kotlin.math.roundToInt

/**
 * Compact Frosted-Glass Multi-Language Translation Card shown right over the reading page.
 * Includes an interactive compact language pill (e.g., Auto → Hindi ▾) to switch among all 14 languages on the fly.
 */
@Composable
fun TranslationOverlayCard(
    card: ActiveTranslationCard,
    onSwitchLanguage: (TranslationLanguage) -> Unit,
    onToggleSaveWord: () -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    val entry = card.entry
    var showLangMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceGlass,
        border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.45f)),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("translation_overlay_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = FolioIcons.Translate,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )

                    // Compact Interactive Language Pill (e.g. Auto → Hindi ▾)
                    Box {
                        Surface(
                            onClick = { showLangMenu = true },
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accent.copy(alpha = 0.14f),
                            border = BorderStroke(0.5.dp, palette.accent.copy(alpha = 0.4f)),
                            modifier = Modifier.testTag("translation_language_pill")
                        ) {
                            Text(
                                text = "Auto → ${card.targetLanguage.label} ▾",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showLangMenu,
                            onDismissRequest = { showLangMenu = false },
                            containerColor = palette.surface,
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            TranslationLanguage.entries.forEach { lang ->
                                val selected = lang == card.targetLanguage
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = lang.label,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                                ),
                                                color = if (selected) palette.accent else palette.text
                                            )
                                            if (lang.nativeName != lang.label) {
                                                Text(
                                                    text = lang.nativeName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textMuted
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showLangMenu = false
                                        if (lang != card.targetLanguage) {
                                            onSwitchLanguage(lang)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (card.fromOfflineCache && !card.isLoading) {
                        Text(
                            text = "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close translation",
                        tint = palette.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = card.sourceText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                color = palette.textMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            if (card.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = palette.accent
                    )
                    Text(
                        text = "Translating to ${card.targetLanguage.label}…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted
                    )
                }
            } else if (entry != null) {
                Text(
                    text = entry.translatedText,
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text
                )
                if (!card.errorHint.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = card.errorHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onToggleSaveWord,
                        shape = RoundedCornerShape(10.dp),
                        color = if (entry.isSavedToWordBank) palette.accent else palette.background.copy(alpha = 0.7f),
                        border = BorderStroke(0.75.dp, if (entry.isSavedToWordBank) palette.accent else palette.divider),
                        modifier = Modifier.testTag("save_word_bank_button")
                    ) {
                        Text(
                            text = if (entry.isSavedToWordBank) "Saved to Word Bank ✓" else "Save Word",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (entry.isSavedToWordBank) palette.background else palette.text,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }

                    TextButton(onClick = { onCopy(entry.translatedText) }) {
                        Text("Copy", color = palette.accent)
                    }
                }
            } else {
                Text(
                    text = card.errorHint ?: "Could not translate while offline.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textMuted
                )
            }
        }
    }
}

/**
 * Tiny Left-Edge Brightness Percentage Pill shown while sliding vertically on the left edge.
 */
@Composable
fun BrightnessIndicatorPill(
    visible: Boolean,
    brightnessLevel: Float,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, palette.divider),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = FolioIcons.LightMode,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${(brightnessLevel * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = palette.text
                )
            }
        }
    }
}

/**
 * Unobtrusive Floating Mini-Player Pill while Offline Read-Aloud (TTS) is active.
 */
@Composable
fun TtsMiniPlayerPill(
    visible: Boolean,
    ttsSpeed: Float,
    sleepRemainingSec: Int,
    onStop: () -> Unit,
    onCycleSpeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.5f)),
            shadowElevation = 6.dp,
            modifier = Modifier.testTag("tts_mini_player_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = FolioIcons.Pause,
                        contentDescription = "Pause read aloud",
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Reading aloud",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.text
                )
                Surface(
                    onClick = onCycleSpeed,
                    shape = RoundedCornerShape(10.dp),
                    color = palette.background.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, palette.divider)
                ) {
                    Text(
                        text = String.format("%.2g×", ttsSpeed),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (sleepRemainingSec > 0) {
                    val m = (sleepRemainingSec + 59) / 60
                    Text(
                        text = "· ${m}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                }
            }
        }
    }
}

/**
 * Tiny 1-Tap Pause/Resume Pill while Auto-Flip Timer is enabled.
 */
@Composable
fun AutoFlipControlPill(
    intervalSeconds: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = intervalSeconds > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, palette.divider),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onTogglePause, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = if (isPaused) FolioIcons.Play else FolioIcons.Pause,
                        contentDescription = if (isPaused) "Resume auto-flip" else "Pause auto-flip",
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (isPaused) "Auto-flip paused" else "Auto ${intervalSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.text
                )
                IconButton(onClick = onStop, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Disable auto-flip",
                        tint = palette.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * PDF Zoom Lock Chip shown only when the user has pinched-zoomed a PDF page (> 1.05x).
 */
@Composable
fun PdfZoomLockChip(
    visible: Boolean,
    zoomScale: Float,
    isLocked: Boolean,
    onToggleLock: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, if (isLocked) palette.accent else palette.divider),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${(zoomScale * 100f).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted
                )
                Surface(
                    onClick = onToggleLock,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLocked) palette.accent else palette.background.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("pdf_lock_zoom_chip")
                ) {
                    Text(
                        text = if (isLocked) "Zoom Locked" else "Lock Zoom",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isLocked) palette.background else palette.text,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(onClick = onReset, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Reset zoom",
                        tint = palette.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Subtle Edge Pill when a page is pinned for Quick-Peek comparison.
 */
@Composable
fun PinnedPageEdgePill(
    pinnedPageIndex: Int?,
    currentPage: Int,
    onPeekClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = pinnedPageIndex != null && pinnedPageIndex != currentPage,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (pinnedPageIndex != null) {
            Surface(
                onClick = onPeekClick,
                shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
                color = palette.surfaceGlass,
                border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.5f)),
                shadowElevation = 5.dp,
                modifier = Modifier.testTag("pinned_page_edge_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = FolioIcons.Pin,
                        contentDescription = "Peek pinned page",
                        tint = palette.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "p.${pinnedPageIndex + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.text
                    )
                }
            }
        }
    }
}

/**
 * Clean Quick-Peek Overlay showing the pinned page without losing current reading position.
 */
@Composable
fun PinnedPagePeekOverlay(
    pinnedPageIndex: Int,
    bitmap: Bitmap?,
    onClose: () -> Unit
) {
    val palette = LocalFolioPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(onClick = onClose)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.background,
            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.5f)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {}
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Pinned Page ${pinnedPageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceGlass,
                    border = BorderStroke(0.75.dp, palette.divider),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = FolioIcons.Pin,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Quick-Peek · Page ${pinnedPageIndex + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.text
                        )
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.accent,
                            modifier = Modifier.clickable(onClick = onClose)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draggable Translucent Reading Ruler Focus Bar (helps track 1-2 lines at a time).
 * Only the small handle strip intercepts vertical drag so page curls remain untouched.
 */
@Composable
fun ReadingRulerOverlay(
    containerHeightPx: Int
) {
    val palette = LocalFolioPalette.current
    val density = LocalDensity.current.density
    val bandHeightPx = 56f * density
    var rulerY by remember(containerHeightPx) {
        mutableFloatStateOf((containerHeightPx * 0.42f).coerceAtLeast(160f))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, rulerY.roundToInt()) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(palette.accent.copy(alpha = 0.13f))
                .border(
                    width = 0.75.dp,
                    color = palette.accent.copy(alpha = 0.35f)
                )
        )
        Surface(
            shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.5f)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .pointerInput(containerHeightPx) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val maxY = (containerHeightPx - bandHeightPx - 48f * density).coerceAtLeast(120f)
                        rulerY = (rulerY + dragAmount).coerceIn(80f * density, maxY)
                    }
                }
                .testTag("reading_ruler_handle")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(1.5.dp)
                            .background(palette.accent)
                    )
                }
            }
        }
    }
}
