package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.config.APP_NAME
import com.example.data.model.BookEntity
import com.example.data.model.LibraryShelfFilter
import com.example.data.model.TranslationEntryEntity
import com.example.ui.theme.CuratedCoverStyles
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.FolioLogoMark
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.viewmodel.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (Long) -> Unit
) {
    val visibleBooks by viewModel.visibleBooks.collectAsStateWithLifecycle()
    val filteredBooks by viewModel.filteredBooks.collectAsStateWithLifecycle()
    val continueReadingBook by viewModel.continueReadingBook.collectAsStateWithLifecycle()
    val selectedShelf by viewModel.selectedShelf.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val remoteConfig by viewModel.remoteConfig.collectAsStateWithLifecycle()
    val hasSecretCode by viewModel.hasSecretCode.collectAsStateWithLifecycle()
    val welcomeGuideBookId by viewModel.welcomeGuideBookId.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val wordBankEntries by viewModel.wordBankEntries.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsStateWithLifecycle()
    val vaultBooks by viewModel.vaultBooks.collectAsStateWithLifecycle()

    val palette = LocalFolioPalette.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 0 = Books, 1 = Word Bank
    var activeTab by remember { mutableIntStateOf(0) }
    var wordBankSearch by remember { mutableStateOf("") }

    // Modal & menu visibility state
    var isOverflowMenuOpen by remember { mutableStateOf(false) }
    var showSettingsAboutSheet by remember { mutableStateOf(false) }
    var showDictionarySheet by remember { mutableStateOf(false) }
    var showHeatmapSheet by remember { mutableStateOf(false) }
    var showFlashcardModal by remember { mutableStateOf(false) }
    var managingBook by remember { mutableStateOf<BookEntity?>(null) }
    var renamingBook by remember { mutableStateOf<BookEntity?>(null) }
    var coverPickerBook by remember { mutableStateOf<BookEntity?>(null) }
    var pendingCoverBook by remember { mutableStateOf<BookEntity?>(null) }
    var importIntoVault by remember { mutableStateOf(false) }

    val showFirstTimeGreeting = !settings.hasDismissedFirstTimeGreeting
    val todayKey = remember { viewModel.todayDateKey() }

    val anyOverlayOpen = isOverflowMenuOpen ||
        isSearchExpanded ||
        showSettingsAboutSheet ||
        showDictionarySheet ||
        showHeatmapSheet ||
        showFlashcardModal ||
        isVaultUnlocked ||
        managingBook != null ||
        renamingBook != null ||
        coverPickerBook != null ||
        showFirstTimeGreeting

    // Smoothly hide FAB whenever any modal, sheet, dropdown, search bar, greeting card, or Word Bank is open
    val showFab = activeTab == 0 && !anyOverlayOpen

    BackHandler(enabled = isSearchExpanded || isVaultUnlocked || showFlashcardModal) {
        when {
            showFlashcardModal -> showFlashcardModal = false
            isVaultUnlocked -> viewModel.lockVault()
            isSearchExpanded -> viewModel.setSearchExpanded(false)
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Zero-permission Android Storage Access Framework document picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBook(uri, intoHiddenVault = importIntoVault)
        }
        importIntoVault = false
    }

    // Zero-permission Android Photo Picker for Custom Book Covers (Google Play Policy compliant)
    val coverPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = pendingCoverBook
        pendingCoverBook = null
        if (uri != null && target != null) {
            viewModel.updateBookCoverFromUri(target, uri)
        }
    }

    fun launchDocumentPicker(intoVault: Boolean) {
        importIntoVault = intoVault
        importLauncher.launch(
            arrayOf(
                "application/epub+zip",
                "application/pdf",
                "text/plain",
                "application/octet-stream",
                "*/*"
            )
        )
    }

    Scaffold(
        containerColor = palette.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFab,
                enter = fadeIn() + scaleIn(initialScale = 0.85f),
                exit = fadeOut() + scaleOut(targetScale = 0.85f)
            ) {
                FloatingActionButton(
                    onClick = { launchDocumentPicker(intoVault = false) },
                    shape = CircleShape,
                    containerColor = palette.text,
                    contentColor = palette.background,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp, end = 4.dp)
                        .size(56.dp)
                        .testTag("import_book_fab")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.library_import_book),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(palette.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Minimalist Header: Left = Logo + Epos; Right = ONLY Search + 3-Dot Menu
                AnimatedContent(
                    targetState = isSearchExpanded,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "library_header_search_transition"
                ) { expanded ->
                    if (expanded) {
                        ExpandableHeaderSearchBar(
                            query = searchQuery,
                            onQueryChange = { viewModel.onLibrarySearchQueryChange(it) },
                            onClose = { viewModel.setSearchExpanded(false) }
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 10.dp, top = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(11.dp),
                                modifier = Modifier.testTag("library_header_brand")
                            ) {
                                FolioLogoMark(size = 34.dp)
                                Column {
                                    Text(
                                        text = APP_NAME,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Normal,
                                            letterSpacing = (-0.4).sp
                                        ),
                                        color = palette.text
                                    )
                                    val countLabel = if (visibleBooks.size == 1) {
                                        stringResource(R.string.library_subtitle_single)
                                    } else {
                                        stringResource(R.string.library_subtitle_plural, visibleBooks.size)
                                    }
                                    Text(
                                        text = "$countLabel · Offline",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted
                                    )
                                }
                            }

                            // ONLY Search (🔍) + 3-Dot Menu (⋮)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        activeTab = 0
                                        viewModel.setSearchExpanded(true)
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("library_search_icon_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search library",
                                        tint = palette.text,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Box {
                                    IconButton(
                                        onClick = { isOverflowMenuOpen = true },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("library_overflow_menu_button")
                                    ) {
                                        Icon(
                                            imageVector = FolioIcons.MoreVert,
                                            contentDescription = "Library menu",
                                            tint = palette.text,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isOverflowMenuOpen,
                                        onDismissRequest = { isOverflowMenuOpen = false },
                                        containerColor = palette.surfaceGlass,
                                        border = BorderStroke(0.75.dp, palette.divider),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        text = "Dictionary & Language",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = palette.text
                                                    )
                                                    Text(
                                                        text = "Default: ${settings.defaultTargetLanguage.label}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.textMuted
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = FolioIcons.Translate,
                                                    contentDescription = null,
                                                    tint = palette.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                isOverflowMenuOpen = false
                                                showDictionarySheet = true
                                            },
                                            modifier = Modifier.testTag("menu_dictionary_language")
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Reading Stats & Goal",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.text
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = FolioIcons.OpenBook,
                                                    contentDescription = null,
                                                    tint = palette.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                isOverflowMenuOpen = false
                                                showHeatmapSheet = true
                                            },
                                            modifier = Modifier.testTag("menu_reading_stats")
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Settings & About",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.text
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = FolioIcons.Info,
                                                    contentDescription = null,
                                                    tint = palette.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                isOverflowMenuOpen = false
                                                showSettingsAboutSheet = true
                                            },
                                            modifier = Modifier.testTag("menu_settings_about")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Clean Books | Word Bank Segmented Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryModeTabPill(
                        label = "Books",
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_tab_books")
                    )
                    LibraryModeTabPill(
                        label = if (wordBankEntries.isEmpty()) "Word Bank" else "Word Bank (${wordBankEntries.size})",
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_tab_wordbank")
                    )
                }

                if (activeTab == 0) {
                    // Horizontal Shelf Filter Chips (All, Reading, Completed, Study, Favorites)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_shelf_chips")
                    ) {
                        items(LibraryShelfFilter.entries) { shelf ->
                            val selected = selectedShelf == shelf
                            Surface(
                                onClick = { viewModel.selectShelf(shelf) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) palette.text else palette.surface,
                                border = BorderStroke(
                                    width = 0.75.dp,
                                    color = if (selected) palette.text else palette.divider
                                ),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("shelf_chip_${shelf.name.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = shelf.label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (selected) palette.background else palette.textMuted
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = palette.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    if (filteredBooks.isEmpty() && !isImporting) {
                        EmptyLibraryState(
                            shelf = selectedShelf,
                            hasActiveSearch = searchQuery.isNotBlank(),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        )
                    } else {
                        val gridCols = settings.shelfGridColumns.coerceIn(2, 4)
                        val hSpace = when (gridCols) {
                            4 -> 10.dp
                            3 -> 13.dp
                            else -> 18.dp
                        }
                        val vSpace = when (gridCols) {
                            4 -> 16.dp
                            3 -> 20.dp
                            else -> 24.dp
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridCols),
                            contentPadding = PaddingValues(
                                start = 20.dp,
                                end = 20.dp,
                                top = 18.dp,
                                bottom = 132.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(hSpace),
                            verticalArrangement = Arrangement.spacedBy(vSpace),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("library_bookshelf_grid")
                        ) {
                            items(filteredBooks, key = { it.id }) { book ->
                                BookShelfCard(
                                    book = book,
                                    gridColumns = gridCols,
                                    onClick = { onOpenBook(book.id) },
                                    onLongClick = { managingBook = book }
                                )
                            }
                        }
                    }
                } else {
                    // Saved Offline Multi-Language Word Bank + Flashcard Practice
                    WordBankTabContent(
                        entries = wordBankEntries,
                        searchQuery = wordBankSearch,
                        onSearchChange = { wordBankSearch = it },
                        onOpenPractice = { showFlashcardModal = true },
                        onCopy = { entry ->
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Epos Word Bank",
                                    "${entry.sourceText} — ${entry.translatedText}"
                                )
                            )
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Copied translation")
                            }
                        },
                        onDelete = { entryId -> viewModel.deleteWordBankEntry(entryId) }
                    )
                }
            }

            // 1-Tap Continue Reading Slim Frosted-Glass Pill (visible non-hidden books only)
            AnimatedVisibility(
                visible = activeTab == 0 && continueReadingBook != null && !anyOverlayOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 92.dp, bottom = 22.dp)
            ) {
                continueReadingBook?.let { recent ->
                    ContinueReadingPill(
                        book = recent,
                        onClick = { onOpenBook(recent.id) }
                    )
                }
            }

            // Subtle Importing Indicator
            AnimatedVisibility(
                visible = isImporting,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceGlass,
                    tonalElevation = 6.dp,
                    border = BorderStroke(1.dp, palette.divider)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = palette.accent
                        )
                        Text(
                            text = "Preparing volume…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.text
                        )
                    }
                }
            }

            // Refined Ultra-Minimalist First-Time Greeting Card (shown once on first launch)
            AnimatedVisibility(
                visible = showFirstTimeGreeting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FirstTimeGreetingCardOverlay(
                    onOpenGuide = {
                        viewModel.dismissFirstTimeGreeting()
                        val guideId = welcomeGuideBookId ?: visibleBooks.firstOrNull()?.id
                        if (guideId != null) {
                            onOpenBook(guideId)
                        }
                    },
                    onSkipToLibrary = { viewModel.dismissFirstTimeGreeting() }
                )
            }
            // Full-Screen Zero-Trace Private Shelf (replaces bottom drawer; auto-locks on Back or ON_STOP)
            AnimatedVisibility(
                visible = isVaultUnlocked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PrivateShelfScreen(
                    vaultBooks = vaultBooks,
                    visibleLibraryBooks = visibleBooks,
                    gridColumns = settings.shelfGridColumns,
                    onOpenBook = { bookId -> onOpenBook(bookId) },
                    onImportToVault = { launchDocumentPicker(intoVault = true) },
                    onMoveBooksToVault = { ids -> viewModel.moveLibraryBooksToVault(ids) },
                    onRenameBookClick = { target -> renamingBook = target },
                    onChangeCoverClick = { target -> coverPickerBook = target },
                    onRestoreBook = { book -> viewModel.restoreFromVault(book) },
                    onDeleteBook = { book -> viewModel.deleteBook(book) },
                    onLockVault = { viewModel.lockVault() }
                )
            }
        }
    }

    // 1. Settings & About Sheet (Remote GitHub Config, Update Pill, Theme, Grid Size, Verified Secret Code, Full-Page Translate, Gemini Key)
    if (showSettingsAboutSheet) {
        SettingsAndAboutSheet(
            settings = settings,
            remoteConfig = remoteConfig,
            hasSecretCode = hasSecretCode,
            onSelectTheme = { mode -> viewModel.setThemeMode(mode) },
            onSelectGridColumns = { cols -> viewModel.setShelfGridColumns(cols) },
            onToggleFullPageTranslate = { enabled -> viewModel.setFullPageTranslateEnabled(enabled) },
            onCreateSecretCode = { newCode, confirm -> viewModel.createSecretShelfCode(newCode, confirm) },
            onChangeSecretCode = { current, newCode, confirm -> viewModel.changeSecretShelfCode(current, newCode, confirm) },
            onRemoveSecretCode = { current -> viewModel.removeSecretShelfCode(current) },
            onSaveGeminiKey = { key -> viewModel.updateGeminiApiKey(key) },
            onDismiss = { showSettingsAboutSheet = false }
        )
    }

    // 2. Dictionary & Language Sheet (15 Languages including Arabic RTL + 1-Tap Full-Page Translate + Word Bank Shortcut)
    if (showDictionarySheet) {
        DictionaryAndLanguageSheet(
            currentLanguage = settings.defaultTargetLanguage,
            fullPageTranslateEnabled = settings.fullPageTranslateEnabled,
            wordBankCount = wordBankEntries.size,
            onSelectLanguage = { lang -> viewModel.setDefaultTargetLanguage(lang) },
            onToggleFullPageTranslate = { enabled -> viewModel.setFullPageTranslateEnabled(enabled) },
            onOpenWordBank = { activeTab = 1 },
            onDismiss = { showDictionarySheet = false }
        )
    }

    // 3. Daily Reading Goal & 10-Week Calendar Heatmap Sheet
    if (showHeatmapSheet) {
        ReadingCalendarHeatmapSheet(
            activities = recentActivity,
            todayDateKey = todayKey,
            settings = settings,
            onUpdateGoal = { target, unit -> viewModel.updateDailyGoal(target, unit) },
            onDismiss = { showHeatmapSheet = false }
        )
    }

    // 5. Long-Press Book Management Sheet (zero mention of Private Shelf; includes Rename & Change Cover)
    managingBook?.let { target ->
        BookManageSheet(
            book = target,
            onAssignShelf = { shelf -> viewModel.updateBookShelf(target.id, shelf) },
            onToggleFavorite = { viewModel.toggleBookFavorite(target) },
            onRenameClick = { renamingBook = target },
            onChangeCoverClick = { coverPickerBook = target },
            onDelete = { viewModel.deleteBook(target) },
            onDismiss = { managingBook = null }
        )
    }

    // 6. Rename Book Dialog
    renamingBook?.let { target ->
        RenameBookDialog(
            book = target,
            onSave = { newTitle, newAuthor -> viewModel.renameBook(target.id, newTitle, newAuthor) },
            onDismiss = { renamingBook = null }
        )
    }

    // 7. Custom Book Cover Picker Sheet (Android Photo Picker + 4 minimalist typography covers)
    coverPickerBook?.let { target ->
        ChangeCoverSheet(
            book = target,
            onPickImageFromDevice = {
                pendingCoverBook = target
                coverPhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSelectTypographicStyle = { idx -> viewModel.updateBookCoverStyle(target, idx) },
            onDismiss = { coverPickerBook = null }
        )
    }

    // 8. Word Bank Flashcard Practice Modal
    if (showFlashcardModal && wordBankEntries.isNotEmpty()) {
        WordBankFlashcardDialog(
            entries = wordBankEntries,
            onDismiss = { showFlashcardModal = false }
        )
    }
}

