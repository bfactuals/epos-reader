package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.curl.PageCurlCanvas
import com.example.engine.export.QuoteCardAndExport
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.viewmodel.ReaderViewModel

@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val palette = LocalFolioPalette.current
    val context = LocalContext.current
    val density = LocalDensity.current

    val topInsetPx = WindowInsets.statusBars.getTop(density)
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(density)
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showGoToPageDialog by remember { mutableStateOf(false) }

    // Track active foreground Reader lifecycle so Reading Stats & Ambient Sound run ONLY while actively reading
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setReadingActive(true)
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> viewModel.setReadingActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.setReadingActive(true)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setReadingActive(false)
        }
    }

    // Apply real Android screen brightness (or restore System brightness) cleanly
    DisposableEffect(settings.useSystemBrightness, settings.brightnessLevel) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val lp = window.attributes
            lp.screenBrightness = if (settings.useSystemBrightness) {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                settings.brightnessLevel.coerceIn(0.05f, 1.0f)
            }
            window.attributes = lp
        }
        onDispose {
            val w = (context as? Activity)?.window
            if (w != null) {
                val lp = w.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                w.attributes = lp
            }
        }
    }

    // Keep Screen Awake while reading (FLAG_KEEP_SCREEN_ON)
    DisposableEffect(settings.keepScreenAwake) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenAwake) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Smart Back navigation: close active overlays / dialogs / sheets before returning to Library
    BackHandler {
        when {
            showGoToPageDialog -> showGoToPageDialog = false
            uiState.sessionTimer.showCompleteAlert -> viewModel.dismissSessionTimerAlert()
            uiState.sessionTimer.isSheetOpen -> viewModel.openSessionTimerSheet(false)
            uiState.fullPageTranslation != null -> viewModel.dismissFullPageTranslation()
            uiState.quoteCardQuote != null -> viewModel.dismissQuoteCard()
            uiState.editingNoteHighlight != null -> viewModel.dismissNoteEditor()
            uiState.pdfSelectionPromptText != null -> viewModel.dismissPdfSelectionPrompt()
            uiState.isPeekingPinnedPage -> viewModel.closePinnedPagePeek()
            uiState.activeTranslation != null -> viewModel.dismissTranslationCard()
            uiState.isSearchOpen -> viewModel.openSearch(false)
            uiState.isNotebookOpen -> viewModel.openNotebook(false)
            uiState.activeSelection != null -> viewModel.dismissSelection()
            uiState.isSettingsExpanded -> viewModel.setSettingsExpanded(false)
            uiState.isOverlayVisible -> viewModel.toggleOverlay()
            else -> onNavigateBack()
        }
    }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Epos Excerpt", text))
        viewModel.showToast("Copied to clipboard")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        // 1. Full-Screen Zero-Clutter 60fps 2D Canvas Page-Curl Engine (INTACT + Layered Gestures)
        val selectionOverlay = remember(uiState.activeSelection) {
            viewModel.buildSelectionOverlay()
        }

        PageCurlCanvas(
            currentBitmap = uiState.currentBitmap,
            prevBitmap = uiState.prevBitmap,
            nextBitmap = uiState.nextBitmap,
            currentPage = uiState.currentPage,
            totalPages = uiState.totalPages,
            palette = palette,
            pageStiffness = settings.pageStiffness,
            selectionOverlay = selectionOverlay,
            searchGlowRects = uiState.searchGlowRects,
            searchGlowAlpha = uiState.searchGlowAlpha,
            onSizeReady = { size ->
                canvasSize = size
                viewModel.onViewportReady(
                    size = size,
                    density = density.density,
                    topInset = topInsetPx,
                    bottomInset = bottomInsetPx
                )
            },
            onPageTurnCommit = { delta -> viewModel.commitPageTurn(delta) },
            onPlayRustle = { vel -> viewModel.playPaperRustle(vel) },
            onPageLandHaptic = { viewModel.triggerPageLandHaptic() },
            onCenterTap = { viewModel.toggleOverlay() },
            onLongPressAt = { offset -> viewModel.onLongPressAt(offset) },
            onTapAtRestingPage = { offset -> viewModel.onTapAtRestingPage(offset) },
            onDragSelectionHandle = { isStart, pos -> viewModel.onDragSelectionHandle(isStart, pos) },
            oneHandedTapMode = settings.oneHandedTapMode,
            autoFlipTrigger = uiState.autoFlipTrigger,
            onLeftEdgeBrightnessDrag = { delta -> viewModel.onLeftEdgeBrightnessDrag(delta) },
            onLeftEdgeBrightnessEnd = { viewModel.onLeftEdgeBrightnessEnd() },
            isPdf = uiState.isPdf,
            pdfZoomScale = uiState.pdfZoomScale,
            pdfPanOffset = uiState.pdfPanOffset,
            onPdfPinchTransform = { scale, pan -> viewModel.onPdfPinchTransform(scale, pan) },
            modifier = Modifier.fillMaxSize()
        )

        // 1b. Warmth / Blue-Light Amber Filter Overlay (non-touch-blocking)
        if (settings.warmthFilter > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFA83A).copy(alpha = (settings.warmthFilter * 0.36f).coerceIn(0f, 0.28f)))
            )
        }

        // 1c. Extra-Dim Scrim Overlay ONLY when custom brightness is set below 22% (hardware backlight handles 22%..100%)
        if (!settings.useSystemBrightness && settings.brightnessLevel < 0.22f) {
            val extraDimAlpha = ((0.22f - settings.brightnessLevel) / 0.17f).coerceIn(0f, 1f) * 0.48f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = extraDimAlpha))
            )
        }

        // 1d. Draggable Translucent Reading Ruler Focus Bar
        if (settings.readingRulerEnabled && canvasSize.height > 0) {
            ReadingRulerOverlay(containerHeightPx = canvasSize.height)
        }

        // Loading indicator only on very first parse frame
        if (uiState.isLoading) {
            CircularProgressIndicator(
                color = palette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }

        // 2. Floating Glassmorphism Selection / Highlight Action Bar (with Translate & Share Card)
        uiState.activeSelection?.let { sel ->
            SelectionActionBar(
                selection = sel,
                containerWidthPx = canvasSize.width,
                containerHeightPx = canvasSize.height,
                topInsetPx = topInsetPx,
                onPickColor = { colorIdx -> viewModel.applyHighlightColor(colorIdx) },
                onTranslate = { viewModel.translateActiveSelection() },
                onShareCard = { viewModel.openQuoteCard(sel.selectedText) },
                onAddNote = { viewModel.openNoteEditorForSelection() },
                onCopy = {
                    copyToClipboard(sel.selectedText)
                    viewModel.dismissSelection()
                },
                onRemove = { viewModel.removeActiveHighlight() }
            )
        }

        // 2b. Compact Frosted-Glass Multi-Language Translation Card
        uiState.activeTranslation?.let { card ->
            TranslationOverlayCard(
                card = card,
                onSwitchLanguage = { lang -> viewModel.switchTranslationLanguage(lang) },
                onToggleSaveWord = { viewModel.toggleSaveActiveTranslation() },
                onCopy = { txt -> copyToClipboard(txt) },
                onDismiss = { viewModel.dismissTranslationCard() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp)
            )
        }

        // 2d. Left-Edge Brightness Percentage Pill
        BrightnessIndicatorPill(
            visible = uiState.showBrightnessIndicator,
            brightnessLevel = settings.brightnessLevel,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 18.dp)
        )

        // 2e. PDF Lock Zoom Chip (shown when pinched-zoomed)
        PdfZoomLockChip(
            visible = uiState.isPdf && uiState.pdfZoomScale > 1.05f && !uiState.isOverlayVisible,
            zoomScale = uiState.pdfZoomScale,
            isLocked = uiState.isPdfZoomLocked,
            onToggleLock = { viewModel.togglePdfZoomLock() },
            onReset = { viewModel.resetPdfZoom() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )

        // 2f. Offline Read-Aloud (TTS) Tiny Floating Mini-Player Pill
        TtsMiniPlayerPill(
            visible = uiState.isTtsPlaying && !uiState.isOverlayVisible,
            ttsSpeed = settings.ttsSpeed,
            sleepRemainingSec = uiState.sleepTimerRemainingSec,
            onStop = { viewModel.stopTts() },
            onCycleSpeed = {
                val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                val idx = speeds.indexOfFirst { kotlin.math.abs(it - settings.ttsSpeed) < 0.08f }
                val next = speeds[(idx + 1) % speeds.size]
                viewModel.setTtsSpeed(next)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 42.dp)
        )

        // 2g. Auto-Flip 1-Tap Pause/Resume Pill
        AutoFlipControlPill(
            intervalSeconds = if (!uiState.isOverlayVisible && !uiState.isTtsPlaying) settings.autoFlipSeconds else 0,
            isPaused = uiState.isAutoFlipPaused,
            onTogglePause = { viewModel.toggleAutoFlipPause() },
            onStop = { viewModel.setAutoFlipSeconds(0) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 42.dp)
        )

        // 2h. Quick-Peek Pinned Page Edge Pill
        PinnedPageEdgePill(
            pinnedPageIndex = uiState.pinnedPageIndex,
            currentPage = uiState.currentPage,
            onPeekClick = { viewModel.openPinnedPagePeek() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 84.dp)
        )

        // 2i. Subtle Corner Live Reading Session Timer Pill (e.g., "14:32")
        ReadingSessionTimerPill(
            timer = uiState.sessionTimer,
            visible = !uiState.isOverlayVisible && uiState.fullPageTranslation == null,
            onClick = { viewModel.openSessionTimerSheet(true) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 14.dp)
        )

        // 2j. Unobtrusive Corner 1-Tap Full-Page Translation Button (default OFF; enabled in settings)
        FloatingTranslatePagePill(
            visible = settings.fullPageTranslateEnabled &&
                !uiState.isOverlayVisible &&
                uiState.fullPageTranslation == null &&
                uiState.activeSelection == null &&
                uiState.activeTranslation == null,
            targetLanguage = settings.defaultTargetLanguage,
            onClick = { viewModel.translateCurrentPage() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 26.dp)
        )

        // 3. Temporary 5-Second "Return to Page X" Pill (INTACT)
        ReturnToPagePill(
            originPage = uiState.returnToPageOrigin,
            onReturnClick = { viewModel.returnToOriginPage() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (uiState.isOverlayVisible) 104.dp else 44.dp)
        )

        // 4. Subtle Toast Pill
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 66.dp)
        ) {
            uiState.toastMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceGlass,
                    border = BorderStroke(0.5.dp, palette.divider),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.text,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 5. Progressive Disclosure Frosted-Glass Top Bar (with Timer, Pin, and optional Full-Page Translate)
        ReaderTopBar(
            visible = uiState.isOverlayVisible && uiState.fullPageTranslation == null,
            title = uiState.book?.title.orEmpty(),
            chapterSubtitle = uiState.currentChapterTitle,
            isBookmarked = uiState.isCurrentPageBookmarked,
            isPinned = uiState.pinnedPageIndex == uiState.currentPage,
            isSessionTimerActive = uiState.sessionTimer.isActivePillVisible,
            showFullPageTranslateAction = settings.fullPageTranslateEnabled,
            onBack = onNavigateBack,
            onOpenSessionTimer = { viewModel.openSessionTimerSheet(true) },
            onTranslateFullPage = { viewModel.translateCurrentPage() },
            onTogglePin = { viewModel.togglePinCurrentPage() },
            onOpenSearch = { viewModel.openSearch(true) },
            onOpenNotebook = { viewModel.openNotebook(true) },
            onToggleBookmark = { viewModel.toggleBookmark() },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 6. Progressive Disclosure Frosted-Glass Bottom Bar + Scrubber + [Appearance | Tools] Sheet
        ReaderBottomSheetBar(
            visible = uiState.isOverlayVisible && uiState.fullPageTranslation == null,
            isExpanded = uiState.isSettingsExpanded,
            isPdf = uiState.isPdf,
            currentPage = uiState.currentPage,
            totalPages = uiState.totalPages,
            settings = settings,
            isTtsPlaying = uiState.isTtsPlaying,
            sleepTimerRemainingSec = uiState.sleepTimerRemainingSec,
            sessionTimer = uiState.sessionTimer,
            chapterTitleForPage = { pageIdx -> viewModel.getChapterTitleForPage(pageIdx) },
            onScrubCommit = { targetPage -> viewModel.jumpToPage(targetPage, offerReturnPill = true) },
            onOpenGoToPage = { showGoToPageDialog = true },
            onOpenSessionTimer = { viewModel.openSessionTimerSheet(true) },
            onSetExpanded = { expanded -> viewModel.setSettingsExpanded(expanded) },
            onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
            onSetUseSystemBrightness = { useSys -> viewModel.setUseSystemBrightness(useSys) },
            onSetCustomBrightness = { lvl -> viewModel.setCustomBrightness(lvl) },
            onToggleTts = { viewModel.toggleTts() },
            onSetTtsSpeed = { spd -> viewModel.setTtsSpeed(spd) },
            onSetSleepTimerMinutes = { mins -> viewModel.setSleepTimerMinutes(mins) },
            onSetAutoFlipSeconds = { sec -> viewModel.setAutoFlipSeconds(sec) },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 6b. Quick-Peek Pinned Page Overlay
        if (uiState.isPeekingPinnedPage && uiState.pinnedPageIndex != null) {
            PinnedPagePeekOverlay(
                pinnedPageIndex = uiState.pinnedPageIndex!!,
                bitmap = uiState.pinnedPageBitmap,
                onClose = { viewModel.closePinnedPagePeek() }
            )
        }

        // 6c. Full-Page Translated Reading Overlay (1-Tap Toggle Back to Original)
        uiState.fullPageTranslation?.let { fullPageState ->
            FullPageTranslationOverlay(
                state = fullPageState,
                settings = settings,
                onSwitchLanguage = { lang -> viewModel.switchFullPageTranslationLanguage(lang) },
                onRetry = { viewModel.translateCurrentPage(fullPageState.targetLanguage) },
                onCopyAll = { txt -> copyToClipboard(txt) },
                onShowOriginal = { viewModel.dismissFullPageTranslation() }
            )
        }
    }

    // 7. In-Book Search Frosted Drawer
    if (uiState.isSearchOpen) {
        SearchDrawerSheet(
            query = uiState.searchQuery,
            results = uiState.searchResults,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onResultClick = { result ->
                viewModel.jumpToPage(
                    targetPage = result.pageIndex,
                    offerReturnPill = true,
                    highlightSearchRange = result.startOffsetInPage to result.endOffsetInPage
                )
            },
            onDismiss = { viewModel.openSearch(false) }
        )
    }

    // 8. Notebook (Highlights / Bookmarks / Contents) Drawer with Export .txt & Share Card
    if (uiState.isNotebookOpen) {
        val title = uiState.book?.title.orEmpty()
        val author = uiState.book?.author.orEmpty()
        NotebookDrawerSheet(
            bookTitle = title,
            bookAuthor = author,
            currentPage = uiState.currentPage,
            highlights = uiState.highlights,
            bookmarks = uiState.bookmarks,
            tocItems = uiState.tocItems,
            onJumpToPage = { pageIdx -> viewModel.jumpToPage(pageIdx, offerReturnPill = true) },
            onEditHighlightNote = { hl -> viewModel.openNoteEditorForHighlight(hl) },
            onShareQuoteCard = { quote ->
                viewModel.openNotebook(false)
                viewModel.openQuoteCard(quote)
            },
            onExportHighlightsTxt = {
                val ok = QuoteCardAndExport.shareHighlightsTxt(context, title, author, uiState.highlights)
                if (!ok) viewModel.showToast("Could not export highlights")
            },
            onDeleteHighlight = { id -> viewModel.deleteHighlight(id) },
            onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
            onDismiss = { viewModel.openNotebook(false) }
        )
    }

    // 9. Aesthetic Quote Card Maker Dialog
    uiState.quoteCardQuote?.let { quote ->
        QuoteCardDialog(
            quote = quote,
            bookTitle = uiState.book?.title.orEmpty(),
            bookAuthor = uiState.book?.author.orEmpty(),
            initialTheme = settings.themeMode,
            onDismiss = { viewModel.dismissQuoteCard() }
        )
    }

    // 10. Margin Note Editor Dialog
    uiState.editingNoteHighlight?.let { hl ->
        HighlightNoteDialog(
            highlight = hl,
            onSave = { note -> viewModel.saveHighlightNote(hl.id, note) },
            onDismiss = { viewModel.dismissNoteEditor() }
        )
    }

    // 11. PDF Selectable Text / Highlight Dialog
    uiState.pdfSelectionPromptText?.let { text ->
        PdfHighlightDialog(
            extractedText = text,
            onSave = { excerpt, colorIdx, note -> viewModel.savePdfHighlight(excerpt, colorIdx, note) },
            onCopy = { copyToClipboard(it) },
            onDismiss = { viewModel.dismissPdfSelectionPrompt() }
        )
    }

    // 12. Reading Session Timer Sheet
    if (uiState.sessionTimer.isSheetOpen) {
        ReadingSessionTimerSheet(
            timer = uiState.sessionTimer,
            onStartMinutes = { mins -> viewModel.startSessionTimer(mins) },
            onTogglePause = { viewModel.togglePauseSessionTimer() },
            onReset = { viewModel.resetSessionTimer() },
            onAddTenMinutes = { viewModel.addTenMinutesToSessionTimer() },
            onDismiss = { viewModel.openSessionTimerSheet(false) }
        )
    }

    // 13. Reading Session Complete Alert Dialog
    if (uiState.sessionTimer.showCompleteAlert) {
        SessionTimerCompleteDialog(
            onDismiss = { viewModel.dismissSessionTimerAlert() },
            onAddTenMinutes = { viewModel.addTenMinutesToSessionTimer() }
        )
    }

    // 14. Go to Page Numeric Jump Dialog
    if (showGoToPageDialog) {
        GoToPageDialog(
            currentPage = uiState.currentPage,
            totalPages = uiState.totalPages.coerceAtLeast(1),
            onJumpToPage = { targetIdx -> viewModel.jumpToPage(targetIdx, offerReturnPill = true) },
            onDismiss = { showGoToPageDialog = false }
        )
    }
}
