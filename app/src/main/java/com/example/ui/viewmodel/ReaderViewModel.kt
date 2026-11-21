package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesRepository
import com.example.data.model.AmbientSoundMode
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookmarkEntity
import com.example.data.model.ChapterTocItem
import com.example.data.model.HighlightEntity
import com.example.data.model.ReaderSettings
import com.example.data.model.SearchResult
import com.example.data.model.TranslationEntryEntity
import com.example.data.model.TranslationLanguage
import com.example.data.repository.LibraryRepository
import com.example.engine.audio.PaperAudioEngine
import com.example.engine.audio.ReaderTtsEngine
import com.example.engine.curl.SelectionOverlayState
import com.example.engine.parser.BookDocumentParser
import com.example.engine.parser.PageBitmapRenderer
import com.example.engine.parser.PageTextGeometry
import com.example.engine.parser.PaginatedPage
import com.example.engine.parser.Paginator
import com.example.engine.parser.ParsedBook
import com.example.engine.translate.TranslationService
import com.example.ui.theme.HighlightColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sin

data class ActiveTextSelection(
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val selectedText: String,
    val rects: List<RectF>,
    val existingHighlightId: Long? = null,
    val colorIndex: Int = 0,
    val existingNote: String = ""
) {
    val anchorTopY: Float
        get() = rects.minOfOrNull { it.top } ?: 160f

    val anchorBottomY: Float
        get() = rects.maxOfOrNull { it.bottom } ?: 260f
}

data class ActiveTranslationCard(
    val sourceText: String,
    val targetLanguage: TranslationLanguage,
    val isLoading: Boolean = true,
    val entry: TranslationEntryEntity? = null,
    val fromOfflineCache: Boolean = false,
    val errorHint: String? = null
)

data class ReaderUiState(
    val book: BookEntity? = null,
    val isLoading: Boolean = true,
    val isPdf: Boolean = false,
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val estimatedMinutesLeft: Int = 1,
    val currentChapterTitle: String = "",
    val currentBitmap: Bitmap? = null,
    val prevBitmap: Bitmap? = null,
    val nextBitmap: Bitmap? = null,
    val tocItems: List<ChapterTocItem> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    val isCurrentPageBookmarked: Boolean = false,
    val isOverlayVisible: Boolean = false,
    val isSettingsExpanded: Boolean = false,
    val isSearchOpen: Boolean = false,
    val isNotebookOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val returnToPageOrigin: Int? = null,
    val activeSelection: ActiveTextSelection? = null,
    val editingNoteHighlight: HighlightEntity? = null,
    val pdfSelectionPromptText: String? = null,
    val searchGlowRects: List<RectF> = emptyList(),
    val searchGlowAlpha: Float = 0f,
    // Reading & Dictionary Tools
    val activeTranslation: ActiveTranslationCard? = null,
    val quoteCardQuote: String? = null,
    val pinnedPageIndex: Int? = null,
    val isPeekingPinnedPage: Boolean = false,
    val pinnedPageBitmap: Bitmap? = null,
    val pdfZoomScale: Float = 1f,
    val pdfPanOffset: Offset = Offset.Zero,
    val isPdfZoomLocked: Boolean = false,
    val showBrightnessIndicator: Boolean = false,
    val isTtsPlaying: Boolean = false,
    val sleepTimerRemainingSec: Int = 0,
    val isAutoFlipPaused: Boolean = false,
    val autoFlipTrigger: Int = 0,
    val toastMessage: String? = null
)

