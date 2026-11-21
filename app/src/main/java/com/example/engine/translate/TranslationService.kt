package com.example.engine.translate

import com.example.data.local.TranslationDao
import com.example.data.model.TranslationEntryEntity
import com.example.data.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class TranslationResult(
    val entry: TranslationEntryEntity,
    val fromOfflineCache: Boolean,
    val usedGemini: Boolean,
    val errorHint: String? = null
)

/**
 * Lightweight zero-dependency 14-language Translator with automatic source language detection
 * and Room offline caching keyed by (text + targetLanguage).
 */
class TranslationService(private val translationDao: TranslationDao) {

    /**
     * Detects likely source language code via Unicode script blocks and light heuristics.
     */
    fun detectSourceLanguageCode(text: String): String {
        for (ch in text) {
            val code = ch.code
            when (code) {
                in 0x0900..0x097F -> return "hi" // Devanagari (Hindi/Marathi)
                in 0x0600..0x06FF -> return "ur" // Arabic/Urdu
                in 0x0980..0x09FF -> return "bn" // Bengali
                in 0x0B80..0x0BFF -> return "ta" // Tamil
                in 0x0C00..0x0C7F -> return "te" // Telugu
                in 0x0A80..0x0AFF -> return "gu" // Gujarati
                in 0x0A00..0x0A7F -> return "pa" // Gurmukhi (Punjabi)
                in 0x3040..0x30FF, in 0x4E00..0x9FFF -> return "ja" // Japanese Kana / Kanji
            }
        }
        val lower = text.lowercase()
        if (lower.any { it in "ñ¿¡" }) return "es"
        if (lower.any { it in "äöüß" }) return "de"
        if (lower.any { it in "çœàèêëîïôùû" }) return "fr"
        return "en"
    }

    /**
     * Resolves the effective target language: if user's selected text is already in the chosen
     * target language, automatically flip to English so the reader always gets a useful meaning.
     */
    fun resolveTargetLanguage(
        rawText: String,
        preferredTarget: TranslationLanguage,
        explicitOverride: Boolean = false
    ): Pair<String, TranslationLanguage> {
        val detectedSrc = detectSourceLanguageCode(rawText)
        if (explicitOverride) {
            return detectedSrc to preferredTarget
        }
        val sameAsPreferred = when (preferredTarget) {
            TranslationLanguage.HINDI -> detectedSrc == "hi"
            TranslationLanguage.MARATHI -> detectedSrc == "hi" || detectedSrc == "mr"
            TranslationLanguage.ENGLISH -> detectedSrc == "en"
            else -> detectedSrc == preferredTarget.myMemoryCode
        }
        val effectiveTarget = if (sameAsPreferred && preferredTarget != TranslationLanguage.ENGLISH) {
            TranslationLanguage.ENGLISH
        } else {
            preferredTarget
        }
        return detectedSrc to effectiveTarget
    }

