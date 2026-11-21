package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.config.APP_META
import com.example.config.RemoteAppConfig
import com.example.config.isSafeExternalUrl
import com.example.data.model.DailyGoalUnit
import com.example.data.model.ReaderSettings
import com.example.data.model.ReadingActivityEntity
import com.example.data.model.TranslationLanguage
import com.example.ui.theme.FolioIcons
import com.example.ui.theme.FolioLogoMark
import com.example.ui.theme.LocalFolioPalette
import com.example.ui.theme.NightBg
import com.example.ui.theme.PaperBg
import com.example.ui.theme.ReaderThemeMode
import com.example.ui.theme.SepiaBg
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Refined, ultra-minimalist First-Time Greeting Card shown once on first launch.
 * Sleek frosted glass in rich off-black / off-white editorial aesthetic.
 */
@Composable
fun FirstTimeGreetingCardOverlay(
    onOpenGuide: () -> Unit,
    onSkipToLibrary: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val cardBg = if (palette.isDark) Color(0xF018181B) else Color(0xF5F7F5F0)
    val cardBorder = if (palette.isDark) Color(0x33F7F5F0) else Color(0x26121214)
    val primaryInk = if (palette.isDark) Color(0xFFF7F5F0) else Color(0xFF141416)
    val mutedInk = if (palette.isDark) Color(0xFFB3AEA4) else Color(0xFF635E56)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xB8121214),
                        Color(0xCC121214)
                    )
                )
            )
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .testTag("first_time_greeting_card")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FolioLogoMark(size = 56.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "WELCOME TO ${APP_META.APP_TITLE.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.2.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = palette.accent
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "A quiet space made for reading.",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 32.sp
                    ),
                    color = primaryInk,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "A short personal note and a quick guide to every gesture are already waiting on your shelf.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp
                    ),
                    color = mutedInk,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                Button(
                    onClick = onOpenGuide,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryInk,
                        contentColor = cardBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("greeting_open_guide_button")
                ) {
                    Text(
                        text = "Open Your Guide",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.3.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onSkipToLibrary,
                    modifier = Modifier.testTag("greeting_skip_to_library_button")
                ) {
                    Text(
                        text = "Skip to Library",
                        style = MaterialTheme.typography.labelMedium,
                        color = mutedInk
                    )
                }
            }
        }
    }
}

