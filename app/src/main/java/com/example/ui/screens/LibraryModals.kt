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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
    onSelectGridColumns: (Int) -> Unit,
    onToggleFullPageTranslate: (Boolean) -> Unit,
    onCreateSecretCode: (newCode: String, confirmCode: String) -> String?,
    onChangeSecretCode: (currentCode: String, newCode: String, confirmCode: String) -> String?,
    onRemoveSecretCode: (currentCode: String) -> String?,
    onSaveGeminiKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = LocalFolioPalette.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Secret code strictly verified & masked state: 0 = idle, 1 = change, 2 = remove
    var secretActionMode by remember(hasSecretCode) { mutableStateOf(0) }
    var currentCodeDraft by remember { mutableStateOf("") }
    var newCodeDraft by remember { mutableStateOf("") }
    var confirmCodeDraft by remember { mutableStateOf("") }
    var showCurrentPlain by remember { mutableStateOf(false) }
    var showNewPlain by remember { mutableStateOf(false) }
    var showConfirmPlain by remember { mutableStateOf(false) }
    var secretError by remember { mutableStateOf<String?>(null) }

    var keyDraft by remember(settings.geminiApiKey) { mutableStateOf(settings.geminiApiKey) }
    var showKeyPlain by remember { mutableStateOf(false) }

    fun resetSecretDrafts() {
        currentCodeDraft = ""
        newCodeDraft = ""
        confirmCodeDraft = ""
        showCurrentPlain = false
        showNewPlain = false
        showConfirmPlain = false
        secretError = null
        secretActionMode = 0
    }

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

            // 2b. Adjustable Shelf Layout / Grid Size (2x2, 3x3, 4x4)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = FolioIcons.GridView,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "SHELF LAYOUT / GRID SIZE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                            color = palette.accent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Applies smoothly to both your Library and Private Shelf.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            2 to "2×2",
                            3 to "3×3",
                            4 to "4×4"
                        ).forEach { (cols, label) ->
                            val selected = settings.shelfGridColumns == cols
                            Surface(
                                onClick = { onSelectGridColumns(cols) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) palette.accent.copy(alpha = 0.16f) else palette.surface,
                                border = BorderStroke(
                                    if (selected) 1.5.dp else 0.75.dp,
                                    if (selected) palette.accent else palette.divider
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("settings_grid_cols_$cols")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                        ),
                                        color = if (selected) palette.accent else palette.text
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Strictly Masked Zero-Trace Secret Shelf Code Setting
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.72f),
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
                                text = if (hasSecretCode) "Secret Code Active" else "Not set",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (hasSecretCode) palette.accent else palette.textMuted,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hasSecretCode) {
                            "Secret Code Active. Typing your exact code into the Library Search bar silently opens your Private Shelf. Changing or removing requires your current code."
                        } else {
                            "Set a private masked code (3+ characters). Typing your exact code into the Library Search bar silently opens your Private Shelf."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasSecretCode) {
                        // Create new Secret Code (masked + confirm)
                        MaskedSecretField(
                            value = newCodeDraft,
                            onValueChange = {
                                newCodeDraft = it
                                secretError = null
                            },
                            label = "New secret code",
                            showPlain = showNewPlain,
                            onToggleShow = { showNewPlain = !showNewPlain },
                            testTag = "settings_secret_code_input"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MaskedSecretField(
                            value = confirmCodeDraft,
                            onValueChange = {
                                confirmCodeDraft = it
                                secretError = null
                            },
                            label = "Confirm secret code",
                            showPlain = showConfirmPlain,
                            onToggleShow = { showConfirmPlain = !showConfirmPlain },
                            testTag = "settings_secret_code_confirm_input"
                        )
                        if (secretError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = secretError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD14B4B)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val err = onCreateSecretCode(newCodeDraft, confirmCodeDraft)
                                if (err == null) resetSecretDrafts() else secretError = err
                            },
                            enabled = newCodeDraft.trim().length >= 3 && confirmCodeDraft.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = palette.background
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("settings_save_secret_code_button")
                        ) {
                            Text("Activate Secret Code")
                        }
                    } else if (secretActionMode == 0) {
                        // Secret Code is active: show Change / Remove triggers (never direct overwrite!)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    resetSecretDrafts()
                                    secretActionMode = 1
                                },
                                border = BorderStroke(0.75.dp, palette.accent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("settings_change_secret_code_button")
                            ) {
                                Text("Change Code", color = palette.accent)
                            }
                            OutlinedButton(
                                onClick = {
                                    resetSecretDrafts()
                                    secretActionMode = 2
                                },
                                border = BorderStroke(0.75.dp, palette.divider),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("settings_remove_secret_code_button")
                            ) {
                                Text("Remove Code", color = Color(0xFFD14B4B))
                            }
                        }
                    } else if (secretActionMode == 1) {
                        // Change Secret Code: MUST verify Current Code first -> New -> Confirm
                        MaskedSecretField(
                            value = currentCodeDraft,
                            onValueChange = {
                                currentCodeDraft = it
                                secretError = null
                            },
                            label = "Current secret code",
                            showPlain = showCurrentPlain,
                            onToggleShow = { showCurrentPlain = !showCurrentPlain },
                            testTag = "settings_current_secret_code_input"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MaskedSecretField(
                            value = newCodeDraft,
                            onValueChange = {
                                newCodeDraft = it
                                secretError = null
                            },
                            label = "New secret code",
                            showPlain = showNewPlain,
                            onToggleShow = { showNewPlain = !showNewPlain },
                            testTag = "settings_new_secret_code_input"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MaskedSecretField(
                            value = confirmCodeDraft,
                            onValueChange = {
                                confirmCodeDraft = it
                                secretError = null
                            },
                            label = "Confirm new secret code",
                            showPlain = showConfirmPlain,
                            onToggleShow = { showConfirmPlain = !showConfirmPlain },
                            testTag = "settings_confirm_new_secret_code_input"
                        )
                        if (secretError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = secretError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD14B4B)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { resetSecretDrafts() }) {
                                Text("Cancel", color = palette.textMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val err = onChangeSecretCode(currentCodeDraft, newCodeDraft, confirmCodeDraft)
                                    if (err == null) resetSecretDrafts() else secretError = err
                                },
                                enabled = currentCodeDraft.isNotEmpty() && newCodeDraft.trim().length >= 3,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.accent,
                                    contentColor = palette.background
                                ),
                                modifier = Modifier.testTag("settings_confirm_change_secret_code_button")
                            ) {
                                Text("Update Code")
                            }
                        }
                    } else {
                        // Remove Secret Code: MUST verify Current Code first
                        MaskedSecretField(
                            value = currentCodeDraft,
                            onValueChange = {
                                currentCodeDraft = it
                                secretError = null
                            },
                            label = "Enter current secret code to remove",
                            showPlain = showCurrentPlain,
                            onToggleShow = { showCurrentPlain = !showCurrentPlain },
                            testTag = "settings_remove_verify_secret_code_input"
                        )
                        if (secretError != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = secretError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD14B4B)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { resetSecretDrafts() }) {
                                Text("Cancel", color = palette.textMuted)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val err = onRemoveSecretCode(currentCodeDraft)
                                    if (err == null) resetSecretDrafts() else secretError = err
                                },
                                enabled = currentCodeDraft.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD14B4B),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.testTag("settings_confirm_remove_secret_code_button")
                            ) {
                                Text("Verify & Remove")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Translation Engine & 1-Tap Full-Page Translation Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.background.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRANSLATION ENGINE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp),
                        color = palette.accent
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = "1-Tap Full-Page Translation Button",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = palette.text
                            )
                            Text(
                                text = "Show an unobtrusive Translate Page button while reading.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textMuted
                            )
                        }
                        Switch(
                            checked = settings.fullPageTranslateEnabled,
                            onCheckedChange = onToggleFullPageTranslate,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.background,
                                checkedTrackColor = palette.accent,
                                uncheckedThumbColor = palette.textMuted,
                                uncheckedTrackColor = palette.surface
                            ),
                            modifier = Modifier.testTag("settings_full_page_translate_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = palette.divider, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Uses a free multi-language translator & offline cache by default. Optionally add your own Gemini API key for richer paragraph & page translations.",
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
                        trailingIcon = {
                            IconButton(onClick = { showKeyPlain = !showKeyPlain }) {
                                Icon(
                                    imageVector = if (showKeyPlain) FolioIcons.VisibilityOff else FolioIcons.Visibility,
                                    contentDescription = if (showKeyPlain) "Hide key" else "Show key",
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
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
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Spacer(modifier = Modifier.width(6.dp))
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

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MaskedSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    showPlain: Boolean,
    onToggleShow: () -> Unit,
    testTag: String
) {
    val palette = LocalFolioPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = palette.textMuted) },
        singleLine = true,
        visualTransformation = if (showPlain) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleShow) {
                Icon(
                    imageVector = if (showPlain) FolioIcons.VisibilityOff else FolioIcons.Visibility,
                    contentDescription = if (showPlain) "Hide secret code" else "Show secret code",
                    tint = palette.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.divider,
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            cursorColor = palette.accent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

/**
 * Dictionary & Language Sheet opened from the 3-dot Library Menu:
 * - Select default target translation language across 15 supported languages (including Arabic RTL)
 * - Toggle 1-Tap Full-Page Translation Button in Reader
 * - Quick shortcut to Word Bank & Flashcard Practice
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DictionaryAndLanguageSheet(
    currentLanguage: TranslationLanguage,
    fullPageTranslateEnabled: Boolean,
    wordBankCount: Int,
    onSelectLanguage: (TranslationLanguage) -> Unit,
    onToggleFullPageTranslate: (Boolean) -> Unit,
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

            Spacer(modifier = Modifier.height(18.dp))

            // 1-Tap Full-Page Translation Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = palette.background.copy(alpha = 0.72f),
                border = BorderStroke(0.75.dp, palette.divider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "1-Tap Full-Page Translation Button",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = palette.text
                        )
                        Text(
                            text = "Translate the entire visible book page in one tap while reading.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textMuted
                        )
                    }
                    Switch(
                        checked = fullPageTranslateEnabled,
                        onCheckedChange = onToggleFullPageTranslate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = palette.background,
                            checkedTrackColor = palette.accent,
                            uncheckedThumbColor = palette.textMuted,
                            uncheckedTrackColor = palette.surface
                        ),
                        modifier = Modifier.testTag("dict_full_page_translate_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
