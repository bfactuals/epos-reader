package com.example.engine.parser

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.config.WelcomeGuideBook
import com.example.data.local.BookChapter
import com.example.data.model.BookFormat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.nio.charset.Charset
import java.util.zip.Inflater
import java.util.zip.ZipFile

sealed class ParsedBook {
    abstract val title: String
    abstract val author: String

    data class TextBook(
        override val title: String,
        override val author: String,
        val chapters: List<BookChapter>
    ) : ParsedBook()

    data class PdfBook(
        override val title: String,
        override val author: String,
        val filePath: String,
        val pageCount: Int,
        val pageTexts: List<String>
    ) : ParsedBook()
}

data class ImportedMetadata(
    val title: String,
    val author: String,
    val totalPagesEstimate: Int,
    val coverImagePath: String?
)

object BookDocumentParser {

    fun loadBook(filePath: String, format: BookFormat, fallbackTitle: String, fallbackAuthor: String): ParsedBook {
        if (filePath == WelcomeGuideBook.FILE_PATH) {
            return ParsedBook.TextBook(
                title = WelcomeGuideBook.TITLE,
                author = WelcomeGuideBook.AUTHOR,
                chapters = WelcomeGuideBook.CHAPTERS
            )
        }
        val file = File(filePath)
        if (!file.exists()) {
            return ParsedBook.TextBook(
                title = fallbackTitle,
                author = fallbackAuthor,
                chapters = listOf(
                    BookChapter(
                        title = fallbackTitle,
                        paragraphs = listOf("This file could not be found in offline storage. Try importing it again.")
                    )
                )
            )
        }
        return when (format) {
            BookFormat.EPUB -> parseEpub(file, fallbackTitle, fallbackAuthor)
            BookFormat.TXT -> parseTxt(file, fallbackTitle, fallbackAuthor)
            BookFormat.PDF -> parsePdf(file, fallbackTitle, fallbackAuthor)
        }
    }

