package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookChapter
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
import kotlinx.coroutines.async
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

data class FullPageTranslationState(
    val pageIndex: Int,
    val targetLanguage: TranslationLanguage,
    val isLoading: Boolean = true,
    val translatedText: String = "",
    val fromCache: Boolean = false,
    val errorHint: String? = null
)

data class ReadingSessionTimerState(
    val configuredMinutes: Int = 30,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isSheetOpen: Boolean = false,
    val showCompleteAlert: Boolean = false
) {
    val isActivePillVisible: Boolean
        get() = remainingSeconds > 0 && (isRunning || isPaused)
}

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
    val fullPageTranslation: FullPageTranslationState? = null,
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
    val sessionTimer: ReadingSessionTimerState = ReadingSessionTimerState(),
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
    // Full pagination list for normal books (<= 180k chars)
    private var paginatedPages: List<PaginatedPage> = emptyList()
    // On-demand chapter pagination state for huge books (1000+ pages)
    private var isLazyChapterMode: Boolean = false
    private var lazyChapters: List<BookChapter> = emptyList()
    private var lazyChapterStarts: IntArray = IntArray(0)
    private var lazyChapterCounts: IntArray = IntArray(0)
    private val lazyChapterCache = HashMap<Int, List<PaginatedPage>>()
    private val lazyChapterLock = Any()

    // Lazy PDF text cache (extracted on-demand only when Search / TTS / Translate is used)
    private var lazyPdfTexts: List<String>? = null

    private var currentGeometry: PageTextGeometry? = null

    // Viewport metrics
    private var viewportSize = IntSize.Zero
    private var screenDensity = 2f
    private var topInsetPx = 0
    private var bottomInsetPx = 0

    // Thread-safe 28 MB in-memory LRU Bitmap cache keyed by "${pageIndex}_${visualSignature}"
    private val pageCache = object : LruCache<String, Bitmap>(28 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return (value.allocationByteCount / 1024).coerceAtLeast(1)
        }
    }

    private var paginationJob: Job? = null
    private var renderJob: Job? = null
    private var returnPillJob: Job? = null
    private var glowJob: Job? = null
    private var searchJob: Job? = null
    private var toastJob: Job? = null
    private var brightnessHideJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var sessionTimerJob: Job? = null
    private var autoFlipJob: Job? = null
    private var readingMinutesJob: Job? = null
    private var fullPageTranslateJob: Job? = null

    private var lastPaginationSignature: String = ""
    private var lastVisualSignature: String = ""
    private var lastPageTurnUptimeMs: Long = 0L

    // Strictly counts reading minutes ONLY while ReaderScreen is resumed & foreground
    @Volatile
    private var isReaderForegroundActive: Boolean = false
    private var accumulatedActiveSeconds: Int = 0

    init {
        observeSettings()
        loadBookAndObserve()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            var lastAmbientMode: AmbientSoundMode? = null
            var lastAutoFlip = -1
            preferencesRepository.settings.collectLatest { s ->
                if (isReaderForegroundActive) {
                    if (s.ambientSoundMode != lastAmbientMode) {
                        lastAmbientMode = s.ambientSoundMode
                        audioEngine.setAmbientSound(s.ambientSoundMode, s.ambientVolume)
                    } else {
                        audioEngine.updateAmbientVolume(s.ambientVolume)
                    }
                } else {
                    lastAmbientMode = s.ambientSoundMode
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
                        val visualSig = buildVisualSignature(s, viewportSize.width, viewportSize.height)
                        if (visualSig != lastVisualSignature) {
                            lastVisualSignature = visualSig
                            invalidateAndRenderAroundCurrent()
                        }
                    }
                }
            }
        }
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

            // Start lifecycle-aware active reading minute tracker (STRICTLY skipped if book.isHidden == true)
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
                    if (parsedBook != null && viewportSize.width > 0 && viewportSize.height > 0) {
                        invalidateAndRenderAroundCurrent()
                    }
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
                val step = when {
                    parsed.pageCount > 400 -> 25
                    parsed.pageCount > 120 -> 10
                    else -> 5
                }
                val toc = (0 until parsed.pageCount step step).map { idx ->
                    ChapterTocItem(title = "Page ${idx + 1}", pageIndex = idx, chapterIndex = idx / step)
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
                    lastVisualSignature = buildVisualSignature(settings.value, viewportSize.width, viewportSize.height)
                    invalidateAndRenderAroundCurrent()
                }
            } else {
                if (viewportSize.width > 0 && viewportSize.height > 0) {
                    repaginateIfNeeded(force = true)
                }
            }
        }
    }

    /**
     * Driven strictly by ReaderScreen lifecycle (ON_RESUME = true, ON_PAUSE/ON_STOP/dispose = false).
     * Ensures reading stats never tick while on Library, Stats, Settings, or app background.
     */
    fun setReadingActive(active: Boolean) {
        isReaderForegroundActive = active
        val s = settings.value
        if (active) {
            if (s.ambientSoundMode != AmbientSoundMode.OFF) {
                audioEngine.setAmbientSound(s.ambientSoundMode, s.ambientVolume)
            }
        } else {
            audioEngine.stopAmbient()
        }
    }

    private fun startVisibleReadingMinuteTracker() {
        readingMinutesJob?.cancel()
        readingMinutesJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val st = _uiState.value
                val book = st.book ?: continue
                if (book.isHidden) continue
                val activelyReading = isReaderForegroundActive &&
                    !st.isLoading &&
                    !st.isSettingsExpanded &&
                    !st.isSearchOpen &&
                    !st.isNotebookOpen &&
                    !st.sessionTimer.isSheetOpen &&
                    !st.sessionTimer.showCompleteAlert
                if (activelyReading) {
                    accumulatedActiveSeconds++
                    if (accumulatedActiveSeconds >= 60) {
                        accumulatedActiveSeconds = 0
                        repository.recordVisibleReadingActivity(
                            isHiddenBook = false,
                            deltaMinutes = 1,
                            deltaPages = 0
                        )
                    }
                }
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
                lastVisualSignature = buildVisualSignature(settings.value, size.width, size.height)
                invalidateAndRenderAroundCurrent()
            }
        }
    }

    private fun buildLayoutSignature(s: ReaderSettings, w: Int, h: Int): String {
        return "${w}x${h}_${topInsetPx}_${bottomInsetPx}_${s.fontStyle}_${s.fontSizeSp}_${s.lineSpacing}_${s.marginDp}"
    }

    private fun buildVisualSignature(s: ReaderSettings, w: Int, h: Int): String {
        return "${w}x${h}_${topInsetPx}_${bottomInsetPx}_${s.themeMode}_${s.fontStyle}_" +
            "${s.fontSizeSp}_${s.lineSpacing}_${s.marginDp}_${s.bionicModeEnabled}_" +
            "${s.paperGrainEnabled}_${s.pdfAutoCropEnabled}"
    }

    private fun repaginateIfNeeded(force: Boolean) {
        val textBook = parsedBook as? ParsedBook.TextBook ?: return
        val s = settings.value
        val w = viewportSize.width
        val h = viewportSize.height
        if (w <= 0 || h <= 0) return

        val layoutSig = buildLayoutSignature(s, w, h)
        val visualSig = buildVisualSignature(s, w, h)

        if (!force && layoutSig == lastPaginationSignature) {
            if (visualSig != lastVisualSignature) {
                lastVisualSignature = visualSig
                invalidateAndRenderAroundCurrent()
            }
            return
        }

        lastPaginationSignature = layoutSig
        lastVisualSignature = visualSig

        paginationJob?.cancel()
        paginationJob = viewModelScope.launch {
            val prevState = _uiState.value
            val prevRatio = if (prevState.totalPages <= 1) 0f else {
                prevState.currentPage.toFloat() / (prevState.totalPages - 1).coerceAtLeast(1)
            }
            val wasFirstLoad = paginatedPages.isEmpty() && !isLazyChapterMode

            val totalChars = withContext(Dispatchers.Default) {
                var sum = 0
                for (ch in textBook.chapters) {
                    for (p in ch.paragraphs) sum += p.length
                }
                sum
            }

            if (totalChars > LARGE_BOOK_CHAR_THRESHOLD && textBook.chapters.size > 1) {
                // Fast on-demand chapter pagination for huge 1000+ page books (<30ms!)
                val estCounts = withContext(Dispatchers.Default) {
                    Paginator.estimateChapterPageCounts(
                        chapters = textBook.chapters,
                        widthPx = w,
                        heightPx = h,
                        density = screenDensity,
                        topInsetPx = topInsetPx,
                        bottomInsetPx = bottomInsetPx,
                        settings = s
                    )
                }
                val starts = IntArray(estCounts.size)
                var running = 0
                val toc = ArrayList<ChapterTocItem>(estCounts.size)
                for (i in estCounts.indices) {
                    starts[i] = running
                    toc.add(ChapterTocItem(textBook.chapters[i].title, running, i))
                    running += estCounts[i]
                }
                val total = running.coerceAtLeast(1)
                val newPage = if (wasFirstLoad) {
                    (prevState.book?.currentPage ?: 0).coerceIn(0, total - 1)
                } else {
                    (prevRatio * (total - 1)).toInt().coerceIn(0, total - 1)
                }

                synchronized(lazyChapterLock) {
                    isLazyChapterMode = true
                    paginatedPages = emptyList()
                    lazyChapters = textBook.chapters
                    lazyChapterStarts = starts
                    lazyChapterCounts = estCounts
                    lazyChapterCache.clear()
                }

                // Paginate ONLY the active chapter containing newPage immediately
                val activePage = withContext(Dispatchers.Default) {
                    getOrPaginateLazyPage(newPage, w, h, s)
                }
                val updatedTotal = currentLazyTotalPages().coerceAtLeast(1)
                val clampedNewPage = newPage.coerceIn(0, updatedTotal - 1)

                _uiState.update { st ->
                    st.copy(
                        isLoading = false,
                        currentPage = clampedNewPage,
                        totalPages = updatedTotal,
                        estimatedMinutesLeft = computeEstMinutesLeft(clampedNewPage, updatedTotal),
                        currentChapterTitle = activePage?.chapterTitle ?: textBook.title,
                        tocItems = buildLazyToc(),
                        isCurrentPageBookmarked = st.bookmarks.any { it.pageIndex == clampedNewPage },
                        activeSelection = null
                    )
                }
                persistProgress(clampedNewPage, updatedTotal)
                invalidateAndRenderAroundCurrent()
            } else {
                // Fast chunked full pagination for standard books (<60ms)
                val result = withContext(Dispatchers.Default) {
                    Paginator.paginate(
                        chapters = textBook.chapters,
                        widthPx = w,
                        heightPx = h,
                        density = screenDensity,
                        topInsetPx = topInsetPx,
                        bottomInsetPx = bottomInsetPx,
                        settings = s,
                        includeFeaturedDedication = false
                    )
                }
                synchronized(lazyChapterLock) {
                    isLazyChapterMode = false
                    lazyChapterCache.clear()
                    paginatedPages = result.pages
                }
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
    }

    private fun currentLazyTotalPages(): Int = synchronized(lazyChapterLock) {
        if (lazyChapterStarts.isEmpty()) 1
        else lazyChapterStarts.last() + lazyChapterCounts.last()
    }

    private fun buildLazyToc(): List<ChapterTocItem> = synchronized(lazyChapterLock) {
        lazyChapters.mapIndexed { idx, ch ->
            ChapterTocItem(title = ch.title, pageIndex = lazyChapterStarts[idx], chapterIndex = idx)
        }
    }

    private fun getOrPaginateLazyPage(pageIndex: Int, w: Int, h: Int, s: ReaderSettings): PaginatedPage? {
        if (!isLazyChapterMode) return paginatedPages.getOrNull(pageIndex)
        val chIdx: Int
        val chapter: BookChapter
        val chStart: Int
        synchronized(lazyChapterLock) {
            if (lazyChapters.isEmpty()) return null
            // Binary search chapter containing pageIndex
            var lo = 0
            var hi = lazyChapterStarts.lastIndex
            var found = 0
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (lazyChapterStarts[mid] <= pageIndex) {
                    found = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            chIdx = found
            val cached = lazyChapterCache[chIdx]
            if (cached != null) {
                val localIdx = (pageIndex - lazyChapterStarts[chIdx]).coerceIn(0, cached.lastIndex)
                return cached[localIdx].copy(pageIndex = pageIndex)
            }
            chapter = lazyChapters[chIdx]
            chStart = lazyChapterStarts[chIdx]
        }

        // Paginate ONLY this single chapter on background thread
        val metrics = Paginator.computeMetrics(w, h, screenDensity, topInsetPx, bottomInsetPx, s)
        val bodyPaint = Paginator.createBodyTextPaint(s, screenDensity, 0xFF000000.toInt())
        val headingPaint = Paginator.createHeadingTextPaint(s, screenDensity, 0xFF000000.toInt())
        val pages = Paginator.paginateSingleChapter(
            chapter = chapter,
            chapterIndex = chIdx,
            startPageIndex = chStart,
            metrics = metrics,
            bodyPaint = bodyPaint,
            headingPaint = headingPaint,
            density = screenDensity,
            settings = s
        )

        synchronized(lazyChapterLock) {
            lazyChapterCache[chIdx] = pages
            val oldCount = lazyChapterCounts[chIdx]
            val newCount = pages.size.coerceAtLeast(1)
            val diff = newCount - oldCount
            if (diff != 0) {
                lazyChapterCounts[chIdx] = newCount
                for (i in (chIdx + 1) until lazyChapterStarts.size) {
                    lazyChapterStarts[i] = (lazyChapterStarts[i] + diff).coerceAtLeast(0)
                }
            }
            // Keep at most 14 chapters cached in memory to stay ultra-lightweight
            if (lazyChapterCache.size > 14) {
                val farthestKey = lazyChapterCache.keys.maxByOrNull { abs(it - chIdx) }
                if (farthestKey != null && farthestKey != chIdx) {
                    lazyChapterCache.remove(farthestKey)
                }
            }
            val localIdx = (pageIndex - lazyChapterStarts[chIdx]).coerceIn(0, pages.lastIndex)
            return pages[localIdx].copy(pageIndex = pageIndex)
        }
    }

    private fun computeEstMinutesLeft(pageIndex: Int, totalPages: Int): Int {
        val remainingPages = (totalPages - 1 - pageIndex).coerceAtLeast(0)
        if (remainingPages == 0) return 0
        return ceil(remainingPages * 1.15).toInt().coerceAtLeast(1)
    }

    private fun pageCacheKey(
        pageIndex: Int,
        visualSig: String,
        highlights: List<HighlightEntity>
    ): String {
        var hlHash = 0
        for (hl in highlights) {
            if (hl.pageIndex == pageIndex) {
                hlHash = 31 * hlHash + hl.id.toInt()
                hlHash = 31 * hlHash + hl.colorIndex
                hlHash = 31 * hlHash + hl.note.hashCode()
            }
        }
        return "${pageIndex}_${visualSig}_$hlHash"
    }

    private fun invalidateAndRenderAroundCurrent() {
        pageCache.evictAll()
        renderWindow(_uiState.value.currentPage)
    }

    /**
     * Fast 2-phase on-demand rendering:
     * Phase 1: Immediately render/display `centerPage` so settings, theme, and scrubber changes appear instantaneously.
     * Phase 2: Pre-render `centerPage + 1` and `centerPage - 1` asynchronously in background so page curls stay 60fps.
     */
    private fun renderWindow(centerPage: Int) {
        val pb = parsedBook ?: return
        val w = viewportSize.width
        val h = viewportSize.height
        if (w <= 0 || h <= 0) return

        val s = settings.value
        val visualSig = buildVisualSignature(s, w, h)
        val highlights = _uiState.value.highlights

        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val total = if (pb is ParsedBook.TextBook && isLazyChapterMode) {
                currentLazyTotalPages().coerceAtLeast(1)
            } else {
                _uiState.value.totalPages.coerceAtLeast(1)
            }
            val safeCenter = centerPage.coerceIn(0, total - 1)

            // Phase 1: Render and emit current page immediately
            val currBmp = getOrRenderPage(pb, safeCenter, total, w, h, s, visualSig, highlights)
            val resolvedTotal = if (pb is ParsedBook.TextBook && isLazyChapterMode) {
                currentLazyTotalPages().coerceAtLeast(1)
            } else total

            val activeChapterTitle = if (pb is ParsedBook.TextBook) {
                withContext(Dispatchers.Default) {
                    getOrPaginateLazyPage(safeCenter, w, h, s)?.chapterTitle ?: pb.title
                }
            } else {
                "Page ${safeCenter + 1}"
            }

            currentGeometry = if (pb is ParsedBook.TextBook) {
                val page = withContext(Dispatchers.Default) { getOrPaginateLazyPage(safeCenter, w, h, s) }
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

            // Check if neighbors are already warm in LRU cache
            val warmPrev = if (safeCenter > 0) pageCache.get(pageCacheKey(safeCenter - 1, visualSig, highlights)) else null
            val warmNext = if (safeCenter < resolvedTotal - 1) pageCache.get(pageCacheKey(safeCenter + 1, visualSig, highlights)) else null

            _uiState.update { st ->
                st.copy(
                    isLoading = false,
                    totalPages = resolvedTotal,
                    currentChapterTitle = activeChapterTitle,
                    tocItems = if (pb is ParsedBook.TextBook && isLazyChapterMode) buildLazyToc() else st.tocItems,
                    currentBitmap = currBmp,
                    prevBitmap = warmPrev,
                    nextBitmap = warmNext
                )
            }

            // Phase 2: Pre-render adjacent pages (+1 first, then -1) in background
            val nextDeferred = if (safeCenter < resolvedTotal - 1 && warmNext == null) {
                async(Dispatchers.Default) {
                    getOrRenderPage(pb, safeCenter + 1, resolvedTotal, w, h, s, visualSig, highlights)
                }
            } else null

            val prevDeferred = if (safeCenter > 0 && warmPrev == null) {
                async(Dispatchers.Default) {
                    getOrRenderPage(pb, safeCenter - 1, resolvedTotal, w, h, s, visualSig, highlights)
                }
            } else null

            val nextBmp = warmNext ?: nextDeferred?.await()
            val prevBmp = warmPrev ?: prevDeferred?.await()

            if (isActive && _uiState.value.currentPage == safeCenter) {
                _uiState.update { st ->
                    st.copy(
                        prevBitmap = prevBmp,
                        nextBitmap = nextBmp
                    )
                }
            }
        }
    }

    private suspend fun getOrRenderPage(
        pb: ParsedBook,
        pageIndex: Int,
        totalPages: Int,
        w: Int,
        h: Int,
        s: ReaderSettings,
        visualSig: String,
        allHighlights: List<HighlightEntity>
    ): Bitmap? {
        val key = pageCacheKey(pageIndex, visualSig, allHighlights)
        pageCache.get(key)?.let { return it }

        val pageHighlights = allHighlights.filter { it.pageIndex == pageIndex }
        val estLeft = computeEstMinutesLeft(pageIndex, totalPages)
        val bmp = withContext(Dispatchers.Default) {
            when (pb) {
                is ParsedBook.TextBook -> {
                    val page = getOrPaginateLazyPage(pageIndex, w, h, s) ?: return@withContext null
                    val actualTotal = if (isLazyChapterMode) currentLazyTotalPages().coerceAtLeast(1) else totalPages
                    bitmapRenderer.renderTextPage(
                        page = page,
                        totalPages = actualTotal,
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
            pageCache.put(key, bmp)
        }
        return bmp
    }

    // --- Page Turning & Navigation (strictly locked to +/- 1 page with debounce) ---
    fun commitPageTurn(delta: Int) {
        val step = delta.coerceIn(-1, 1)
        if (step == 0) return

        val now = SystemClock.uptimeMillis()
        if (now - lastPageTurnUptimeMs < 170L) return
        lastPageTurnUptimeMs = now

        val st = _uiState.value
        val target = (st.currentPage + step).coerceIn(0, (st.totalPages - 1).coerceAtLeast(0))
        if (target == st.currentPage) return

        val s = settings.value
        val visualSig = buildVisualSignature(s, viewportSize.width, viewportSize.height)
        val cachedTarget = pageCache.get(pageCacheKey(target, visualSig, st.highlights))
        val cachedPrev = if (target > 0) pageCache.get(pageCacheKey(target - 1, visualSig, st.highlights)) else null
        val cachedNext = if (target < st.totalPages - 1) pageCache.get(pageCacheKey(target + 1, visualSig, st.highlights)) else null

        val chapterTitle = getChapterTitleForPage(target)
        val keepZoom = st.isPdfZoomLocked
        fullPageTranslateJob?.cancel()

        _uiState.update { state ->
            state.copy(
                currentPage = target,
                estimatedMinutesLeft = computeEstMinutesLeft(target, state.totalPages),
                currentChapterTitle = chapterTitle,
                isCurrentPageBookmarked = state.bookmarks.any { it.pageIndex == target },
                activeSelection = null,
                activeTranslation = null,
                fullPageTranslation = null,
                searchGlowRects = emptyList(),
                searchGlowAlpha = 0f,
                pdfZoomScale = if (keepZoom) state.pdfZoomScale else 1f,
                pdfPanOffset = if (keepZoom) state.pdfPanOffset else Offset.Zero,
                currentBitmap = cachedTarget ?: state.currentBitmap,
                prevBitmap = cachedPrev,
                nextBitmap = cachedNext
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

        val s = settings.value
        val visualSig = buildVisualSignature(s, viewportSize.width, viewportSize.height)
        val cachedTarget = pageCache.get(pageCacheKey(clamped, visualSig, st.highlights))
        val chapterTitle = getChapterTitleForPage(clamped)
        fullPageTranslateJob?.cancel()

        _uiState.update { state ->
            state.copy(
                currentPage = clamped,
                estimatedMinutesLeft = computeEstMinutesLeft(clamped, state.totalPages),
                currentChapterTitle = chapterTitle,
                isCurrentPageBookmarked = state.bookmarks.any { it.pageIndex == clamped },
                activeSelection = null,
                activeTranslation = null,
                fullPageTranslation = null,
                isSearchOpen = false,
                isNotebookOpen = false,
                currentBitmap = cachedTarget ?: state.currentBitmap
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
        val s = settings.value
        val visualSig = buildVisualSignature(s, viewportSize.width, viewportSize.height)
        viewModelScope.launch {
            val bmp = getOrRenderPage(
                pb = pb,
                pageIndex = pinned,
                totalPages = _uiState.value.totalPages,
                w = viewportSize.width,
                h = viewportSize.height,
                s = s,
                visualSig = visualSig,
                allHighlights = _uiState.value.highlights
            )
            _uiState.update { it.copy(isPeekingPinnedPage = true, pinnedPageBitmap = bmp, isOverlayVisible = false) }
        }
    }

    fun closePinnedPagePeek() {
        _uiState.update { it.copy(isPeekingPinnedPage = false) }
    }

    // --- Brightness Control (Slider + Use System Toggle + Left-Edge Gesture) ---
    fun setUseSystemBrightness(useSystem: Boolean) {
        preferencesRepository.updateSettings { it.copy(useSystemBrightness = useSystem) }
    }

    fun setCustomBrightness(level: Float) {
        preferencesRepository.updateSettings {
            it.copy(
                useSystemBrightness = false,
                brightnessLevel = level.coerceIn(0.05f, 1.0f)
            )
        }
    }

    fun onLeftEdgeBrightnessDrag(deltaFraction: Float) {
        preferencesRepository.updateSettings { s ->
            s.copy(
                useSystemBrightness = false,
                brightnessLevel = (s.brightnessLevel + deltaFraction).coerceIn(0.05f, 1.0f)
            )
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

    // --- Lazy PDF Text Extraction Helper ---
    private suspend fun ensurePdfTextsLoaded(pb: ParsedBook.PdfBook): List<String> {
        lazyPdfTexts?.let { return it }
        val extracted = withContext(Dispatchers.IO) {
            BookDocumentParser.extractPdfTextsOnDemand(pb.filePath, pb.pageCount)
        }
        lazyPdfTexts = extracted
        return extracted
    }

    private suspend fun getCurrentPagePlainText(pageIdx: Int): String {
        return when (val pb = parsedBook) {
            is ParsedBook.TextBook -> {
                val p = withContext(Dispatchers.Default) {
                    getOrPaginateLazyPage(pageIdx, viewportSize.width, viewportSize.height, settings.value)
                }
                listOfNotNull(p?.headingText, p?.bodyText)
                    .filter { it.isNotBlank() }
                    .joinToString("\n\n")
            }
            is ParsedBook.PdfBook -> {
                val texts = ensurePdfTextsLoaded(pb)
                texts.getOrNull(pageIdx).orEmpty()
            }
            null -> ""
        }
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
        viewModelScope.launch {
            val text = getCurrentPagePlainText(pageIdx)
            if (text.isBlank()) {
                stopTts()
                showToast("No readable text on this page")
                return@launch
            }
            ttsEngine.speak(text, settings.value.ttsSpeed)
        }
    }

    private fun onTtsPageFinished() {
        val st = _uiState.value
        if (!st.isTtsPlaying) return
        if (st.currentPage < st.totalPages - 1) {
            _uiState.update { it.copy(autoFlipTrigger = it.autoFlipTrigger + 1) }
        } else {
            stopTts()
            showToast("Finished reading volume")
        }
    }

    // --- Reading Session Timer (Live Pill + Completion Chime & Alert) ---
    fun openSessionTimerSheet(open: Boolean) {
        _uiState.update {
            it.copy(
                sessionTimer = it.sessionTimer.copy(isSheetOpen = open),
                isOverlayVisible = if (open) false else it.isOverlayVisible,
                isSettingsExpanded = false
            )
        }
    }

    fun startSessionTimer(minutes: Int) {
        val clampedMin = minutes.coerceIn(1, 240)
        val totalSec = clampedMin * 60
        sessionTimerJob?.cancel()
        _uiState.update {
            it.copy(
                sessionTimer = ReadingSessionTimerState(
                    configuredMinutes = clampedMin,
                    totalSeconds = totalSec,
                    remainingSeconds = totalSec,
                    isRunning = true,
                    isPaused = false,
                    isSheetOpen = false,
                    showCompleteAlert = false
                )
            )
        }
        launchSessionTimerTicker()
        showToast("Reading timer started: ${clampedMin}m")
    }

    fun togglePauseSessionTimer() {
        val timer = _uiState.value.sessionTimer
        if (timer.remainingSeconds <= 0) return
        if (timer.isRunning) {
            sessionTimerJob?.cancel()
            _uiState.update {
                it.copy(sessionTimer = it.sessionTimer.copy(isRunning = false, isPaused = true))
            }
        } else {
            _uiState.update {
                it.copy(sessionTimer = it.sessionTimer.copy(isRunning = true, isPaused = false))
            }
            launchSessionTimerTicker()
        }
    }

    fun resetSessionTimer() {
        sessionTimerJob?.cancel()
        _uiState.update {
            it.copy(
                sessionTimer = it.sessionTimer.copy(
                    totalSeconds = 0,
                    remainingSeconds = 0,
                    isRunning = false,
                    isPaused = false,
                    showCompleteAlert = false
                )
            )
        }
    }

    fun addTenMinutesToSessionTimer() {
        val current = _uiState.value.sessionTimer
        val addedSec = 10 * 60
        val nextRem = current.remainingSeconds + addedSec
        val nextTotal = maxOf(current.totalSeconds, nextRem)
        sessionTimerJob?.cancel()
        _uiState.update {
            it.copy(
                sessionTimer = current.copy(
                    configuredMinutes = nextTotal / 60,
                    totalSeconds = nextTotal,
                    remainingSeconds = nextRem,
                    isRunning = true,
                    isPaused = false,
                    showCompleteAlert = false,
                    isSheetOpen = false
                )
            )
        }
        launchSessionTimerTicker()
        showToast("+10 min added to timer")
    }

    fun dismissSessionTimerAlert() {
        _uiState.update {
            it.copy(sessionTimer = it.sessionTimer.copy(showCompleteAlert = false))
        }
    }

    private fun launchSessionTimerTicker() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val timer = _uiState.value.sessionTimer
                if (!timer.isRunning) break
                val next = (timer.remainingSeconds - 1).coerceAtLeast(0)
                if (next <= 0) {
                    _uiState.update {
                        it.copy(
                            sessionTimer = it.sessionTimer.copy(
                                remainingSeconds = 0,
                                isRunning = false,
                                isPaused = false,
                                showCompleteAlert = true
                            )
                        )
                    }
                    audioEngine.playGentleCompletionChime()
                    audioEngine.triggerTimerCompleteHaptic()
                    break
                } else {
                    _uiState.update {
                        it.copy(sessionTimer = it.sessionTimer.copy(remainingSeconds = next))
                    }
                }
            }
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
                    !st.isNotebookOpen && st.activeSelection == null && st.fullPageTranslation == null &&
                    st.currentPage < st.totalPages - 1
                ) {
                    _uiState.update { it.copy(autoFlipTrigger = it.autoFlipTrigger + 1) }
                }
            }
        }
    }

    // --- Multi-Language Selection Translation & 1-Tap Full-Page Translation ---
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

    fun translateCurrentPage(overrideLanguage: TranslationLanguage? = null) {
        val pageIdx = _uiState.value.currentPage
        val targetLang = overrideLanguage ?: settings.value.defaultTargetLanguage
        fullPageTranslateJob?.cancel()
        _uiState.update {
            it.copy(
                isOverlayVisible = false,
                isSettingsExpanded = false,
                activeSelection = null,
                activeTranslation = null,
                fullPageTranslation = FullPageTranslationState(
                    pageIndex = pageIdx,
                    targetLanguage = targetLang,
                    isLoading = true
                )
            )
        }
        fullPageTranslateJob = viewModelScope.launch {
            val pageText = getCurrentPagePlainText(pageIdx)
            val result = translationService.translateFullPage(
                pageText = pageText,
                targetLanguage = targetLang,
                optionalGeminiApiKey = settings.value.geminiApiKey
            )
            _uiState.update { st ->
                if (st.currentPage != pageIdx) st
                else st.copy(
                    fullPageTranslation = FullPageTranslationState(
                        pageIndex = pageIdx,
                        targetLanguage = result.targetLanguage,
                        isLoading = false,
                        translatedText = result.translatedText,
                        fromCache = result.fromCache,
                        errorHint = result.errorHint
                    )
                )
            }
        }
    }

    fun switchFullPageTranslationLanguage(language: TranslationLanguage) {
        translateCurrentPage(overrideLanguage = language)
    }

    fun dismissFullPageTranslation() {
        fullPageTranslateJob?.cancel()
        _uiState.update { it.copy(fullPageTranslation = null) }
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
                val page = withContext(Dispatchers.Default) {
                    getOrPaginateLazyPage(pageIndex, viewportSize.width, viewportSize.height, settings.value)
                }
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
            is ParsedBook.TextBook -> {
                if (isLazyChapterMode) {
                    synchronized(lazyChapterLock) {
                        var found = 0
                        for (i in lazyChapterStarts.indices) {
                            if (lazyChapterStarts[i] <= pageIndex) found = i else break
                        }
                        lazyChapters.getOrNull(found)?.title ?: pb.title
                    }
                } else {
                    paginatedPages.getOrNull(pageIndex)?.chapterTitle ?: pb.title
                }
            }
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
        viewModelScope.launch {
            val excerpt = getCurrentPagePlainText(pageIdx).take(110)
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
            viewModelScope.launch {
                val texts = ensurePdfTextsLoaded(pb)
                val pageText = texts.getOrNull(st.currentPage).orEmpty().trim()
                _uiState.update {
                    it.copy(
                        isOverlayVisible = false,
                        pdfSelectionPromptText = pageText.ifEmpty { "Page ${st.currentPage + 1} (${pb.title})" }
                    )
                }
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

    // --- In-Book Search (supports normal books, lazy huge books, and lazy PDF text extraction) ---
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(160L)
            val pb = parsedBook ?: return@launch
            val results = withContext(Dispatchers.Default) {
                val out = mutableListOf<SearchResult>()
                when (pb) {
                    is ParsedBook.TextBook -> {
                        if (isLazyChapterMode) {
                            // Fast chapter-level search without forcing upfront pagination of 1000+ pages
                            lazyChapters.forEachIndexed { chIdx, chapter ->
                                if (out.size >= 80) return@forEachIndexed
                                val full = chapter.paragraphs.joinToString("\n\n")
                                val chStart = synchronized(lazyChapterLock) { lazyChapterStarts.getOrElse(chIdx) { 0 } }
                                val chCount = synchronized(lazyChapterLock) { lazyChapterCounts.getOrElse(chIdx) { 1 } }
                                var start = 0
                                while (start < full.length && out.size < 80) {
                                    val idx = full.indexOf(trimmed, start, ignoreCase = true)
                                    if (idx < 0) break
                                    val end = idx + trimmed.length
                                    val frac = if (full.isEmpty()) 0f else idx.toFloat() / full.length
                                    val approxPage = chStart + (frac * (chCount - 1)).toInt().coerceIn(0, (chCount - 1).coerceAtLeast(0))
                                    val snipStart = (idx - 36).coerceAtLeast(0)
                                    val snipEnd = (end + 50).coerceAtMost(full.length)
                                    val before = (if (snipStart > 0) "…" else "") + full.substring(snipStart, idx).replace('\n', ' ')
                                    val matched = full.substring(idx, end)
                                    val after = full.substring(end, snipEnd).replace('\n', ' ') + (if (snipEnd < full.length) "…" else "")
                                    out.add(SearchResult(approxPage, chapter.title, before, matched, after, 0, 0))
                                    start = end
                                }
                            }
                        } else {
                            paginatedPages.forEachIndexed { pageIdx, page ->
                                if (out.size >= 80) return@forEachIndexed
                                val text = page.bodyText
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
                                    out.add(SearchResult(pageIdx, page.chapterTitle, before, matched, after, idx, end))
                                    start = end
                                }
                            }
                        }
                    }
                    is ParsedBook.PdfBook -> {
                        val texts = ensurePdfTextsLoaded(pb)
                        texts.forEachIndexed { pageIdx, text ->
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
                                out.add(SearchResult(pageIdx, "Page ${pageIdx + 1}", before, matched, after, idx, end))
                                start = end
                            }
                        }
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
        isReaderForegroundActive = false
        paginationJob?.cancel()
        renderJob?.cancel()
        readingMinutesJob?.cancel()
        autoFlipJob?.cancel()
        sleepTimerJob?.cancel()
        sessionTimerJob?.cancel()
        fullPageTranslateJob?.cancel()
        ttsEngine.shutdown()
        audioEngine.stopAmbient()
        pageCache.evictAll()
        bitmapRenderer.release()
    }

    companion object {
        private const val LARGE_BOOK_CHAR_THRESHOLD = 180_000
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
