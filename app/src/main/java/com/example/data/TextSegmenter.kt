package com.example.data

import java.text.BreakIterator
import java.util.Locale

enum class SplitMode(val title: String, val description: String) {
    BREAK_ITERATOR(
        title = "系统国际断句 (默认)",
        description = "Java/Android 原生语言学断句算法，已全面加入规避规则：完美避开小数点（3.14）、网址（open.ai）、英文缩写（Dr. Smith）及单换行符。"
    ),
    FULL_SENTENCE(
        title = "长句模式 (依据句号等标点断句)",
        description = "保持长句完整，仅在句号、问号、感叹号（.?!。？！…）或双换行段落处断句；保留句中所有逗号，自动平滑单换行符使显示连贯。"
    )
}

object TextSegmenter {

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "vs", "etc", "e.g", "i.e",
        "st", "co", "inc", "ltd", "jan", "feb", "mar", "apr", "aug", "sept", "oct",
        "nov", "dec", "no", "vol", "pp", "p", "u.s", "u.s.a", "u.k", "a.m", "p.m"
    )

    /**
     * Splits raw text into sentences based on the chosen [splitMode] and optional [isSplitEnabled].
     */
    fun splitIntoSentences(
        rawText: String,
        splitMode: SplitMode = SplitMode.BREAK_ITERATOR,
        isSplitEnabled: Boolean = false
    ): List<String> {
        if (rawText.isBlank()) return emptyList()

        val baseSentences = when (splitMode) {
            SplitMode.BREAK_ITERATOR -> splitWithBreakIterator(rawText)
            SplitMode.FULL_SENTENCE -> splitWithLongSentenceParser(rawText)
        }

        return if (isSplitEnabled) {
            applySecondarySplitIfNeeded(baseSentences)
        } else {
            baseSentences
        }
    }

    /**
     * Overload for backwards compatibility
     */
    fun splitIntoSentences(
        rawText: String,
        splitComma: Boolean,
        splitMode: SplitMode = SplitMode.BREAK_ITERATOR
    ): List<String> {
        return splitIntoSentences(rawText, splitMode, isSplitEnabled = splitComma)
    }

    /**
     * Secondary splitting on long sentences when [isSplitEnabled] is true.
     * Rules:
     * - Secondary punctuation marks: ，, ；; ：: 、 —
     * - Case 1: Length <= 80
     *   - If punctuation exists in character index range 20..59 (21st to 60th character),
     *     split into 2 parts at the punctuation closest to middle (length / 2).
     *   - Otherwise, do NOT split.
     * - Case 2: 80 < Length <= 150
     *   - Find the punctuation closest to middle (length / 2).
     *   - If found, split into 2 parts.
     *   - Otherwise, do NOT split.
     * - Case 3: Length > 150
     *   - Split into AT MOST 3 parts (never more than 3).
     *   - Find 2 punctuation marks closest to length/3 and 2*length/3.
     *   - If only 1 punctuation mark exists, split into 2 parts.
     *   - If no punctuation mark exists, do NOT split.
     */
    private fun applySecondarySplitIfNeeded(sentences: List<String>): List<String> {
        if (sentences.isEmpty()) return sentences

        val result = mutableListOf<String>()
        for (sentence in sentences) {
            val subSegments = splitSingleSentenceByLength(sentence)
            result.addAll(subSegments)
        }
        return result
    }

    private fun splitSingleSentenceByLength(sentence: String): List<String> {
        val len = sentence.length
        if (len <= 20) return listOf(sentence)

        val subPuncts = setOf('，', ',', '；', ';', '：', ':', '、', '—')

        val punctIndices = mutableListOf<Int>()
        for (i in 0 until len) {
            if (subPuncts.contains(sentence[i])) {
                punctIndices.add(i)
            }
        }

        if (punctIndices.isEmpty()) {
            return listOf(sentence)
        }

        // Case 1: Length <= 80
        if (len <= 80) {
            val eligible = punctIndices.filter { it in 20..59 }
            if (eligible.isEmpty()) {
                return listOf(sentence)
            }
            val mid = len / 2
            val bestIdx = eligible.minByOrNull { kotlin.math.abs(it - mid) } ?: return listOf(sentence)
            val part1 = cleanAndFormatSentence(sentence.substring(0, bestIdx + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(bestIdx + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        // Case 2: 80 < Length <= 150
        if (len <= 150) {
            val mid = len / 2
            val bestIdx = punctIndices.minByOrNull { kotlin.math.abs(it - mid) } ?: return listOf(sentence)
            val part1 = cleanAndFormatSentence(sentence.substring(0, bestIdx + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(bestIdx + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        // Case 3: Length > 150 (At most 3 parts)
        val target1 = len / 3
        val target2 = (len * 2) / 3

        if (punctIndices.size == 1) {
            val idx = punctIndices[0]
            val part1 = cleanAndFormatSentence(sentence.substring(0, idx + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(idx + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        var best1 = punctIndices.minByOrNull { kotlin.math.abs(it - target1) }!!
        var best2 = punctIndices.minByOrNull { kotlin.math.abs(it - target2) }!!

        if (best1 == best2) {
            val otherFor2 = punctIndices.filter { it != best1 }.minByOrNull { kotlin.math.abs(it - target2) }
            if (otherFor2 != null) {
                best2 = otherFor2
            }
        }

        if (best1 > best2) {
            val temp = best1
            best1 = best2
            best2 = temp
        }

        if (best1 == best2) {
            val part1 = cleanAndFormatSentence(sentence.substring(0, best1 + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(best1 + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        val part1 = cleanAndFormatSentence(sentence.substring(0, best1 + 1))
        val part2 = cleanAndFormatSentence(sentence.substring(best1 + 1, best2 + 1))
        val part3 = cleanAndFormatSentence(sentence.substring(best2 + 1))

        val res = mutableListOf<String>()
        if (part1.isNotEmpty()) res.add(part1)
        if (part2.isNotEmpty()) res.add(part2)
        if (part3.isNotEmpty()) res.add(part3)

        return if (res.isEmpty()) listOf(sentence) else res
    }

    /**
     * Removes internal newlines and formats spacing so the sentence renders continuously
     */
    private fun cleanAndFormatSentence(sentence: String): String {
        var cleaned = sentence.replace(Regex("(?<=[\u4e00-\u9fa5])\r?\n(?=[\u4e00-\u9fa5])"), "")
        cleaned = cleaned.replace(Regex("[\r\n]+"), " ")
        cleaned = cleaned.replace(Regex("[ \\t]+"), " ")
        return cleaned.trim()
    }

    /**
     * Strategy 1: java.text.BreakIterator for international sentence boundary detection,
     * enhanced with full rule avoidance (decimals, URLs, abbreviations, single newlines).
     */
    private fun splitWithBreakIterator(rawText: String): List<String> {
        val paragraphs = rawText.split(Regex("(\r?\n){2,}"))
        val result = mutableListOf<String>()

        for (paragraph in paragraphs) {
            val trimmedP = paragraph.trim()
            if (trimmedP.isEmpty()) continue

            val smoothedParagraph = cleanAndFormatSentence(trimmedP)

            val iterator = BreakIterator.getSentenceInstance(Locale.US)
            iterator.setText(smoothedParagraph)

            val rawChunks = mutableListOf<String>()
            var start = iterator.first()
            var end = iterator.next()

            while (end != BreakIterator.DONE) {
                val chunk = smoothedParagraph.substring(start, end).trim()
                if (chunk.isNotEmpty() && !isOnlyPunctuationOrWhitespace(chunk)) {
                    rawChunks.add(chunk)
                }
                start = end
                end = iterator.next()
            }

            // Post-merge pass to handle decimals (3.14), URLs (open.ai), abbreviations (Dr. Smith)
            val mergedChunks = mutableListOf<String>()
            var idx = 0
            while (idx < rawChunks.size) {
                var current = rawChunks[idx]
                while (idx + 1 < rawChunks.size && shouldMergeWithNext(current, rawChunks[idx + 1])) {
                    current = cleanAndFormatSentence("$current ${rawChunks[idx + 1]}")
                    idx++
                }
                val cleaned = cleanAndFormatSentence(current)
                if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned)) {
                    mergedChunks.add(cleaned)
                }
                idx++
            }

            result.addAll(mergedChunks)
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    /**
     * Strategy 2: Long Sentence Parser
     * 1. Preserves commas, semicolons, colons inside long sentences.
     * 2. Smooths single internal newlines (\n) so the sentence displays continuously on card.
     * 3. Respects double newlines (\n\n) as paragraph boundaries.
     * 4. Ignores decimals (3.14), URLs (google.com, open.ai), and abbreviations (Dr. Smith, U.S.A.).
     */
    private fun splitWithLongSentenceParser(rawText: String): List<String> {
        val paragraphs = rawText.split(Regex("(\r?\n){2,}"))
        val result = mutableListOf<String>()

        for (p in paragraphs) {
            val trimmedP = p.trim()
            if (trimmedP.isEmpty()) continue

            val sentencesInParagraph = splitParagraphIntoSentences(trimmedP)
            result.addAll(sentencesInParagraph)
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    private fun splitParagraphIntoSentences(paragraph: String): List<String> {
        if (paragraph.isBlank()) return emptyList()

        val text = cleanAndFormatSentence(paragraph)

        val sentences = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val ch = text[i]
            current.append(ch)

            // Chinese terminators: 。 ？！ …
            if (ch == '।' || ch == '？' || ch == '！' || ch == '…') {
                while (i + 1 < len && "'\"”’）)]}】".contains(text[i + 1])) {
                    i++
                    current.append(text[i])
                }
                val s = cleanAndFormatSentence(current.toString())
                if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s)) {
                    sentences.add(s)
                }
                current.clear()
            } else if (ch == '.' || ch == '?' || ch == '!') {
                var isSentenceEnd = true

                if (ch == '.') {
                    // Check Rule 1: Decimal number (3.14, 99.9%, $1250.50)
                    val isPrevDigit = i > 0 && text[i - 1].isDigit()
                    val isNextDigit = i + 1 < len && text[i + 1].isDigit()
                    if (isPrevDigit && isNextDigit) {
                        isSentenceEnd = false
                    }

                    // Check Rule 2: URL / Domain / File extension (google.com, open.ai, file.apk)
                    if (isSentenceEnd && i + 1 < len && text[i + 1].isLetter()) {
                        if (i > 0 && (text[i - 1].isLetterOrDigit() || text[i - 1] == '-')) {
                            isSentenceEnd = false
                        }
                    }

                    // Check Rule 3: Common abbreviation (Dr., Mr., Prof., U.S.A., e.g., i.e., A.M., Inc.)
                    if (isSentenceEnd) {
                        val prevText = current.toString().dropLast(1).trimEnd()
                        val lastWord = prevText.substringAfterLast(' ').lowercase()
                        if (ABBREVIATIONS.contains(lastWord) || ABBREVIATIONS.contains(lastWord.removePrefix("."))) {
                            isSentenceEnd = false
                        } else if (lastWord.length == 1 && lastWord[0].isUpperCase()) {
                            isSentenceEnd = false
                        }
                    }

                    // Check Rule 4: Standard sentence dot should be followed by whitespace, closing bracket/quote, or end
                    if (isSentenceEnd && i + 1 < len) {
                        val nextChar = text[i + 1]
                        if (!nextChar.isWhitespace() && !"'\"”’）)]}】".contains(nextChar)) {
                            isSentenceEnd = false
                        }
                    }
                }

                if (isSentenceEnd) {
                    while (i + 1 < len && "'\"”’）)]}】".contains(text[i + 1])) {
                        i++
                        current.append(text[i])
                    }
                    val s = cleanAndFormatSentence(current.toString())
                    if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s)) {
                        sentences.add(s)
                    }
                    current.clear()
                }
            }
            i++
        }

        if (current.isNotBlank()) {
            val s = cleanAndFormatSentence(current.toString())
            if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s)) {
                sentences.add(s)
            }
        }

        return sentences
    }

    private fun shouldMergeWithNext(currentChunk: String, nextChunk: String): Boolean {
        if (currentChunk.isEmpty() || nextChunk.isEmpty()) return false
        val trimmedCurrent = currentChunk.trimEnd()
        if (!trimmedCurrent.endsWith('.')) return false

        val lastChar = trimmedCurrent.dropLast(1).lastOrNull()
        val firstChar = nextChunk.trimStart().firstOrNull()

        // Rule 1: Decimal numbers
        if (lastChar != null && lastChar.isDigit() && firstChar != null && firstChar.isDigit()) {
            return true
        }

        // Rule 2: URLs / domains
        if (lastChar != null && (lastChar.isLetterOrDigit() || lastChar == '-') && firstChar != null && firstChar.isLetter()) {
            if (!currentChunk.endsWith(" ") && !nextChunk.startsWith(" ")) {
                return true
            }
        }

        // Rule 3: Common abbreviations
        val prevText = trimmedCurrent.dropLast(1).trimEnd()
        val lastWord = prevText.substringAfterLast(' ').lowercase()
        if (ABBREVIATIONS.contains(lastWord) || ABBREVIATIONS.contains(lastWord.removePrefix("."))) {
            return true
        }
        if (lastWord.length == 1 && lastWord[0].isUpperCase()) {
            return true
        }

        return false
    }

    private fun splitByLines(rawText: String): List<String> {
        return rawText.lines().map { cleanAndFormatSentence(it) }.filter {
            it.isNotEmpty() && !isOnlyPunctuationOrWhitespace(it)
        }
    }

    private fun isOnlyPunctuationOrWhitespace(text: String): Boolean {
        return text.all { it.isWhitespace() || "。？！.?!…，,；;：:\"'“”‘’（）()[]【】".contains(it) }
    }
}
