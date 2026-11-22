package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BookEntity
import com.example.data.model.LibraryShelfFilter
import com.example.data.model.TranslationEntryEntity
import com.example.ui.theme.CuratedCoverStyles
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.LocalFolioPalette

/**
 * Full-Screen Zero-Trace Private Shelf matching the exact visual polish of the Main Library.
 * - Full bookshelf grid using BookShelfCard (respecting user's shelfGridColumns: 2, 3, or 4)
 * - Subtle Lock icon + "Private Shelf" header badge
 * - `+` menu: (1) Import Directly, (2) Move from Library checklist
 * - 1-tap Lock & Exit button + automatic lock when app minimizes (ON_STOP)
 * - Long-press any book cover for Rename, Change Cover, Restore to Library, or Delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateShelfScreen(
    vaultBooks: List<BookEntity>,
    visibleLibraryBooks: List<BookEntity>,
    gridColumns: Int,
    onOpenBook: (Long) -> Unit,
    onImportToVault: () -> Unit,
    onMoveBooksToVault: (Set<Long>) -> Unit,
    onRenameBookClick: (BookEntity) -> Unit,
    onChangeCoverClick: (BookEntity) -> Unit,
    onRestoreBook: (BookEntity) -> Unit,
    onDeleteBook: (BookEntity) -> Unit,
    onLockVault: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showAddMenu by remember { mutableStateOf(false) }
    var showMoveChecklist by remember { mutableStateOf(false) }
    var selectedToMove by remember { mutableStateOf(emptySet<Long>()) }
    var managingPrivateBook by remember { mutableStateOf<BookEntity?>(null) }

    // Immediately auto-lock Private Shelf when the app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                onLockVault()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val safeCols = gridColumns.coerceIn(2, 4)
    val hSpacing = when (safeCols) {
        4 -> 10.dp
        3 -> 13.dp
        else -> 18.dp
    }
    val vSpacing = when (safeCols) {
        4 -> 16.dp
        3 -> 20.dp
        else -> 24.dp
    }

    Surface(
        color = palette.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("private_shelf_fullscreen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Clean Minimalist Private Shelf Header (Badge on left, Lock & Exit on right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.accent.copy(alpha = 0.14f),
                            border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = FolioIcons.Lock,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Private Shelf",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = palette.text
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (vaultBooks.size == 1) "1 hidden volume · Zero-trace"
                            else "${vaultBooks.size} hidden volumes · Zero-trace",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textMuted
                        )
                    }

                    OutlinedButton(
                        onClick = onLockVault,
                        border = BorderStroke(0.85.dp, palette.accent),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(42.dp)
                            .testTag("vault_lock_button")
                    ) {
                        Icon(
                            imageVector = FolioIcons.Lock,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Lock & Exit",
                            color = palette.accent,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }
                }

            HorizontalDivider(
                color = palette.divider,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (showMoveChecklist) {
                // Full-screen Move from Library checklist view
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Move from Library",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                            color = palette.text
                        )
                        TextButton(onClick = { showMoveChecklist = false }) {
                            Text("Cancel", color = palette.textMuted)
                        }
                    }

                    if (visibleLibraryBooks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No visible books available in your Library.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(visibleLibraryBooks, key = { it.id }) { book ->
                                val checked = book.id in selectedToMove
                                Surface(
                                    onClick = {
                                        selectedToMove = if (checked) selectedToMove - book.id else selectedToMove + book.id
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (checked) palette.accent.copy(alpha = 0.14f) else palette.surface,
                                    border = BorderStroke(
                                        width = if (checked) 1.2.dp else 0.75.dp,
                                        color = if (checked) palette.accent else palette.divider
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = checked,
                                            onCheckedChange = { on ->
                                                selectedToMove = if (on) selectedToMove + book.id else selectedToMove - book.id
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = palette.accent,
                                                uncheckedColor = palette.textMuted,
                                                checkmarkColor = palette.background
                                            )
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = book.title,
                                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Serif),
                                                color = palette.text,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${book.author} · ${book.bookFormat.badge}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onMoveBooksToVault(selectedToMove)
                                selectedToMove = emptySet()
                                showMoveChecklist = false
                            },
                            enabled = selectedToMove.isNotEmpty(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = palette.background
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("vault_confirm_move_button")
                        ) {
                            Text(
                                text = if (selectedToMove.isEmpty()) "Select books to move"
                                else "Move ${selectedToMove.size} to Private Shelf"
                            )
                        }
                    }
                }
            } else if (vaultBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = FolioIcons.Lock,
                            contentDescription = null,
                            tint = palette.textMuted.copy(alpha = 0.55f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Private Shelf is empty",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                            color = palette.text
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + below to import directly or move volumes from your Library. Long-press any book cover here to manage or restore it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Full-screen Bookshelf Grid identical to Main Library
                LazyVerticalGrid(
                    columns = GridCells.Fixed(safeCols),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 18.dp,
                        bottom = 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(hSpacing),
                    verticalArrangement = Arrangement.spacedBy(vSpacing),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("private_shelf_bookshelf_grid")
                ) {
                    items(vaultBooks, key = { it.id }) { book ->
                        BookShelfCard(
                            book = book,
                            gridColumns = safeCols,
                            onClick = { onOpenBook(book.id) },
                            onLongClick = { managingPrivateBook = book }
                        )
                    }
                }
            }
            }

            // Bottom-Right Circular `+` FAB matching Main Library (Import Directly / Move from Library)
            if (!showMoveChecklist) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 20.dp, bottom = 20.dp)
                ) {
                    FloatingActionButton(
                        onClick = { showAddMenu = true },
                        shape = CircleShape,
                        containerColor = palette.text,
                        contentColor = palette.background,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("vault_add_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add to Private Shelf",
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        containerColor = palette.surfaceGlass,
                        border = BorderStroke(0.75.dp, palette.divider),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "Import Directly",
                                        color = palette.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Pick EPUB, PDF, or TXT into Private Shelf",
                                        color = palette.textMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            onClick = {
                                showAddMenu = false
                                onImportToVault()
                            },
                            modifier = Modifier.testTag("vault_import_direct_option")
                        )
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "Move from Library",
                                        color = palette.text,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Select books from your main shelf to hide",
                                        color = palette.textMuted,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            },
                            onClick = {
                                showAddMenu = false
                                selectedToMove = emptySet()
                                showMoveChecklist = true
                            },
                            modifier = Modifier.testTag("vault_move_from_library_option")
                        )
                    }
                }
            }
        }
    }

    // Long-press Management Sheet for Private Shelf books
    managingPrivateBook?.let { target ->
        PrivateBookManageSheet(
            book = target,
            onRenameClick = { onRenameBookClick(target) },
            onChangeCoverClick = { onChangeCoverClick(target) },
            onRestoreToLibrary = { onRestoreBook(target) },
            onDelete = { onDeleteBook(target) },
            onDismiss = { managingPrivateBook = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivateBookManageSheet(
    book: BookEntity,
    onRenameClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onRestoreToLibrary: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${book.author} · Private Shelf",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = palette.textMuted
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            ManageActionRow(
                icon = FolioIcons.Undo,
                label = "Restore to Main Library",
                tint = palette.accent,
                onClick = {
                    onRestoreToLibrary()
                    onDismiss()
                },
                testTag = "private_manage_restore_row"
            )
            ManageActionRow(
                icon = FolioIcons.EditNote,
                label = "Rename",
                tint = palette.text,
                onClick = {
                    onDismiss()
                    onRenameClick()
                }
            )
            ManageActionRow(
                icon = FolioIcons.ImageCover,
                label = "Change Cover",
                tint = palette.text,
                onClick = {
                    onDismiss()
                    onChangeCoverClick()
                }
            )
            ManageActionRow(
                icon = FolioIcons.Delete,
                label = "Delete Volume",
                tint = Color(0xFFD14B4B),
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Long-Press Management Sheet for visible Library books.
 * Strictly zero mention of Private Shelf or Hide.
 * Includes: Shelf selector, Favorite toggle, Rename, Change Cover, and Remove.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManageSheet(
    book: BookEntity,
    onAssignShelf: (LibraryShelfFilter) -> Unit,
    onToggleFavorite: () -> Unit,
    onRenameClick: () -> Unit,
    onChangeCoverClick: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                color = palette.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                color = palette.textMuted
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SHELF",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = palette.textMuted
            )
            Spacer(modifier = Modifier.height(8.dp))

            val shelves = listOf(
                LibraryShelfFilter.READING,
                LibraryShelfFilter.COMPLETED,
                LibraryShelfFilter.STUDY
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shelves.forEach { shelf ->
                    val selected = book.shelfTag == shelf.name
                    Surface(
                        onClick = {
                            onAssignShelf(shelf)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.background.copy(alpha = 0.55f),
                        border = BorderStroke(
                            if (selected) 1.5.dp else 0.75.dp,
                            if (selected) palette.accent else palette.divider
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = shelf.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) palette.accent else palette.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)

            ManageActionRow(
                icon = FolioIcons.StarFilled,
                label = if (book.isFavorite) "Remove from Favorites" else "Add to Favorites",
                tint = if (book.isFavorite) Color(0xFFC59B56) else palette.text,
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                }
            )
            ManageActionRow(
                icon = FolioIcons.EditNote,
                label = "Rename",
                tint = palette.text,
                onClick = {
                    onDismiss()
                    onRenameClick()
                },
                testTag = "manage_rename_book_row"
            )
            ManageActionRow(
                icon = FolioIcons.ImageCover,
                label = "Change Cover",
                tint = palette.text,
                onClick = {
                    onDismiss()
                    onChangeCoverClick()
                },
                testTag = "manage_change_cover_row"
            )
            ManageActionRow(
                icon = FolioIcons.Delete,
                label = "Remove from Library",
                tint = Color(0xFFD14B4B),
                onClick = {
                    onDelete()
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ManageActionRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint
        )
    }
}

/**
 * Clean Rename Book Dialog (Title & Author).
 */
