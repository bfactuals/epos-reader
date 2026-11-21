package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.example.config.WelcomeGuideBook
import com.example.data.local.FolioDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookFormat
import com.example.data.model.BookmarkEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.LibraryShelfFilter
import com.example.data.model.ReadingActivityEntity
import com.example.data.model.TranslationEntryEntity
import com.example.engine.parser.BookDocumentParser
import com.example.ui.theme.CuratedCoverStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class LibraryRepository(
    private val context: Context,
    private val database: FolioDatabase
) {
    private val bookDao = database.bookDao()
    private val bookmarkDao = database.bookmarkDao()
    private val highlightDao = database.highlightDao()
    private val activityDao = database.readingActivityDao()
    private val translationDao = database.translationDao()

    /** Strictly visible (non-hidden) books for all standard library shelves, search & counts. */
    val visibleBooks: Flow<List<BookEntity>> = bookDao.getVisibleBooks()

    /** Private shelf books — ONLY observed while Private Shelf is explicitly unlocked in memory. */
    val hiddenBooks: Flow<List<BookEntity>> = bookDao.getHiddenBooks()

    /** Daily reading activity (strictly excludes hidden shelf books). */
    val recentReadingActivity: Flow<List<ReadingActivityEntity>> = activityDao.getRecentActivity()

    /** Saved offline Word Bank entries. */
    val savedWordBank: Flow<List<TranslationEntryEntity>> = translationDao.getSavedWordBank()

    fun getTranslationDao() = translationDao

    /**
     * Purges any legacy pre-loaded books and seeds ONLY the single "Welcome to Epos" guide
     * on first launch (deletable by the user afterwards without being re-seeded).
     */
    suspend fun ensureWelcomeGuideSeeded(alreadySeededOnce: Boolean): Long? = withContext(Dispatchers.IO) {
        // Purge all legacy pre-loaded volumes
        WelcomeGuideBook.LEGACY_BUILTIN_PATHS.forEach { oldPath ->
            bookDao.deleteBookByFilePath(oldPath)
        }

        val existing = bookDao.getBookByFilePath(WelcomeGuideBook.FILE_PATH)
        if (existing != null) return@withContext existing.id
        if (alreadySeededOnce) return@withContext null

        bookDao.insertBook(
            BookEntity(
                title = WelcomeGuideBook.TITLE,
                author = WelcomeGuideBook.AUTHOR,
                format = BookFormat.EPUB.name,
                filePath = WelcomeGuideBook.FILE_PATH,
                coverStyleIndex = 4, // Minimalist Graphite Linen + Warm Gold typography cover
                currentPage = 0,
                totalPages = 5,
                lastReadAt = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis(),
                shelfTag = LibraryShelfFilter.READING.name,
                isFavorite = false,
                isHidden = false,
                isFeaturedStory = false
            )
        )
    }

    suspend fun getWelcomeGuideBookId(): Long? = withContext(Dispatchers.IO) {
        bookDao.getBookByFilePath(WelcomeGuideBook.FILE_PATH)?.id
    }

    suspend fun getBookById(id: Long): BookEntity? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)
    }

    suspend fun importDocumentFromUri(uri: Uri, intoHiddenVault: Boolean = false): Result<BookEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val displayName = queryDisplayName(uri) ?: "Untitled Book"
                val mimeType = resolver.getType(uri)?.lowercase().orEmpty()
                val lowerName = displayName.lowercase()

                val format = when {
                    lowerName.endsWith(".epub") || mimeType.contains("epub") -> BookFormat.EPUB
                    lowerName.endsWith(".pdf") || mimeType.contains("pdf") -> BookFormat.PDF
                    lowerName.endsWith(".txt") || mimeType.startsWith("text/") -> BookFormat.TXT
                    else -> throw IllegalArgumentException("Unsupported format. Please select an EPUB, PDF, or TXT file.")
                }

                val ext = format.name.lowercase()
                val booksDir = File(context.filesDir, "books").apply { mkdirs() }
                val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                val localFile = File(booksDir, "book_${System.currentTimeMillis()}.$ext")

                resolver.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Could not open selected file.")

                val cleanFallbackTitle = displayName
                    .substringBeforeLast('.')
                    .replace('_', ' ')
                    .replace('-', ' ')
                    .trim()
                    .ifBlank { "Untitled Volume" }

                val metadata = BookDocumentParser.inspectImportedFile(
                    file = localFile,
                    format = format,
                    fallbackTitle = cleanFallbackTitle,
                    coverOutputDir = coversDir
                )

                val styleIndex = abs(metadata.title.hashCode()) % CuratedCoverStyles.size
                val entity = BookEntity(
                    title = metadata.title,
                    author = metadata.author,
                    format = format.name,
                    filePath = localFile.absolutePath,
                    coverImagePath = metadata.coverImagePath,
                    coverStyleIndex = styleIndex,
                    currentPage = 0,
                    totalPages = metadata.totalPagesEstimate.coerceAtLeast(1),
                    lastReadAt = System.currentTimeMillis(),
                    addedAt = System.currentTimeMillis(),
                    shelfTag = LibraryShelfFilter.READING.name,
                    isFavorite = false,
                    isHidden = intoHiddenVault,
                    isFeaturedStory = false
                )
                val newId = bookDao.insertBook(entity)
                entity.copy(id = newId)
            }
        }

    suspend fun renameBook(bookId: Long, newTitle: String, newAuthor: String) =
        withContext(Dispatchers.IO) {
            val cleanTitle = newTitle.trim().take(120)
            if (cleanTitle.isBlank()) return@withContext
            val cleanAuthor = newAuthor.trim().take(80).ifBlank { "Unknown Author" }
            bookDao.updateBookMetadata(bookId, cleanTitle, cleanAuthor)
        }

    suspend fun updateBookCoverStyle(book: BookEntity, styleIndex: Int) =
        withContext(Dispatchers.IO) {
            // Clean up any previous custom cover file on disk
            book.coverImagePath?.let { oldPath -> runCatching { File(oldPath).delete() } }
            val safeIdx = styleIndex.coerceIn(0, CuratedCoverStyles.lastIndex)
            bookDao.updateBookCover(book.id, null, safeIdx)
        }

    /**
     * Compresses a user-selected Photo Picker cover image cleanly (max 540x780 JPEG)
     * into app-private offline storage so the library stays fast and lightweight.
     */
    suspend fun updateBookCoverFromUri(book: BookEntity, imageUri: Uri): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }

                val rawW = boundsOpts.outWidth.coerceAtLeast(1)
                val rawH = boundsOpts.outHeight.coerceAtLeast(1)
                var sampleSize = 1
                while (rawW / (sampleSize * 2) >= 540 && rawH / (sampleSize * 2) >= 780) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val decoded = resolver.openInputStream(imageUri)?.use {
                    BitmapFactory.decodeStream(it, null, decodeOpts)
                } ?: throw IllegalStateException("Could not decode image.")

                val scale = minOf(540f / decoded.width, 780f / decoded.height, 1f)
                val finalBmp = if (scale < 0.98f) {
                    val w = (decoded.width * scale).toInt().coerceAtLeast(1)
                    val h = (decoded.height * scale).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, w, h, true).also {
                        if (it !== decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }

                val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
                val outFile = File(coversDir, "cover_${book.id}_${System.currentTimeMillis()}.jpg")
                outFile.outputStream().use { out ->
                    finalBmp.compress(Bitmap.CompressFormat.JPEG, 82, out)
                }
                finalBmp.recycle()

                book.coverImagePath?.let { oldPath ->
                    if (oldPath != outFile.absolutePath) runCatching { File(oldPath).delete() }
                }
                bookDao.updateBookCover(book.id, outFile.absolutePath, book.coverStyleIndex)
            }
        }

    suspend fun updateReadingProgress(bookId: Long, currentPage: Int, totalPages: Int) =
        withContext(Dispatchers.IO) {
            val safeTotal = totalPages.coerceAtLeast(1)
            val safePage = currentPage.coerceIn(0, safeTotal - 1)
            bookDao.updateProgress(
                bookId = bookId,
                currentPage = safePage,
                totalPages = safeTotal,
                timestamp = System.currentTimeMillis()
            )
            val book = bookDao.getBookById(bookId)
            if (book != null && !book.isHidden && safeTotal > 1 && safePage >= safeTotal - 1 &&
                book.shelfTag == LibraryShelfFilter.READING.name
            ) {
                bookDao.updateShelfTag(bookId, LibraryShelfFilter.COMPLETED.name)
            }
        }

    suspend fun updateShelfTag(bookId: Long, shelf: LibraryShelfFilter) = withContext(Dispatchers.IO) {
        bookDao.updateShelfTag(bookId, shelf.name)
    }

    suspend fun toggleFavorite(bookId: Long, current: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateFavorite(bookId, !current)
    }

    suspend fun setBookHidden(bookId: Long, hidden: Boolean) = withContext(Dispatchers.IO) {
        bookDao.updateHidden(bookId, hidden)
    }

    suspend fun moveBooksToHiddenVault(bookIds: Collection<Long>) = withContext(Dispatchers.IO) {
        bookIds.forEach { id -> bookDao.updateHidden(id, true) }
    }

    suspend fun deleteBook(book: BookEntity) = withContext(Dispatchers.IO) {
        bookDao.deleteBookById(book.id)
        if (!book.filePath.startsWith("builtin://")) {
            runCatching { File(book.filePath).delete() }
        }
        book.coverImagePath?.let { path ->
            runCatching { File(path).delete() }
        }
    }

    // --- Daily Reading Activity (STRICT ZERO-TRACE: never records hidden shelf books) ---
    suspend fun recordVisibleReadingActivity(isHiddenBook: Boolean, deltaMinutes: Int, deltaPages: Int) =
        withContext(Dispatchers.IO) {
            if (isHiddenBook) return@withContext
            if (deltaMinutes <= 0 && deltaPages <= 0) return@withContext
            val key = todayDateKey()
            val existing = activityDao.getByDate(key) ?: ReadingActivityEntity(dateKey = key)
            activityDao.upsert(
                existing.copy(
                    minutesRead = existing.minutesRead + deltaMinutes.coerceAtLeast(0),
                    pagesRead = existing.pagesRead + deltaPages.coerceAtLeast(0)
                )
            )
        }

    fun todayDateKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    // --- Word Bank ---
    suspend fun setWordBankSaved(entryId: Long, saved: Boolean) = withContext(Dispatchers.IO) {
        translationDao.setSavedToWordBank(entryId, saved)
    }

    suspend fun deleteWordBankEntry(entryId: Long) = withContext(Dispatchers.IO) {
        translationDao.deleteById(entryId)
    }

    // --- Bookmarks ---
    fun getBookmarks(bookId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForBook(bookId)

    suspend fun toggleBookmark(
        bookId: Long,
        pageIndex: Int,
        chapterTitle: String,
        excerpt: String
    ): Boolean = withContext(Dispatchers.IO) {
        val existing = bookmarkDao.getBookmarkAtPage(bookId, pageIndex)
        if (existing != null) {
            bookmarkDao.deleteBookmarkAtPage(bookId, pageIndex)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    pageIndex = pageIndex,
                    chapterTitle = chapterTitle,
                    excerpt = excerpt.take(120)
                )
            )
            true
        }
    }

    suspend fun deleteBookmark(id: Long) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkById(id)
    }

    // --- Highlights & Notes ---
    fun getHighlights(bookId: Long): Flow<List<HighlightEntity>> = highlightDao.getHighlightsForBook(bookId)

    suspend fun insertHighlight(highlight: HighlightEntity): Long = withContext(Dispatchers.IO) {
        highlightDao.insertHighlight(highlight)
    }

    suspend fun updateHighlightColor(id: Long, colorIndex: Int) = withContext(Dispatchers.IO) {
        highlightDao.updateHighlightColor(id, colorIndex)
    }

    suspend fun updateHighlightNote(id: Long, note: String) = withContext(Dispatchers.IO) {
        highlightDao.updateHighlightNote(id, note)
    }

    suspend fun deleteHighlight(id: Long) = withContext(Dispatchers.IO) {
        highlightDao.deleteHighlightById(id)
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }
}
