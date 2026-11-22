package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AmbientSoundMode
import com.example.data.model.PageTurnStyle
import com.example.data.model.ReaderSettings
import com.example.ui.theme.AmoledBg
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.HighlightColorNames
import com.example.ui.theme.HighlightColors
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.theme.NightBg
import com.example.ui.theme.PaperBg
import com.example.ui.theme.ReaderFontStyle
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.theme.SepiaBg
import com.example.ui.viewmodel.ActiveTextSelection
import com.example.ui.viewmodel.ReadingSessionTimerState
import kotlin.math.roundToInt

/**
 * Clean 95% Opaque Top Bar shown on center tap — balanced icon-only spacing with 1-tap Day/Night toggle.
 */
@Composable
fun ReaderTopBar(
    visible: Boolean,
    isDarkTheme: Boolean,
    isBookmarked: Boolean,
    isPinned: Boolean,
    isSessionTimerActive: Boolean,
    showFullPageTranslateAction: Boolean,
    onBack: () -> Unit,
    onToggleDayNight: () -> Unit,
    onOpenSessionTimer: () -> Unit,
    onTranslateFullPage: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotebook: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        Surface(
            color = palette.surfaceGlass,
            border = BorderStroke(0.5.dp, palette.divider),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("reader_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.reader_back),
                        tint = palette.text
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onToggleDayNight,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reader_day_night_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) FolioIcons.LightMode else FolioIcons.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Day theme" else "Switch to Dark theme",
                            tint = if (isDarkTheme) palette.accent else palette.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showFullPageTranslateAction) {
                        IconButton(
                            onClick = onTranslateFullPage,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("reader_topbar_translate_page_button")
                        ) {
                            Icon(
                                imageVector = FolioIcons.Translate,
                                contentDescription = "Translate current page",
                                tint = palette.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSessionTimer,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reader_timer_button")
                    ) {
                        Icon(
                            imageVector = FolioIcons.Timer,
                            contentDescription = "Reading session timer",
                            tint = if (isSessionTimerActive) palette.accent else palette.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reader_pin_button")
                    ) {
                        Icon(
                            imageVector = FolioIcons.Pin,
                            contentDescription = if (isPinned) "Unpin page" else "Pin page for quick peek",
                            tint = if (isPinned) palette.accent else palette.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reader_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.reader_search),
                            tint = palette.text
                        )
                    }

                    IconButton(
                        onClick = onOpenNotebook,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reader_notebook_button")
                    ) {
                        Icon(
                            imageVector = FolioIcons.NotebookList,
                            contentDescription = stringResource(R.string.reader_notebook),
                            tint = palette.text
                        )
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("reader_bookmark_toggle")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) FolioIcons.BookmarkFilled else FolioIcons.BookmarkOutline,
                            contentDescription = if (isBookmarked) {
                                stringResource(R.string.reader_bookmark_remove)
                            } else {
                                stringResource(R.string.reader_bookmark_add)
                            },
                            tint = if (isBookmarked) palette.accent else palette.text
                        )
                    }
                }
            }
        }
    }
}

/**
 * Frosted-Glass Bottom Bar with Fast Page-Scrubber Slider, Floating Target Bubble,
 * and Draggable Expandable Settings Sheet with clean [Appearance | Tools] tabs.
 */
