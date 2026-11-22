package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ui.theme.ReaderFontStyle
import com.example.ui.theme.ReaderThemeMode

enum class BookFormat(val badge: String) {
    EPUB("EPUB"),
    PDF("PDF"),
    TXT("TXT")
}

enum class LibraryShelfFilter(val label: String) {
    ALL("All"),
    READING("Reading"),
    COMPLETED("Completed"),
    STUDY("Study"),
    FAVORITES("Favorites")
}

enum class AmbientSoundMode(val label: String) {
    OFF("Off"),
    RAIN("Soft Rain"),
    VINYL("Vinyl Crackle")
}

enum class DailyGoalUnit(val label: String, val shortUnit: String) {
    MINUTES("Minutes", "m"),
    PAGES("Pages", "p")
}

enum class TranslationLanguage(
    val code: String,
    val label: String,
    val myMemoryCode: String,
    val promptName: String,
    val nativeName: String,
    val isRtl: Boolean = false
) {
    HINDI("hi", "Hindi", "hi", "Hindi (Devanagari script)", "हिन्दी"),
    ENGLISH("en", "English", "en", "English", "English"),
    HINGLISH("hi-Latn", "Hinglish", "hi", "Hinglish (natural Hindi written in Latin/Roman script)", "Hindi · Latin"),
    ARABIC("ar", "Arabic", "ar", "Arabic", "العربية", isRtl = true),
    URDU("ur", "Urdu", "ur", "Urdu", "اردو", isRtl = true),
    MARATHI("mr", "Marathi", "mr", "Marathi", "मराठी"),
    BENGALI("bn", "Bengali", "bn", "Bengali", "বাংলা"),
    TAMIL("ta", "Tamil", "ta", "Tamil", "தமிழ்"),
    TELUGU("te", "Telugu", "te", "Telugu", "తెలుగు"),
    GUJARATI("gu", "Gujarati", "gu", "Gujarati", "ગુજરાતી"),
    PUNJABI("pa", "Punjabi", "pa", "Punjabi (Gurmukhi script)", "ਪੰਜਾਬੀ"),
    SPANISH("es", "Spanish", "es", "Spanish", "Español"),
    FRENCH("fr", "French", "fr", "French", "Français"),
    GERMAN("de", "German", "de", "German", "Deutsch"),
    JAPANESE("ja", "Japanese", "ja", "Japanese", "日本語");

    companion object {
        fun fromCode(code: String?): TranslationLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: HINDI
        }
    }
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val format: String, // BookFormat.name
    val filePath: String, // "builtin://..." or internal file path
    val coverImagePath: String? = null,
    val coverStyleIndex: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 1,
    val lastReadAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val shelfTag: String = LibraryShelfFilter.READING.name, // READING, COMPLETED, STUDY
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false, // Zero-trace private shelf flag
    val isFeaturedStory: Boolean = false
) {
    val bookFormat: BookFormat
        get() = runCatching { BookFormat.valueOf(format) }.getOrDefault(BookFormat.TXT)

    val progressFraction: Float
        get() = if (totalPages <= 1) 0f else (currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1)).coerceIn(0f, 1f)
}

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageIndex: Int,
    val chapterTitle: String,
    val excerpt: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageIndex: Int,
    val startOffset: Int,
    val endOffset: Int,
    val quoteText: String,
    val colorIndex: Int = 0, // 0..3 matching HighlightColors
    val note: String = "",
    val chapterTitle: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Daily reading activity for visible (non-hidden) books only.
 * Hidden shelf reading never writes to this table.
 */
@Entity(tableName = "reading_activity")
data class ReadingActivityEntity(
    @PrimaryKey val dateKey: String, // yyyy-MM-dd
    val minutesRead: Int = 0,
    val pagesRead: Int = 0
)

/**
 * Offline multi-language translation cache & user-curated Word Bank.
 * Keyed strictly by (normalizedSourceText + targetLanguageCode).
 */
@Entity(
    tableName = "translations",
    indices = [Index(value = ["normalizedKey"], unique = true)]
)
data class TranslationEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedKey: String, // "${targetLanguage}|${sourceText.lowercase().trim()}"
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String = "auto",
    val targetLanguage: String = TranslationLanguage.HINDI.code,
    val isSavedToWordBank: Boolean = false,
    val bookTitle: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val targetLanguageEnum: TranslationLanguage
        get() = TranslationLanguage.fromCode(targetLanguage)
}

data class ReaderSettings(
    val themeMode: ReaderThemeMode = ReaderThemeMode.PAPER,
    val fontStyle: ReaderFontStyle = ReaderFontStyle.CLASSIC_BOOK,
    val fontSizeSp: Float = 18f,
    val lineSpacing: Float = 1.48f,
    val marginDp: Float = 24f,
    val paperSoundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val paperGrainEnabled: Boolean = true,
    val pageStiffness: Float = 0.55f, // 0.2f (supple) .. 1.0f (crisp)
    // Reading Comfort Tools
    val warmthFilter: Float = 0f, // 0f..0.65f blue-light amber warmth
    val useSystemBrightness: Boolean = true,
    val brightnessLevel: Float = 0.85f, // 0.05f..1.0f custom reader brightness
    val readingRulerEnabled: Boolean = false,
    val bionicModeEnabled: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val oneHandedTapMode: Boolean = false,
    val pdfAutoCropEnabled: Boolean = false,
    val ambientSoundMode: AmbientSoundMode = AmbientSoundMode.OFF,
    val ambientVolume: Float = 0.55f,
    val ttsSpeed: Float = 1.0f,
    val autoFlipSeconds: Int = 0, // 0 = off, else 8 / 15 / 25 / 40
    val dailyGoalTarget: Int = 20,
    val dailyGoalUnit: DailyGoalUnit = DailyGoalUnit.MINUTES,
    val defaultTargetLanguage: TranslationLanguage = TranslationLanguage.HINDI,
    val fullPageTranslateEnabled: Boolean = false,
    val shelfGridColumns: Int = 2, // 2, 3, or 4 columns
    val geminiApiKey: String = "",
    val hasDismissedFirstTimeGreeting: Boolean = false,
    val hasSeededWelcomeGuide: Boolean = false
)

data class ChapterTocItem(
    val title: String,
    val pageIndex: Int,
    val chapterIndex: Int
)

data class SearchResult(
    val pageIndex: Int,
    val chapterTitle: String,
    val snippetBefore: String,
    val matchText: String,
    val snippetAfter: String,
    val startOffsetInPage: Int,
    val endOffsetInPage: Int
)
