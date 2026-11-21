package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BookEntity
import com.example.data.model.BookmarkEntity
import com.example.data.model.HighlightEntity
import com.example.data.model.ReadingActivityEntity
import com.example.data.model.TranslationEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    /** Strictly excludes hidden shelf books from every public Library view, search & count. */
    @Query("SELECT * FROM books WHERE isHidden = 0 ORDER BY lastReadAt DESC")
    fun getVisibleBooks(): Flow<List<BookEntity>>

    /** Queried ONLY while Private Shelf is explicitly unlocked in memory. */
    @Query("SELECT * FROM books WHERE isHidden = 1 ORDER BY lastReadAt DESC")
    fun getHiddenBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE filePath = :filePath LIMIT 1")
    suspend fun getBookByFilePath(filePath: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Query("UPDATE books SET currentPage = :currentPage, totalPages = :totalPages, lastReadAt = :timestamp WHERE id = :bookId")
    suspend fun updateProgress(bookId: Long, currentPage: Int, totalPages: Int, timestamp: Long)

    @Query("UPDATE books SET title = :title, author = :author WHERE id = :bookId")
    suspend fun updateBookMetadata(bookId: Long, title: String, author: String)

    @Query("UPDATE books SET coverImagePath = :coverImagePath, coverStyleIndex = :coverStyleIndex WHERE id = :bookId")
    suspend fun updateBookCover(bookId: Long, coverImagePath: String?, coverStyleIndex: Int)

    @Query("UPDATE books SET shelfTag = :shelfTag WHERE id = :bookId")
    suspend fun updateShelfTag(bookId: Long, shelfTag: String)

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavorite(bookId: Long, isFavorite: Boolean)

    @Query("UPDATE books SET isHidden = :isHidden WHERE id = :bookId")
    suspend fun updateHidden(bookId: Long, isHidden: Boolean)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    @Query("DELETE FROM books WHERE filePath = :filePath")
    suspend fun deleteBookByFilePath(filePath: String)
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageIndex ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getBookmarkAtPage(bookId: Long, pageIndex: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageIndex = :pageIndex")
    suspend fun deleteBookmarkAtPage(bookId: Long, pageIndex: Int)
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY pageIndex ASC, startOffset ASC")
    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("UPDATE highlights SET colorIndex = :colorIndex WHERE id = :id")
    suspend fun updateHighlightColor(id: Long, colorIndex: Int)

    @Query("UPDATE highlights SET note = :note WHERE id = :id")
    suspend fun updateHighlightNote(id: Long, note: String)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: Long)
}

@Dao
interface ReadingActivityDao {
    @Query("SELECT * FROM reading_activity ORDER BY dateKey DESC LIMIT 90")
    fun getRecentActivity(): Flow<List<ReadingActivityEntity>>

    @Query("SELECT * FROM reading_activity WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getByDate(dateKey: String): ReadingActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ReadingActivityEntity)
}

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations WHERE normalizedKey = :key LIMIT 1")
    suspend fun findByKey(key: String): TranslationEntryEntity?

    @Query("SELECT * FROM translations WHERE isSavedToWordBank = 1 ORDER BY createdAt DESC")
    fun getSavedWordBank(): Flow<List<TranslationEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: TranslationEntryEntity): Long

    @Query("UPDATE translations SET isSavedToWordBank = :saved WHERE id = :id")
    suspend fun setSavedToWordBank(id: Long, saved: Boolean)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        ReadingActivityEntity::class,
        TranslationEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun readingActivityDao(): ReadingActivityDao
    abstract fun translationDao(): TranslationDao

    companion object {
        @Volatile
        private var instance: FolioDatabase? = null

        fun getInstance(context: Context): FolioDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FolioDatabase::class.java,
                    "epos_offline.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