@Composable
fun ReaderBottomSheetBar(
    visible: Boolean,
    isExpanded: Boolean,
    isPdf: Boolean,
    currentPage: Int,
    totalPages: Int,
    settings: ReaderSettings,
    isTtsPlaying: Boolean,
    sleepTimerRemainingSec: Int,
    sessionTimer: ReadingSessionTimerState,
    chapterTitleForPage: (Int) -> String,
    onScrubCommit: (Int) -> Unit,
    onOpenGoToPage: () -> Unit,
    onOpenSessionTimer: () -> Unit,
    onSetExpanded: (Boolean) -> Unit,
    onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onSetUseSystemBrightness: (Boolean) -> Unit,
    onSetCustomBrightness: (Float) -> Unit,
    onToggleTts: () -> Unit,
    onSetTtsSpeed: (Float) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetAutoFlipSeconds: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember(currentPage) { mutableFloatStateOf(currentPage.toFloat()) }
    // 0 = Appearance, 1 = Tools
    var settingsTab by remember { mutableIntStateOf(0) }

    val safeMaxPage = (totalPages - 1).coerceAtLeast(1)
    val previewPage = if (isScrubbing) scrubValue.roundToInt().coerceIn(0, safeMaxPage) else currentPage
    val previewPct = if (totalPages <= 1) 100 else ((previewPage.toFloat() / safeMaxPage) * 100f).roundToInt()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating Frosted-Glass Bubble shown while actively scrubbing pages (INTACT)
            AnimatedVisibility(
                visible = isScrubbing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceGlass,
                    border = BorderStroke(0.75.dp, palette.divider),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 10.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Page ${previewPage + 1} of $totalPages · $previewPct%",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = palette.text
                        )
                        val chap = chapterTitleForPage(previewPage)
                        if (chap.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = chap,
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Frosted-Glass Bottom Bar + Draggable Expandable Settings Panel
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = palette.surfaceGlass,
                border = BorderStroke(0.75.dp, palette.divider),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Sleek Slim Drag Handle (swipe up/down or tap to toggle Settings Sheet)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .pointerInput(isExpanded) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -10f && !isExpanded) onSetExpanded(true)
                                    if (dragAmount > 10f && isExpanded) onSetExpanded(false)
                                }
                            }
                            .clickable { onSetExpanded(!isExpanded) }
                            .testTag("reader_settings_drag_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(3.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(palette.textMuted.copy(alpha = 0.42f))
                        )
                    }

                    // Slimmer, Sleeker Page-Scrubber Row + Tappable "X / Y" Go-to-Page Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SlimPageScrubber(
                            value = if (isScrubbing) scrubValue else currentPage.toFloat(),
                            maxValue = safeMaxPage.toFloat(),
                            enabled = totalPages > 1,
                            onValueChange = { v ->
                                isScrubbing = true
                                scrubValue = v
                            },
                            onValueChangeFinished = { finalVal ->
                                val target = finalVal.roundToInt().coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                                isScrubbing = false
                                onScrubCommit(target)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reader_page_scrubber")
                        )

                        Surface(
                            onClick = onOpenGoToPage,
                            shape = RoundedCornerShape(9.dp),
                            color = palette.background.copy(alpha = 0.72f),
                            border = BorderStroke(0.75.dp, palette.divider),
                            modifier = Modifier.testTag("reader_go_to_page_trigger")
                        ) {
                            Text(
                                text = "${previewPage + 1} / $totalPages",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = palette.text,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Expandable Reading Settings Panel: [Appearance | Tools]
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Clean 2-tab Switcher: Appearance | Tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SheetModeTabChip(
                                label = "Appearance",
                                selected = settingsTab == 0,
                                onClick = { settingsTab = 0 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_tab_appearance")
                            )
                            SheetModeTabChip(
                                label = "Tools",
                                selected = settingsTab == 1,
                                onClick = { settingsTab = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_tab_tools")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 390.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (settingsTab == 0) {
                                AppearanceSettingsContent(
                                    isPdf = isPdf,
                                    settings = settings,
                                    onUpdateSettings = onUpdateSettings
                                )
                            } else {
                                ToolsSettingsContent(
                                    isPdf = isPdf,
                                    settings = settings,
                                    isTtsPlaying = isTtsPlaying,
                                    sleepTimerRemainingSec = sleepTimerRemainingSec,
                                    sessionTimer = sessionTimer,
                                    onUpdateSettings = onUpdateSettings,
                                    onSetUseSystemBrightness = onSetUseSystemBrightness,
                                    onSetCustomBrightness = onSetCustomBrightness,
                                    onOpenSessionTimer = onOpenSessionTimer,
                                    onToggleTts = onToggleTts,
                                    onSetTtsSpeed = onSetTtsSpeed,
                                    onSetSleepTimerMinutes = onSetSleepTimerMinutes,
                                    onSetAutoFlipSeconds = onSetAutoFlipSeconds
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsContent(
    isPdf: Boolean,
    settings: ReaderSettings,
    onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit
) {
    val palette = LocalFolioPalette.current
    // 1. 4 Curated Themes: Paper, Sepia, Night, AMOLED Pure Black
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ThemeSwatchChip(
            label = stringResource(R.string.reader_theme_paper),
            swatchBg = PaperBg,
            selected = settings.themeMode == ReaderThemeMode.PAPER,
            onClick = { onUpdateSettings { it.copy(themeMode = ReaderThemeMode.PAPER) } },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_paper_chip")
        )
        ThemeSwatchChip(
            label = stringResource(R.string.reader_theme_sepia),
            swatchBg = SepiaBg,
            selected = settings.themeMode == ReaderThemeMode.SEPIA,
            onClick = { onUpdateSettings { it.copy(themeMode = ReaderThemeMode.SEPIA) } },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_sepia_chip")
        )
        ThemeSwatchChip(
            label = stringResource(R.string.reader_theme_night),
            swatchBg = NightBg,
            selected = settings.themeMode == ReaderThemeMode.NIGHT,
            onClick = { onUpdateSettings { it.copy(themeMode = ReaderThemeMode.NIGHT) } },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_night_chip")
        )
        ThemeSwatchChip(
            label = "AMOLED",
            swatchBg = AmoledBg,
            selected = settings.themeMode == ReaderThemeMode.AMOLED,
            onClick = { onUpdateSettings { it.copy(themeMode = ReaderThemeMode.AMOLED) } },
            modifier = Modifier
                .weight(1f)
                .testTag("theme_amoled_chip")
        )
    }

    // 1b. Page-Turn Style Selector (Curl, Slide, Fade, Instant, Vertical Scroll)
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Page-Turn Style",
        style = MaterialTheme.typography.labelMedium,
        color = palette.textMuted
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(PageTurnStyle.CURL, PageTurnStyle.SLIDE, PageTurnStyle.FADE).forEach { style ->
            SmallOptionChip(
                label = style.label,
                selected = settings.pageTurnStyle == style,
                onClick = { onUpdateSettings { it.copy(pageTurnStyle = style) } },
                modifier = Modifier
                    .weight(1f)
                    .testTag("page_turn_style_${style.name.lowercase()}")
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(PageTurnStyle.INSTANT, PageTurnStyle.VERTICAL_SCROLL).forEach { style ->
            SmallOptionChip(
                label = style.label,
                selected = settings.pageTurnStyle == style,
                onClick = { onUpdateSettings { it.copy(pageTurnStyle = style) } },
                modifier = Modifier
                    .weight(1f)
                    .testTag("page_turn_style_${style.name.lowercase()}")
            )
        }
    }

    // 2. Typography Controls (EPUB/TXT)
    if (!isPdf) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FontOptionChip(
                label = stringResource(R.string.reader_font_serif),
                fontFamily = FontFamily.Serif,
                selected = settings.fontStyle == ReaderFontStyle.SERIF,
                onClick = { onUpdateSettings { it.copy(fontStyle = ReaderFontStyle.SERIF) } },
                modifier = Modifier.weight(1f)
            )
            FontOptionChip(
                label = stringResource(R.string.reader_font_sans),
                fontFamily = FontFamily.SansSerif,
                selected = settings.fontStyle == ReaderFontStyle.SANS_SERIF,
                onClick = { onUpdateSettings { it.copy(fontStyle = ReaderFontStyle.SANS_SERIF) } },
                modifier = Modifier.weight(1f)
            )
            FontOptionChip(
                label = stringResource(R.string.reader_font_classic),
                fontFamily = FontFamily.Serif,
                selected = settings.fontStyle == ReaderFontStyle.CLASSIC_BOOK,
                onClick = { onUpdateSettings { it.copy(fontStyle = ReaderFontStyle.CLASSIC_BOOK) } },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        CompactSliderRow(
            label = stringResource(R.string.reader_font_size),
            valueText = "${settings.fontSizeSp.roundToInt()}pt",
            value = settings.fontSizeSp,
            range = 13f..26f,
            onValueChange = { v -> onUpdateSettings { it.copy(fontSizeSp = v) } }
        )
        CompactSliderRow(
            label = stringResource(R.string.reader_line_spacing),
            valueText = String.format("%.2f×", settings.lineSpacing),
            value = settings.lineSpacing,
            range = 1.2f..1.9f,
            onValueChange = { v -> onUpdateSettings { it.copy(lineSpacing = v) } }
        )
        CompactSliderRow(
            label = stringResource(R.string.reader_margins),
            valueText = "${settings.marginDp.roundToInt()}dp",
            value = settings.marginDp,
            range = 14f..40f,
            onValueChange = { v -> onUpdateSettings { it.copy(marginDp = v) } }
        )
    } else {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Fixed-layout PDF automatically adapts colors to your selected theme. Pinch to zoom.",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textMuted
        )
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(8.dp))

    // 3. Tactile Realism Controls
    CompactSliderRow(
        label = stringResource(R.string.reader_page_stiffness),
        valueText = when {
            settings.pageStiffness < 0.42f -> "Supple"
            settings.pageStiffness > 0.72f -> "Crisp"
            else -> "Natural"
        },
        value = settings.pageStiffness,
        range = 0.2f..1.0f,
        onValueChange = { v -> onUpdateSettings { it.copy(pageStiffness = v) } }
    )

    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TactileTogglePill(
            label = stringResource(R.string.reader_paper_sound),
            checked = settings.paperSoundEnabled,
            onCheckedChange = { on -> onUpdateSettings { it.copy(paperSoundEnabled = on) } }
        )
        TactileTogglePill(
            label = stringResource(R.string.reader_haptic_tick),
            checked = settings.hapticEnabled,
            onCheckedChange = { on -> onUpdateSettings { it.copy(hapticEnabled = on) } }
        )
        TactileTogglePill(
            label = stringResource(R.string.reader_paper_grain),
            checked = settings.paperGrainEnabled,
            onCheckedChange = { on -> onUpdateSettings { it.copy(paperGrainEnabled = on) } }
        )
    }
}

@Composable
private fun ToolsSettingsContent(
    isPdf: Boolean,
    settings: ReaderSettings,
    isTtsPlaying: Boolean,
    sleepTimerRemainingSec: Int,
    sessionTimer: ReadingSessionTimerState,
    onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onSetUseSystemBrightness: (Boolean) -> Unit,
    onSetCustomBrightness: (Float) -> Unit,
    onOpenSessionTimer: () -> Unit,
    onToggleTts: () -> Unit,
    onSetTtsSpeed: (Float) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onSetAutoFlipSeconds: (Int) -> Unit
) {
    val palette = LocalFolioPalette.current

    // 1a. Brightness Slider + "Use System" Toggle (directly ABOVE Warmth)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = FolioIcons.LightMode,
                contentDescription = null,
                tint = palette.accent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Brightness",
                style = MaterialTheme.typography.labelLarge,
                color = palette.text
            )
            Text(
                text = if (settings.useSystemBrightness) "System" else "${(settings.brightnessLevel * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = palette.textMuted
            )
        }
        Surface(
            onClick = { onSetUseSystemBrightness(!settings.useSystemBrightness) },
            shape = RoundedCornerShape(10.dp),
            color = if (settings.useSystemBrightness) palette.accent else palette.background.copy(alpha = 0.65f),
            border = BorderStroke(
                0.75.dp,
                if (settings.useSystemBrightness) palette.accent else palette.divider
            ),
            modifier = Modifier.testTag("tool_use_system_brightness_toggle")
        ) {
            Text(
                text = "Use System",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (settings.useSystemBrightness) palette.background else palette.text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
    Slider(
        value = settings.brightnessLevel.coerceIn(0.05f, 1.0f),
        onValueChange = { level -> onSetCustomBrightness(level) },
        valueRange = 0.05f..1.0f,
        colors = SliderDefaults.colors(
            thumbColor = if (settings.useSystemBrightness) palette.textMuted else palette.accent,
            activeTrackColor = if (settings.useSystemBrightness) palette.textMuted.copy(alpha = 0.55f) else palette.accent,
            inactiveTrackColor = palette.divider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .testTag("tool_brightness_slider")
    )

    Spacer(modifier = Modifier.height(4.dp))

    // 1b. Warmth / Blue-Light Filter
    CompactSliderRow(
        label = "Warmth",
        valueText = "${(settings.warmthFilter / 0.65f * 100f).roundToInt()}%",
        value = settings.warmthFilter,
        range = 0f..0.65f,
        onValueChange = { v -> onUpdateSettings { it.copy(warmthFilter = v) } }
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 1c. Reading Session Timer quick launcher
    Surface(
        onClick = onOpenSessionTimer,
        shape = RoundedCornerShape(12.dp),
        color = palette.background.copy(alpha = 0.65f),
        border = BorderStroke(
            0.75.dp,
            if (sessionTimer.isActivePillVisible) palette.accent else palette.divider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tool_session_timer_row")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Reading Session Timer",
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.text
                    )
                    Text(
                        text = if (sessionTimer.isActivePillVisible) {
                            val m = sessionTimer.remainingSeconds / 60
                            val s = sessionTimer.remainingSeconds % 60
                            val stateLbl = if (sessionTimer.isPaused) "Paused" else "Running"
                            String.format("%s · %02d:%02d remaining", stateLbl, m, s)
                        } else {
                            "Set a focused reading session with chime"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textMuted
                    )
                }
            }
            Text(
                text = if (sessionTimer.isActivePillVisible) "Manage" else "Set",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.accent
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 2. Quick Reading Aids Toggle Grid
    ToolSwitchRow(
        label = "Reading Ruler",
        subtitle = "Draggable focus band across lines",
        checked = settings.readingRulerEnabled,
        onCheckedChange = { on -> onUpdateSettings { it.copy(readingRulerEnabled = on) } },
        testTag = "tool_reading_ruler_switch"
    )
    if (!isPdf) {
        ToolSwitchRow(
            label = "Bionic Reading",
            subtitle = "Emphasizes opening letters for faster focus",
            checked = settings.bionicModeEnabled,
            onCheckedChange = { on -> onUpdateSettings { it.copy(bionicModeEnabled = on) } },
            testTag = "tool_bionic_switch"
        )
        // Progressive Disclosure: Bionic Boldness slider appears ONLY when Bionic Reading is ON
        AnimatedVisibility(visible = settings.bionicModeEnabled) {
            val intensityLabel = when {
                settings.bionicIntensity < 0.42f -> "Light"
                settings.bionicIntensity > 0.68f -> "Strong"
                else -> "Medium"
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 2.dp, bottom = 4.dp)
            ) {
                CompactSliderRow(
                    label = "Boldness",
                    valueText = intensityLabel,
                    value = settings.bionicIntensity,
                    range = 0.25f..0.85f,
                    onValueChange = { v -> onUpdateSettings { it.copy(bionicIntensity = v) } }
                )
            }
        }
    } else {
        ToolSwitchRow(
            label = "PDF Auto-Crop",
            subtitle = "Trims empty white margins for larger text",
            checked = settings.pdfAutoCropEnabled,
            onCheckedChange = { on -> onUpdateSettings { it.copy(pdfAutoCropEnabled = on) } },
            testTag = "tool_pdf_autocrop_switch"
        )
    }
    ToolSwitchRow(
        label = "One-Handed Tap Mode",
        subtitle = "On: tap edges to turn · Off: swipe only (tap toggles controls)",
        checked = settings.oneHandedTapMode,
        onCheckedChange = { on -> onUpdateSettings { it.copy(oneHandedTapMode = on) } },
        testTag = "tool_one_handed_switch"
    )
    ToolSwitchRow(
        label = "Keep Screen Awake",
        subtitle = "Prevents screen timeout while reading",
        checked = settings.keepScreenAwake,
        onCheckedChange = { on -> onUpdateSettings { it.copy(keepScreenAwake = on) } },
        testTag = "tool_keep_awake_switch"
    )

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(10.dp))

    // 3. Offline Read-Aloud (TTS)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Offline Read-Aloud",
                style = MaterialTheme.typography.labelLarge,
                color = palette.text
            )
            Text(
                text = "Zero-asset device speech synthesis",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }
        Surface(
            onClick = onToggleTts,
            shape = RoundedCornerShape(12.dp),
            color = if (isTtsPlaying) palette.accent else palette.background.copy(alpha = 0.7f),
            border = BorderStroke(0.75.dp, if (isTtsPlaying) palette.accent else palette.divider),
            modifier = Modifier.testTag("tool_tts_toggle")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (isTtsPlaying) FolioIcons.Pause else FolioIcons.Play,
                    contentDescription = null,
                    tint = if (isTtsPlaying) palette.background else palette.text,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isTtsPlaying) "Stop" else "Listen",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isTtsPlaying) palette.background else palette.text
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        speeds.forEach { spd ->
            val sel = kotlin.math.abs(settings.ttsSpeed - spd) < 0.05f
            SmallOptionChip(
                label = "${spd}×",
                selected = sel,
                onClick = { onSetTtsSpeed(spd) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
    Spacer(modifier = Modifier.height(10.dp))

    // 4. Cozy Procedural Ambient Sound (0 KB AudioTrack)
    Text(
        text = "Cozy Ambient Sound",
        style = MaterialTheme.typography.labelLarge,
        color = palette.text
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AmbientSoundMode.entries.forEach { mode ->
            SmallOptionChip(
                label = mode.label,
                selected = settings.ambientSoundMode == mode,
                onClick = { onUpdateSettings { it.copy(ambientSoundMode = mode) } },
                modifier = Modifier.weight(1f)
            )
        }
    }
    if (settings.ambientSoundMode != AmbientSoundMode.OFF) {
        Spacer(modifier = Modifier.height(4.dp))
        CompactSliderRow(
            label = "Volume",
            valueText = "${(settings.ambientVolume * 100f).roundToInt()}%",
            value = settings.ambientVolume,
            range = 0.05f..1.0f,
            onValueChange = { v -> onUpdateSettings { it.copy(ambientVolume = v) } }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 5. Sleep Timer (fades out Ambient Sound + TTS)
    val sleepLabel = if (sleepTimerRemainingSec > 0) {
        val m = sleepTimerRemainingSec / 60
        val s = sleepTimerRemainingSec % 60
        String.format("Sleep Timer (%d:%02d left)", m, s)
    } else "Sleep Timer"
    Text(
        text = sleepLabel,
        style = MaterialTheme.typography.labelLarge,
        color = palette.text
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m").forEach { (mins, lbl) ->
            val isActive = if (mins == 0) sleepTimerRemainingSec == 0
            else sleepTimerRemainingSec in ((mins - 14) * 60)..(mins * 60) && sleepTimerRemainingSec > 0
            SmallOptionChip(
                label = lbl,
                selected = isActive && mins > 0 || (mins == 0 && sleepTimerRemainingSec == 0),
                onClick = { onSetSleepTimerMinutes(mins) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // 6. Auto-Flip Timer (hands-free curved page turns)
    Text(
        text = "Auto-Flip Timer",
        style = MaterialTheme.typography.labelLarge,
        color = palette.text
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(0 to "Off", 8 to "8s", 15 to "15s", 25 to "25s", 40 to "40s").forEach { (sec, lbl) ->
            SmallOptionChip(
                label = lbl,
                selected = settings.autoFlipSeconds == sec,
                onClick = { onSetAutoFlipSeconds(sec) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SheetModeTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.background.copy(alpha = 0.45f),
        border = BorderStroke(if (selected) 1.2.dp else 0.75.dp, if (selected) palette.accent else palette.divider),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) palette.accent else palette.textMuted
            )
        }
    }
}

@Composable
private fun SmallOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) palette.accent else palette.background.copy(alpha = 0.55f),
        border = BorderStroke(0.75.dp, if (selected) palette.accent else palette.divider),
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) palette.background else palette.text,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToolSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalFolioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 5.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = palette.text)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.background,
                uncheckedBorderColor = palette.divider
            )
        )
    }
}

@Composable
private fun SlimPageScrubber(
    value: Float,
    maxValue: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    val safeMax = maxValue.coerceAtLeast(1f)
    val fraction = (value / safeMax).coerceIn(0f, 1f)
    var latestDragValue by remember(value) { mutableFloatStateOf(value) }

    BoxWithConstraints(
        modifier = modifier
            .height(34.dp)
            .pointerInput(enabled, safeMax) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val target = ((offset.x / w).coerceIn(0f, 1f)) * safeMax
                    onValueChange(target)
                    onValueChangeFinished(target)
                }
            }
            .pointerInput(enabled, safeMax) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val v = ((offset.x / w).coerceIn(0f, 1f)) * safeMax
                        latestDragValue = v
                        onValueChange(v)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val v = ((change.position.x / w).coerceIn(0f, 1f)) * safeMax
                        latestDragValue = v
                        onValueChange(v)
                    },
                    onDragEnd = { onValueChangeFinished(latestDragValue) },
                    onDragCancel = { onValueChangeFinished(latestDragValue) }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val trackWidth = maxWidth
        val thumbSize = 14.dp
        val travel = (trackWidth - thumbSize).coerceAtLeast(0.dp)
        val thumbOffset = travel * fraction

        // Slim 3dp inactive track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.divider)
        )
        // Slim 3dp active fill
        Box(
            modifier = Modifier
                .width(thumbOffset + thumbSize / 2)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.accent)
        )
        // Subtle 14dp thumb handle
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(palette.accent)
                .border(1.5.dp, palette.surface, CircleShape)
        )
    }
}