    fun inspectImportedFile(
        file: File,
        format: BookFormat,
        fallbackTitle: String,
        coverOutputDir: File
    ): ImportedMetadata {
        return when (format) {
            BookFormat.EPUB -> inspectEpub(file, fallbackTitle, coverOutputDir)
            BookFormat.PDF -> {
                val count = runCatching {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { it.pageCount }
                    }
                }.getOrDefault(1).coerceAtLeast(1)
                ImportedMetadata(
                    title = fallbackTitle,
                    author = "PDF Document",
                    totalPagesEstimate = count,
                    coverImagePath = null
                )
            }
            BookFormat.TXT -> {
                val parsed = parseTxt(file, fallbackTitle, "Plain Text")
                val charCount = parsed.chapters.sumOf { c -> c.paragraphs.sumOf { it.length } }
                val estPages = (charCount / 1450).coerceAtLeast(1)
                ImportedMetadata(
                    title = parsed.title,
                    author = parsed.author,
                    totalPagesEstimate = estPages,
                    coverImagePath = null
                )
            }
        }
    }

    private fun inspectEpub(file: File, fallbackTitle: String, coverOutputDir: File): ImportedMetadata {
        var title = fallbackTitle
        var author = "Unknown Author"
        var coverPath: String? = null
        var estPages = 10

        runCatching {
            ZipFile(file).use { zip ->
                val opfPath = findOpfPath(zip)
                if (opfPath != null) {
                    val opfEntry = zip.getEntry(opfPath)
                    val opfDir = opfPath.substringBeforeLast('/', "")
                    if (opfEntry != null) {
                        val opfXml = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                        val opf = parseOpf(opfXml)
                        if (opf.title.isNotBlank()) title = opf.title
                        if (opf.author.isNotBlank()) author = opf.author
                        estPages = (opf.spineIds.size * 4).coerceAtLeast(1)

                        val coverHref = opf.coverHref
                        if (coverHref != null) {
                            val resolved = resolveZipPath(opfDir, coverHref)
                            val coverEntry = zip.getEntry(resolved)
                            if (coverEntry != null) {
                                coverOutputDir.mkdirs()
                                val ext = resolved.substringAfterLast('.', "jpg").lowercase()
                                val outFile = File(coverOutputDir, "cover_${System.currentTimeMillis()}.$ext")
                                zip.getInputStream(coverEntry).use { input ->
                                    outFile.outputStream().use { output -> input.copyTo(output) }
                                }
                                coverPath = outFile.absolutePath
                            }
                        }
                    }
                }
            }
        }

        return ImportedMetadata(
            title = title,
            author = author,
            totalPagesEstimate = estPages,
            coverImagePath = coverPath
        )
    }

    private data class OpfInfo(
        val title: String,
        val author: String,
        val manifest: Map<String, String>, // id -> href
        val spineIds: List<String>,
        val ncxHref: String?,
        val navHref: String?,
        val coverHref: String?
    )

    private fun findOpfPath(zip: ZipFile): String? {
        val container = zip.getEntry("META-INF/container.xml")
        if (container != null) {
            val xml = zip.getInputStream(container).bufferedReader().use { it.readText() }
            val match = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(xml)
            if (match != null) return match.groupValues[1]
        }
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val e = entries.nextElement()
            if (e.name.endsWith(".opf", ignoreCase = true)) return e.name
        }
        return null
    }

    private fun parseOpf(xml: String): OpfInfo {
        var title = ""
        var author = ""
        val manifest = LinkedHashMap<String, String>()
        val spineIds = mutableListOf<String>()
        var ncxHref: String? = null
        var navHref: String? = null
        var coverId: String? = null
        var coverHref: String? = null

        runCatching {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val tag = parser.name.substringAfter(':').lowercase()
                    when (tag) {
                        "title" -> if (title.isBlank()) title = parser.nextText().trim()
                        "creator" -> if (author.isBlank()) author = parser.nextText().trim()
                        "meta" -> {
                            val name = parser.getAttributeValue(null, "name")
                            val content = parser.getAttributeValue(null, "content")
                            if (name.equals("cover", ignoreCase = true) && !content.isNullOrBlank()) {
                                coverId = content
                            }
                        }
                        "item" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                            val props = parser.getAttributeValue(null, "properties") ?: ""
                            if (id.isNotBlank() && href.isNotBlank()) {
                                manifest[id] = href
                            }
                            if (mediaType.contains("dtbncx", ignoreCase = true) || href.endsWith(".ncx", true)) {
                                ncxHref = href
                            }
                            if (props.contains("nav", ignoreCase = true)) {
                                navHref = href
                            }
                            if (props.contains("cover-image", ignoreCase = true)) {
                                coverHref = href
                            }
                        }
                        "itemref" -> {
                            val idref = parser.getAttributeValue(null, "idref")
                            if (!idref.isNullOrBlank()) spineIds.add(idref)
                        }
                    }
                }
                event = parser.next()
            }
        }

        if (coverHref == null && coverId != null) {
            coverHref = manifest[coverId]
        }
        if (coverHref == null) {
            coverHref = manifest.entries.firstOrNull { (id, href) ->
                (id.contains("cover", true) || href.contains("cover", true)) &&
                    (href.endsWith(".jpg", true) || href.endsWith(".jpeg", true) || href.endsWith(".png", true))
            }?.value
        }

        return OpfInfo(title, author, manifest, spineIds, ncxHref, navHref, coverHref)
    }

    private fun parseEpub(file: File, fallbackTitle: String, fallbackAuthor: String): ParsedBook.TextBook {
        var title = fallbackTitle
        var author = fallbackAuthor
        val chapters = mutableListOf<BookChapter>()

        runCatching {
            ZipFile(file).use { zip ->
                val opfPath = findOpfPath(zip)
                val opfDir = opfPath?.substringBeforeLast('/', "") ?: ""
                val opfEntry = opfPath?.let { zip.getEntry(it) }

                if (opfEntry != null) {
                    val opfXml = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                    val opf = parseOpf(opfXml)
                    if (opf.title.isNotBlank()) title = opf.title
                    if (opf.author.isNotBlank()) author = opf.author

                    val tocMap = buildTocTitleMap(zip, opfDir, opf.ncxHref, opf.navHref)

                    opf.spineIds.forEachIndexed { idx, idref ->
                        val href = opf.manifest[idref] ?: return@forEachIndexed
                        val fullPath = resolveZipPath(opfDir, href)
                        val entry = zip.getEntry(fullPath) ?: return@forEachIndexed
                        val html = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                        val (heading, paragraphs) = extractHtmlChapter(html)
                        if (paragraphs.isNotEmpty()) {
                            val cleanHref = href.substringBefore('#').substringAfterLast('/')
                            val chapterTitle = tocMap[cleanHref]
                                ?: heading
                                ?: "Section ${idx + 1}"
                            chapters.add(BookChapter(chapterTitle, paragraphs))
                        }
                    }
                }

                // Fallback if spine was empty
                if (chapters.isEmpty()) {
                    val htmlEntries = zip.entries().toList()
                        .filter { !it.isDirectory && (it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) || it.name.endsWith(".htm", true)) }
                        .sortedBy { it.name }
                    htmlEntries.forEachIndexed { idx, entry ->
                        val html = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                        val (heading, paragraphs) = extractHtmlChapter(html)
                        if (paragraphs.isNotEmpty()) {
                            chapters.add(BookChapter(heading ?: "Section ${idx + 1}", paragraphs))
                        }
                    }
                }
            }
        }

        if (chapters.isEmpty()) {
            chapters.add(
                BookChapter(
                    title = title,
                    paragraphs = listOf("Unable to extract readable text from this EPUB archive.")
                )
            )
        }

        return ParsedBook.TextBook(title = title, author = author, chapters = chapters)
    }

    private fun buildTocTitleMap(
        zip: ZipFile,
        opfDir: String,
        ncxHref: String?,
        navHref: String?
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (ncxHref != null) {
            val entry = zip.getEntry(resolveZipPath(opfDir, ncxHref))
            if (entry != null) {
                val ncxXml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val navPointRegex = Regex(
                    """<navLabel>.*?<text>(.*?)</text>.*?</navLabel>.*?<content[^>]+src=["']([^"']+)["']""",
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
                )
                navPointRegex.findAll(ncxXml).forEach { m ->
                    val label = decodeHtmlEntities(m.groupValues[1].trim())
                    val src = m.groupValues[2].substringBefore('#').substringAfterLast('/')
                    if (label.isNotBlank() && src.isNotBlank() && !map.containsKey(src)) {
                        map[src] = label
                    }
                }
            }
        }
        if (map.isEmpty() && navHref != null) {
            val entry = zip.getEntry(resolveZipPath(opfDir, navHref))
            if (entry != null) {
                val navHtml = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                val linkRegex = Regex(
                    """<a[^>]+href=["']([^"']+)["'][^>]*>(.*?)</a>""",
                    setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
                )
                linkRegex.findAll(navHtml).forEach { m ->
                    val src = m.groupValues[1].substringBefore('#').substringAfterLast('/')
                    val label = decodeHtmlEntities(m.groupValues[2].replace(Regex("<[^>]+>"), "").trim())
                    if (label.isNotBlank() && src.isNotBlank() && !map.containsKey(src)) {
                        map[src] = label
                    }
                }
            }
        }
        return map
    }

    private fun resolveZipPath(baseDir: String, relativeHref: String): String {
        val cleanHref = relativeHref.substringBefore('#').removePrefix("./")
        if (baseDir.isBlank()) return cleanHref
        val combined = "$baseDir/$cleanHref"
        val parts = ArrayDeque<String>()
        combined.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(segment)
            }
        }
        return parts.joinToString("/")
    }

    private fun extractHtmlChapter(rawHtml: String): Pair<String?, List<String>> {
        val body = Regex("""<body[^>]*>(.*)</body>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(rawHtml)?.groupValues?.get(1) ?: rawHtml

        val cleaned = body
            .replace(Regex("""<script[^>]*>.*?</script>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("""<style[^>]*>.*?</style>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)), "")
            .replace(Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL), "")

        var heading: String? = null
        val headingMatch = Regex("""<h[1-3][^>]*>(.*?)</h[1-3]>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(cleaned)
        if (headingMatch != null) {
            val h = decodeHtmlEntities(headingMatch.groupValues[1].replace(Regex("<[^>]+>"), " "))
                .replace(Regex("\\s+"), " ").trim()
            if (h.length in 1..120) heading = h
        }

        // Convert block endings into paragraph markers
        val marked = cleaned
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("""</(p|div|h[1-6]|blockquote|li|tr|section|article)>""", RegexOption.IGNORE_CASE), "\n\n")
            .replace(Regex("""<[^>]+>"""), "")

        val decoded = decodeHtmlEntities(marked)
        val paragraphs = decoded
            .split(Regex("""\n\s*\n+"""))
            .map { it.replace(Regex("""[ \t\r\n]+"""), " ").trim() }
            .filter { it.isNotBlank() }
            .let { list ->
                // Avoid repeating the chapter title as the very first paragraph if identical
                if (heading != null && list.isNotEmpty() && list.first().equals(heading, ignoreCase = true)) {
                    list.drop(1)
                } else {
                    list
                }
            }

        return heading to paragraphs
    }

    private fun decodeHtmlEntities(input: String): String {
        var result = input
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")

        result = Regex("""&#(\d+);""").replace(result) { m ->
            m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: m.value
        }
        result = Regex("""&#x([0-9a-fA-F]+);""").replace(result) { m ->
            m.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: m.value
        }
        return result
    }

    private fun parseTxt(file: File, fallbackTitle: String, fallbackAuthor: String): ParsedBook.TextBook {
        val bytes = file.readBytes()
        val charset = when {
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16LE
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16BE
            else -> Charset.forName("UTF-8")
        }
        val raw = String(bytes, charset).removePrefix("\uFEFF").replace("\r\n", "\n").replace("\r", "\n")

        val blocks = raw.split(Regex("""\n\s*\n+"""))
            .map { block ->
                block.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString(" ")
            }
            .filter { it.isNotBlank() }

        val chapterRegex = Regex(
            """^(chapter\s+[\divxlc]+.*|part\s+[\divxlc]+.*|book\s+[\divxlc]+.*|prologue|epilogue|[ivxlc]+\.\s+.{2,60})$""",
            RegexOption.IGNORE_CASE
        )

        val chapters = mutableListOf<BookChapter>()
        var currentTitle = fallbackTitle
        var currentParagraphs = mutableListOf<String>()

        for (block in blocks) {
            if (block.length <= 85 && chapterRegex.matches(block)) {
                if (currentParagraphs.isNotEmpty()) {
                    chapters.add(BookChapter(currentTitle, currentParagraphs))
                    currentParagraphs = mutableListOf()
                }
                currentTitle = block
            } else {
                currentParagraphs.add(block)
            }
        }
        if (currentParagraphs.isNotEmpty()) {
            chapters.add(BookChapter(currentTitle, currentParagraphs))
        }
        if (chapters.isEmpty()) {
            chapters.add(BookChapter(fallbackTitle, listOf("This text file appears to be empty.")))
        }

        return ParsedBook.TextBook(title = fallbackTitle, author = fallbackAuthor, chapters = chapters)
    }

    /**
     * Opens PDF using Android's zero-bloat native PdfRenderer instantly (<15ms even for 1000+ pages).
     * Text extraction is deferred on-demand so large PDFs never hang on a loading spinner.
     */
    private fun parsePdf(file: File, fallbackTitle: String, fallbackAuthor: String): ParsedBook.PdfBook {
        val pageCount = runCatching {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            }
        }.getOrDefault(1).coerceAtLeast(1)

        return ParsedBook.PdfBook(
            title = fallbackTitle,
            author = fallbackAuthor,
            filePath = file.absolutePath,
            pageCount = pageCount,
            pageTexts = List(pageCount) { "" }
        )
    }

    /**
     * On-demand PDF text extraction (invoked lazily only when Search, TTS, or Full-Page Translate is used).
     * Safely capped to <= 14 MB files so huge image-heavy PDFs never cause OOM or UI freezes.
     */
    fun extractPdfTextsOnDemand(filePath: String, pageCount: Int): List<String> {
        val file = File(filePath)
        if (!file.exists() || file.length() > 14L * 1024L * 1024L) {
            return List(pageCount.coerceAtLeast(1)) { "" }
        }
        return extractPdfTextsLightweight(file, pageCount.coerceAtLeast(1))
    }

    /**
     * Lightweight pure-Kotlin PDF stream scanner (supports uncompressed & FlateDecode content streams).
     * Maps extracted BT..ET text blocks across the PDF's pages so search & highlighting work natively.
     */
    private fun extractPdfTextsLightweight(file: File, pageCount: Int): List<String> {
        return runCatching {
            val bytes = file.readBytes()
            val latin = String(bytes, Charsets.ISO_8859_1)
            val extractedChunks = mutableListOf<String>()

            val streamRegex = Regex("""<<([^>]{0,2000})>>\s*stream\r?\n""")
            var searchIndex = 0
            while (searchIndex < latin.length) {
                val match = streamRegex.find(latin, searchIndex) ?: break
                val dict = match.groupValues[1]
                val streamStart = match.range.last + 1
                val endIdx = latin.indexOf("endstream", streamStart)
                if (endIdx < 0) break
                searchIndex = endIdx + 9

                // Skip image XObjects & embedded fonts
                if (dict.contains("/Image") || dict.contains("/FontFile") || dict.contains("/DCTDecode")) {
                    continue
                }

                val rawSlice = bytes.copyOfRange(streamStart, endIdx.coerceAtLeast(streamStart))
                val content = if (dict.contains("/FlateDecode")) {
                    inflateSafe(rawSlice)
                } else {
                    rawSlice
                } ?: continue

                val textStr = String(content, Charsets.ISO_8859_1)
                if (textStr.contains("BT") && (textStr.contains("Tj") || textStr.contains("TJ"))) {
                    val pageText = parsePdfTextOperators(textStr)
                    if (pageText.length >= 6) {
                        extractedChunks.add(pageText)
                    }
                }
            }

            if (extractedChunks.isEmpty()) {
                List(pageCount) { "" }
            } else if (extractedChunks.size == pageCount) {
                extractedChunks
            } else {
                // Distribute extracted chunks evenly across pageCount
                val result = MutableList(pageCount) { "" }
                extractedChunks.forEachIndexed { idx, chunk ->
                    val targetPage = ((idx.toFloat() / extractedChunks.size) * pageCount)
                        .toInt().coerceIn(0, pageCount - 1)
                    result[targetPage] = if (result[targetPage].isBlank()) chunk else "${result[targetPage]}\n$chunk"
                }
                result
            }
        }.getOrElse { List(pageCount) { "" } }
    }

    private fun inflateSafe(input: ByteArray): ByteArray? {
        return runCatching {
            val inflater = Inflater()
            inflater.setInput(input)
            val out = ByteArrayOutputStream()
            val buf = ByteArray(4096)
            var loops = 0
            while (!inflater.finished() && loops < 1024) {
                val count = inflater.inflate(buf)
                if (count == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) break
                } else {
                    out.write(buf, 0, count)
                }
                loops++
            }
            inflater.end()
            out.toByteArray()
        }.getOrNull()
    }

    private fun parsePdfTextOperators(stream: String): String {
        val sb = StringBuilder()
        val btRegex = Regex("""BT(.*?)ET""", RegexOption.DOT_MATCHES_ALL)
        btRegex.findAll(stream).forEach { block ->
            val content = block.groupValues[1]
            // Literal strings (...) followed by Tj or inside [...] TJ
            var i = 0
            while (i < content.length) {
                if (content[i] == '(') {
                    var depth = 1
                    i++
                    val token = StringBuilder()
                    while (i < content.length && depth > 0) {
                        val c = content[i]
                        when {
                            c == '\\' && i + 1 < content.length -> {
                                i++
                                when (val esc = content[i]) {
                                    'n' -> token.append('\n')
                                    'r' -> token.append('\r')
                                    't' -> token.append('\t')
                                    '(', ')', '\\' -> token.append(esc)
                                    else -> token.append(esc)
                                }
                            }
                            c == '(' -> {
                                depth++
                                token.append(c)
                            }
                            c == ')' -> {
                                depth--
                                if (depth > 0) token.append(c)
                            }
                            else -> {
                                if (c.code in 32..126 || c.code in 160..255) token.append(c)
                            }
                        }
                        i++
                    }
                    if (token.isNotEmpty()) {
                        sb.append(token)
                    }
                } else if (content[i] == 'T' && i + 1 < content.length && (content[i + 1] == 'd' || content[i + 1] == 'D' || content[i + 1] == '*')) {
                    if (sb.isNotEmpty() && sb.last() != ' ' && sb.last() != '\n') sb.append(' ')
                    i += 2
                } else {
                    i++
                }
            }
            if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
        }
        return sb.toString()
            .replace(Regex("""[ \t]{2,}"""), " ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }
}
