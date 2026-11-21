package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BookmarkEntity
import com.example.data.model.ChapterTocItem
import com.example.data.model.HighlightEntity
import com.example.data.model.SearchResult
import com.example.engine.export.QuoteCardAndExport
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.HighlightColors
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.theme.ReaderThemeMode

/**
 * Minimalist Frosted-Glass In-Book Search Drawer (works across EPUB, TXT, and PDF extracted text).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDrawerSheet(
    query: String,
    results: List<SearchResult>,
    onQueryChange: (String) -> Unit,
    onResultClick: (SearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.textMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_placeholder),
                        color = palette.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = palette.textMuted
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                                tint = palette.textMuted
                            )
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
                    focusedContainerColor = palette.background.copy(alpha = 0.7f),
                    unfocusedContainerColor = palette.background.copy(alpha = 0.55f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (query.trim().length >= 2 && results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted
                    )
                }
            } else {
                if (results.isNotEmpty()) {
                    Text(
                        text = "${results.size} match${if (results.size == 1) "" else "es"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(results) { item ->
                        Surface(
                            onClick = { onResultClick(item) },
                            shape = RoundedCornerShape(12.dp),
                            color = palette.background.copy(alpha = 0.65f),
                            border = BorderStroke(0.5.dp, palette.divider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.chapterTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.accent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Page ${item.pageIndex + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = buildAnnotatedString {
                                        append(item.snippetBefore)
                                        withStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = palette.text,
                                                background = palette.accent.copy(alpha = 0.22f)
                                            )
                                        ) {
                                            append(item.matchText)
                                        }
                                        append(item.snippetAfter)
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif
                                    ),
                                    color = palette.text.copy(alpha = 0.85f),
                                    maxLines = 3,
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

/**
 * Clean Slide-Up Notebook / Bookmarks / Table of Contents Drawer
 * with 1-tap Export .txt and per-highlight Quote Card sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookDrawerSheet(
    bookTitle: String,
    bookAuthor: String,
    currentPage: Int,
    highlights: List<HighlightEntity>,
    bookmarks: List<BookmarkEntity>,
    tocItems: List<ChapterTocItem>,
    onJumpToPage: (Int) -> Unit,
    onEditHighlightNote: (HighlightEntity) -> Unit,
    onShareQuoteCard: (String) -> Unit,
    onExportHighlightsTxt: () -> Unit,
    onDeleteHighlight: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.textMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.76f)
                .navigationBarsPadding()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = palette.surfaceGlass,
                contentColor = palette.text,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = palette.accent
                    )
                },
                divider = { HorizontalDivider(color = palette.divider, thickness = 0.5.dp) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.notebook_highlights)} (${highlights.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "${stringResource(R.string.notebook_bookmarks)} (${bookmarks.size})",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = stringResource(R.string.notebook_contents),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> HighlightsTabContent(
                    highlights = highlights,
                    onJumpToPage = onJumpToPage,
                    onEditNote = onEditHighlightNote,
                    onShareQuoteCard = onShareQuoteCard,
                    onExportTxt = onExportHighlightsTxt,
                    onDelete = onDeleteHighlight
                )
                1 -> BookmarksTabContent(
                    bookmarks = bookmarks,
                    onJumpToPage = onJumpToPage,
                    onDelete = onDeleteBookmark
                )
                2 -> ContentsTabContent(
                    currentPage = currentPage,
                    tocItems = tocItems,
                    onJumpToPage = onJumpToPage
                )
            }
        }
    }
}

@Composable
private fun HighlightsTabContent(
    highlights: List<HighlightEntity>,
    onJumpToPage: (Int) -> Unit,
    onEditNote: (HighlightEntity) -> Unit,
    onShareQuoteCard: (String) -> Unit,
    onExportTxt: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val palette = LocalFolioPalette.current
    if (highlights.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.notebook_empty_highlights),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1-Tap Export .txt Header Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                onClick = onExportTxt,
                shape = RoundedCornerShape(10.dp),
                color = palette.background.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.55f)),
                modifier = Modifier.testTag("export_highlights_txt_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = FolioIcons.Download,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Export .txt",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.text
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(highlights, key = { it.id }) { hl ->
                val hlColor = HighlightColors[hl.colorIndex.coerceIn(0, HighlightColors.lastIndex)]
                Surface(
                    onClick = { onJumpToPage(hl.pageIndex) },
                    shape = RoundedCornerShape(12.dp),
                    color = palette.background.copy(alpha = 0.7f),
                    border = BorderStroke(0.5.dp, palette.divider),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(52.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(hlColor)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "“${hl.quoteText}”",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Serif
                                ),
                                color = palette.text,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hl.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = hl.note,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = palette.accent
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page ${hl.pageIndex + 1}" + if (hl.chapterTitle.isNotBlank()) " · ${hl.chapterTitle}" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = { onShareQuoteCard(hl.quoteText) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = FolioIcons.ShareCard,
                                            contentDescription = "Share quote card",
                                            tint = palette.accent,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onEditNote(hl) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = FolioIcons.EditNote,
                                            contentDescription = "Edit note",
                                            tint = palette.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(hl.id) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = FolioIcons.Delete,
                                            contentDescription = "Delete highlight",
                                            tint = palette.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksTabContent(
    bookmarks: List<BookmarkEntity>,
    onJumpToPage: (Int) -> Unit,
    onDelete: (Long) -> Unit
) {
    val palette = LocalFolioPalette.current
    if (bookmarks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.notebook_empty_bookmarks),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(bookmarks, key = { it.id }) { bm ->
            Surface(
                onClick = { onJumpToPage(bm.pageIndex) },
                shape = RoundedCornerShape(12.dp),
                color = palette.background.copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FolioIcons.BookmarkFilled,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Page ${bm.pageIndex + 1} · ${bm.chapterTitle}",
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bm.excerpt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = bm.excerpt,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                                color = palette.textMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = { onDelete(bm.id) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = FolioIcons.Delete,
                            contentDescription = "Delete bookmark",
                            tint = palette.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentsTabContent(
    currentPage: Int,
    tocItems: List<ChapterTocItem>,
    onJumpToPage: (Int) -> Unit
) {
    val palette = LocalFolioPalette.current
    if (tocItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.notebook_empty_contents),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted
            )
        }
        return
    }

    val activeTocIndex = tocItems.indexOfLast { it.pageIndex <= currentPage }.coerceAtLeast(0)

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tocItems.indices.toList()) { idx ->
            val item = tocItems[idx]
            val isActive = idx == activeTocIndex
            Surface(
                onClick = { onJumpToPage(item.pageIndex) },
                shape = RoundedCornerShape(10.dp),
                color = if (isActive) palette.accent.copy(alpha = 0.12f) else palette.background.copy(alpha = 0.45f),
                border = BorderStroke(0.5.dp, if (isActive) palette.accent else palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isActive) palette.accent else palette.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "p. ${item.pageIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textMuted
                    )
                }
            }
        }
    }
}

/**
 * Aesthetic Quote Card Maker Dialog (Paper / Sepia / Off-Black preview + 1080x1350 Canvas PNG share).
 */