@Composable
private fun ThemeSwatchChip(
    label: String,
    swatchBg: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(11.dp),
        color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.background.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.75.dp,
            color = if (selected) palette.accent else palette.divider
        ),
        modifier = modifier.height(42.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(swatchBg)
                    .border(0.75.dp, palette.divider, CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (selected) palette.accent else palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FontOptionChip(
    label: String,
    fontFamily: FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.background.copy(alpha = 0.5f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.75.dp,
            color = if (selected) palette.accent else palette.divider
        ),
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = fontFamily),
                color = palette.text
            )
        }
    }
}

@Composable
private fun CompactSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalFolioPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textMuted,
            modifier = Modifier.width(84.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.divider
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            color = palette.text,
            modifier = Modifier.width(48.dp)
        )
    }
}

@Composable
private fun TactileTogglePill(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalFolioPalette.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.background,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textMuted,
                uncheckedTrackColor = palette.background,
                uncheckedBorderColor = palette.divider
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted
        )
    }
}

/**
 * Temporary 5-second frosted-glass pill allowing instant jump back after peeking ahead/back.
 */
@Composable
fun ReturnToPagePill(
    originPage: Int?,
    onReturnClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    AnimatedVisibility(
        visible = originPage != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        if (originPage != null) {
            Surface(
                onClick = onReturnClick,
                shape = RoundedCornerShape(24.dp),
                color = palette.surfaceGlass,
                border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.55f)),
                shadowElevation = 6.dp,
                modifier = Modifier.testTag("return_to_page_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = FolioIcons.Undo,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.reader_return_to_page, originPage + 1),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.text
                    )
                }
            }
        }
    }
}