class ReaderViewModel(
    private val appContext: Context,
    private val bookId: Long,
    private val repository: LibraryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val audioEngine: PaperAudioEngine,
    private val translationService: TranslationService
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = preferencesRepository.settings

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val bitmapRenderer = PageBitmapRenderer()
    private val ttsEngine = ReaderTtsEngine(appContext) {
        viewModelScope.launch {
            onTtsPageFinished()
        }
    }

    private var parsedBook: ParsedBook? = null
    private var paginatedPages: List<PaginatedPage> = emptyList()
    private var currentGeometry: PageTextGeometry? = null

    // Viewport metrics
    private var viewportSize = IntSize.Zero
    private var screenDensity = 2f
    private var topInsetPx = 0
    private var bottomInsetPx = 0

    // In-memory 3-page + neighbor bitmap cache
    private val pageCache = LinkedHashMap<Int, Bitmap>(6, 0.75f, true)
    private var renderJob: Job? = null
    private var returnPillJob: Job? = null
    private var glowJob: Job? = null
    private var searchJob: Job? = null
    private var toastJob: Job? = null
    private var brightnessHideJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var autoFlipJob: Job? = null
    private var readingMinutesJob: Job? = null
    private var frostFadeJob: Job? = null

    private var lastPaginationSignature: String = ""

    init {
        loadBookAndObserve()
    }

    private fun loadBookAndObserve() {
        viewModelScope.launch {
            val entity = repository.getBookById(bookId)
            if (entity == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    book = entity,
                    isPdf = entity.bookFormat == BookFormat.PDF,
                    currentPage = entity.currentPage.coerceAtLeast(0),
                    totalPages = entity.totalPages.coerceAtLeast(1)
                )
            }

            // Start active minute tracker (STRICTLY skipped if book.isHidden == true)
            if (!entity.isHidden) {
                startVisibleReadingMinuteTracker()
            }

            launch {
                repository.getBookmarks(bookId).collectLatest { list ->
                    _uiState.update { state ->
                        state.copy(
                            bookmarks = list,
                            isCurrentPageBookmarked = list.any { it.pageIndex == state.currentPage }
                        )
                    }
                }
            }
            launch {
                repository.getHighlights(bookId).collectLatest { list ->
                    _uiState.update { it.copy(highlights = list) }
                    invalidateAndRenderAroundCurrent()
                }
            }

            val parsed = withContext(Dispatchers.IO) {
                BookDocumentParser.loadBook(
                    filePath = entity.filePath,
                    format = entity.bookFormat,
                    fallbackTitle = entity.title,
                    fallbackAuthor = entity.author
                )
            }
            parsedBook = parsed

            if (parsed is ParsedBook.PdfBook) {
                val safePage = entity.currentPage.coerceIn(0, (parsed.pageCount - 1).coerceAtLeast(0))
                val toc = (0 until parsed.pageCount step 5).map { idx ->
                    ChapterTocItem(title = "Page ${idx + 1}", pageIndex = idx, chapterIndex = idx / 5)
                }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isPdf = true,
                        currentPage = safePage,
                        totalPages = parsed.pageCount,
                        estimatedMinutesLeft = computeEstMinutesLeft(safePage, parsed.pageCount),
                        currentChapterTitle = "Page ${safePage + 1}",
                        tocItems = toc,
                        isCurrentPageBookmarked = state.bookmarks.any { it.pageIndex == safePage }
                    )
                }
                if (viewportSize.width > 0 && viewportSize.height > 0) {
                    invalidateAndRenderAroundCurrent()
                }
            } else {
                if (viewportSize.width > 0 && viewportSize.height > 0) {
                    repaginateIfNeeded(force = true)
                }
            }

            // Observe settings changes for typography, PDF auto-crop, ambient sound, auto-flip
            launch {
                var lastAmbientMode: AmbientSoundMode? = null
                var lastAutoFlip = -1
                preferencesRepository.settings.collectLatest { s ->
                    if (s.ambientSoundMode != lastAmbientMode) {
                        lastAmbientMode = s.ambientSoundMode
                        audioEngine.setAmbientSound(s.ambientSoundMode, s.ambientVolume)
                    } else {
                        audioEngine.updateAmbientVolume(s.ambientVolume)
                    }

                    if (s.autoFlipSeconds != lastAutoFlip) {
                        lastAutoFlip = s.autoFlipSeconds
                        restartAutoFlipLoop(s.autoFlipSeconds)
                    }

                    if (viewportSize.width > 0 && viewportSize.height > 0 && parsedBook != null) {
                        if (parsedBook is ParsedBook.TextBook) {
                            repaginateIfNeeded(force = false)
                        } else {
                            invalidateAndRenderAroundCurrent()
                        }
                    }
                }
            }
        }
    }

    private fun startVisibleReadingMinuteTracker() {
        readingMinutesJob?.cancel()
        readingMinutesJob = viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                val book = _uiState.value.book ?: continue
                repository.recordVisibleReadingActivity(
                    isHiddenBook = book.isHidden,
                    deltaMinutes = 1,
                    deltaPages = 0
                )
            }
        }
    }

    fun onViewportReady(size: IntSize, density: Float, topInset: Int, bottomInset: Int) {
        if (size.width <= 0 || size.height <= 0) return
        val changed = size != viewportSize || topInset != topInsetPx || bottomInset != bottomInsetPx
        viewportSize = size
        screenDensity = density
        topInsetPx = topInset
        bottomInsetPx = bottomInset

        if (changed && parsedBook != null) {
            if (parsedBook is ParsedBook.TextBook) {
                repaginateIfNeeded(force = true)
            } else {
                invalidateAndRenderAroundCurrent()
            }
        }
    }

    private fun repaginateIfNeeded(force: Boolean) {
        val textBook = parsedBook as? ParsedBook.TextBook ?: return
        val s = settings.value
        val layoutSig = "${viewportSize.width}x${viewportSize.height}_${topInsetPx}_${bottomInsetPx}_" +
            "${s.fontStyle}_${s.fontSizeSp}_${s.lineSpacing}_${s.marginDp}_${s.bionicModeEnabled}"
        val visualChangedOnly = !force && layoutSig == lastPaginationSignature

        if (visualChangedOnly) {
            invalidateAndRenderAroundCurrent()
            return
        }
        lastPaginationSignature = layoutSig

        viewModelScope.launch {
            val prevState = _uiState.value
            val prevRatio = if (prevState.totalPages <= 1) 0f else {
                prevState.currentPage.toFloat() / (prevState.totalPages - 1).coerceAtLeast(1)
            }
            val wasFirstLoad = paginatedPages.isEmpty()

            val result = withContext(Dispatchers.Default) {
                Paginator.paginate(
                    chapters = textBook.chapters,
                    widthPx = viewportSize.width,
                    heightPx = viewportSize.height,
                    density = screenDensity,
                    topInsetPx = topInsetPx,
                    bottomInsetPx = bottomInsetPx,
                    settings = s,
                    includeFeaturedDedication = false
                )
            }
            paginatedPages = result.pages
            val total = result.pages.size.coerceAtLeast(1)
            val newPage = if (wasFirstLoad) {
                (prevState.book?.currentPage ?: 0).coerceIn(0, total - 1)
            } else {
                (prevRatio * (total - 1)).toInt().coerceIn(0, total - 1)
            }

            _uiState.update { st ->
                st.copy(
                    isLoading = false,
                    currentPage = newPage,
                    totalPages = total,
                    estimatedMinutesLeft = computeEstMinutesLeft(newPage, total),
                    currentChapterTitle = result.pages.getOrNull(newPage)?.chapterTitle.orEmpty(),
                    tocItems = result.toc,
                    isCurrentPageBookmarked = st.bookmarks.any { it.pageIndex == newPage },
                    activeSelection = null
                )
            }

            persistProgress(newPage, total)
            invalidateAndRenderAroundCurrent()
        }
    }

    private fun computeEstMinutesLeft(pageIndex: Int, totalPages: Int): Int {
        val remainingPages = (totalPages - 1 - pageIndex).coerceAtLeast(0)
        if (remainingPages == 0) return 0
        val pb = parsedBook
        return if (pb is ParsedBook.TextBook && paginatedPages.isNotEmpty()) {
            var remainingWords = 0
            for (i in (pageIndex + 1) until paginatedPages.size) {
                val txt = paginatedPages[i].bodyText
                if (txt.isNotEmpty()) {
                    remainingWords += txt.split(Regex("\\s+")).size
                }
            }
            ceil(remainingWords / 210.0).toInt().coerceAtLeast(1)
        } else {
            ceil(remainingPages * 1.2).toInt().coerceAtLeast(1)
        }
    }

    private fun invalidateAndRenderAroundCurrent() {
        pageCache.clear()
        renderWindow(_uiState.value.currentPage)
    }

    private fun renderWindow(centerPage: Int) {
        val pb = parsedBook ?: return
        val w = viewportSize.width
        val h = viewportSize.height
        if (w <= 0 || h <= 0) return

        val s = settings.value
        val highlights = _uiState.value.highlights
        val total = _uiState.value.totalPages

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val currBmp = getOrRenderPage(pb, centerPage, total, w, h, s, highlights)
            val prevBmp = if (centerPage > 0) getOrRenderPage(pb, centerPage - 1, total, w, h, s, highlights) else null
            val nextBmp = if (centerPage < total - 1) getOrRenderPage(pb, centerPage + 1, total, w, h, s, highlights) else null

            currentGeometry = if (pb is ParsedBook.TextBook) {
                val page = paginatedPages.getOrNull(centerPage)
                if (page != null) {
                    withContext(Dispatchers.Default) {
                        Paginator.buildPageGeometry(
                            page = page,
                            widthPx = w,
                            heightPx = h,
                            density = screenDensity,
                            topInsetPx = topInsetPx,
                            bottomInsetPx = bottomInsetPx,
                            settings = s
                        )
                    }
                } else null
            } else null

            _uiState.update { st ->
                st.copy(
                    currentBitmap = currBmp,
                    prevBitmap = prevBmp,
                    nextBitmap = nextBmp
                )
            }

            val keysToDrop = pageCache.keys.filter { abs(it - centerPage) > 2 }
            keysToDrop.forEach { k -> pageCache.remove(k) }
        }
    }

    private suspend fun getOrRenderPage(
        pb: ParsedBook,
        pageIndex: Int,
        totalPages: Int,
        w: Int,
        h: Int,
        s: ReaderSettings,
        allHighlights: List<HighlightEntity>
    ): Bitmap? {
        pageCache[pageIndex]?.let { return it }
        val pageHighlights = allHighlights.filter { it.pageIndex == pageIndex }
        val estLeft = computeEstMinutesLeft(pageIndex, totalPages)
        val bmp = withContext(Dispatchers.Default) {
            when (pb) {
                is ParsedBook.TextBook -> {
                    val page = paginatedPages.getOrNull(pageIndex) ?: return@withContext null
                    bitmapRenderer.renderTextPage(
                        page = page,
                        totalPages = totalPages,
                        estimatedMinutesLeft = estLeft,
                        widthPx = w,
                        heightPx = h,
                        density = screenDensity,
                        topInsetPx = topInsetPx,
                        bottomInsetPx = bottomInsetPx,
                        settings = s,
                        highlights = pageHighlights,
                        isFeaturedStory = false
                    )
                }
                is ParsedBook.PdfBook -> {
                    bitmapRenderer.renderPdfPage(
                        pdfFilePath = pb.filePath,
                        pageIndex = pageIndex,
                        totalPages = totalPages,
                        estimatedMinutesLeft = estLeft,
                        widthPx = w,
                        heightPx = h,
                        density = screenDensity,
                        topInsetPx = topInsetPx,
                        bottomInsetPx = bottomInsetPx,
                        settings = s,
                        highlights = pageHighlights
                    )
                }
            }
        }
        if (bmp != null) {
            pageCache[pageIndex] = bmp
        }
        return bmp
    }

    // --- Page Turning & Navigation ---
    fun commitPageTurn(delta: Int) {
        val st = _uiState.value
        val target = (st.currentPage + delta).coerceIn(0, (st.totalPages - 1).coerceAtLeast(0))
        if (target == st.currentPage) return

        val chapterTitle = getChapterTitleForPage(target)
        val keepZoom = st.isPdfZoomLocked
        _uiState.update { state ->
            state.copy(
                currentPage = target,
                estimatedMinutesLeft = computeEstMinutesLeft(target, state.totalPages),
                currentChapterTitle = chapterTitle,
                isCurrentPageBookmarked = state.bookmarks.any { it.pageIndex == target },
                activeSelection = null,
                activeTranslation = null,
                searchGlowRects = emptyList(),
                searchGlowAlpha = 0f,
                pdfZoomScale = if (keepZoom) state.pdfZoomScale else 1f,
                pdfPanOffset = if (keepZoom) state.pdfPanOffset else Offset.Zero,
                currentBitmap = pageCache[target] ?: state.currentBitmap,
                prevBitmap = pageCache[target - 1],
                nextBitmap = pageCache[target + 1]
            )
        }
        persistProgress(target, st.totalPages)
        // Log 1 visible page read (STRICTLY skipped if book.isHidden == true)
        st.book?.let { b ->
            viewModelScope.launch {
                repository.recordVisibleReadingActivity(isHiddenBook = b.isHidden, deltaMinutes = 0, deltaPages = 1)
            }
        }
        renderWindow(target)

        if (_uiState.value.isTtsPlaying) {
            speakCurrentPage()
        }
    }

    fun jumpToPage(
        targetPage: Int,
        offerReturnPill: Boolean = true,
        highlightSearchRange: Pair<Int, Int>? = null
    ) {
        val st = _uiState.value
        val clamped = targetPage.coerceIn(0, (st.totalPages - 1).coerceAtLeast(0))
        val previousPage = st.currentPage

        if (offerReturnPill && abs(clamped - previousPage) >= 3) {
            showReturnToPagePill(previousPage)
        }

        val chapterTitle = getChapterTitleForPage(clamped)
        _uiState.update { state ->
            state.copy(
                currentPage = clamped,
                estimatedMinutesLeft = computeEstMinutesLeft(clamped, state.totalPages),
                currentChapterTitle = chapterTitle,
                isCurrentPageBookmarked = state.bookmarks.any { it.pageIndex == clamped },
                activeSelection = null,
                activeTranslation = null,
                isSearchOpen = false,
                isNotebookOpen = false
            )
        }
        persistProgress(clamped, st.totalPages)
        renderWindow(clamped)

        if (highlightSearchRange != null) {
            triggerSearchMatchGlow(clamped, highlightSearchRange.first, highlightSearchRange.second)
        }
        if (_uiState.value.isTtsPlaying) {
            speakCurrentPage()
        }
    }

    fun returnToOriginPage() {
        val origin = _uiState.value.returnToPageOrigin ?: return
        returnPillJob?.cancel()
        _uiState.update { it.copy(returnToPageOrigin = null) }
        jumpToPage(origin, offerReturnPill = false)
    }

    private fun showReturnToPagePill(originPage: Int) {
        returnPillJob?.cancel()
        _uiState.update { it.copy(returnToPageOrigin = originPage) }
        returnPillJob = viewModelScope.launch {
            delay(5000L)
            _uiState.update { it.copy(returnToPageOrigin = null) }
        }
    }

    // --- Quick-Peek Pin ---
    fun togglePinCurrentPage() {
        val st = _uiState.value
        if (st.pinnedPageIndex == st.currentPage) {
            _uiState.update { it.copy(pinnedPageIndex = null, isPeekingPinnedPage = false, pinnedPageBitmap = null) }
            showToast("Page unpinned")
        } else {
            _uiState.update { it.copy(pinnedPageIndex = st.currentPage) }
            showToast("Pinned Page ${st.currentPage + 1}")
        }
    }

    fun openPinnedPagePeek() {
        val pinned = _uiState.value.pinnedPageIndex ?: return
        val pb = parsedBook ?: return
        viewModelScope.launch {
            val bmp = getOrRenderPage(
                pb = pb,
                pageIndex = pinned,
                totalPages = _uiState.value.totalPages,
                w = viewportSize.width,
                h = viewportSize.height,
                s = settings.value,
                allHighlights = _uiState.value.highlights
            )
            _uiState.update { it.copy(isPeekingPinnedPage = true, pinnedPageBitmap = bmp, isOverlayVisible = false) }
        }
    }

    fun closePinnedPagePeek() {
        _uiState.update { it.copy(isPeekingPinnedPage = false) }
    }

    // --- Left-Edge Brightness Gesture ---
    fun onLeftEdgeBrightnessDrag(deltaFraction: Float) {
        preferencesRepository.updateSettings { s ->
            s.copy(brightnessLevel = (s.brightnessLevel + deltaFraction).coerceIn(0.15f, 1.0f))
        }
        _uiState.update { it.copy(showBrightnessIndicator = true) }
        brightnessHideJob?.cancel()
    }

    fun onLeftEdgeBrightnessEnd() {
        brightnessHideJob?.cancel()
        brightnessHideJob = viewModelScope.launch {
            delay(1100L)
            _uiState.update { it.copy(showBrightnessIndicator = false) }
        }
    }

    // --- PDF Pinch-Zoom & Zoom Lock ---
    fun onPdfPinchTransform(scale: Float, pan: Offset) {
        val clamped = scale.coerceIn(1.0f, 2.8f)
        _uiState.update {
            it.copy(
                pdfZoomScale = clamped,
                pdfPanOffset = if (clamped <= 1.02f) Offset.Zero else pan,
                isPdfZoomLocked = if (clamped <= 1.02f) false else it.isPdfZoomLocked
            )
        }
    }

    fun togglePdfZoomLock() {
        _uiState.update { st ->
            val next = !st.isPdfZoomLocked
            st.copy(isPdfZoomLocked = next)
        }
        val locked = _uiState.value.isPdfZoomLocked
        showToast(if (locked) "PDF zoom locked across pages" else "PDF zoom unlocked")
    }

    fun resetPdfZoom() {
        _uiState.update { it.copy(pdfZoomScale = 1f, pdfPanOffset = Offset.Zero, isPdfZoomLocked = false) }
    }

    // --- Offline Read-Aloud (TTS) ---
    fun toggleTts() {
        if (_uiState.value.isTtsPlaying) {
            stopTts()
        } else {
            _uiState.update { it.copy(isTtsPlaying = true) }
            speakCurrentPage()
        }
    }

    fun stopTts() {
        ttsEngine.stop()
        _uiState.update { it.copy(isTtsPlaying = false) }
    }

    fun setTtsSpeed(speed: Float) {
        preferencesRepository.updateSettings { it.copy(ttsSpeed = speed.coerceIn(0.75f, 2.0f)) }
        ttsEngine.setSpeed(speed)
        if (_uiState.value.isTtsPlaying) {
            speakCurrentPage()
        }
    }

    private fun speakCurrentPage() {
        val pageIdx = _uiState.value.currentPage
        val text = when (val pb = parsedBook) {
            is ParsedBook.TextBook -> {
                val p = paginatedPages.getOrNull(pageIdx)
                listOfNotNull(p?.headingText, p?.bodyText).joinToString(". ")
            }
            is ParsedBook.PdfBook -> pb.pageTexts.getOrNull(pageIdx).orEmpty()
            null -> ""
        }
        if (text.isBlank()) {
            stopTts()
            showToast("No readable text on this page")
            return
        }
        ttsEngine.speak(text, settings.value.ttsSpeed)
    }

    private fun onTtsPageFinished() {
        val st = _uiState.value
        if (!st.isTtsPlaying) return
        if (st.currentPage < st.totalPages - 1) {
            // Trigger smooth curved flip to next page and continue reading
            _uiState.update { it.copy(autoFlipTrigger = it.autoFlipTrigger + 1) }
        } else {
            stopTts()
            showToast("Finished reading volume")
        }
    }

    // --- Sleep Timer (fades out both Ambient Sound & TTS) ---
    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        audioEngine.setAmbientFadeMultiplier(1.0f)
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerRemainingSec = 0) }
            return
        }
        val totalSec = minutes * 60
        _uiState.update { it.copy(sleepTimerRemainingSec = totalSec) }
        showToast("Sleep timer set for ${minutes}m")

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _uiState.update { it.copy(sleepTimerRemainingSec = remaining) }
                if (remaining in 1..15) {
                    audioEngine.setAmbientFadeMultiplier(remaining / 15f)
                }
            }
            if (isActive) {
                stopTts()
                audioEngine.stopAmbient()
                preferencesRepository.updateSettings { it.copy(ambientSoundMode = AmbientSoundMode.OFF) }
                _uiState.update { it.copy(sleepTimerRemainingSec = 0) }
                showToast("Sleep timer finished")
            }
        }
    }

    // --- Auto-Flip Timer ---
    fun setAutoFlipSeconds(seconds: Int) {
        preferencesRepository.updateSettings { it.copy(autoFlipSeconds = seconds) }
        _uiState.update { it.copy(isAutoFlipPaused = false) }
    }

    fun toggleAutoFlipPause() {
        _uiState.update { it.copy(isAutoFlipPaused = !it.isAutoFlipPaused) }
    }

    private fun restartAutoFlipLoop(intervalSeconds: Int) {
        autoFlipJob?.cancel()
        if (intervalSeconds <= 0) return
        autoFlipJob = viewModelScope.launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                val st = _uiState.value
                if (!st.isAutoFlipPaused && !st.isOverlayVisible && !st.isSearchOpen &&
                    !st.isNotebookOpen && st.activeSelection == null && st.currentPage < st.totalPages - 1
                ) {
                    _uiState.update { it.copy(autoFlipTrigger = it.autoFlipTrigger + 1) }
                }
            }
        }
    }

    // --- Multi-Language Translation & Offline Word Bank ---
    fun translateActiveSelection() {
        val sel = _uiState.value.activeSelection ?: return
        val source = sel.selectedText
        val preferred = settings.value.defaultTargetLanguage
        val (_, effectiveTarget) = translationService.resolveTargetLanguage(source, preferred, explicitOverride = false)
        _uiState.update { it.copy(activeSelection = null) }
        runTranslation(source, effectiveTarget, explicitOverride = false)
    }

    fun switchTranslationLanguage(targetLanguage: TranslationLanguage) {
        val source = _uiState.value.activeTranslation?.sourceText ?: return
        runTranslation(source, targetLanguage, explicitOverride = true)
    }

    private fun runTranslation(
        source: String,
        targetLanguage: TranslationLanguage,
        explicitOverride: Boolean
    ) {
        val bookTitle = _uiState.value.book?.title.orEmpty()
        _uiState.update {
            it.copy(
                activeTranslation = ActiveTranslationCard(
                    sourceText = source,
                    targetLanguage = targetLanguage,
                    isLoading = true
                )
            )
        }
        viewModelScope.launch {
            val result = translationService.translate(
                rawText = source,
                targetLanguage = targetLanguage,
                bookTitle = bookTitle,
                optionalGeminiApiKey = settings.value.geminiApiKey,
                explicitTargetOverride = explicitOverride
            )
            _uiState.update {
                it.copy(
                    activeTranslation = ActiveTranslationCard(
                        sourceText = source,
                        targetLanguage = result.entry.targetLanguageEnum,
                        isLoading = false,
                        entry = result.entry,
                        fromOfflineCache = result.fromOfflineCache,
                        errorHint = result.errorHint
                    )
                )
            }
        }
    }

    fun toggleSaveActiveTranslation() {
        val card = _uiState.value.activeTranslation ?: return
        val entry = card.entry ?: return
        if (entry.id == 0L) {
            showToast("Connect briefly to translate before saving")
            return
        }
        val nextSaved = !entry.isSavedToWordBank
        viewModelScope.launch {
            repository.setWordBankSaved(entry.id, nextSaved)
            _uiState.update { st ->
                st.copy(
                    activeTranslation = st.activeTranslation?.copy(
                        entry = entry.copy(isSavedToWordBank = nextSaved)
                    )
                )
            }
            showToast(if (nextSaved) "Saved to Word Bank" else "Removed from Word Bank")
        }
    }

    fun dismissTranslationCard() {
        _uiState.update { it.copy(activeTranslation = null) }
    }

    // --- Quote Card Maker ---
    fun openQuoteCard(quoteText: String) {
        _uiState.update {
            it.copy(
                activeSelection = null,
                quoteCardQuote = quoteText.trim()
            )
        }
    }

    fun dismissQuoteCard() {
        _uiState.update { it.copy(quoteCardQuote = null) }
    }

    // --- Search Match Glow ---
    private fun triggerSearchMatchGlow(pageIndex: Int, startOffset: Int, endOffset: Int) {
        glowJob?.cancel()
        glowJob = viewModelScope.launch {
            delay(90L)
            val rects = if (parsedBook is ParsedBook.TextBook) {
                val page = paginatedPages.getOrNull(pageIndex)
                if (page != null && viewportSize.width > 0) {
                    val geom = Paginator.buildPageGeometry(
                        page = page,
                        widthPx = viewportSize.width,
                        heightPx = viewportSize.height,
                        density = screenDensity,
                        topInsetPx = topInsetPx,
                        bottomInsetPx = bottomInsetPx,
                        settings = settings.value
                    )
                    geom.getSelectionRects(startOffset, endOffset)
                } else emptyList()
            } else {
                listOf(
                    RectF(
                        viewportSize.width * 0.14f,
                        viewportSize.height * 0.42f,
                        viewportSize.width * 0.86f,
                        viewportSize.height * 0.48f
                    )
                )
            }

            if (rects.isEmpty()) return@launch
            _uiState.update { it.copy(searchGlowRects = rects) }

            val totalMs = 2200L
            val stepMs = 16L
            var elapsed = 0L
            while (elapsed < totalMs) {
                val frac = elapsed.toFloat() / totalMs
                val pulse = abs(sin(frac * PI * 2.5)).toFloat() * (1f - frac * 0.65f)
                _uiState.update { it.copy(searchGlowAlpha = pulse.coerceIn(0f, 1f)) }
                delay(stepMs)
                elapsed += stepMs
            }
            _uiState.update { it.copy(searchGlowRects = emptyList(), searchGlowAlpha = 0f) }
        }
    }

    fun getChapterTitleForPage(pageIndex: Int): String {
        return when (val pb = parsedBook) {
            is ParsedBook.TextBook -> paginatedPages.getOrNull(pageIndex)?.chapterTitle ?: pb.title
            is ParsedBook.PdfBook -> "Page ${pageIndex + 1}"
            null -> ""
        }
    }

    private fun persistProgress(page: Int, total: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(bookId, page, total)
        }
    }

    // --- Sound & Haptics ---
    fun playPaperRustle(velocityNorm: Float) {
        val s = settings.value
        if (s.paperSoundEnabled) {
            audioEngine.playPageRustle(velocityNorm, s.pageStiffness)
        }
    }

    fun triggerPageLandHaptic() {
        if (settings.value.hapticEnabled) {
            audioEngine.triggerPageLandHaptic()
        }
    }

    // --- Overlays & Settings ---
    fun toggleOverlay() {
        _uiState.update { st ->
            when {
                st.activeTranslation != null -> st.copy(activeTranslation = null)
                st.activeSelection != null -> st.copy(activeSelection = null)
                else -> {
                    val nextVisible = !st.isOverlayVisible
                    st.copy(
                        isOverlayVisible = nextVisible,
                        isSettingsExpanded = if (!nextVisible) false else st.isSettingsExpanded
                    )
                }
            }
        }
    }

    fun setSettingsExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isSettingsExpanded = expanded) }
    }

    fun openSearch(open: Boolean) {
        _uiState.update {
            it.copy(
                isSearchOpen = open,
                isOverlayVisible = if (open) false else it.isOverlayVisible,
                isSettingsExpanded = false
            )
        }
    }

    fun openNotebook(open: Boolean) {
        _uiState.update {
            it.copy(
                isNotebookOpen = open,
                isOverlayVisible = if (open) false else it.isOverlayVisible,
                isSettingsExpanded = false
            )
        }
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        preferencesRepository.updateSettings(transform)
    }

    // --- Bookmarks ---
    fun toggleBookmark() {
        val st = _uiState.value
        val pageIdx = st.currentPage
        val chapter = getChapterTitleForPage(pageIdx)
        val excerpt = when (val pb = parsedBook) {
            is ParsedBook.TextBook -> paginatedPages.getOrNull(pageIdx)?.bodyText?.take(110).orEmpty()
            is ParsedBook.PdfBook -> pb.pageTexts.getOrNull(pageIdx)?.take(110).orEmpty()
            null -> ""
        }
        viewModelScope.launch {
            val added = repository.toggleBookmark(bookId, pageIdx, chapter, excerpt)
            showToast(if (added) "Bookmarked Page ${pageIdx + 1}" else "Bookmark removed")
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    // --- Text Selection, Highlights & Notes ---
    fun onLongPressAt(offset: Offset) {
        val st = _uiState.value
        val pb = parsedBook ?: return

        if (pb is ParsedBook.PdfBook) {
            val pageText = pb.pageTexts.getOrNull(st.currentPage).orEmpty().trim()
            _uiState.update {
                it.copy(
                    isOverlayVisible = false,
                    pdfSelectionPromptText = pageText.ifEmpty { "Page ${st.currentPage + 1} (${pb.title})" }
                )
            }
            return
        }

        val geom = currentGeometry ?: return
        val word = geom.findWordAt(offset.x, offset.y) ?: return
        val rects = geom.getSelectionRects(word.startOffset, word.endOffset)
        if (rects.isEmpty()) return

        _uiState.update {
            it.copy(
                isOverlayVisible = false,
                isSettingsExpanded = false,
                activeTranslation = null,
                activeSelection = ActiveTextSelection(
                    pageIndex = st.currentPage,
                    startOffset = word.startOffset,
                    endOffset = word.endOffset,
                    selectedText = geom.bodyText.substring(word.startOffset, word.endOffset).trim(),
                    rects = rects,
                    existingHighlightId = null,
                    colorIndex = 0
                )
            )
        }
    }

    fun dismissPdfSelectionPrompt() {
        _uiState.update { it.copy(pdfSelectionPromptText = null) }
    }

    fun savePdfHighlight(excerpt: String, colorIndex: Int, note: String = "") {
        val st = _uiState.value
        if (excerpt.isBlank()) return
        viewModelScope.launch {
            repository.insertHighlight(
                HighlightEntity(
                    bookId = bookId,
                    pageIndex = st.currentPage,
                    startOffset = 0,
                    endOffset = excerpt.length,
                    quoteText = excerpt.trim(),
                    colorIndex = colorIndex,
                    note = note.trim(),
                    chapterTitle = "Page ${st.currentPage + 1}"
                )
            )
            _uiState.update { it.copy(pdfSelectionPromptText = null) }
            showToast("Highlight saved")
        }
    }

    fun onDragSelectionHandle(isStartHandle: Boolean, position: Offset) {
        val sel = _uiState.value.activeSelection ?: return
        val geom = currentGeometry ?: return
        val word = geom.findWordAt(position.x, position.y, slopPx = 42f) ?: return

        val newStart = if (isStartHandle) minOf(word.startOffset, sel.endOffset - 1).coerceAtLeast(0) else sel.startOffset
        val newEnd = if (!isStartHandle) maxOf(word.endOffset, sel.startOffset + 1).coerceAtMost(geom.bodyText.length) else sel.endOffset
        if (newStart >= newEnd) return

        val rects = geom.getSelectionRects(newStart, newEnd)
        if (rects.isEmpty()) return

        _uiState.update {
            it.copy(
                activeSelection = sel.copy(
                    startOffset = newStart,
                    endOffset = newEnd,
                    selectedText = geom.bodyText.substring(newStart, newEnd).trim(),
                    rects = rects
                )
            )
        }
    }

    /**
     * Handles resting taps:
     * 1. Dismisses active selection / translation card
     * 2. Detects taps on existing highlights
     */
    fun onTapAtRestingPage(offset: Offset): Boolean {
        val st = _uiState.value
        if (st.activeTranslation != null) {
            _uiState.update { it.copy(activeTranslation = null) }
            return true
        }
        if (st.activeSelection != null) {
            _uiState.update { it.copy(activeSelection = null) }
            return true
        }
        val geom = currentGeometry ?: return false

        // Tap existing highlight
        val pageHighlights = st.highlights.filter { it.pageIndex == st.currentPage }
        for (hl in pageHighlights) {
            val rects = geom.getSelectionRects(hl.startOffset, hl.endOffset)
            if (rects.any { it.contains(offset.x, offset.y) }) {
                _uiState.update {
                    it.copy(
                        isOverlayVisible = false,
                        isSettingsExpanded = false,
                        activeSelection = ActiveTextSelection(
                            pageIndex = st.currentPage,
                            startOffset = hl.startOffset,
                            endOffset = hl.endOffset,
                            selectedText = hl.quoteText,
                            rects = rects,
                            existingHighlightId = hl.id,
                            colorIndex = hl.colorIndex,
                            existingNote = hl.note
                        )
                    )
                }
                return true
            }
        }
        return false
    }

    fun applyHighlightColor(colorIndex: Int) {
        val sel = _uiState.value.activeSelection ?: return
        viewModelScope.launch {
            if (sel.existingHighlightId != null) {
                repository.updateHighlightColor(sel.existingHighlightId, colorIndex)
                _uiState.update { it.copy(activeSelection = null) }
            } else {
                repository.insertHighlight(
                    HighlightEntity(
                        bookId = bookId,
                        pageIndex = sel.pageIndex,
                        startOffset = sel.startOffset,
                        endOffset = sel.endOffset,
                        quoteText = sel.selectedText,
                        colorIndex = colorIndex,
                        note = "",
                        chapterTitle = getChapterTitleForPage(sel.pageIndex)
                    )
                )
                _uiState.update { it.copy(activeSelection = null) }
            }
        }
    }

    fun openNoteEditorForSelection() {
        val sel = _uiState.value.activeSelection ?: return
        viewModelScope.launch {
            val hl = if (sel.existingHighlightId != null) {
                _uiState.value.highlights.firstOrNull { it.id == sel.existingHighlightId }
            } else {
                val newEntity = HighlightEntity(
                    bookId = bookId,
                    pageIndex = sel.pageIndex,
                    startOffset = sel.startOffset,
                    endOffset = sel.endOffset,
                    quoteText = sel.selectedText,
                    colorIndex = sel.colorIndex,
                    note = "",
                    chapterTitle = getChapterTitleForPage(sel.pageIndex)
                )
                val newId = repository.insertHighlight(newEntity)
                newEntity.copy(id = newId)
            }
            _uiState.update { it.copy(activeSelection = null, editingNoteHighlight = hl) }
        }
    }

    fun openNoteEditorForHighlight(highlight: HighlightEntity) {
        _uiState.update { it.copy(editingNoteHighlight = highlight) }
    }

    fun saveHighlightNote(highlightId: Long, note: String) {
        viewModelScope.launch {
            repository.updateHighlightNote(highlightId, note.trim())
            _uiState.update { it.copy(editingNoteHighlight = null) }
            showToast("Note saved")
        }
    }

    fun dismissNoteEditor() {
        _uiState.update { it.copy(editingNoteHighlight = null) }
    }

    fun removeActiveHighlight() {
        val id = _uiState.value.activeSelection?.existingHighlightId ?: return
        viewModelScope.launch {
            repository.deleteHighlight(id)
            _uiState.update { it.copy(activeSelection = null) }
            showToast("Highlight removed")
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch { repository.deleteHighlight(id) }
    }

    fun dismissSelection() {
        _uiState.update { it.copy(activeSelection = null) }
    }

    fun buildSelectionOverlay(): SelectionOverlayState? {
        val sel = _uiState.value.activeSelection ?: return null
        if (sel.rects.isEmpty()) return null
        val first = sel.rects.first()
        val last = sel.rects.last()
        val colorArgb = HighlightColors[sel.colorIndex.coerceIn(0, HighlightColors.lastIndex)].toArgb()
        return SelectionOverlayState(
            rects = sel.rects,
            startHandle = if (sel.existingHighlightId == null) PointF(first.left, first.bottom + 8f) else null,
            endHandle = if (sel.existingHighlightId == null) PointF(last.right, last.bottom + 8f) else null,
            highlightColorArgb = colorArgb
        )
    }

    // --- In-Book Search ---
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(140L)
            val pb = parsedBook ?: return@launch
            val results = withContext(Dispatchers.Default) {
                val out = mutableListOf<SearchResult>()
                val pageTexts: List<Pair<String, String>> = when (pb) {
                    is ParsedBook.TextBook -> paginatedPages.map { it.chapterTitle to it.bodyText }
                    is ParsedBook.PdfBook -> pb.pageTexts.mapIndexed { i, txt -> "Page ${i + 1}" to txt }
                }

                pageTexts.forEachIndexed { pageIdx, (chapterTitle, text) ->
                    if (out.size >= 80) return@forEachIndexed
                    var start = 0
                    while (start < text.length && out.size < 80) {
                        val idx = text.indexOf(trimmed, start, ignoreCase = true)
                        if (idx < 0) break
                        val end = idx + trimmed.length
                        val snipStart = (idx - 38).coerceAtLeast(0)
                        val snipEnd = (end + 52).coerceAtMost(text.length)
                        val before = (if (snipStart > 0) "…" else "") + text.substring(snipStart, idx).replace('\n', ' ')
                        val matched = text.substring(idx, end)
                        val after = text.substring(end, snipEnd).replace('\n', ' ') + (if (snipEnd < text.length) "…" else "")
                        out.add(
                            SearchResult(
                                pageIndex = pageIdx,
                                chapterTitle = chapterTitle,
                                snippetBefore = before,
                                matchText = matched,
                                snippetAfter = after,
                                startOffsetInPage = idx,
                                endOffsetInPage = end
                            )
                        )
                        start = end
                    }
                }
                out
            }
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(2200L)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        readingMinutesJob?.cancel()
        autoFlipJob?.cancel()
        sleepTimerJob?.cancel()
        ttsEngine.shutdown()
        audioEngine.stopAmbient()
        pageCache.clear()
        bitmapRenderer.release()
    }

    class Factory(
        private val appContext: Context,
        private val bookId: Long,
        private val repository: LibraryRepository,
        private val preferencesRepository: PreferencesRepository,
        private val audioEngine: PaperAudioEngine,
        private val translationService: TranslationService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReaderViewModel(
                appContext,
                bookId,
                repository,
                preferencesRepository,
                audioEngine,
                translationService
            ) as T
        }
    }
}
