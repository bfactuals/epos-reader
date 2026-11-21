package com.example.engine.parser

import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import com.example.data.local.BookChapter
import com.example.data.model.ChapterTocItem
import com.example.data.model.ReaderSettings
import com.example.ui.theme.ReaderFontStyle

data class PaginatedPage(
    val pageIndex: Int,
    val chapterIndex: Int,
    val chapterTitle: String,
    val headingText: String?, // non-null on first page of chapter
    val bodyText: String,
    val isDedicationPage: Boolean = false
)

data class WordBox(
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val rect: RectF
)

data class PageTextGeometry(
    val bodyText: String,
    val bodyTopY: Float,
    val bodyLeftX: Float,
    val bodyLayout: StaticLayout,
    val wordBoxes: List<WordBox>
) {
    fun findWordAt(x: Float, y: Float, slopPx: Float = 24f): WordBox? {
        wordBoxes.firstOrNull { it.rect.contains(x, y) }?.let { return it }
        var best: WordBox? = null
        var bestDistSq = slopPx * slopPx * 4f
        for (wb in wordBoxes) {
            val cx = wb.rect.centerX()
            val cy = wb.rect.centerY()
            val dx = x - cx
            val dy = y - cy
            val dSq = dx * dx + dy * dy
            if (dSq < bestDistSq) {
                bestDistSq = dSq
                best = wb
            }
        }
        return best
    }

    fun getSelectionRects(startOffset: Int, endOffset: Int): List<RectF> {
        val safeStart = startOffset.coerceIn(0, bodyText.length)
        val safeEnd = endOffset.coerceIn(safeStart, bodyText.length)
        if (safeStart >= safeEnd) return emptyList()

        val rects = mutableListOf<RectF>()
        val startLine = bodyLayout.getLineForOffset(safeStart)
        val endLine = bodyLayout.getLineForOffset((safeEnd - 1).coerceAtLeast(safeStart))

        for (line in startLine..endLine) {
            val lineStart = bodyLayout.getLineStart(line)
            val lineEnd = bodyLayout.getLineVisibleEnd(line)
            val segStart = maxOf(safeStart, lineStart)
            val segEnd = minOf(safeEnd, lineEnd)
            if (segStart >= segEnd) continue

            val left = bodyLeftX + bodyLayout.getPrimaryHorizontal(segStart)
            val right = if (segEnd >= lineEnd) {
                bodyLeftX + bodyLayout.getLineRight(line)
            } else {
                bodyLeftX + bodyLayout.getPrimaryHorizontal(segEnd)
            }
            val top = bodyTopY + bodyLayout.getLineTop(line)
            val bottom = bodyTopY + bodyLayout.getLineBottom(line) - bodyLayout.spacingAdd * 0.35f
            if (right > left + 1f) {
                rects.add(RectF(left - 3f, top - 1f, right + 3f, bottom + 2f))
            }
        }
        return rects
    }

    fun findPhraseRects(phrase: String): List<RectF> {
        if (phrase.isBlank()) return emptyList()
        val idx = bodyText.indexOf(phrase, ignoreCase = true)
        if (idx < 0) return emptyList()
        return getSelectionRects(idx, idx + phrase.length)
    }
}

data class PaginationResult(
    val pages: List<PaginatedPage>,
    val toc: List<ChapterTocItem>
)

object Paginator {

