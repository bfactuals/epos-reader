package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.FolioDatabase
import com.example.data.local.PreferencesRepository
import com.example.data.repository.LibraryRepository
import com.example.engine.audio.PaperAudioEngine
import com.example.engine.translate.TranslationService
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.ReaderScreen
import com.example.ui.theme.FolioTheme
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.ReaderViewModel

class MainActivity : ComponentActivity() {

    private val database by lazy { FolioDatabase.getInstance(applicationContext) }
    private val preferencesRepository by lazy { PreferencesRepository(applicationContext) }
    private val libraryRepository by lazy { LibraryRepository(applicationContext, database) }
    private val paperAudioEngine by lazy { PaperAudioEngine(applicationContext) }
    private val translationService by lazy { TranslationService(database.translationDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by preferencesRepository.settings.collectAsStateWithLifecycle()
            val isDark = settings.themeMode == ReaderThemeMode.NIGHT

            // Keep edge-to-edge status & navigation bar icons harmonized with Paper / Sepia / Night
            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
            }

            FolioTheme(themeMode = settings.themeMode) {
                var activeBookId by remember { mutableStateOf<Long?>(null) }
                var activeBookIsHidden by remember { mutableStateOf(false) }

                val libraryViewModel: LibraryViewModel = viewModel(
                    factory = LibraryViewModel.Factory(libraryRepository, preferencesRepository)
                )

                // Track whether the currently open book belongs to the zero-trace private shelf
                LaunchedEffect(activeBookId) {
                    activeBookIsHidden = activeBookId?.let { id ->
                        libraryRepository.getBookById(id)?.isHidden == true
                    } ?: false
                }

                // Zero-Trace Security Rule: closing or backgrounding the app immediately locks the vault
                // and closes any active hidden book so nothing remains visible on resume.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, activeBookIsHidden) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) {
                            libraryViewModel.lockVault()
                            if (activeBookIsHidden) {
                                activeBookId = null
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Crossfade(targetState = activeBookId, label = "screen_transition") { openId ->
                    if (openId == null) {
                        LibraryScreen(
                            viewModel = libraryViewModel,
                            onOpenBook = { bookId -> activeBookId = bookId }
                        )
                    } else {
                        val readerViewModel: ReaderViewModel = viewModel(
                            key = "reader_$openId",
                            factory = ReaderViewModel.Factory(
                                appContext = applicationContext,
                                bookId = openId,
                                repository = libraryRepository,
                                preferencesRepository = preferencesRepository,
                                audioEngine = paperAudioEngine,
                                translationService = translationService
                            )
                        )
                        ReaderScreen(
                            viewModel = readerViewModel,
                            onNavigateBack = { activeBookId = null }
                        )
                    }
                }
            }
        }
    }
}