/**
 * Compact Floating Glassmorphism Action Bar positioned right above selected text / tapped highlight.
 * Includes the 4 Highlight Swatches + Translate (EN <-> HI) + Share Quote Card + Note + Copy + Remove.
 */
@Composable
fun SelectionActionBar(
    selection: ActiveTextSelection,
    containerWidthPx: Int,
    containerHeightPx: Int,
    topInsetPx: Int,
    onPickColor: (Int) -> Unit,
    onTranslate: () -> Unit,
    onShareCard: () -> Unit,
    onAddNote: () -> Unit,
    onCopy: () -> Unit,
    onRemove: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val density = LocalDensity.current.density

    val barHeightPx = (52f * density).roundToInt()
    val marginPx = (12f * density).roundToInt()
    val preferredY = (selection.anchorTopY - barHeightPx - marginPx).roundToInt()
    val finalY = if (preferredY > topInsetPx + marginPx) {
        preferredY
    } else {
        (selection.anchorBottomY + marginPx).roundToInt()
            .coerceAtMost((containerHeightPx - barHeightPx - 80 * density).roundToInt().coerceAtLeast(topInsetPx))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, finalY.coerceAtLeast(topInsetPx + 8)) },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(0.75.dp, palette.divider),
            shadowElevation = 10.dp,
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .testTag("selection_action_bar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 4 Muted, Eye-Friendly Highlight Swatches (INTACT)
                HighlightColors.forEachIndexed { idx, color ->
                    val isCurrent = selection.existingHighlightId != null && selection.colorIndex == idx
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable { onPickColor(idx) }
                            .testTag("highlight_color_$idx"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isCurrent) 2.dp else 0.75.dp,
                                    color = if (isCurrent) palette.text else palette.divider,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = HighlightColorNames[idx],
                                    tint = Color(0xFF1F1E1C),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(palette.divider)
                )

                // Translate (Multi-Language)
                IconButton(
                    onClick = onTranslate,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("selection_translate_button")
                ) {
                    Icon(
                        imageVector = FolioIcons.Translate,
                        contentDescription = "Translate",
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Share Quote Card
                IconButton(
                    onClick = onShareCard,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("selection_share_card_button")
                ) {
                    Icon(
                        imageVector = FolioIcons.ShareCard,
                        contentDescription = "Share Quote Card",
                        tint = palette.text,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Add / Edit Note
                IconButton(
                    onClick = onAddNote,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("selection_add_note_button")
                ) {
                    Icon(
                        imageVector = FolioIcons.EditNote,
                        contentDescription = stringResource(R.string.reader_add_note),
                        tint = palette.text,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Copy text
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("selection_copy_button")
                ) {
                    Icon(
                        imageVector = FolioIcons.Copy,
                        contentDescription = stringResource(R.string.reader_copy),
                        tint = palette.text,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Remove option when tapping an existing highlight
                if (selection.existingHighlightId != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("selection_remove_button")
                    ) {
                        Icon(
                            imageVector = FolioIcons.Delete,
                            contentDescription = stringResource(R.string.reader_remove),
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