@Composable
fun RenameBookDialog(
    book: BookEntity,
    onSave: (title: String, author: String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    var titleDraft by remember(book.id) { mutableStateOf(book.title) }
    var authorDraft by remember(book.id) { mutableStateOf(book.author) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surface,
        titleContentColor = palette.text,
        textContentColor = palette.text,
        title = {
            Text(
                text = "Rename Book",
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    label = { Text("Title", color = palette.textMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        cursorColor = palette.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_book_title_input")
                )
                OutlinedTextField(
                    value = authorDraft,
                    onValueChange = { authorDraft = it },
                    label = { Text("Author", color = palette.textMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accent,
                        unfocusedBorderColor = palette.divider,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        cursorColor = palette.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_book_author_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleDraft.isNotBlank()) {
                        onSave(titleDraft.trim(), authorDraft.trim())
                        onDismiss()
                    }
                },
                enabled = titleDraft.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accent,
                    contentColor = palette.background
                ),
                modifier = Modifier.testTag("rename_book_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.textMuted)
            }
        }
    )
}

/**
 * Custom Book Cover Picker Sheet:
 * - Pick image from device (via zero-permission Android Photo Picker)
 * - Or choose from 4 minimalist solid-color typography covers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCoverSheet(
    book: BookEntity,
    onPickImageFromDevice: () -> Unit,
    onSelectTypographicStyle: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fourMinimalCovers = remember {
        listOf(
            0 to "Pine",
            1 to "Oxblood",
            2 to "Indigo",
            4 to "Graphite"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Change Cover",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = palette.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = book.title,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onDismiss()
                    onPickImageFromDevice()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.text,
                    contentColor = palette.background
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("cover_pick_image_button")
            ) {
                Icon(
                    imageVector = FolioIcons.ImageCover,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Image from Device")
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "MINIMALIST TYPOGRAPHY COVERS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = palette.textMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                fourMinimalCovers.forEach { (styleIdx, label) ->
                    val style = CuratedCoverStyles[styleIdx]
                    val isSelected = book.coverImagePath == null && book.coverStyleIndex == styleIdx
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectTypographicStyle(styleIdx)
                                onDismiss()
                            }
                            .testTag("cover_style_swatch_$styleIdx")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                                .clip(RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp))
                                .background(style.background)
                                .border(
                                    width = if (isSelected) 2.dp else 0.75.dp,
                                    color = if (isSelected) palette.accent else style.accent.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(8.dp)
                                    .background(style.spine)
                            )
                            Text(
                                text = book.title.take(12),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                ),
                                color = style.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 13.dp, top = 10.dp, end = 6.dp)
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected",
                                        tint = palette.background,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) palette.accent else palette.textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Minimalist Flashcard Practice Modal inside Word Bank:
 * Front: saved word + language pill; Tap to flip -> Back: meaning + book title.
 * Includes Previous / Shuffle / Next controls.
 */
@Composable
fun WordBankFlashcardDialog(
    entries: List<TranslationEntryEntity>,
    onDismiss: () -> Unit
) {
    if (entries.isEmpty()) {
        onDismiss()
        return
    }
    val palette = LocalFolioPalette.current
    var deck by remember(entries) { mutableStateOf(entries) }
    var currentIndex by remember(deck) { mutableIntStateOf(0) }
    var isFlipped by remember(currentIndex, deck) { mutableStateOf(false) }

    val safeIdx = currentIndex.coerceIn(0, deck.lastIndex)
    val card = deck[safeIdx]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.surfaceGlass,
            border = BorderStroke(1.dp, palette.divider),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .testTag("wordbank_flashcard_modal")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Flashcard Practice",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif),
                            color = palette.text
                        )
                        Text(
                            text = "${safeIdx + 1} of ${deck.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accent
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("flashcard_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close flashcards",
                            tint = palette.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Tap-to-Flip Flashcard
                Surface(
                    onClick = { isFlipped = !isFlipped },
                    shape = RoundedCornerShape(20.dp),
                    color = palette.background,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isFlipped) palette.accent.copy(alpha = 0.65f) else palette.divider
                    ),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .testTag("flashcard_surface")
                ) {
                    AnimatedContent(
                        targetState = isFlipped,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "flashcard_flip"
                    ) { flipped ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.accent.copy(alpha = 0.14f),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = if (flipped) card.targetLanguageEnum.label else "WORD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.8.sp
                                    ),
                                    color = palette.accent,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (!flipped) {
                                    Text(
                                        text = card.sourceText,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = palette.text,
                                        textAlign = TextAlign.Center,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = card.translatedText,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = palette.accent,
                                        textAlign = TextAlign.Center,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (card.bookTitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "From ${card.bookTitle}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontStyle = FontStyle.Italic),
                                            color = palette.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (flipped) "Tap to show word" else "Tap to reveal meaning",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textMuted,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Previous / Shuffle / Next Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            currentIndex = if (safeIdx > 0) safeIdx - 1 else deck.lastIndex
                            isFlipped = false
                        },
                        border = BorderStroke(0.75.dp, palette.divider),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("flashcard_prev_button")
                    ) {
                        Text("Previous", color = palette.text)
                    }

                    OutlinedButton(
                        onClick = {
                            deck = deck.shuffled()
                            currentIndex = 0
                            isFlipped = false
                        },
                        border = BorderStroke(0.75.dp, palette.accent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("flashcard_shuffle_button")
                    ) {
                        Text("Shuffle", color = palette.accent)
                    }

                    Button(
                        onClick = {
                            currentIndex = (safeIdx + 1) % deck.size
                            isFlipped = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.text,
                            contentColor = palette.background
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("flashcard_next_button")
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}
