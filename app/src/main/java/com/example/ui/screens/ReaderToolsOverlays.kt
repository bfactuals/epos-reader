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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ReaderSettings
import com.example.data.model.TranslationLanguage
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.viewmodel.ActiveTranslationCard
import com.example.ui.viewmodel.FullPageTranslationState
import com.example.ui.viewmodel.ReadingSessionTimerState
import kotlinx.coroutines.flow.distinctUntilChanged
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
            val sourceIsRtl = remember(card.sourceText) {
                card.sourceText.any { ch -> ch in '\u0600'..'\u06FF' || ch in '\u0750'..'\u077F' }
            }
            CompositionLocalProvider(
                LocalLayoutDirection provides if (sourceIsRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Text(
                    text = card.sourceText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                    color = palette.textMuted,
                    textAlign = TextAlign.Start,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides if (card.targetLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    ) {
                        Text(
                            text = entry.translatedText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                lineHeight = 25.sp
                            ),
                            color = palette.text,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
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

/**
 * Compact, freely draggable 1-Tap Full-Page Translation pill (remembers its last dragged position).
 */
@Composable
fun FloatingTranslatePagePill(
    visible: Boolean,
    targetLanguage: TranslationLanguage,
    containerSize: IntSize,
    topInsetPx: Int,
    bottomInsetPx: Int,
    savedPosX: Float,
    savedPosY: Float,
    onPositionSaved: (Float, Float) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    val density = LocalDensity.current.density
    val marginPx = 10f * density
    var pillSize by remember { mutableStateOf(IntSize.Zero) }

    val minX = marginPx
    val maxX = (containerSize.width - pillSize.width - marginPx).coerceAtLeast(minX)
    val minY = topInsetPx + marginPx
    val maxY = (containerSize.height - bottomInsetPx - pillSize.height - marginPx).coerceAtLeast(minY)

    var posX by remember(containerSize, pillSize, savedPosX) {
        mutableFloatStateOf(minX + (maxX - minX) * savedPosX.coerceIn(0f, 1f))
    }
    var posY by remember(containerSize, pillSize, savedPosY) {
        mutableFloatStateOf(minY + (maxY - minY) * savedPosY.coerceIn(0f, 1f))
    }
    val latestOnSave by rememberUpdatedState(onPositionSaved)

    AnimatedVisibility(
        visible = visible && containerSize.width > 0 && containerSize.height > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.45f)),
            shadowElevation = 5.dp,
            modifier = Modifier
                .onSizeChanged { pillSize = it }
                .pointerInput(containerSize, pillSize, topInsetPx, bottomInsetPx) {
                    detectDragGestures(
                        onDragEnd = {
                            val nx = if (maxX > minX) ((posX - minX) / (maxX - minX)).coerceIn(0f, 1f) else 0.9f
                            val ny = if (maxY > minY) ((posY - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0.88f
                            latestOnSave(nx, ny)
                        },
                        onDragCancel = {
                            val nx = if (maxX > minX) ((posX - minX) / (maxX - minX)).coerceIn(0f, 1f) else 0.9f
                            val ny = if (maxY > minY) ((posY - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0.88f
                            latestOnSave(nx, ny)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        posX = (posX + dragAmount.x).coerceIn(minX, maxX)
                        posY = (posY + dragAmount.y).coerceIn(minY, maxY)
                    }
                }
                .testTag("reader_translate_page_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = FolioIcons.Translate,
                    contentDescription = "Translate current page (drag to move)",
                    tint = palette.accent,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = targetLanguage.code.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.text
                )
            }
        }
    }
}

/**
 * Full-Page Translated Reading Overlay matching the active Paper/Sepia/Night theme.
 * Supports Arabic & Urdu RTL layout, on-the-fly language switching, and 1-tap "Show Original".
 */
@Composable
fun FullPageTranslationOverlay(
    state: FullPageTranslationState,
    settings: ReaderSettings,
    onSwitchLanguage: (TranslationLanguage) -> Unit,
    onRetry: () -> Unit,
    onCopyAll: (String) -> Unit,
    onShowOriginal: () -> Unit
) {
    val palette = LocalFolioPalette.current
    var showLangMenu by remember { mutableStateOf(false) }

    Surface(
        color = palette.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("full_page_translation_overlay")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Frosted Control Bar
            Surface(
                color = palette.surfaceGlass,
                border = BorderStroke(0.5.dp, palette.divider),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                            modifier = Modifier.size(18.dp)
                        )
                        Box {
                            Surface(
                                onClick = { showLangMenu = true },
                                shape = RoundedCornerShape(10.dp),
                                color = palette.accent.copy(alpha = 0.14f),
                                border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("full_page_language_switcher")
                            ) {
                                Text(
                                    text = "${state.targetLanguage.label} ▾",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.accent,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showLangMenu,
                                onDismissRequest = { showLangMenu = false },
                                containerColor = palette.surface,
                                modifier = Modifier.heightIn(max = 340.dp)
                            ) {
                                TranslationLanguage.entries.forEach { lang ->
                                    val selected = lang == state.targetLanguage
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
                                            if (lang != state.targetLanguage) {
                                                onSwitchLanguage(lang)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (state.fromCache && !state.isLoading) {
                            Text(
                                text = "Cached",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!state.isLoading && state.translatedText.isNotBlank()) {
                            TextButton(onClick = { onCopyAll(state.translatedText) }) {
                                Text("Copy", color = palette.textMuted)
                            }
                        }
                        Surface(
                            onClick = onShowOriginal,
                            shape = RoundedCornerShape(12.dp),
                            color = palette.accent,
                            modifier = Modifier.testTag("full_page_show_original_button")
                        ) {
                            Text(
                                text = "Show Original",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.background,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Translated Page Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                when {
                    state.isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = palette.accent,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "Translating Page ${state.pageIndex + 1} to ${state.targetLanguage.label}…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textMuted
                            )
                        }
                    }

                    state.translatedText.isBlank() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = state.errorHint ?: "Could not translate this page right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textMuted,
                                textAlign = TextAlign.Center
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accent,
                                        contentColor = palette.background
                                    )
                                ) {
                                    Text("Retry")
                                }
                                TextButton(onClick = onShowOriginal) {
                                    Text("Show Original", color = palette.text)
                                }
                            }
                        }
                    }

                    else -> {
                        val readingSp = settings.fontSizeSp.coerceIn(14f, 26f)
                        val lineHeightSp = (readingSp * settings.lineSpacing.coerceIn(1.3f, 1.9f))
                        val marginDp = settings.marginDp.coerceIn(16f, 36f).dp

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = marginDp, vertical = 22.dp)
                        ) {
                            Text(
                                text = "Page ${state.pageIndex + 1} · ${state.targetLanguage.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            CompositionLocalProvider(
                                LocalLayoutDirection provides if (state.targetLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                            ) {
                                Text(
                                    text = state.translatedText,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontSize = readingSp.sp,
                                        lineHeight = lineHeightSp.sp
                                    ),
                                    color = palette.text,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (!state.errorHint.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = state.errorHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact, freely draggable Live Countdown Pill (e.g., "14:32") that remembers its last dragged coordinates.
 */
@Composable
fun ReadingSessionTimerPill(
    timer: ReadingSessionTimerState,
    visible: Boolean,
    containerSize: IntSize,
    topInsetPx: Int,
    bottomInsetPx: Int,
    savedPosX: Float,
    savedPosY: Float,
    onPositionSaved: (Float, Float) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    val density = LocalDensity.current.density
    val marginPx = 10f * density
    var pillSize by remember { mutableStateOf(IntSize.Zero) }

    val minX = marginPx
    val maxX = (containerSize.width - pillSize.width - marginPx).coerceAtLeast(minX)
    val minY = topInsetPx + marginPx
    val maxY = (containerSize.height - bottomInsetPx - pillSize.height - marginPx).coerceAtLeast(minY)

    var posX by remember(containerSize, pillSize, savedPosX) {
        mutableFloatStateOf(minX + (maxX - minX) * savedPosX.coerceIn(0f, 1f))
    }
    var posY by remember(containerSize, pillSize, savedPosY) {
        mutableFloatStateOf(minY + (maxY - minY) * savedPosY.coerceIn(0f, 1f))
    }
    val latestOnSave by rememberUpdatedState(onPositionSaved)

    val mins = timer.remainingSeconds / 60
    val secs = timer.remainingSeconds % 60
    val formatted = String.format("%02d:%02d", mins, secs)

    AnimatedVisibility(
        visible = visible && timer.isActivePillVisible && containerSize.width > 0 && containerSize.height > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .offset { IntOffset(posX.roundToInt(), posY.roundToInt()) }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(18.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(
                0.75.dp,
                if (timer.isPaused) palette.divider else palette.accent.copy(alpha = 0.55f)
            ),
            shadowElevation = 4.dp,
            modifier = Modifier
                .onSizeChanged { pillSize = it }
                .pointerInput(containerSize, pillSize, topInsetPx, bottomInsetPx) {
                    detectDragGestures(
                        onDragEnd = {
                            val nx = if (maxX > minX) ((posX - minX) / (maxX - minX)).coerceIn(0f, 1f) else 0.9f
                            val ny = if (maxY > minY) ((posY - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0.04f
                            latestOnSave(nx, ny)
                        },
                        onDragCancel = {
                            val nx = if (maxX > minX) ((posX - minX) / (maxX - minX)).coerceIn(0f, 1f) else 0.9f
                            val ny = if (maxY > minY) ((posY - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0.04f
                            latestOnSave(nx, ny)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        posX = (posX + dragAmount.x).coerceIn(minX, maxX)
                        posY = (posY + dragAmount.y).coerceIn(minY, maxY)
                    }
                }
                .testTag("reader_session_timer_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = FolioIcons.Timer,
                    contentDescription = "Reading session timer $formatted (drag to move)",
                    tint = if (timer.isPaused) palette.textMuted else palette.accent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (timer.isPaused) "$formatted ⏸" else formatted,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (timer.isPaused) palette.textMuted else palette.text
                )
            }
        }
    }
}

/**
 * Frosted-Glass Reading Session Timer Sheet (15m, 30m, 45m, 60m, Custom + Start/Pause/Reset).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSessionTimerSheet(
    timer: ReadingSessionTimerState,
    onStartMinutes: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onReset: () -> Unit,
    onAddTenMinutes: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customInput by remember { mutableStateOf("") }
    val customParsed = customInput.toIntOrNull()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        modifier = Modifier.testTag("reading_session_timer_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
                        imageVector = FolioIcons.Timer,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Reading Session Timer",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                        color = palette.text
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close timer",
                        tint = palette.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Countdown Display
            val displaySec = if (timer.remainingSeconds > 0) timer.remainingSeconds else timer.configuredMinutes * 60
            val mm = displaySec / 60
            val ss = displaySec % 60
            Text(
                text = String.format("%02d:%02d", mm, ss),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                ),
                color = if (timer.isRunning) palette.accent else palette.text
            )
            Text(
                text = when {
                    timer.isRunning -> "Session in progress"
                    timer.isPaused -> "Paused"
                    else -> "Choose a duration to begin"
                },
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted
            )

            // Active Timer Controls (Pause/Resume, +10m, Reset)
            if (timer.isActivePillVisible) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.accent,
                            contentColor = palette.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("session_timer_pause_resume")
                    ) {
                        Text(if (timer.isRunning) "Pause" else "Resume")
                    }
                    Surface(
                        onClick = onAddTenMinutes,
                        shape = RoundedCornerShape(12.dp),
                        color = palette.background.copy(alpha = 0.7f),
                        border = BorderStroke(0.75.dp, palette.divider),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("session_timer_add_ten")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "+10m",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.text
                            )
                        }
                    }
                    Surface(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        color = palette.background.copy(alpha = 0.7f),
                        border = BorderStroke(0.75.dp, palette.divider),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("session_timer_reset")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Quick Presets
            Text(
                text = "Quick Presets",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    val isSelected = timer.configuredMinutes == mins && timer.isActivePillVisible
                    Surface(
                        onClick = { onStartMinutes(mins) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) palette.accent else palette.background.copy(alpha = 0.65f),
                        border = BorderStroke(0.75.dp, if (isSelected) palette.accent else palette.divider),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("session_timer_preset_${mins}m")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${mins}m",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isSelected) palette.background else palette.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Minutes Input + Start
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { raw ->
                        customInput = raw.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Custom minutes (1–240)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (customParsed != null && customParsed in 1..240) {
                                onStartMinutes(customParsed)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        focusedLabelColor = palette.accent,
                        unfocusedLabelColor = palette.textMuted,
                        cursorColor = palette.accent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("session_timer_custom_input")
                )
                Button(
                    onClick = {
                        if (customParsed != null && customParsed in 1..240) {
                            onStartMinutes(customParsed)
                        }
                    },
                    enabled = customParsed != null && customParsed in 1..240,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.accent,
                        contentColor = palette.background
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("session_timer_custom_start")
                ) {
                    Text("Start")
                }
            }
        }
    }
}

/**
 * Calm Completion Dialog shown when the Reading Session Timer hits 00:00.
 */
@Composable
fun SessionTimerCompleteDialog(
    onDismiss: () -> Unit,
    onAddTenMinutes: () -> Unit
) {
    val palette = LocalFolioPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.textMuted,
        icon = {
            Icon(
                imageVector = FolioIcons.Timer,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = "Reading session complete!",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                color = palette.text,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "Great focus. Take a breather, or add 10 more minutes to keep your flow going.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onAddTenMinutes,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.background
                ),
                modifier = Modifier.testTag("session_complete_add_ten")
            ) {
                Text("+10 min")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("session_complete_dismiss")
            ) {
                Text("Dismiss", color = palette.textMuted)
            }
        }
    )
}

/**
 * Compact Numeric "Go to Page" Dialog opened by tapping the page counter (e.g., 559 / 1020).
 */
@Composable
fun GoToPageDialog(
    currentPage: Int,
    totalPages: Int,
    onJumpToPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    var input by remember { mutableStateOf("${currentPage + 1}") }
    val parsed = input.toIntOrNull()
    val isValid = parsed != null && parsed in 1..totalPages

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.textMuted,
        title = {
            Text(
                text = "Go to Page",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                color = palette.text
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Enter a page number between 1 and $totalPages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textMuted
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        input = raw.filter { it.isDigit() }.take(6)
                    },
                    singleLine = true,
                    placeholder = { Text("1 – $totalPages", color = palette.textMuted) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            if (isValid && parsed != null) {
                                onJumpToPage(parsed - 1)
                                onDismiss()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        cursorColor = palette.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("go_to_page_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && parsed != null) {
                        onJumpToPage(parsed - 1)
                        onDismiss()
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.background
                ),
                modifier = Modifier.testTag("go_to_page_confirm")
            ) {
                Text("Go")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textMuted)
            }
        },
        modifier = Modifier.testTag("go_to_page_dialog")
    )
}

/**
 * Smooth continuous Vertical Scroll reader mode (great for PDFs and long study notes).
 * Renders each page on-demand using the same LRU Bitmap pipeline and respects theme/font/footer changes.
 */
@Composable
fun VerticalScrollReaderView(
    totalPages: Int,
    initialPage: Int,
    jumpTrigger: Pair<Int, Int>,
    renderSignature: String,
    bottomInsetPx: Int,
    onSizeReady: (IntSize) -> Unit,
    loadPageBitmap: suspend (Int) -> Bitmap?,
    onVisiblePageChanged: (Int) -> Unit,
    onCenterTap: () -> Unit,
    onFooterTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    val density = LocalDensity.current
    val safeTotal = totalPages.coerceAtLeast(1)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialPage.coerceIn(0, safeTotal - 1)
    )
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val latestOnVisibleChanged by rememberUpdatedState(onVisiblePageChanged)
    val latestOnCenterTap by rememberUpdatedState(onCenterTap)
    val latestOnFooterTap by rememberUpdatedState(onFooterTap)

    // Scroll programmatically when user scrubs, jumps via TOC/Go-to-Page, or presses Volume keys
    LaunchedEffect(jumpTrigger.first) {
        if (jumpTrigger.first > 0) {
            val target = jumpTrigger.second.coerceIn(0, safeTotal - 1)
            if (listState.firstVisibleItemIndex != target) {
                listState.scrollToItem(target)
            }
        }
    }

    // Track visible page index smoothly as user scrolls
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                latestOnVisibleChanged(idx)
            }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background)
            .onSizeChanged { sz ->
                viewportSize = sz
                if (sz.width > 0 && sz.height > 0) {
                    onSizeReady(sz)
                }
            }
            .testTag("reader_vertical_scroll_view")
    ) {
        if (viewportSize.width > 0 && viewportSize.height > 0) {
            val pageHeightDp = with(density) { viewportSize.height.toDp() }
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(viewportSize, bottomInsetPx) {
                        detectTapGestures { offset ->
                            val footerTop = viewportSize.height - bottomInsetPx - 42f * density.density
                            val tappedFooter = offset.y >= footerTop &&
                                offset.x in (viewportSize.width * 0.15f)..(viewportSize.width * 0.85f)
                            if (tappedFooter) {
                                latestOnFooterTap()
                            } else {
                                latestOnCenterTap()
                            }
                        }
                    }
            ) {
                items(count = safeTotal, key = { it }) { pageIdx ->
                    val bmp by produceState<Bitmap?>(
                        initialValue = null,
                        key1 = pageIdx,
                        key2 = renderSignature
                    ) {
                        value = loadPageBitmap(pageIdx)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pageHeightDp)
                            .background(palette.background),
                        contentAlignment = Alignment.Center
                    ) {
                        val rendered = bmp
                        if (rendered != null) {
                            Image(
                                bitmap = rendered.asImageBitmap(),
                                contentDescription = "Page ${pageIdx + 1}",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            CircularProgressIndicator(
                                color = palette.accent.copy(alpha = 0.5f),
                                strokeWidth = 1.8.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        color = palette.divider.copy(alpha = 0.55f),
                        thickness = 0.75.dp
                    )
                }
            }
        }
    }
}


