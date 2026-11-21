package com.example.engine.parser

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.HighlightEntity
import com.example.data.model.ReaderSettings
import com.example.ui.theme.HighlightColors
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.theme.paletteFor
import java.io.File
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Unified offscreen 2D Canvas Bitmap renderer for EPUB, TXT, and PDF pages.
 * Produces identical hardware-friendly Bitmaps for the 60fps 2D page-curl engine,
 * with procedural 2% paper grain, bionic reading, PDF auto-crop, estimated time left,
 * highlights, and isolated featured-story touches.
 */
class PageBitmapRenderer {

    private val grainShaderLock = Any()
    private var lightGrainShader: BitmapShader? = null
    private var darkGrainShader: BitmapShader? = null

    private var activePdfPath: String? = null
    private var pdfPfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private val pdfLock = Any()

    private fun getGrainShader(isDark: Boolean): BitmapShader {
        synchronized(grainShaderLock) {
            val existing = if (isDark) darkGrainShader else lightGrainShader
            if (existing != null) return existing

            val size = 128
            val tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(size * size)
            val rng = Random(42071)
            for (i in pixels.indices) {
                val grain = rng.nextInt(100)
                pixels[i] = if (isDark) {
                    when {
                        grain < 35 -> android.graphics.Color.argb(6, 230, 224, 212)
                        grain < 70 -> android.graphics.Color.argb(5, 0, 0, 0)
                        else -> 0
                    }
                } else {
                    when {
                        grain < 38 -> android.graphics.Color.argb(6, 70, 56, 40)
                        grain < 72 -> android.graphics.Color.argb(5, 255, 252, 245)
                        else -> 0
                    }
                }
            }
            tile.setPixels(pixels, 0, size, 0, 0, size, size)
            val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            if (isDark) darkGrainShader = shader else lightGrainShader = shader
            return shader
        }
    }

