package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.config.APP_META
import com.example.config.RemoteAppConfig
import com.example.data.model.AmbientSoundMode
import com.example.data.model.DailyGoalUnit
import com.example.data.model.ReaderSettings
import com.example.data.model.TranslationLanguage
import com.example.engine.config.RemoteConfigService
import com.example.ui.theme.ReaderFontStyle
import com.example.ui.theme.ReaderThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import java.security.SecureRandom

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    private val _remoteConfig = MutableStateFlow(loadCachedRemoteConfig())
    val remoteConfig: StateFlow<RemoteAppConfig> = _remoteConfig.asStateFlow()

    private fun loadSettings(): ReaderSettings {
        val themeName = prefs.getString(KEY_THEME, ReaderThemeMode.PAPER.name)
        val fontName = prefs.getString(KEY_FONT, ReaderFontStyle.CLASSIC_BOOK.name)
        val ambientName = prefs.getString(KEY_AMBIENT_MODE, AmbientSoundMode.OFF.name)
        val goalUnitName = prefs.getString(KEY_GOAL_UNIT, DailyGoalUnit.MINUTES.name)
        val targetLangCode = prefs.getString(KEY_DEFAULT_TARGET_LANG, TranslationLanguage.HINDI.code)
        return ReaderSettings(
            themeMode = runCatching { ReaderThemeMode.valueOf(themeName ?: "") }
                .getOrDefault(ReaderThemeMode.PAPER),
            fontStyle = runCatching { ReaderFontStyle.valueOf(fontName ?: "") }
                .getOrDefault(ReaderFontStyle.CLASSIC_BOOK),
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE, 18f).coerceIn(13f, 28f),
            lineSpacing = prefs.getFloat(KEY_LINE_SPACING, 1.48f).coerceIn(1.15f, 2.0f),
            marginDp = prefs.getFloat(KEY_MARGIN_DP, 24f).coerceIn(14f, 44f),
            paperSoundEnabled = prefs.getBoolean(KEY_PAPER_SOUND, true),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
            paperGrainEnabled = prefs.getBoolean(KEY_PAPER_GRAIN, true),
            pageStiffness = prefs.getFloat(KEY_STIFFNESS, 0.55f).coerceIn(0.2f, 1.0f),
            warmthFilter = prefs.getFloat(KEY_WARMTH, 0f).coerceIn(0f, 0.65f),
            brightnessLevel = prefs.getFloat(KEY_BRIGHTNESS, 1.0f).coerceIn(0.15f, 1.0f),
            readingRulerEnabled = prefs.getBoolean(KEY_RULER, false),
            bionicModeEnabled = prefs.getBoolean(KEY_BIONIC, false),
            keepScreenAwake = prefs.getBoolean(KEY_KEEP_AWAKE, true),
            oneHandedTapMode = prefs.getBoolean(KEY_ONE_HANDED, false),
            pdfAutoCropEnabled = prefs.getBoolean(KEY_PDF_CROP, false),
            ambientSoundMode = runCatching { AmbientSoundMode.valueOf(ambientName ?: "") }
                .getOrDefault(AmbientSoundMode.OFF),
            ambientVolume = prefs.getFloat(KEY_AMBIENT_VOL, 0.45f).coerceIn(0.05f, 1.0f),
            ttsSpeed = prefs.getFloat(KEY_TTS_SPEED, 1.0f).coerceIn(0.75f, 2.0f),
            autoFlipSeconds = prefs.getInt(KEY_AUTO_FLIP, 0),
            dailyGoalTarget = prefs.getInt(KEY_GOAL_TARGET, 20).coerceIn(5, 240),
            dailyGoalUnit = runCatching { DailyGoalUnit.valueOf(goalUnitName ?: "") }
                .getOrDefault(DailyGoalUnit.MINUTES),
            defaultTargetLanguage = TranslationLanguage.fromCode(targetLangCode),
            geminiApiKey = prefs.getString(KEY_GEMINI_KEY, "") ?: "",
            hasDismissedFirstTimeGreeting = prefs.getBoolean(KEY_FIRST_GREETING_DISMISSED, false),
            hasSeededWelcomeGuide = prefs.getBoolean(KEY_WELCOME_GUIDE_SEEDED, false)
        )
    }

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        _settings.update { current ->
            val updated = transform(current)
            persist(updated)
            updated
        }
    }

    // --- Cached Remote GitHub Config ---
    private fun loadCachedRemoteConfig(): RemoteAppConfig {
        val raw = prefs.getString(KEY_REMOTE_CONFIG_JSON, null)
        return RemoteConfigService.parseJson(raw) ?: APP_META.DEFAULT_REMOTE_CONFIG
    }

    fun saveRemoteConfigJson(rawJson: String) {
        val parsed = RemoteConfigService.parseJson(rawJson) ?: return
        prefs.edit().putString(KEY_REMOTE_CONFIG_JSON, rawJson).apply()
        _remoteConfig.value = parsed
    }

    // --- Salted SHA-256 Secret Shelf Code Storage (never stored in plaintext) ---
    fun hasSecretShelfCode(): Boolean = !prefs.getString(KEY_SECRET_CODE_HASH, null).isNullOrBlank()

    fun setSecretShelfCode(rawCode: String): Boolean {
        val clean = rawCode.trim()
        if (clean.length < 3) return false
        val saltBytes = ByteArray(16)
        SecureRandom().nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val hashHex = sha256Hex("$saltHex:$clean")
        prefs.edit()
            .putString(KEY_SECRET_CODE_SALT, saltHex)
            .putString(KEY_SECRET_CODE_HASH, hashHex)
            .apply()
        return true
    }

    fun clearSecretShelfCode() {
        prefs.edit()
            .remove(KEY_SECRET_CODE_SALT)
            .remove(KEY_SECRET_CODE_HASH)
            .apply()
    }

    fun matchesSecretShelfCode(candidate: String): Boolean {
        val clean = candidate.trim()
        if (clean.isEmpty()) return false
        val salt = prefs.getString(KEY_SECRET_CODE_SALT, null) ?: return false
        val expectedHash = prefs.getString(KEY_SECRET_CODE_HASH, null) ?: return false
        val candidateHash = sha256Hex("$salt:$clean")
        return MessageDigest.isEqual(expectedHash.toByteArray(), candidateHash.toByteArray())
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun persist(s: ReaderSettings) {
        prefs.edit()
            .putString(KEY_THEME, s.themeMode.name)
            .putString(KEY_FONT, s.fontStyle.name)
            .putFloat(KEY_FONT_SIZE, s.fontSizeSp)
            .putFloat(KEY_LINE_SPACING, s.lineSpacing)
            .putFloat(KEY_MARGIN_DP, s.marginDp)
            .putBoolean(KEY_PAPER_SOUND, s.paperSoundEnabled)
            .putBoolean(KEY_HAPTIC, s.hapticEnabled)
            .putBoolean(KEY_PAPER_GRAIN, s.paperGrainEnabled)
            .putFloat(KEY_STIFFNESS, s.pageStiffness)
            .putFloat(KEY_WARMTH, s.warmthFilter)
            .putFloat(KEY_BRIGHTNESS, s.brightnessLevel)
            .putBoolean(KEY_RULER, s.readingRulerEnabled)
            .putBoolean(KEY_BIONIC, s.bionicModeEnabled)
            .putBoolean(KEY_KEEP_AWAKE, s.keepScreenAwake)
            .putBoolean(KEY_ONE_HANDED, s.oneHandedTapMode)
            .putBoolean(KEY_PDF_CROP, s.pdfAutoCropEnabled)
            .putString(KEY_AMBIENT_MODE, s.ambientSoundMode.name)
            .putFloat(KEY_AMBIENT_VOL, s.ambientVolume)
            .putFloat(KEY_TTS_SPEED, s.ttsSpeed)
            .putInt(KEY_AUTO_FLIP, s.autoFlipSeconds)
            .putInt(KEY_GOAL_TARGET, s.dailyGoalTarget)
            .putString(KEY_GOAL_UNIT, s.dailyGoalUnit.name)
            .putString(KEY_DEFAULT_TARGET_LANG, s.defaultTargetLanguage.code)
            .putString(KEY_GEMINI_KEY, s.geminiApiKey.trim())
            .putBoolean(KEY_FIRST_GREETING_DISMISSED, s.hasDismissedFirstTimeGreeting)
            .putBoolean(KEY_WELCOME_GUIDE_SEEDED, s.hasSeededWelcomeGuide)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "epos_reader_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_FONT = "font_style"
        private const val KEY_FONT_SIZE = "font_size_sp"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_MARGIN_DP = "margin_dp"
        private const val KEY_PAPER_SOUND = "paper_sound"
        private const val KEY_HAPTIC = "haptic_tick"
        private const val KEY_PAPER_GRAIN = "paper_grain"
        private const val KEY_STIFFNESS = "page_stiffness"
        private const val KEY_WARMTH = "warmth_filter"
        private const val KEY_BRIGHTNESS = "brightness_level"
        private const val KEY_RULER = "reading_ruler"
        private const val KEY_BIONIC = "bionic_mode"
        private const val KEY_KEEP_AWAKE = "keep_awake"
        private const val KEY_ONE_HANDED = "one_handed_tap"
        private const val KEY_PDF_CROP = "pdf_auto_crop"
        private const val KEY_AMBIENT_MODE = "ambient_mode"
        private const val KEY_AMBIENT_VOL = "ambient_vol"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_AUTO_FLIP = "auto_flip_sec"
        private const val KEY_GOAL_TARGET = "daily_goal_target"
        private const val KEY_GOAL_UNIT = "daily_goal_unit"
        private const val KEY_DEFAULT_TARGET_LANG = "default_target_language"
        private const val KEY_GEMINI_KEY = "optional_gemini_key"
        private const val KEY_FIRST_GREETING_DISMISSED = "first_greeting_dismissed"
        private const val KEY_WELCOME_GUIDE_SEEDED = "welcome_guide_seeded"
        private const val KEY_REMOTE_CONFIG_JSON = "cached_remote_config_json"
        private const val KEY_SECRET_CODE_SALT = "secret_shelf_code_salt"
        private const val KEY_SECRET_CODE_HASH = "secret_shelf_code_hash"
    }
}