    /**
     * Subtly bolds the first 2-3 letters of each word for Bionic / Fast-Reading Mode (EPUB/TXT).
     * Preserves exact character offsets for highlights, search, and selection.
     */
    fun formatDisplayText(raw: String, bionicEnabled: Boolean): CharSequence {
        if (!bionicEnabled || raw.isEmpty()) return raw
        val ssb = SpannableStringBuilder(raw)
        val wordRegex = Regex("""[A-Za-z\u00C0-\u024F]+""")
        wordRegex.findAll(raw).forEach { match ->
            val start = match.range.first
            val len = match.range.last - start + 1
            val boldCount = when {
                len <= 1 -> 0
                len <= 3 -> 1
                len <= 6 -> 2
                else -> 3
            }
            if (boldCount > 0) {
                ssb.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    (start + boldCount).coerceAtMost(raw.length),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
        return ssb
    }

    fun createBodyTextPaint(settings: ReaderSettings, density: Float, textColor: Int): TextPaint {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG)
        paint.color = textColor
        paint.textSize = settings.fontSizeSp * density
        paint.typeface = when (settings.fontStyle) {
            ReaderFontStyle.SERIF -> Typeface.SERIF
            ReaderFontStyle.SANS_SERIF -> Typeface.SANS_SERIF
            ReaderFontStyle.CLASSIC_BOOK -> Typeface.create("serif", Typeface.NORMAL)
        }
        paint.letterSpacing = when (settings.fontStyle) {
            ReaderFontStyle.CLASSIC_BOOK -> 0.015f
            ReaderFontStyle.SERIF -> 0.005f
            ReaderFontStyle.SANS_SERIF -> 0.01f
        }
        return paint
    }

    fun createHeadingTextPaint(settings: ReaderSettings, density: Float, textColor: Int): TextPaint {
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG or TextPaint.SUBPIXEL_TEXT_FLAG)
        paint.color = textColor
        paint.textSize = (settings.fontSizeSp * 1.32f).coerceIn(20f, 34f) * density
        paint.typeface = when (settings.fontStyle) {
            ReaderFontStyle.SANS_SERIF -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            ReaderFontStyle.SERIF -> Typeface.create(Typeface.SERIF, Typeface.BOLD)
            ReaderFontStyle.CLASSIC_BOOK -> Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        paint.letterSpacing = if (settings.fontStyle == ReaderFontStyle.CLASSIC_BOOK) 0.03f else 0.0f
        return paint
    }

    fun computeMetrics(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        topInsetPx: Int,
        bottomInsetPx: Int,
        settings: ReaderSettings
    ): PageContentMetrics {
        val horizontalMarginPx = (settings.marginDp * density).toInt().coerceIn(24, widthPx / 4)
        val contentWidth = (widthPx - horizontalMarginPx * 2).coerceAtLeast(120)
        val topPadding = topInsetPx + (32f * density).toInt()
        val bottomPadding = bottomInsetPx + (46f * density).toInt()
        val contentHeight = (heightPx - topPadding - bottomPadding).coerceAtLeast(160)
        return PageContentMetrics(
            leftX = horizontalMarginPx.toFloat(),
            topY = topPadding.toFloat(),
            contentWidth = contentWidth,
            contentHeight = contentHeight,
            bottomInsetPx = bottomInsetPx
        )
    }

    data class PageContentMetrics(
        val leftX: Float,
        val topY: Float,
        val contentWidth: Int,
        val contentHeight: Int,
        val bottomInsetPx: Int
    )

    fun paginate(
        chapters: List<BookChapter>,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        topInsetPx: Int,
        bottomInsetPx: Int,
        settings: ReaderSettings,
        includeFeaturedDedication: Boolean = false
    ): PaginationResult {
        if (widthPx <= 0 || heightPx <= 0) {
            return PaginationResult(emptyList(), emptyList())
        }
        val metrics = computeMetrics(widthPx, heightPx, density, topInsetPx, bottomInsetPx, settings)
        val bodyPaint = createBodyTextPaint(settings, density, 0xFF000000.toInt())
        val headingPaint = createHeadingTextPaint(settings, density, 0xFF000000.toInt())

        val pages = mutableListOf<PaginatedPage>()
        val toc = mutableListOf<ChapterTocItem>()
        val headingGapPx = (24f * density).toInt()

        chapters.forEachIndexed { chapterIdx, chapter ->
            val chapterStartPage = pages.size
            toc.add(ChapterTocItem(title = chapter.title, pageIndex = chapterStartPage, chapterIndex = chapterIdx))

            val fullChapterText = chapter.paragraphs.joinToString("\n\n")
            if (fullChapterText.isBlank()) {
                pages.add(
                    PaginatedPage(
                        pageIndex = pages.size,
                        chapterIndex = chapterIdx,
                        chapterTitle = chapter.title,
                        headingText = chapter.title,
                        bodyText = ""
                    )
                )
                return@forEachIndexed
            }

            val headingLayout = buildStaticLayout(chapter.title, headingPaint, metrics.contentWidth, 1.2f)
            val headingReserve = headingLayout.height + headingGapPx
            val styledChapter = formatDisplayText(fullChapterText, settings.bionicModeEnabled)

            val fullLayout = buildStaticLayout(
                styledChapter,
                bodyPaint,
                metrics.contentWidth,
                settings.lineSpacing
            )

            val totalLines = fullLayout.lineCount
            var startLine = 0
            var isFirstPageOfChapter = true

            while (startLine < totalLines) {
                val availableHeight = if (isFirstPageOfChapter) {
                    (metrics.contentHeight - headingReserve).coerceAtLeast((bodyPaint.textSize * 3f).toInt())
                } else {
                    metrics.contentHeight
                }
                val startTop = fullLayout.getLineTop(startLine)
                var endLine = startLine
                while (endLine < totalLines) {
                    val lineBottom = fullLayout.getLineBottom(endLine)
                    if (lineBottom - startTop > availableHeight && endLine > startLine) {
                        break
                    }
                    endLine++
                }

                val charStart = fullLayout.getLineStart(startLine)
                val charEnd = fullLayout.getLineEnd((endLine - 1).coerceAtLeast(startLine))
                val slice = fullChapterText.substring(charStart, charEnd).trim()

                if (slice.isNotEmpty() || isFirstPageOfChapter) {
                    pages.add(
                        PaginatedPage(
                            pageIndex = pages.size,
                            chapterIndex = chapterIdx,
                            chapterTitle = chapter.title,
                            headingText = if (isFirstPageOfChapter) chapter.title else null,
                            bodyText = slice
                        )
                    )
                }

                isFirstPageOfChapter = false
                startLine = endLine
            }
        }

        if (pages.isEmpty()) {
            pages.add(PaginatedPage(0, 0, "Empty", null, ""))
        }

        return PaginationResult(pages = pages, toc = toc)
    }

    fun buildPageGeometry(
        page: PaginatedPage,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        topInsetPx: Int,
        bottomInsetPx: Int,
        settings: ReaderSettings
    ): PageTextGeometry {
        val metrics = computeMetrics(widthPx, heightPx, density, topInsetPx, bottomInsetPx, settings)
        val bodyPaint = createBodyTextPaint(settings, density, 0xFF000000.toInt())
        val headingPaint = createHeadingTextPaint(settings, density, 0xFF000000.toInt())

        val headingGapPx = 24f * density
        val bodyTopY = if (page.isDedicationPage) {
            heightPx * 0.44f
        } else if (page.headingText != null) {
            val hLayout = buildStaticLayout(page.headingText, headingPaint, metrics.contentWidth, 1.2f)
            metrics.topY + hLayout.height + headingGapPx
        } else {
            metrics.topY
        }

        val styledBody = formatDisplayText(page.bodyText, settings.bionicModeEnabled && !page.isDedicationPage)
        val bodyLayout = buildStaticLayout(
            styledBody,
            bodyPaint,
            metrics.contentWidth,
            settings.lineSpacing,
            alignment = if (page.isDedicationPage) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        )

        val wordBoxes = mutableListOf<WordBox>()
        val wordRegex = Regex("""[^\s]+""")
        wordRegex.findAll(page.bodyText).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            val line = bodyLayout.getLineForOffset(start)
            val lineEnd = bodyLayout.getLineVisibleEnd(line)
            val clampedEnd = minOf(end, lineEnd)
            if (clampedEnd > start) {
                val left = metrics.leftX + bodyLayout.getPrimaryHorizontal(start)
                val right = if (clampedEnd >= lineEnd) {
                    metrics.leftX + bodyLayout.getLineRight(line)
                } else {
                    metrics.leftX + bodyLayout.getPrimaryHorizontal(clampedEnd)
                }
                val top = bodyTopY + bodyLayout.getLineTop(line)
                val bottom = bodyTopY + bodyLayout.getLineBottom(line)
                if (right > left) {
                    wordBoxes.add(WordBox(start, end, match.value, RectF(left, top, right, bottom)))
                }
            }
        }

        return PageTextGeometry(
            bodyText = page.bodyText,
            bodyTopY = bodyTopY,
            bodyLeftX = metrics.leftX,
            bodyLayout = bodyLayout,
            wordBoxes = wordBoxes
        )
    }

    fun buildStaticLayout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        lineSpacingMultiplier: Float,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(40))
            .setAlignment(alignment)
            .setLineSpacing(0f, lineSpacingMultiplier)
            .setIncludePad(false)
            .build()
    }
}