    fun renderTextPage(
        page: PaginatedPage,
        totalPages: Int,
        estimatedMinutesLeft: Int,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        topInsetPx: Int,
        bottomInsetPx: Int,
        settings: ReaderSettings,
        highlights: List<HighlightEntity>,
        isFeaturedStory: Boolean = false
    ): Bitmap {
        val safeW = widthPx.coerceAtLeast(1)
        val safeH = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val palette = paletteFor(settings.themeMode)

        // 1. Curated paper background
        canvas.drawColor(palette.background.toArgb())

        // 2. Procedural 2% matte paper grain
        if (settings.paperGrainEnabled) {
            val grainPaint = Paint().apply { shader = getGrainShader(palette.isDark) }
            canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), grainPaint)
        }

        val metrics = Paginator.computeMetrics(safeW, safeH, density, topInsetPx, bottomInsetPx, settings)
        val textColor = palette.text.toArgb()
        val mutedColor = palette.textMuted.toArgb()
        val accentColor = palette.accent.toArgb()

        val bodyPaint = Paginator.createBodyTextPaint(settings, density, textColor)
        val headingPaint = Paginator.createHeadingTextPaint(settings, density, textColor)

        var currentY = metrics.topY

        // 3. Chapter Heading on opening page of chapter
        if (page.headingText != null) {
            val headingLayout = Paginator.buildStaticLayout(
                page.headingText,
                headingPaint,
                metrics.contentWidth,
                1.2f
            )
            canvas.save()
            canvas.translate(metrics.leftX, currentY)
            headingLayout.draw(canvas)
            canvas.restore()

            val ruleY = currentY + headingLayout.height + 10f * density
            val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                alpha = 95
                strokeWidth = 1.2f * density
            }
            canvas.drawLine(metrics.leftX, ruleY, metrics.leftX + 44f * density, ruleY, rulePaint)
            currentY += headingLayout.height + 24f * density
        }

        // 4. Highlights & Whisper-Note Underlines underneath text
        val styledBody = Paginator.formatDisplayText(page.bodyText, settings.bionicModeEnabled)
        val bodyLayout = Paginator.buildStaticLayout(
            styledBody,
            bodyPaint,
            metrics.contentWidth,
            settings.lineSpacing
        )

        val geometry = if ((highlights.isNotEmpty() || isFeaturedStory) && page.bodyText.isNotEmpty()) {
            PageTextGeometry(
                bodyText = page.bodyText,
                bodyTopY = currentY,
                bodyLeftX = metrics.leftX,
                bodyLayout = bodyLayout,
                wordBoxes = emptyList()
            )
        } else null

        if (geometry != null && highlights.isNotEmpty()) {
            val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = 3.5f * density

            highlights.forEach { hl ->
                val baseColor = HighlightColors[hl.colorIndex.coerceIn(0, HighlightColors.lastIndex)].toArgb()
                hlPaint.color = baseColor
                hlPaint.alpha = if (palette.isDark) 78 else 128

                val rects = geometry.getSelectionRects(hl.startOffset, hl.endOffset)
                rects.forEach { r ->
                    canvas.drawRoundRect(r, radius, radius, hlPaint)
                }

                if (hl.note.isNotBlank() && rects.isNotEmpty()) {
                    dotPaint.color = accentColor
                    dotPaint.alpha = 200
                    val firstRect = rects.first()
                    val dotX = (metrics.leftX - 10f * density).coerceAtLeast(8f * density)
                    canvas.drawCircle(dotX, firstRect.centerY(), 3f * density, dotPaint)
                }
            }
        }

        // 5. Crisp body text
        canvas.save()
        canvas.translate(metrics.leftX, currentY)
        bodyLayout.draw(canvas)
        canvas.restore()

        // 6. Zero-clutter tiny low-contrast bottom margin footer (page number, percentage, est. time left)
        drawBottomFooter(
            canvas = canvas,
            pageIndex = page.pageIndex,
            totalPages = totalPages,
            estimatedMinutesLeft = estimatedMinutesLeft,
            widthPx = safeW,
            heightPx = safeH,
            bottomInsetPx = bottomInsetPx,
            density = density,
            mutedColor = mutedColor
        )

        return bitmap
    }

    fun renderPdfPage(
        pdfFilePath: String,
        pageIndex: Int,
        totalPages: Int,
        estimatedMinutesLeft: Int,
        widthPx: Int,
        heightPx: Int,
        density: Float,
        topInsetPx: Int,
        bottomInsetPx: Int,
        settings: ReaderSettings,
        highlights: List<HighlightEntity>
    ): Bitmap {
        val safeW = widthPx.coerceAtLeast(1)
        val safeH = heightPx.coerceAtLeast(1)
        val resultBitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        val palette = paletteFor(settings.themeMode)

        canvas.drawColor(palette.background.toArgb())

        synchronized(pdfLock) {
            ensurePdfOpen(pdfFilePath)
            val renderer = pdfRenderer
            if (renderer != null && pageIndex in 0 until renderer.pageCount) {
                runCatching {
                    renderer.openPage(pageIndex).use { page ->
                        val availW = (safeW - (10f * density).toInt()).coerceAtLeast(100)
                        val availH = (safeH - topInsetPx - bottomInsetPx - (42f * density).toInt()).coerceAtLeast(100)
                        // Render slightly oversampled when Auto-Crop is on so cropped text stays razor sharp
                        val oversample = if (settings.pdfAutoCropEnabled) 1.35f else 1.0f
                        val baseScale = minOf(
                            availW.toFloat() / page.width.coerceAtLeast(1),
                            availH.toFloat() / page.height.coerceAtLeast(1)
                        ) * oversample
                        val renderW = (page.width * baseScale).roundToInt().coerceIn(64, 2200)
                        val renderH = (page.height * baseScale).roundToInt().coerceIn(64, 2800)

                        val rawPdf = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                        rawPdf.eraseColor(android.graphics.Color.WHITE)
                        page.render(rawPdf, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        val srcRect = if (settings.pdfAutoCropEnabled) {
                            detectPdfInkBounds(rawPdf)
                        } else {
                            Rect(0, 0, renderW, renderH)
                        }

                        val srcW = srcRect.width().coerceAtLeast(1)
                        val srcH = srcRect.height().coerceAtLeast(1)
                        val fitScale = minOf(availW.toFloat() / srcW, availH.toFloat() / srcH)
                        val drawW = srcW * fitScale
                        val drawH = srcH * fitScale

                        val dstLeft = (safeW - drawW) / 2f
                        val dstTop = topInsetPx + ((availH - drawH) / 2f).coerceAtLeast(8f * density)
                        val dstRect = RectF(dstLeft, dstTop, dstLeft + drawW, dstTop + drawH)

                        val pdfPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            colorFilter = buildPdfThemeColorFilter(settings.themeMode)
                        }
                        canvas.drawBitmap(rawPdf, srcRect, dstRect, pdfPaint)
                        rawPdf.recycle()
                    }
                }
            }
        }

        if (settings.paperGrainEnabled) {
            val grainPaint = Paint().apply { shader = getGrainShader(palette.isDark) }
            canvas.drawRect(0f, 0f, safeW.toFloat(), safeH.toFloat(), grainPaint)
        }

        if (highlights.isNotEmpty()) {
            val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            highlights.take(4).forEachIndexed { idx, hl ->
                pillPaint.color = HighlightColors[hl.colorIndex.coerceIn(0, HighlightColors.lastIndex)].toArgb()
                pillPaint.alpha = 210
                val x = safeW - (16f + idx * 10f) * density
                val y = topInsetPx + 14f * density
                canvas.drawCircle(x, y, 3.5f * density, pillPaint)
            }
        }

        drawBottomFooter(
            canvas = canvas,
            pageIndex = pageIndex,
            totalPages = totalPages,
            estimatedMinutesLeft = estimatedMinutesLeft,
            widthPx = safeW,
            heightPx = safeH,
            bottomInsetPx = bottomInsetPx,
            density = density,
            mutedColor = palette.textMuted.toArgb()
        )

        return resultBitmap
    }

    /**
     * 1-Tap PDF Auto-Crop: scans rendered white PDF page at coarse stride (~4ms)
     * to find tight non-white content bounds with subtle breathing padding.
     */
    private fun detectPdfInkBounds(bmp: Bitmap): Rect {
        val w = bmp.width
        val h = bmp.height
        val stride = 4
        var minX = w
        var maxX = 0
        var minY = h
        var maxY = 0
        val row = IntArray(w)

        var y = 0
        while (y < h) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            var x = 0
            while (x < w) {
                val p = row[x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                if (r < 242 || g < 242 || b < 242) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                x += stride
            }
            y += stride
        }

        if (maxX <= minX + 20 || maxY <= minY + 20) {
            return Rect(0, 0, w, h)
        }
        val padX = (w * 0.025f).toInt()
        val padY = (h * 0.025f).toInt()
        return Rect(
            (minX - padX).coerceAtLeast(0),
            (minY - padY).coerceAtLeast(0),
            (maxX + padX).coerceAtMost(w),
            (maxY + padY).coerceAtMost(h)
        )
    }

    private fun buildPdfThemeColorFilter(mode: ReaderThemeMode): ColorMatrixColorFilter {
        val matrix = when (mode) {
            ReaderThemeMode.PAPER -> {
                ColorMatrix(
                    floatArrayOf(
                        0.968f, 0f, 0f, 0f, 0f,
                        0f, 0.960f, 0f, 0f, 0f,
                        0f, 0f, 0.941f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ReaderThemeMode.SEPIA -> {
                ColorMatrix(
                    floatArrayOf(
                        0.82f, 0f, 0f, 0f, 30f,
                        0f, 0.80f, 0f, 0f, 24f,
                        0f, 0f, 0.74f, 0f, 18f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            ReaderThemeMode.NIGHT -> {
                ColorMatrix(
                    floatArrayOf(
                        -0.76f, 0f, 0f, 0f, 212f,
                        0f, -0.75f, 0f, 0f, 210f,
                        0f, 0f, -0.72f, 0f, 204f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
        return ColorMatrixColorFilter(matrix)
    }

    private fun drawBottomFooter(
        canvas: Canvas,
        pageIndex: Int,
        totalPages: Int,
        estimatedMinutesLeft: Int,
        widthPx: Int,
        heightPx: Int,
        bottomInsetPx: Int,
        density: Float,
        mutedColor: Int
    ) {
        val safeTotal = totalPages.coerceAtLeast(1)
        val pct = if (safeTotal <= 1) 100 else ((pageIndex.toFloat() / (safeTotal - 1)) * 100f).roundToInt().coerceIn(0, 100)
        val timePart = if (estimatedMinutesLeft <= 0) "done" else "${estimatedMinutesLeft}m left"
        val label = "${pageIndex + 1} / $safeTotal  ·  $pct%  ·  $timePart"
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedColor
            alpha = 135
            textSize = 10.5f * density
            typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        val y = heightPx - bottomInsetPx - 14f * density
        canvas.drawText(label, widthPx / 2f, y, footerPaint)
    }

    private fun ensurePdfOpen(pdfFilePath: String) {
        if (activePdfPath == pdfFilePath && pdfRenderer != null) return
        closePdfInternal()
        val file = File(pdfFilePath)
        if (!file.exists()) return
        runCatching {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfPfd = pfd
            pdfRenderer = PdfRenderer(pfd)
            activePdfPath = pdfFilePath
        }
    }

    private fun closePdfInternal() {
        runCatching { pdfRenderer?.close() }
        runCatching { pdfPfd?.close() }
        pdfRenderer = null
        pdfPfd = null
        activePdfPath = null
    }

    fun release() {
        synchronized(pdfLock) {
            closePdfInternal()
        }
    }
}