    suspend fun translate(
        rawText: String,
        targetLanguage: TranslationLanguage,
        bookTitle: String,
        optionalGeminiApiKey: String,
        explicitTargetOverride: Boolean = false
    ): TranslationResult = withContext(Dispatchers.IO) {
        val cleaned = rawText.trim().replace(Regex("\\s+"), " ").take(450)
        val (detectedSrc, effectiveTarget) = resolveTargetLanguage(
            rawText = cleaned,
            preferredTarget = targetLanguage,
            explicitOverride = explicitTargetOverride
        )
        val cacheKey = "${effectiveTarget.code}|${cleaned.lowercase()}"

        // 1. Offline Room cache keyed by (text + targetLanguage)
        val cached = translationDao.findByKey(cacheKey)
        if (cached != null) {
            return@withContext TranslationResult(
                entry = cached,
                fromOfflineCache = true,
                usedGemini = false
            )
        }

        // 2. Optional user-configured Gemini API key (HTTPS header only)
        val trimmedKey = optionalGeminiApiKey.trim()
        if (trimmedKey.isNotEmpty()) {
            val geminiText = runCatching {
                translateWithGemini(cleaned, effectiveTarget, trimmedKey)
            }.getOrNull()
            if (!geminiText.isNullOrBlank()) {
                val entity = persistCache(
                    key = cacheKey,
                    source = cleaned,
                    translated = geminiText,
                    sourceLang = detectedSrc,
                    targetLang = effectiveTarget,
                    bookTitle = bookTitle
                )
                return@withContext TranslationResult(
                    entry = entity,
                    fromOfflineCache = false,
                    usedGemini = true
                )
            }
        }

        // 3. Free lightweight MyMemory REST endpoint (0 KB deps)
        val apiSource = if (detectedSrc == effectiveTarget.myMemoryCode) {
            if (effectiveTarget == TranslationLanguage.ENGLISH) "hi" else "en"
        } else {
            detectedSrc
        }
        val publicText = runCatching {
            translateWithMyMemory(cleaned, apiSource, effectiveTarget)
        }.getOrNull()
        if (!publicText.isNullOrBlank()) {
            val formatted = if (effectiveTarget == TranslationLanguage.HINGLISH) {
                transliterateDevanagariToLatin(publicText)
            } else {
                publicText
            }
            val entity = persistCache(
                key = cacheKey,
                source = cleaned,
                translated = formatted,
                sourceLang = detectedSrc,
                targetLang = effectiveTarget,
                bookTitle = bookTitle
            )
            return@withContext TranslationResult(
                entry = entity,
                fromOfflineCache = false,
                usedGemini = false
            )
        }

        // 4. Built-in offline mini-glossary fallback (Hindi / Hinglish / English)
        val offlineHit = lookupOfflineFallback(cleaned.lowercase(), effectiveTarget)
        if (offlineHit != null) {
            val entity = persistCache(
                key = cacheKey,
                source = cleaned,
                translated = offlineHit,
                sourceLang = detectedSrc,
                targetLang = effectiveTarget,
                bookTitle = bookTitle
            )
            return@withContext TranslationResult(
                entry = entity,
                fromOfflineCache = true,
                usedGemini = false
            )
        }

        val fallback = TranslationEntryEntity(
            normalizedKey = cacheKey,
            sourceText = cleaned,
            translatedText = "Offline — connect briefly to translate into ${effectiveTarget.label} and cache.",
            sourceLanguage = detectedSrc,
            targetLanguage = effectiveTarget.code,
            isSavedToWordBank = false,
            bookTitle = bookTitle
        )
        TranslationResult(
            entry = fallback,
            fromOfflineCache = false,
            usedGemini = false,
            errorHint = "Offline or unreachable"
        )
    }

    private suspend fun persistCache(
        key: String,
        source: String,
        translated: String,
        sourceLang: String,
        targetLang: TranslationLanguage,
        bookTitle: String
    ): TranslationEntryEntity {
        val draft = TranslationEntryEntity(
            normalizedKey = key,
            sourceText = source,
            translatedText = translated.trim(),
            sourceLanguage = sourceLang,
            targetLanguage = targetLang.code,
            isSavedToWordBank = false,
            bookTitle = bookTitle
        )
        val id = translationDao.upsert(draft)
        return draft.copy(id = id)
    }

