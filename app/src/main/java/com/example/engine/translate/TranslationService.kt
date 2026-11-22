package com.example.engine.translate

import com.example.data.local.TranslationDao
import com.example.data.model.TranslationEntryEntity
import com.example.data.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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

data class FullPageTranslationResult(
    val translatedText: String,
    val targetLanguage: TranslationLanguage,
    val sourceLanguage: String,
    val fromCache: Boolean,
    val usedGemini: Boolean,
    val errorHint: String? = null
)

/**
 * Lightweight zero-dependency 15-language Translator (including Arabic RTL)
 * with automatic source language detection, multi-paragraph chunked translation,
 * full-page translation support, and Room offline caching keyed by (text + targetLanguage).
 */
class TranslationService(private val translationDao: TranslationDao) {

    // In-memory LRU-like cache for full-page translations so toggling Show Original / returning is instant
    private val fullPageMemoryCache = object : LinkedHashMap<String, String>(24, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > 24
    }

    /**
     * Detects likely source language code via Unicode script blocks and light heuristics.
     * Distinguishes Arabic vs Urdu within the Arabic script range via Urdu-specific letters.
     */
    fun detectSourceLanguageCode(text: String): String {
        var hasArabicBlock = false
        var hasUrduSpecific = false
        for (ch in text) {
            val code = ch.code
            when (code) {
                in 0x0900..0x097F -> return "hi" // Devanagari (Hindi/Marathi)
                in 0x0600..0x06FF -> {
                    hasArabicBlock = true
                    if (ch in URDU_SPECIFIC_CHARS) hasUrduSpecific = true
                }
                in 0x0980..0x09FF -> return "bn" // Bengali
                in 0x0B80..0x0BFF -> return "ta" // Tamil
                in 0x0C00..0x0C7F -> return "te" // Telugu
                in 0x0A80..0x0AFF -> return "gu" // Gujarati
                in 0x0A00..0x0A7F -> return "pa" // Gurmukhi (Punjabi)
                in 0x3040..0x30FF, in 0x4E00..0x9FFF -> return "ja" // Japanese Kana / Kanji
            }
        }
        if (hasArabicBlock) return if (hasUrduSpecific) "ur" else "ar"
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
            TranslationLanguage.ARABIC -> detectedSrc == "ar"
            TranslationLanguage.URDU -> detectedSrc == "ur"
            else -> detectedSrc == preferredTarget.myMemoryCode
        }
        val effectiveTarget = if (sameAsPreferred && preferredTarget != TranslationLanguage.ENGLISH) {
            TranslationLanguage.ENGLISH
        } else {
            preferredTarget
        }
        return detectedSrc to effectiveTarget
    }

    /**
     * Translates a single word, multi-word selection, full sentence, or multi-paragraph selection
     * cleanly without truncating at 450 characters.
     */
    suspend fun translate(
        rawText: String,
        targetLanguage: TranslationLanguage,
        bookTitle: String,
        optionalGeminiApiKey: String,
        explicitTargetOverride: Boolean = false
    ): TranslationResult = withContext(Dispatchers.IO) {
        val cleaned = normalizePreservingParagraphs(rawText).take(3000)
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
                translateWithGemini(cleaned, effectiveTarget, trimmedKey, preserveParagraphs = cleaned.contains("\n"))
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

        // 3. Free lightweight MyMemory REST endpoint with smart chunking for longer passages
        val apiSource = if (detectedSrc == effectiveTarget.myMemoryCode) {
            if (effectiveTarget == TranslationLanguage.ENGLISH) "hi" else "en"
        } else {
            detectedSrc
        }
        val publicText = runCatching {
            translateLongTextWithMyMemory(cleaned, apiSource, effectiveTarget)
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

        // 4. Built-in offline mini-glossary fallback (Hindi / Hinglish / English / Arabic)
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

    /**
     * Translates an entire visible book page cleanly while preserving paragraph breaks.
     */
    suspend fun translateFullPage(
        pageText: String,
        targetLanguage: TranslationLanguage,
        optionalGeminiApiKey: String
    ): FullPageTranslationResult = withContext(Dispatchers.IO) {
        val cleaned = normalizePreservingParagraphs(pageText).take(5500)
        if (cleaned.isBlank()) {
            return@withContext FullPageTranslationResult(
                translatedText = "",
                targetLanguage = targetLanguage,
                sourceLanguage = "en",
                fromCache = false,
                usedGemini = false,
                errorHint = "This page has no selectable text to translate."
            )
        }

        val detectedSrc = detectSourceLanguageCode(cleaned)
        val memKey = "page|${targetLanguage.code}|${cleaned.hashCode()}|${cleaned.length}"
        synchronized(fullPageMemoryCache) {
            fullPageMemoryCache[memKey]
        }?.let { hit ->
            return@withContext FullPageTranslationResult(
                translatedText = hit,
                targetLanguage = targetLanguage,
                sourceLanguage = detectedSrc,
                fromCache = true,
                usedGemini = false
            )
        }

        // 1. Optional Gemini API key handles full pages smoothly in one coherent pass
        val trimmedKey = optionalGeminiApiKey.trim()
        if (trimmedKey.isNotEmpty()) {
            val geminiText = runCatching {
                translateWithGemini(cleaned, targetLanguage, trimmedKey, preserveParagraphs = true)
            }.getOrNull()
            if (!geminiText.isNullOrBlank()) {
                synchronized(fullPageMemoryCache) { fullPageMemoryCache[memKey] = geminiText }
                return@withContext FullPageTranslationResult(
                    translatedText = geminiText,
                    targetLanguage = targetLanguage,
                    sourceLanguage = detectedSrc,
                    fromCache = false,
                    usedGemini = true
                )
            }
        }

        // 2. Chunked MyMemory translation preserving paragraph structure
        val apiSource = if (detectedSrc == targetLanguage.myMemoryCode) {
            if (targetLanguage == TranslationLanguage.ENGLISH) "hi" else "en"
        } else {
            detectedSrc
        }
        val translated = runCatching {
            translateLongTextWithMyMemory(cleaned, apiSource, targetLanguage)
        }.getOrNull()

        if (!translated.isNullOrBlank()) {
            val formatted = if (targetLanguage == TranslationLanguage.HINGLISH) {
                transliterateDevanagariToLatin(translated)
            } else {
                translated
            }
            synchronized(fullPageMemoryCache) { fullPageMemoryCache[memKey] = formatted }
            return@withContext FullPageTranslationResult(
                translatedText = formatted,
                targetLanguage = targetLanguage,
                sourceLanguage = detectedSrc,
                fromCache = false,
                usedGemini = false
            )
        }

        FullPageTranslationResult(
            translatedText = "",
            targetLanguage = targetLanguage,
            sourceLanguage = detectedSrc,
            fromCache = false,
            usedGemini = false,
            errorHint = "Unable to translate page while offline. Connect briefly and try again."
        )
    }

    private fun normalizePreservingParagraphs(raw: String): String {
        return raw.replace("\r\n", "\n")
            .replace("\r", "\n")
            .split(Regex("\n\\s*\n"))
            .map { para -> para.trim().replace(Regex("[ \\t\\n]+"), " ") }
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .trim()
    }

    /**
     * Splits multi-paragraph or long passages into <= 340-char sentence-aligned chunks and translates
     * them in bounded parallel batches (3 at a time) under a strict 18s overall deadline so full-page
     * translation resolves rapidly and never hangs on an infinite loading spinner.
     */
    private suspend fun translateLongTextWithMyMemory(
        text: String,
        sourceCode: String,
        target: TranslationLanguage
    ): String? {
        val paragraphs = text.split("\n\n").filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return null

        // Flatten into indexed (paraIndex, chunkText) jobs while capping network calls at 12 chunks
        data class ChunkJob(val paraIdx: Int, val text: String)
        val jobs = mutableListOf<ChunkJob>()
        paragraphs.forEachIndexed { paraIdx, para ->
            val chunks = splitIntoChunks(para, maxChunkChars = 340)
            for (c in chunks) {
                jobs.add(ChunkJob(paraIdx, c))
            }
        }

        val networkJobs = jobs.take(12)
        val overflowJobs = if (jobs.size > 12) jobs.drop(12) else emptyList()
        val results = arrayOfNulls<String>(networkJobs.size)
        var anySucceeded = false

        // Bounded overall timeout so even degraded networks exit cleanly
        withTimeoutOrNull(18_000L) {
            coroutineScope {
                var idx = 0
                while (idx < networkJobs.size) {
                    currentCoroutineContext().ensureActive()
                    val batchEnd = (idx + 3).coerceAtMost(networkJobs.size)
                    val deferred = (idx until batchEnd).map { jobIdx ->
                        async(Dispatchers.IO) {
                            val translated = runCatching {
                                translateWithMyMemory(networkJobs[jobIdx].text, sourceCode, target)
                            }.getOrNull()
                            jobIdx to translated
                        }
                    }
                    val batchOut = deferred.awaitAll()
                    var batchHadSuccess = false
                    for ((jobIdx, translated) in batchOut) {
                        if (!translated.isNullOrBlank()) {
                            results[jobIdx] = translated.trim()
                            anySucceeded = true
                            batchHadSuccess = true
                        }
                    }
                    // Fail fast if first batch completely failed (e.g., offline / rate-limited)
                    if (idx == 0 && !batchHadSuccess) break
                    idx = batchEnd
                }
            }
        }

        if (!anySucceeded) return null

        val grouped = Array(paragraphs.size) { mutableListOf<String>() }
        networkJobs.forEachIndexed { i, job ->
            grouped[job.paraIdx].add(results[i] ?: job.text)
        }
        overflowJobs.forEach { job ->
            grouped[job.paraIdx].add(job.text)
        }
        return grouped
            .map { it.joinToString(" ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    private fun splitIntoChunks(paragraph: String, maxChunkChars: Int): List<String> {
        if (paragraph.length <= maxChunkChars) return listOf(paragraph)
        val result = mutableListOf<String>()
        val sentences = paragraph.split(Regex("(?<=[.!?।؟])\\s+"))
        val current = StringBuilder()

        for (sentence in sentences) {
            if (sentence.length > maxChunkChars) {
                if (current.isNotEmpty()) {
                    result.add(current.toString().trim())
                    current.clear()
                }
                // Word-wrap very long sentences cleanly
                val words = sentence.split(" ")
                val sub = StringBuilder()
                for (w in words) {
                    if (sub.length + w.length + 1 > maxChunkChars && sub.isNotEmpty()) {
                        result.add(sub.toString().trim())
                        sub.clear()
                    }
                    if (sub.isNotEmpty()) sub.append(' ')
                    sub.append(w)
                }
                if (sub.isNotEmpty()) result.add(sub.toString().trim())
            } else {
                if (current.length + sentence.length + 1 > maxChunkChars && current.isNotEmpty()) {
                    result.add(current.toString().trim())
                    current.clear()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(sentence)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString().trim())
        return result
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
            connectTimeout = 4500
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode !in 200..299) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val data = json.optJSONObject("responseData")
            val translated = data?.optString("translatedText")?.takeIf { it.isNotBlank() } ?: return null
            // Avoid returning MyMemory's error banner as a translation
            if (translated.startsWith("MYMEMORY WARNING", ignoreCase = true) ||
                translated.startsWith("QUERY LENGTH LIMIT", ignoreCase = true)
            ) {
                null
            } else {
                translated
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun translateWithGemini(
        text: String,
        target: TranslationLanguage,
        apiKey: String,
        preserveParagraphs: Boolean = false
    ): String? {
        val instruction = if (preserveParagraphs) {
            "Translate the following book passage naturally and accurately into ${target.promptName}. Preserve paragraph breaks. Output ONLY the translation."
        } else {
            "Translate the following text naturally and concisely into ${target.promptName}. Output ONLY the translation."
        }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8500
            readTimeout = 14000
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
            TranslationLanguage.ARABIC -> offlineEnToAr[lower]
            TranslationLanguage.ENGLISH -> offlineHiToEn[lower] ?: offlineArToEn[lower]
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
        // Urdu-specific characters not used in standard Modern Arabic
        private val URDU_SPECIFIC_CHARS = setOf('ٹ', 'ڈ', 'ڑ', 'ں', 'ے', 'ہ', 'ھ', 'پ', 'چ', 'ژ', 'گ')

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

        private val offlineEnToAr = mapOf(
            "quiet" to "هادئ",
            "book" to "كتاب",
            "books" to "كتب",
            "story" to "قصة",
            "stories" to "قصص",
            "smile" to "ابتسامة",
            "eyes" to "عيون",
            "mind" to "عقل",
            "calm" to "هدوء",
            "beautiful" to "جميل",
            "precious" to "ثمين",
            "page" to "صفحة",
            "paper" to "ورق",
            "reading" to "قراءة",
            "happy" to "سعيد",
            "world" to "عالم",
            "moments" to "لحظات"
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

        private val offlineArToEn = mapOf(
            "كتاب" to "book",
            "قصة" to "story",
            "صفحة" to "page",
            "قراءة" to "reading",
            "هادئ" to "quiet",
            "جميل" to "beautiful",
            "عالم" to "world"
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