@Composable
fun QuoteCardDialog(
    quote: String,
    bookTitle: String,
    bookAuthor: String,
    initialTheme: ReaderThemeMode,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val context = LocalContext.current
    var cardTheme by remember { mutableStateOf(initialTheme) }

    val previewBitmap = remember(quote, bookTitle, bookAuthor, cardTheme) {
        QuoteCardAndExport.renderQuoteCardBitmap(
            quote = quote,
            bookTitle = bookTitle,
            bookAuthor = bookAuthor,
            themeMode = cardTheme,
            widthPx = 540,
            heightPx = 675
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        title = {
            Text(
                text = "Quote Card",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(0.75.dp, palette.divider),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Quote card preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3 Card Themes: Paper / Sepia / Off-Black
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        ReaderThemeMode.PAPER to "Paper",
                        ReaderThemeMode.SEPIA to "Sepia",
                        ReaderThemeMode.NIGHT to "Off-Black"
                    ).forEach { (mode, label) ->
                        val sel = cardTheme == mode
                        Surface(
                            onClick = { cardTheme = mode },
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) palette.accent.copy(alpha = 0.16f) else palette.background.copy(alpha = 0.6f),
                            border = BorderStroke(if (sel) 1.5.dp else 0.75.dp, if (sel) palette.accent else palette.divider),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (sel) palette.accent else palette.text
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hdBitmap = QuoteCardAndExport.renderQuoteCardBitmap(
                        quote = quote,
                        bookTitle = bookTitle,
                        bookAuthor = bookAuthor,
                        themeMode = cardTheme,
                        widthPx = 1080,
                        heightPx = 1350
                    )
                    QuoteCardAndExport.shareQuoteCardImage(context, hdBitmap, bookTitle)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.background
                ),
                modifier = Modifier.testTag("quote_card_share_button")
            ) {
                Icon(imageVector = FolioIcons.ShareCard, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel), color = palette.textMuted)
            }
        }
    )
}

/**
 * Clean Note Editor Dialog for annotating highlights.
 */
@Composable
fun HighlightNoteDialog(
    highlight: HighlightEntity,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    var noteText by remember(highlight.id) { mutableStateOf(highlight.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        title = {
            Text(
                text = "Margin Note",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = "“${highlight.quoteText}”",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic
                    ),
                    color = palette.textMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Write a reflection or margin note…", color = palette.textMuted) },
                    minLines = 3,
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        cursorColor = palette.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("highlight_note_input")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(noteText) },
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text(
                    text = stringResource(R.string.reader_save_note),
                    color = palette.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.library_cancel),
                    color = palette.textMuted
                )
            }
        }
    )
}

/**
 * Selectable Text & Highlight Dialog for resting PDF pages (long-press triggers this).
 */
@Composable
fun PdfHighlightDialog(
    extractedText: String,
    onSave: (excerpt: String, colorIndex: Int, note: String) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    var excerpt by remember(extractedText) { mutableStateOf(extractedText.take(260)) }
    var note by remember { mutableStateOf("") }
    var colorIndex by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        title = {
            Text(
                text = "Highlight PDF Passage",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = excerpt,
                    onValueChange = { excerpt = it },
                    label = { Text("Passage", color = palette.textMuted) },
                    minLines = 2,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Note", color = palette.textMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HighlightColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (colorIndex == idx) 2.dp else 0.75.dp,
                                    color = if (colorIndex == idx) palette.text else palette.divider,
                                    shape = CircleShape
                                )
                                .clickable { colorIndex = idx }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onCopy(excerpt) }) {
                        Text(stringResource(R.string.reader_copy), color = palette.textMuted)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(excerpt, colorIndex, note) }) {
                Text("Highlight", color = palette.accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.library_cancel), color = palette.textMuted)
            }
        }
    )
}