    private fun translateWithMyMemory(
        text: String,
        sourceCode: String,
        target: TranslationLanguage
    ): String? {
        val langPair = "$sourceCode|${target.myMemoryCode}"
        val encodedQ = URLEncoder.encode(text, "UTF-8")
        val encodedPair = URLEncoder.encode(langPair, "UTF-8")
        val url = URL("https://api.mymemory.translated.net/get?q=$encodedQ&langpair=$encodedPair")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6500
            readTimeout = 6500
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val data = json.optJSONObject("responseData")
            data?.optString("translatedText")?.takeIf { it.isNotBlank() }
        } finally {
            conn.disconnect()
        }
    }

    private fun translateWithGemini(
        text: String,
        target: TranslationLanguage,
        apiKey: String
    ): String? {
        val instruction =
            "Translate the following text naturally and concisely into ${target.promptName}. Output ONLY the translation."
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        val payload = JSONObject().put(
            "contents",
            JSONArray().put(
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", "$instruction\n\n$text"))
                )
            )
        )
        return try {
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            val first = candidates.optJSONObject(0) ?: return null
            val content = first.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            parts.optJSONObject(0)?.optString("text")?.trim()?.takeIf { it.isNotBlank() }
        } finally {
            conn.disconnect()
        }
    }

    private fun lookupOfflineFallback(lower: String, target: TranslationLanguage): String? {
        return when (target) {
            TranslationLanguage.HINDI -> offlineEnToHi[lower]
            TranslationLanguage.HINGLISH -> offlineEnToHi[lower]?.let { transliterateDevanagariToLatin(it) }
            TranslationLanguage.ENGLISH -> offlineHiToEn[lower]
            else -> null
        }
    }

    /**
     * Zero-dependency Devanagari -> Latin phonetic transliterator for clean offline/free Hinglish output.
     */
    private fun transliterateDevanagariToLatin(input: String): String {
        val sb = StringBuilder(input.length * 2)
        var i = 0
        while (i < input.length) {
            val ch = input[i]
            val consonant = devConsonants[ch]
            if (consonant != null) {
                sb.append(consonant)
                val next = input.getOrNull(i + 1)
                when {
                    next == '\u094D' -> i++ // virama / halant suppresses inherent 'a'
                    next != null && devMatras.containsKey(next) -> {
                        sb.append(devMatras[next])
                        i++
                    }
                    else -> {
                        // Add inherent 'a' unless at end of word
                        val atWordEnd = next == null || next.isWhitespace() || next in ".,!?;:।\"'()-"
                        if (!atWordEnd) sb.append('a')
                    }
                }
            } else {
                sb.append(devVowels[ch] ?: devMisc[ch] ?: ch)
            }
            i++
        }
        return sb.toString().replace("  ", " ").trim()
    }

    companion object {
        private val offlineEnToHi = mapOf(
            "quiet" to "शांत",
            "book" to "किताब",
            "books" to "किताबें",
            "story" to "कहानी",
            "stories" to "कहानियाँ",
            "smile" to "मुस्कान",
            "eyes" to "आँखें",
            "mind" to "मन",
            "calm" to "शांत",
            "beautiful" to "सुंदर",
            "precious" to "अनमोल",
            "page" to "पन्ना",
            "paper" to "कागज़",
            "reading" to "पढ़ना",
            "happy" to "खुश",
            "world" to "दुनिया",
            "moments" to "पल"
        )

        private val offlineHiToEn = mapOf(
            "किताब" to "book",
            "कहानी" to "story",
            "मुस्कान" to "smile",
            "शांत" to "quiet",
            "सुंदर" to "beautiful",
            "पन्ना" to "page",
            "दुनिया" to "world"
        )

        private val devVowels = mapOf(
            'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
            'ऋ' to "ri", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au"
        )

        private val devMatras = mapOf(
            'ा' to "a", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo",
            'ृ' to "ri", 'े' to "e", 'ै' to "ai", 'ो' to "o", 'ौ' to "au"
        )

        private val devConsonants = mapOf(
            'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "ng",
            'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "ny",
            'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
            'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
            'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
            'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v",
            'श' to "sh", 'ष' to "sh", 'स' to "s", 'ह' to "h",
            'क़' to "q", 'ख़' to "kh", 'ग़' to "gh", 'ज़' to "z", 'ड़' to "r", 'ढ़' to "rh", 'फ़' to "f"
        )

        private val devMisc = mapOf(
            'ं' to "n", 'ँ' to "n", 'ः' to "h", '़' to "", '।' to "."
        )
    }
}
