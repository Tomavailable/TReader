package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object DocumentParser {

    data class ParsedDocument(
        val title: String,
        val text: String,
        val format: String
    )

    /**
     * Parses TXT, SRT, VTT, LRC, EPUB directly from Android Uri
     */
    fun parseUri(context: Context, uri: Uri): ParsedDocument {
        var fileName = "导入文档.txt"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    val name = it.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }
        }

        val ext = fileName.substringAfterLast('.', "").lowercase()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            return when (ext) {
                "epub" -> {
                    val epubText = parseEpub(bytes)
                    ParsedDocument(fileName, epubText, "EPUB")
                }
                "srt" -> {
                    val srtText = parseSrt(decodeBytes(bytes))
                    ParsedDocument(fileName, srtText, "SRT")
                }
                "vtt" -> {
                    val vttText = parseVtt(decodeBytes(bytes))
                    ParsedDocument(fileName, vttText, "VTT")
                }
                "lrc" -> {
                    val lrcText = parseLrc(decodeBytes(bytes))
                    ParsedDocument(fileName, lrcText, "LRC")
                }
                else -> {
                    // Default plain text (TXT, MD, etc.)
                    val txt = decodeBytes(bytes)
                    ParsedDocument(fileName, txt, "TXT")
                }
            }
        }

        throw IllegalArgumentException("无法读取该文件内容")
    }

    /**
     * Robust charset decoding with UTF-8 and GBK fallback
     */
    private fun decodeBytes(bytes: ByteArray): String {
        var text = String(bytes, Charsets.UTF_8)
        if (text.contains("\uFFFD")) {
            try {
                val gbkText = String(bytes, Charset.forName("GBK"))
                if (!gbkText.contains("\uFFFD")) {
                    text = gbkText
                }
            } catch (_: Exception) {}
        }
        return text
    }

    /**
     * Parses SubRip (.srt) subtitle files:
     * Strips index numbers, timestamps (00:01:20,000 --> 00:01:23,000), HTML tags.
     */
    fun parseSrt(content: String): String {
        val lines = content.lines()
        val result = StringBuilder()
        var lastLine = ""

        val timestampRegex = Regex("""^\d{1,2}:\d{2}:\d{2}[,\.]\d{3}\s*-->\s*\d{1,2}:\d{2}:\d{2}[,\.]\d{3}""")
        val htmlTagRegex = Regex("""<[^>]+>""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // Skip purely numeric index lines
            if (trimmed.all { it.isDigit() }) continue
            // Skip timestamp lines
            if (timestampRegex.containsMatchIn(trimmed)) continue
            if (trimmed.contains("-->")) continue

            // Strip HTML/formatting tags like <i>, <b>, <font color="...">
            val clean = trimmed.replace(htmlTagRegex, "").trim()
            if (clean.isNotEmpty() && clean != lastLine) {
                result.append(clean).append("\n")
                lastLine = clean
            }
        }
        return result.toString().trim()
    }

    /**
     * Parses WebVTT (.vtt) subtitle files
     */
    fun parseVtt(content: String): String {
        val lines = content.lines()
        val result = StringBuilder()
        val timestampRegex = Regex("""(?:\d{2}:)?\d{2}:\d{2}\.\d{3}\s*-->\s*(?:\d{2}:)?\d{2}:\d{2}\.\d{3}""")
        val htmlTagRegex = Regex("""<[^>]+>""")
        var lastLine = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE") || trimmed.startsWith("STYLE")) continue
            if (trimmed.all { it.isDigit() }) continue
            if (timestampRegex.containsMatchIn(trimmed)) continue
            if (trimmed.contains("-->")) continue

            val clean = trimmed.replace(htmlTagRegex, "").trim()
            if (clean.isNotEmpty() && clean != lastLine) {
                result.append(clean).append("\n")
                lastLine = clean
            }
        }
        return result.toString().trim()
    }

    /**
     * Parses Lyric (.lrc) files by stripping [mm:ss.xx] timestamps
     */
    fun parseLrc(content: String): String {
        val lines = content.lines()
        val result = StringBuilder()
        val lrcRegex = Regex("""\[\d{2}:\d{2}(?:\.\d{2,3})?\]""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // Skip header tags like [ti:Title], [ar:Artist], etc.
            if (trimmed.matches(Regex("""^\[[a-zA-Z]+:.*\]$"""))) continue

            val clean = trimmed.replace(lrcRegex, "").trim()
            if (clean.isNotEmpty()) {
                result.append(clean).append("\n")
            }
        }
        return result.toString().trim()
    }

    /**
     * Lightweight zero-dependency EPUB Parser:
     * 1. Finds OPF manifest path in META-INF/container.xml
     * 2. Reads spine item order
     * 3. Extracts text from XHTML/HTML chapters stripping markup
     */
    fun parseEpub(bytes: ByteArray): String {
        val entryMap = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entryMap[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (entryMap.isEmpty()) return ""

        // 1. Locate root opf file from container.xml
        val containerBytes = entryMap["META-INF/container.xml"]
        var opfPath = ""
        if (containerBytes != null) {
            val xml = String(containerBytes, Charsets.UTF_8)
            val fullPathRegex = Regex("""full-path\s*=\s*["']([^"']+)["']""")
            val match = fullPathRegex.find(xml)
            if (match != null) {
                opfPath = match.groupValues[1]
            }
        }

        if (opfPath.isEmpty()) {
            // Fallback search for any .opf entry
            opfPath = entryMap.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) } ?: ""
        }

        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
        val opfBytes = entryMap[opfPath]

        val orderedFiles = mutableListOf<String>()

        if (opfBytes != null) {
            try {
                val manifestItems = mutableMapOf<String, String>() // id -> href
                val spineIds = mutableListOf<String>()

                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(opfBytes), "UTF-8")

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name.lowercase()) {
                            "item" -> {
                                val id = parser.getAttributeValue(null, "id") ?: ""
                                val href = parser.getAttributeValue(null, "href") ?: ""
                                if (id.isNotEmpty() && href.isNotEmpty()) {
                                    manifestItems[id] = href
                                }
                            }
                            "itemref" -> {
                                val idref = parser.getAttributeValue(null, "idref") ?: ""
                                if (idref.isNotEmpty()) {
                                    spineIds.add(idref)
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                // Gather ordered hrefs
                for (idref in spineIds) {
                    val href = manifestItems[idref]
                    if (href != null) {
                        val fullPath = if (href.startsWith("/")) href.substring(1) else opfDir + href
                        // Resolve relative ../
                        val normalized = normalizePath(fullPath)
                        orderedFiles.add(normalized)
                    }
                }
            } catch (_: Exception) {
                // Ignore parse errors, fallback below
            }
        }

        // If no spine was resolved, grab all html/xhtml files
        if (orderedFiles.isEmpty()) {
            orderedFiles.addAll(
                entryMap.keys.filter { it.endsWith(".html", true) || it.endsWith(".xhtml", true) || it.endsWith(".htm", true) }.sorted()
            )
        }

        val result = StringBuilder()
        val htmlTagRegex = Regex("""<[^>]+>""")

        for (filePath in orderedFiles) {
            val chapterBytes = entryMap[filePath] ?: continue
            val chapterHtml = decodeBytes(chapterBytes)
            // Strip styles and scripts first
            val noStyle = chapterHtml.replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")

            // Replace block tags with newlines
            val withBreaks = noStyle
                .replace(Regex("""<(p|div|h[1-6]|li|tr|br)[^>]*>""", RegexOption.IGNORE_CASE), "\n")

            // Strip remaining tags
            val rawText = withBreaks.replace(htmlTagRegex, " ")

            // Unescape common HTML entities
            val cleanText = unescapeHtml(rawText).lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")

            if (cleanText.isNotBlank()) {
                result.append(cleanText).append("\n\n")
            }
        }

        return result.toString().trim()
    }

    private fun normalizePath(path: String): String {
        val parts = path.split("/")
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else if (part != "." && part.isNotEmpty()) {
                stack.add(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&hellip;", "…")
    }
}