@Composable
private fun ExpandableHeaderSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.55f)),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search books or authors…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textMuted
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = palette.text),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("library_header_search_input")
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("library_header_search_close")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close search",
                        tint = palette.textMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingPill(
    book: BookEntity,
    onClick: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val total = book.totalPages.coerceAtLeast(1)
    val pageNum = (book.currentPage + 1).coerceIn(1, total)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = palette.surfaceGlass,
        border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("continue_reading_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FolioIcons.OpenBook,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium
                    ),
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Continue · Page $pageNum of $total",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textMuted,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = FolioIcons.Play,
                contentDescription = "Resume reading",
                tint = palette.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun LibraryModeTabPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) palette.accent.copy(alpha = 0.14f) else palette.surface,
        border = BorderStroke(
            width = if (selected) 1.2.dp else 0.75.dp,
            color = if (selected) palette.accent else palette.divider
        ),
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (selected) palette.accent else palette.textMuted
            )
        }
    }
}

@Composable
private fun WordBankTabContent(
    entries: List<TranslationEntryEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onOpenPractice: () -> Unit,
    onCopy: (TranslationEntryEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    val palette = LocalFolioPalette.current
    val filtered = remember(entries, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) entries
        else entries.filter {
            it.sourceText.lowercase().contains(q) ||
                it.translatedText.lowercase().contains(q) ||
                it.bookTitle.lowercase().contains(q) ||
                it.targetLanguageEnum.label.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search saved words…", color = palette.textMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = palette.textMuted)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Outlined.Close, contentDescription = "Clear", tint = palette.textMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = palette.accent,
                    unfocusedBorderColor = palette.divider,
                    focusedTextColor = palette.text,
                    unfocusedTextColor = palette.text,
                    cursorColor = palette.accent,
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("wordbank_search_input")
            )

            if (entries.isNotEmpty()) {
                Button(
                    onClick = onOpenPractice,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.text,
                        contentColor = palette.background
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("wordbank_practice_button")
                ) {
                    Icon(
                        imageVector = FolioIcons.Flashcards,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Practice", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = FolioIcons.Translate,
                        contentDescription = null,
                        tint = palette.textMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (entries.isEmpty()) "Your Word Bank is empty" else "No matching entries",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Long-press any word while reading, tap Translate, and save it to study offline with flashcards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { entry ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surface,
                        border = BorderStroke(0.75.dp, palette.divider),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = palette.accent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "→ ${entry.targetLanguageEnum.label}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = palette.accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { onCopy(entry) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = FolioIcons.Copy,
                                            contentDescription = "Copy word",
                                            tint = palette.textMuted,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(entry.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = FolioIcons.Delete,
                                            contentDescription = "Remove word",
                                            tint = palette.textMuted,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.sourceText,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                                color = palette.text
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.translatedText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.accent
                            )
                            if (entry.bookTitle.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "From ${entry.bookTitle}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                    color = palette.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookShelfCard(
    book: BookEntity,
    gridColumns: Int = 2,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val coverStyle = CuratedCoverStyles[book.coverStyleIndex.coerceIn(0, CuratedCoverStyles.lastIndex)]
    val isCompact = gridColumns >= 3
    val isUltraCompact = gridColumns >= 4

    val coverBitmap by produceState<ImageBitmap?>(initialValue = null, book.coverImagePath) {
        value = withContext(Dispatchers.IO) {
            book.coverImagePath?.let { path ->
                val f = File(path)
                if (f.exists()) {
                    runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
                } else null
            }
        }
    }

    val progress = book.progressFraction
    val progressPct = (progress * 100f).roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("book_card_${book.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .shadow(
                    elevation = if (palette.isDark) 4.dp else 7.dp,
                    shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 9.dp, bottomEnd = 9.dp),
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 9.dp, bottomEnd = 9.dp))
                .background(coverStyle.background)
        ) {
            if (coverBitmap != null) {
                Image(
                    bitmap = coverBitmap!!,
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(if (isCompact) 8.dp else 12.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.34f),
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            } else {
                // Curated Procedural Typographic Cloth Cover (0 KB assets)
                val spineW = when {
                    isUltraCompact -> 7.dp
                    isCompact -> 10.dp
                    else -> 14.dp
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(spineW)
                            .background(coverStyle.spine)
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = spineW)
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.16f)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = spineW + if (isUltraCompact) 4.dp else 8.dp,
                                top = if (isUltraCompact) 6.dp else 10.dp,
                                end = if (isUltraCompact) 5.dp else 9.dp,
                                bottom = if (isUltraCompact) 6.dp else 10.dp
                            )
                            .border(
                                width = 0.75.dp,
                                color = coverStyle.accent.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(3.dp)
                            )
                            .padding(
                                horizontal = if (isUltraCompact) 5.dp else if (isCompact) 7.dp else 10.dp,
                                vertical = if (isUltraCompact) 6.dp else if (isCompact) 9.dp else 14.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(if (isUltraCompact) 4.dp else if (isCompact) 8.dp else 14.dp))
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = when {
                                            isUltraCompact -> 10.sp
                                            isCompact -> 12.sp
                                            else -> 15.5.sp
                                        },
                                        lineHeight = when {
                                            isUltraCompact -> 12.5.sp
                                            isCompact -> 15.sp
                                            else -> 20.sp
                                        }
                                    ),
                                    color = coverStyle.text,
                                    maxLines = if (isUltraCompact) 3 else 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!isUltraCompact) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(if (isCompact) 16.dp else 24.dp)
                                            .height(1.dp)
                                            .background(coverStyle.accent.copy(alpha = 0.75f))
                                    )
                                }
                            }

                            if (!isUltraCompact) {
                                Text(
                                    text = book.author,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = if (isCompact) 9.5.sp else 11.sp
                                    ),
                                    color = coverStyle.text.copy(alpha = 0.74f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Crisp Format Badge (EPUB / PDF / TXT)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xCC181715),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(if (isUltraCompact) 4.dp else 7.dp)
            ) {
                Text(
                    text = book.bookFormat.badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isUltraCompact) 7.5.sp else 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp
                    ),
                    color = Color(0xFFF3EFE6),
                    modifier = Modifier.padding(
                        horizontal = if (isUltraCompact) 4.dp else 6.dp,
                        vertical = if (isUltraCompact) 1.5.dp else 2.5.dp
                    )
                )
            }

            // Favorite Star Badge (bottom-right)
            if (book.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(if (isUltraCompact) 4.dp else 7.dp)
                        .size(if (isUltraCompact) 18.dp else 22.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC181715)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FolioIcons.StarFilled,
                        contentDescription = "Favorite",
                        tint = Color(0xFFD9AE5F),
                        modifier = Modifier.size(if (isUltraCompact) 11.dp else 13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isUltraCompact) 6.dp else 9.dp))

        Text(
            text = book.title,
            style = if (isCompact) {
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (isUltraCompact) 11.5.sp else 13.sp
                )
            } else {
                MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                )
            },
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!isUltraCompact) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isCompact) 10.5.sp else 12.sp
                ),
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(if (isUltraCompact) 4.dp else 7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.divider)
            ) {
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceAtLeast(0.04f))
                            .fillMaxHeight()
                            .background(palette.accent)
                    )
                }
            }
            Text(
                text = if (progressPct == 0) "New" else "$progressPct%",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (isUltraCompact) 9.5.sp else 11.sp
                ),
                color = palette.textMuted
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(
    shelf: LibraryShelfFilter,
    hasActiveSearch: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = FolioIcons.OpenBook,
            contentDescription = null,
            tint = palette.textMuted.copy(alpha = 0.6f),
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = when {
                hasActiveSearch -> "No matching volumes"
                shelf == LibraryShelfFilter.ALL -> stringResource(R.string.library_empty_title)
                else -> "No books in ${shelf.label}"
            },
            style = MaterialTheme.typography.titleMedium,
            color = palette.text
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                hasActiveSearch -> "Try another title or author."
                shelf == LibraryShelfFilter.ALL -> stringResource(R.string.library_empty_body)
                else -> "Long-press any book cover to organize it onto this shelf."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textMuted
        )
    }
}