/**
 * Frosted-Glass Settings & About Drawer:
 * - Logo, Epos version & subtle "Update Available" pill (when remote config has newer version)
 * - Dynamic About the Creator card (synced silently from GitHub raw JSON with offline fallback)
 * - Reading Theme selector (Paper / Sepia / Night)
 * - Secret Shelf Code configuration (zero-trace search-bar unlock)
 * - Optional Gemini API Key for enhanced multi-language translation
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsAndAboutSheet(
    settings: ReaderSettings,
    remoteConfig: RemoteAppConfig,
    hasSecretCode: Boolean,
    onSelectTheme: (ReaderThemeMode) -> Unit,
    onSaveSecretCode: (String) -> Boolean,
    onClearSecretCode: () -> Unit,
    onSaveGeminiKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var secretDraft by remember { mutableStateOf("") }
    var showSecretPlain by remember { mutableStateOf(false) }

    var keyDraft by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showKeyPlain by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceGlass,
        contentColor = palette.text,
        dragHandle = {
            SheetDragHandle()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FolioLogoMark(size = 60.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = APP_META.APP_TITLE,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal
                ),
                color = palette.text
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${APP_META.VERSION} · 100% Offline-First",
                style = MaterialTheme.typography.labelMedium,
                color = palette.accent
            )

            // Subtle "Update Available" pill shown only when remote JSON reports a newer version
            if (remoteConfig.hasNewerUpdate) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    onClick = {
                        val url = remoteConfig.updateUrl.trim()
                        if (isSafeExternalUrl(url)) {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    color = palette.accent.copy(alpha = 0.16f),
                    border = BorderStroke(0.75.dp, palette.accent),
                    modifier = Modifier.testTag("about_update_available_pill")
                ) {
                    Text(
                        text = "Update Available (${remoteConfig.latestVersion})",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.accent,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = APP_META.TAGLINE,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 1. Dynamic About the Creator Card (Remote GitHub Config + Fallback)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.68f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ABOUT THE CREATOR",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = remoteConfig.creatorName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = palette.text
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = remoteConfig.creatorBio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted
                    )
                    // Only render link buttons for valid non-empty URLs
                    if (remoteConfig.links.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            remoteConfig.links.forEach { link ->
                                Surface(
                                    onClick = {
                                        val clean = link.url.trim()
                                        if (isSafeExternalUrl(clean)) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = palette.surface,
                                    border = BorderStroke(0.75.dp, palette.divider)
                                ) {
                                    Text(
                                        text = link.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.text,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Reading Theme Selector (Paper / Sepia / Night)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.68f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APPEARANCE THEME",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(ReaderThemeMode.PAPER, "Paper", PaperBg),
                            Triple(ReaderThemeMode.SEPIA, "Sepia", SepiaBg),
                            Triple(ReaderThemeMode.NIGHT, "Night", NightBg)
                        ).forEach { (mode, label, swatch) ->
                            val selected = settings.themeMode == mode
                            Surface(
                                onClick = { onSelectTheme(mode) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surface,
                                border = BorderStroke(
                                    if (selected) 1.5.dp else 0.75.dp,
                                    if (selected) palette.accent else palette.divider
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("settings_theme_${mode.name.lowercase()}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(swatch)
                                            .border(0.75.dp, palette.divider, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) palette.accent else palette.text
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Zero-Trace Secret Shelf Code Setting
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.68f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = FolioIcons.Lock,
                                contentDescription = null,
                                tint = palette.accent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SECRET SHELF CODE",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                                color = palette.accent
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (hasSecretCode) palette.accent.copy(alpha = 0.16f) else palette.surface
                        ) {
                            Text(
                                text = if (hasSecretCode) "Active" else "Not set",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasSecretCode) palette.accent else palette.textMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Set a private word or code. Typing your exact code into the Library Search bar silently opens your Private Shelf.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = secretDraft,
                        onValueChange = { secretDraft = it },
                        label = {
                            Text(
                                text = if (hasSecretCode) "Enter new secret code" else "Choose secret code",
                                color = palette.textMuted
                            )
                        },
                        singleLine = true,
                        visualTransformation = if (showSecretPlain) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.divider,
                            focusedTextColor = palette.text,
                            unfocusedTextColor = palette.text,
                            cursorColor = palette.accent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_secret_code_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showSecretPlain = !showSecretPlain }) {
                            Text(
                                text = if (showSecretPlain) "Hide" else "Show",
                                color = palette.textMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasSecretCode) {
                                TextButton(
                                    onClick = {
                                        secretDraft = ""
                                        onClearSecretCode()
                                    }
                                ) {
                                    Text("Clear", color = palette.textMuted)
                                }
                            }
                            Button(
                                onClick = {
                                    if (onSaveSecretCode(secretDraft)) {
                                        secretDraft = ""
                                    }
                                },
                                enabled = secretDraft.trim().length >= 3,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = palette.background
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("settings_save_secret_code_button")
                            ) {
                                Text("Save Code")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Optional Gemini API Key for Multi-Language Translation
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.68f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRANSLATION ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uses a free multi-language translator & offline cache by default. Optionally add your own Gemini API key for richer sentence translations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text("Optional Gemini API Key", color = palette.textMuted) },
                        singleLine = true,
                        visualTransformation = if (showKeyPlain) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.accent,
                            unfocusedBorderColor = palette.divider,
                            focusedTextColor = palette.text,
                            unfocusedTextColor = palette.text,
                            cursorColor = palette.accent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_gemini_key_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showKeyPlain = !showKeyPlain }) {
                            Text(
                                text = if (showKeyPlain) "Hide key" else "Show key",
                                color = palette.textMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (settings.geminiApiKey.isNotEmpty() || keyDraft.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        keyDraft = ""
                                        onSaveGeminiKey("")
                                    }
                                ) {
                                    Text("Clear", color = palette.textMuted)
                                }
                            }
                            Button(
                                onClick = { onSaveGeminiKey(keyDraft) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = palette.background
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("about_save_gemini_key_button")
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Dictionary & Language Sheet opened from the 3-dot Library Menu:
 * - Select default target translation language across 14 supported languages
 * - Quick shortcut to Word Bank & Flashcard Practice
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DictionaryAndLanguageSheet(
    currentLanguage: TranslationLanguage,
    wordBankCount: Int,
    onSelectLanguage: (TranslationLanguage) -> Unit,
    onOpenWordBank: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Dictionary & Language",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = palette.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Choose your default target language. Source language is auto-detected; if text is already in your target language, Epos translates to English.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DEFAULT TARGET LANGUAGE",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = palette.accent
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TranslationLanguage.entries.forEach { lang ->
                    val selected = lang == currentLanguage
                    Surface(
                        onClick = { onSelectLanguage(lang) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) palette.accent else palette.background.copy(alpha = 0.65f),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 0.75.dp,
                            color = if (selected) palette.accent else palette.divider
                        ),
                        modifier = Modifier.testTag("dict_lang_${lang.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
                        ) {
                            Text(
                                text = lang.label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                ),
                                color = if (selected) palette.background else palette.text
                            )
                            if (lang.nativeName != lang.label) {
                                Text(
                                    text = lang.nativeName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) palette.background.copy(alpha = 0.82f) else palette.textMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    onDismiss()
                    onOpenWordBank()
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.text,
                    contentColor = palette.background
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("dict_open_wordbank_button")
            ) {
                Icon(
                    imageVector = FolioIcons.Flashcards,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (wordBankCount > 0) "Open Word Bank & Flashcards ($wordBankCount)"
                    else "Open Word Bank"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Frosted-Glass Daily Reading Goal & 10-Week Reading Calendar Heatmap Sheet.
 * Counts only visible (non-hidden) library books.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingCalendarHeatmapSheet(
    activities: List<ReadingActivityEntity>,
    todayDateKey: String,
    settings: ReaderSettings,
    onUpdateGoal: (target: Int, unit: DailyGoalUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activityMap = remember(activities) { activities.associateBy { it.dateKey } }
    val todayEntry = activityMap[todayDateKey]
    val todayValue = when (settings.dailyGoalUnit) {
        DailyGoalUnit.MINUTES -> todayEntry?.minutesRead ?: 0
        DailyGoalUnit.PAGES -> todayEntry?.pagesRead ?: 0
    }
    val totalDaysActive = remember(activities) {
        activities.count { it.minutesRead > 0 || it.pagesRead > 0 }
    }
    val totalPagesLogged = remember(activities) { activities.sumOf { it.pagesRead } }
    val totalMinutesLogged = remember(activities) { activities.sumOf { it.minutesRead } }

    // Build 10 weeks x 7 days (70-day rolling calendar ending today)
    val calendarDays = remember(todayDateKey) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val list = ArrayList<String>(70)
        cal.add(Calendar.DAY_OF_YEAR, -69)
        repeat(70) {
            list.add(fmt.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Reading Stats & Goal",
                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = FontFamily.Serif),
                color = palette.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Today: $todayValue / ${settings.dailyGoalTarget} ${settings.dailyGoalUnit.label.lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.accent
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuietStatCard(
                    value = "$totalDaysActive",
                    label = "Active Days",
                    modifier = Modifier.weight(1f)
                )
                QuietStatCard(
                    value = "$totalMinutesLogged",
                    label = "Minutes",
                    modifier = Modifier.weight(1f)
                )
                QuietStatCard(
                    value = "$totalPagesLogged",
                    label = "Pages",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.65f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LAST 10 WEEKS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                            color = palette.textMuted
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Less", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                            listOf(0.10f, 0.32f, 0.62f, 0.95f).forEach { a ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent.copy(alpha = a))
                                )
                            }
                            Text("More", style = MaterialTheme.typography.labelSmall, color = palette.textMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (week in 0 until 10) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (day in 0 until 7) {
                                    val key = calendarDays[week * 7 + day]
                                    val rec = activityMap[key]
                                    val metric = when (settings.dailyGoalUnit) {
                                        DailyGoalUnit.MINUTES -> rec?.minutesRead ?: 0
                                        DailyGoalUnit.PAGES -> rec?.pagesRead ?: 0
                                    }
                                    val ratio = (metric.toFloat() / settings.dailyGoalTarget.coerceAtLeast(1)).coerceIn(0f, 1f)
                                    val dotColor = when {
                                        metric <= 0 -> palette.divider.copy(alpha = 0.7f)
                                        ratio < 0.35f -> palette.accent.copy(alpha = 0.35f)
                                        ratio < 0.85f -> palette.accent.copy(alpha = 0.68f)
                                        else -> palette.accent
                                    }
                                    val isToday = key == todayDateKey
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                            .then(
                                                if (isToday) Modifier.border(1.5.dp, palette.text, CircleShape)
                                                else Modifier
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "DAILY READING GOAL",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                color = palette.textMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DailyGoalUnit.entries.forEach { unit ->
                    val selected = settings.dailyGoalUnit == unit
                    Surface(
                        onClick = { onUpdateGoal(settings.dailyGoalTarget, unit) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.background.copy(alpha = 0.55f),
                        border = BorderStroke(if (selected) 1.5.dp else 0.75.dp, if (selected) palette.accent else palette.divider),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) palette.accent else palette.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val presets = listOf(10, 15, 20, 30, 45, 60)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { target ->
                    val selected = settings.dailyGoalTarget == target
                    Surface(
                        onClick = { onUpdateGoal(target, settings.dailyGoalUnit) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) palette.accent else palette.background.copy(alpha = 0.55f),
                        border = BorderStroke(0.75.dp, if (selected) palette.accent else palette.divider),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$target${settings.dailyGoalUnit.shortUnit}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (selected) palette.background else palette.text
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
internal fun SheetDragHandle() {
    val palette = LocalFolioPalette.current
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 6.dp)
            .width(36.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(palette.textMuted.copy(alpha = 0.4f))
    )
}

@Composable
private fun QuietStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalFolioPalette.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = palette.background.copy(alpha = 0.65f),
        border = BorderStroke(0.75.dp, palette.divider),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                ),
                color = palette.text
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted
            )
        }
    }
}
