package com.example.engine.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.example.config.APP_NAME
import com.example.data.model.HighlightEntity
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.theme.paletteFor
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure 2D Android Canvas Aesthetic Quote Card generator & Highlights .txt Exporter (0 KB assets).
 */
object QuoteCardAndExport {

    fun renderQuoteCardBitmap(
        quote: String,
        bookTitle: String,
        bookAuthor: String,
        themeMode: ReaderThemeMode,
        widthPx: Int = 1080,
        heightPx: Int = 1350
    ): Bitmap {
        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val palette = paletteFor(themeMode)

        // 1. Background
        canvas.drawColor(palette.background.toArgb())

        // 2. Delicate editorial outer frame
        val frameInset = widthPx * 0.065f
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = palette.divider.toArgb()
        }
        canvas.drawRoundRect(
            RectF(frameInset, frameInset, widthPx - frameInset, heightPx - frameInset),
            24f,
            24f,
            framePaint
        )

        val contentLeft = widthPx * 0.13f
        val contentWidth = (widthPx * 0.74f).toInt()

        // 3. Large refined opening quotation mark
        val quoteMarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent.toArgb()
            alpha = 150
            textSize = widthPx * 0.14f
            typeface = Typeface.SERIF
        }
        canvas.drawText("“", contentLeft - 8f, heightPx * 0.24f, quoteMarkPaint)

        // 4. Centered/clean editorial serif quote text (auto-sized by quote length)
        val cleanQuote = quote.trim().take(420)
        val quoteSize = when {
            cleanQuote.length < 90 -> widthPx * 0.052f
            cleanQuote.length < 200 -> widthPx * 0.044f
            else -> widthPx * 0.036f
        }
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = palette.text.toArgb()
            textSize = quoteSize
            typeface = Typeface.SERIF
            letterSpacing = 0.01f
        }
        val quoteLayout = StaticLayout.Builder.obtain(
            cleanQuote, 0, cleanQuote.length, quotePaint, contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.48f)
            .setIncludePad(false)
            .build()

        val quoteTop = (heightPx * 0.27f).coerceAtLeast(heightPx * 0.50f - quoteLayout.height * 0.55f)
        canvas.save()
        canvas.translate(contentLeft, quoteTop)
        quoteLayout.draw(canvas)
        canvas.restore()

        // 5. Hairline accent rule + book title & author
        val ruleY = (quoteTop + quoteLayout.height + 64f).coerceAtMost(heightPx * 0.78f)
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent.toArgb()
            strokeWidth = 3f
        }
        canvas.drawLine(contentLeft, ruleY, contentLeft + 76f, ruleY, rulePaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.text.toArgb()
            textSize = widthPx * 0.030f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(bookTitle.take(44), contentLeft, ruleY + 52f, titlePaint)

        if (bookAuthor.isNotBlank()) {
            val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textMuted.toArgb()
                textSize = widthPx * 0.025f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            canvas.drawText(bookAuthor.take(44), contentLeft, ruleY + 90f, authorPaint)
        }

        // 6. Subtle Folio Fanned-Pages mark + wordmark at bottom
        drawMiniFolioWatermark(
            canvas = canvas,
            cx = contentLeft + 24f,
            cy = heightPx - frameInset - 54f,
            size = 44f,
            textColor = palette.textMuted.toArgb()
        )
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textMuted.toArgb()
            alpha = 180
            textSize = widthPx * 0.024f
            typeface = Typeface.SERIF
            letterSpacing = 0.08f
        }
        canvas.drawText(APP_NAME, contentLeft + 58f, heightPx - frameInset - 44f, brandPaint)

        return bmp
    }

    private fun drawMiniFolioWatermark(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        textColor: Int
    ) {
        val left = cx - size / 2f
        val top = cy - size / 2f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF121214.toInt() }
        canvas.drawRoundRect(RectF(left, top, left + size, top + size), size * 0.24f, size * 0.24f, bgPaint)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFF7F5F0.toInt()
            strokeWidth = size * 0.07f
            strokeCap = Paint.Cap.ROUND
        }
        val base = Path().apply {
            moveTo(left + size * 0.20f, top + size * 0.70f)
            quadTo(left + size * 0.36f, top + size * 0.63f, left + size * 0.50f, top + size * 0.77f)
            quadTo(left + size * 0.64f, top + size * 0.63f, left + size * 0.80f, top + size * 0.70f)
        }
        canvas.drawPath(base, strokePaint)

        val fanAlphas = intArrayOf(245, 200, 155, 115, 75)
        val tipXs = floatArrayOf(0.24f, 0.37f, 0.50f, 0.63f, 0.76f)
        val tipYs = floatArrayOf(0.39f, 0.29f, 0.24f, 0.29f, 0.39f)
        val fanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFF7F5F0.toInt()
            strokeWidth = size * 0.045f
            strokeCap = Paint.Cap.ROUND
        }
        for (i in 0 until 5) {
            fanPaint.alpha = fanAlphas[i]
            canvas.drawLine(
                left + size * 0.50f,
                top + size * 0.72f,
                left + size * tipXs[i],
                top + size * tipYs[i],
                fanPaint
            )
        }
    }

    fun shareQuoteCardImage(context: Context, bitmap: Bitmap, bookTitle: String): Boolean {
        return runCatching {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "epos_quote_${System.currentTimeMillis()}.png")
            file.outputStream().use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "From \"$bookTitle\" — via $APP_NAME")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Quote Card"))
            true
        }.getOrDefault(false)
    }

    fun buildHighlightsTxtContent(
        bookTitle: String,
        bookAuthor: String,
        highlights: List<HighlightEntity>
    ): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.appendLine("$bookTitle — $bookAuthor")
        sb.appendLine("Exported from $APP_NAME on $dateStr (${highlights.size} highlights)")
        sb.appendLine("=".repeat(52))
        sb.appendLine()
        highlights.forEachIndexed { idx, hl ->
            val chap = if (hl.chapterTitle.isNotBlank()) " · ${hl.chapterTitle}" else ""
            sb.appendLine("${idx + 1}. [Page ${hl.pageIndex + 1}$chap]")
            sb.appendLine("“${hl.quoteText}”")
            if (hl.note.isNotBlank()) {
                sb.appendLine("Note: ${hl.note}")
            }
            sb.appendLine()
        }
        return sb.toString()
    }

    fun shareHighlightsTxt(
        context: Context,
        bookTitle: String,
        bookAuthor: String,
        highlights: List<HighlightEntity>
    ): Boolean {
        return runCatching {
            val content = buildHighlightsTxtContent(bookTitle, bookAuthor, highlights)
            val safeName = bookTitle.replace(Regex("[^a-zA-Z0-9]+"), "_").take(32).ifBlank { "book" }
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, "${safeName}_highlights.txt")
            file.writeText(content, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$bookTitle — Highlights")
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Highlights (.txt)"))
            true
        }.getOrDefault(false)
    }
}
