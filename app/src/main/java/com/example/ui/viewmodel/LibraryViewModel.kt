package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.config.RemoteAppConfig
import com.example.data.local.PreferencesRepository
import com.example.data.model.BookEntity
import com.example.data.model.DailyGoalUnit
import com.example.data.model.LibraryShelfFilter
import com.example.data.model.ReaderSettings
import com.example.data.model.ReadingActivityEntity
import com.example.data.model.TranslationEntryEntity
import com.example.data.model.TranslationLanguage
import com.example.data.repository.LibraryRepository
import com.example.engine.config.RemoteConfigService
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: LibraryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val settings: StateFlow<ReaderSettings> = preferencesRepository.settings
    val remoteConfig: StateFlow<RemoteAppConfig> = preferencesRepository.remoteConfig

    private val _selectedShelf = MutableStateFlow(LibraryShelfFilter.ALL)
    val selectedShelf: StateFlow<LibraryShelfFilter> = _selectedShelf.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    val visibleBooks: StateFlow<List<BookEntity>> = repository.visibleBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Filtered visible books respecting both shelf filter and expandable header search. */
    val filteredBooks: StateFlow<List<BookEntity>> =
        combine(visibleBooks, _selectedShelf, _searchQuery) { books, shelf, query ->
            val byShelf = when (shelf) {
                LibraryShelfFilter.ALL -> books
                LibraryShelfFilter.FAVORITES -> books.filter { it.isFavorite }
                else -> books.filter { it.shelfTag == shelf.name }
            }
            val q = query.trim().lowercase()
            if (q.isEmpty()) byShelf
            else byShelf.filter {
                it.title.lowercase().contains(q) || it.author.lowercase().contains(q)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Most recently read visible (non-hidden) book for the 1-tap Continue Reading pill. */
    val continueReadingBook: StateFlow<BookEntity?> = visibleBooks
        .map { list -> list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Zero-Trace Private Shelf state (held strictly in memory; cleared on lock / back / background)
    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    val vaultBooks: StateFlow<List<BookEntity>> =
        combine(repository.hiddenBooks, _isVaultUnlocked) { hidden, unlocked ->
            if (unlocked) hidden else emptyList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hasSecretCode = MutableStateFlow(preferencesRepository.hasSecretShelfCode())
    val hasSecretCode: StateFlow<Boolean> = _hasSecretCode.asStateFlow()

    // Daily Reading Activity & Heatmap (visible books only)
    val recentActivity: StateFlow<List<ReadingActivityEntity>> = repository.recentReadingActivity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Saved offline multi-language Word Bank
    val wordBankEntries: StateFlow<List<TranslationEntryEntity>> = repository.savedWordBank
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _welcomeGuideBookId = MutableStateFlow<Long?>(null)
    val welcomeGuideBookId: StateFlow<Long?> = _welcomeGuideBookId.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            val alreadySeeded = preferencesRepository.settings.value.hasSeededWelcomeGuide
            val guideId = repository.ensureWelcomeGuideSeeded(alreadySeededOnce = alreadySeeded)
            if (!alreadySeeded) {
                preferencesRepository.updateSettings { it.copy(hasSeededWelcomeGuide = true) }
            }
            _welcomeGuideBookId.value = guideId
        }
        // Silently fetch remote GitHub config on startup (zero error popups if offline)
        viewModelScope.launch {
            val freshJson = RemoteConfigService.fetchLatestRawJson()
            if (freshJson != null) {
                preferencesRepository.saveRemoteConfigJson(freshJson)
            }
        }
    }

    fun selectShelf(filter: LibraryShelfFilter) {
        _selectedShelf.value = filter
    }

    fun setSearchExpanded(expanded: Boolean) {
        _isSearchExpanded.value = expanded
        if (!expanded) {
            _searchQuery.value = ""
        }
    }

    /**
     * Updates Library search query. If the exact Secret Shelf Code is typed, immediately clears
     * the search input, collapses the search bar, and opens the zero-trace Private Shelf.
     */
    fun onLibrarySearchQueryChange(newQuery: String) {
        if (preferencesRepository.matchesSecretShelfCode(newQuery)) {
            _searchQuery.value = ""
            _isSearchExpanded.value = false
            _isVaultUnlocked.value = true
            return
        }
        _searchQuery.value = newQuery
    }

    fun createSecretShelfCode(newCode: String, confirmCode: String): String? {
        val cleanNew = newCode.trim()
        val cleanConfirm = confirmCode.trim()
        if (cleanNew.length < 3) return "Secret code must be at least 3 characters"
        if (cleanNew != cleanConfirm) return "Secret codes do not match"
        val ok = preferencesRepository.setSecretShelfCode(cleanNew)
        _hasSecretCode.value = preferencesRepository.hasSecretShelfCode()
        if (ok) {
            _statusMessage.value = "Secret Shelf Code activated"
            return null
        }
        return "Could not save secret code"
    }

    /**
     * Strictly verifies the current Secret Code before updating to a new Secret Code.
     */
    fun changeSecretShelfCode(currentCode: String, newCode: String, confirmCode: String): String? {
        val cleanCurrent = currentCode.trim()
        val cleanNew = newCode.trim()
        val cleanConfirm = confirmCode.trim()
        if (cleanCurrent.isEmpty()) return "Enter your current secret code"
        if (!preferencesRepository.matchesSecretShelfCode(cleanCurrent)) {
            return "Current secret code is incorrect"
        }
        if (cleanNew.length < 3) return "New secret code must be at least 3 characters"
        if (cleanNew != cleanConfirm) return "New secret codes do not match"
        val ok = preferencesRepository.changeSecretShelfCode(cleanCurrent, cleanNew)
        _hasSecretCode.value = preferencesRepository.hasSecretShelfCode()
        if (ok) {
            _statusMessage.value = "Secret Shelf Code updated"
            return null
        }
        return "Could not update secret code"
    }

    /**
     * Strictly verifies the current Secret Code before removing it.
     */
    fun removeSecretShelfCode(currentCode: String): String? {
        val cleanCurrent = currentCode.trim()
        if (cleanCurrent.isEmpty()) return "Enter your current secret code"
        if (!preferencesRepository.removeSecretShelfCode(cleanCurrent)) {
            return "Current secret code is incorrect"
        }
        _hasSecretCode.value = false
        _isVaultUnlocked.value = false
        _statusMessage.value = "Secret Shelf Code removed"
        return null
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun moveLibraryBooksToVault(bookIds: Set<Long>) {
        if (bookIds.isEmpty()) return
        viewModelScope.launch {
            repository.moveBooksToHiddenVault(bookIds)
            _statusMessage.value = if (bookIds.size == 1) {
                "Moved 1 book to Private Shelf"
            } else {
                "Moved ${bookIds.size} books to Private Shelf"
            }
        }
    }

    fun restoreFromVault(book: BookEntity) {
        viewModelScope.launch {
            repository.setBookHidden(book.id, false)
            _statusMessage.value = "Restored \"${book.title}\""
        }
    }

    fun dismissFirstTimeGreeting() {
        preferencesRepository.updateSettings { it.copy(hasDismissedFirstTimeGreeting = true) }
    }

    fun todayDateKey(): String = repository.todayDateKey()

    fun importBook(uri: Uri, intoHiddenVault: Boolean = false) {
        viewModelScope.launch {
            _isImporting.value = true
            val result = repository.importDocumentFromUri(uri, intoHiddenVault = intoHiddenVault)
            _isImporting.value = false
            result.onSuccess { book ->
                _statusMessage.value = if (intoHiddenVault) {
                    "Added to Private Shelf"
                } else {
                    "Added \"${book.title}\""
                }
            }.onFailure { err ->
                _statusMessage.value = err.message ?: "Could not import file"
            }
        }
    }

    fun renameBook(bookId: Long, newTitle: String, newAuthor: String) {
        viewModelScope.launch {
            repository.renameBook(bookId, newTitle, newAuthor)
            _statusMessage.value = "Updated volume details"
        }
    }

    fun updateBookCoverStyle(book: BookEntity, styleIndex: Int) {
        viewModelScope.launch {
            repository.updateBookCoverStyle(book, styleIndex)
            _statusMessage.value = "Cover updated"
        }
    }

    fun updateBookCoverFromUri(book: BookEntity, uri: Uri) {
        viewModelScope.launch {
            val res = repository.updateBookCoverFromUri(book, uri)
            _statusMessage.value = if (res.isSuccess) "Cover updated" else "Could not load image"
        }
    }

    fun updateBookShelf(bookId: Long, shelf: LibraryShelfFilter) {
        viewModelScope.launch {
            repository.updateShelfTag(bookId, shelf)
        }
    }

    fun toggleBookFavorite(book: BookEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id, book.isFavorite)
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            _statusMessage.value = if (book.isHidden) "Removed" else "Removed \"${book.title}\""
        }
    }

    fun deleteWordBankEntry(entryId: Long) {
        viewModelScope.launch {
            repository.deleteWordBankEntry(entryId)
        }
    }

    fun updateDailyGoal(target: Int, unit: DailyGoalUnit) {
        preferencesRepository.updateSettings {
            it.copy(dailyGoalTarget = target.coerceIn(5, 240), dailyGoalUnit = unit)
        }
    }

    fun setDefaultTargetLanguage(language: TranslationLanguage) {
        preferencesRepository.updateSettings { it.copy(defaultTargetLanguage = language) }
        _statusMessage.value = "Default language: ${language.label}"
    }

    fun setFullPageTranslateEnabled(enabled: Boolean) {
        preferencesRepository.updateSettings { it.copy(fullPageTranslateEnabled = enabled) }
    }

    fun setShelfGridColumns(columns: Int) {
        val clamped = columns.coerceIn(2, 4)
        preferencesRepository.updateSettings { it.copy(shelfGridColumns = clamped) }
    }

    fun updateGeminiApiKey(key: String) {
        preferencesRepository.updateSettings { it.copy(geminiApiKey = key.trim()) }
        _statusMessage.value = if (key.isBlank()) "Cleared custom Gemini key" else "Saved custom Gemini key"
    }

    fun setThemeMode(mode: ReaderThemeMode) {
        preferencesRepository.updateSettings { it.copy(themeMode = mode) }
    }

    fun cycleTheme() {
        preferencesRepository.updateSettings { current ->
            val next = when (current.themeMode) {
                ReaderThemeMode.PAPER -> ReaderThemeMode.SEPIA
                ReaderThemeMode.SEPIA -> ReaderThemeMode.NIGHT
                ReaderThemeMode.NIGHT -> ReaderThemeMode.PAPER
            }
            current.copy(themeMode = next)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    class Factory(
        private val repository: LibraryRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(repository, preferencesRepository) as T
        }
    }
}
