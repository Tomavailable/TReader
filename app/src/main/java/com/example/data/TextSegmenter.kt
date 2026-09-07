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

    val DEFAULT_SECONDARY_PUNCTS = setOf('，', ',', '；', ';', '：', ':', '、', '—')
    val DEFAULT_TERMINATOR_PUNCTS = setOf('。', '？', '！', '…', '.', '?', '!')
    val DEFAULT_CLOSING_PUNCTS = setOf('"', '\'', '”', '’', '）', ')', ']', '}', '】')

    private val ABBREVIATIONS = setOf(
        "mr", "mrs", "ms", "dr", "prof", "sr", "jr", "vs", "etc", "e.g", "i.e",
        "st", "co", "inc", "ltd", "jan", "feb", "mar", "apr", "aug", "sept", "oct",
        "nov", "dec", "no", "vol", "pp", "p", "u.s", "u.s.a", "u.k", "a.m", "p.m"
    )

    /**
     * Splits raw text into sentences based on the chosen [splitMode], optional [isSplitEnabled],
     * and user-customizable punctuation rules.
     */
    fun splitIntoSentences(
        rawText: String,
        splitMode: SplitMode = SplitMode.BREAK_ITERATOR,
        isSplitEnabled: Boolean = false,
        secondaryPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
        if (rawText.isBlank()) return emptyList()

        val baseSentences = when (splitMode) {
            SplitMode.BREAK_ITERATOR -> splitWithBreakIterator(rawText, terminatorPuncts, closingPuncts)
            SplitMode.FULL_SENTENCE -> splitWithLongSentenceParser(rawText, terminatorPuncts, closingPuncts)
        }

        return if (isSplitEnabled) {
            applySecondarySplitIfNeeded(baseSentences, secondaryPuncts)
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
     */
    private fun applySecondarySplitIfNeeded(
        sentences: List<String>,
        secondaryPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS
    ): List<String> {
        if (sentences.isEmpty()) return sentences

        val result = mutableListOf<String>()
        for (sentence in sentences) {
            val subSegments = splitSingleSentenceByLength(sentence, secondaryPuncts)
            result.addAll(subSegments)
        }
        return result
    }

    private fun splitSingleSentenceByLength(
        sentence: String,
        subPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS
    ): List<String> {
        val len = sentence.length
        // 规则 1: 长度 ≤ 30 字符：保持整句中间有标点也不拆
        if (len <= 30) return listOf(sentence)

        val punctIndices = mutableListOf<Int>()
        for (i in 0 until len) {
            if (subPuncts.contains(sentence[i])) {
                punctIndices.add(i)
            }
        }

        if (punctIndices.isEmpty()) {
            return listOf(sentence)
        }

        // 规则 2: 31 ~ 80 字符：最靠近中间的标点，拆分为 2 段
        if (len <= 80) {
            val mid = len / 2
            val bestIdx = punctIndices.minByOrNull { kotlin.math.abs(it - mid) } ?: return listOf(sentence)
            val part1 = cleanAndFormatSentence(sentence.substring(0, bestIdx + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(bestIdx + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        // 规则 3: 81 ~ 150 字符：寻找最接近正中点的标点，拆分为 2 段
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

        // 规则 4: 150 字符以上：在整句的 1/3 和 2/3 位置附近寻找最合适的标点，最多平滑拆分为 3 段，避免单卡片阅读负担过重
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
    private fun splitWithBreakIterator(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
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
                if (chunk.isNotEmpty() && !isOnlyPunctuationOrWhitespace(chunk, terminatorPuncts, closingPuncts)) {
                    rawChunks.add(chunk)
                }
                start = end
                end = iterator.next()
            }

            // Post-merge pass to handle decimals (3.14), URLs (open.ai), abbreviations (Dr. Smith)
            // and keep trailing closing quote/bracket with the preceding sentence
            val mergedChunks = mutableListOf<String>()
            var idx = 0
            while (idx < rawChunks.size) {
                var current = rawChunks[idx]
                while (idx + 1 < rawChunks.size && shouldMergeWithNext(current, rawChunks[idx + 1], closingPuncts)) {
                    current = cleanAndFormatSentence("$current ${rawChunks[idx + 1]}")
                    idx++
                }
                val cleaned = cleanAndFormatSentence(current)
                if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, terminatorPuncts, closingPuncts)) {
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
    private fun splitWithLongSentenceParser(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
        val paragraphs = rawText.split(Regex("(\r?\n){2,}"))
        val result = mutableListOf<String>()

        for (p in paragraphs) {
            val trimmedP = p.trim()
            if (trimmedP.isEmpty()) continue

            val sentencesInParagraph = splitParagraphIntoSentences(trimmedP, terminatorPuncts, closingPuncts)
            result.addAll(sentencesInParagraph)
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    private fun splitParagraphIntoSentences(
        paragraph: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
        if (paragraph.isBlank()) return emptyList()

        val text = cleanAndFormatSentence(paragraph)

        val sentences = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        val len = text.length

        while (i < len) {
            val ch = text[i]
            current.append(ch)

            if (terminatorPuncts.contains(ch)) {
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
                        if (!nextChar.isWhitespace() && !closingPuncts.contains(nextChar)) {
                            isSentenceEnd = false
                        }
                    }
                }

                if (isSentenceEnd) {
                    // Evasion rule: attach closing quotes/brackets to current sentence
                    while (i + 1 < len && closingPuncts.contains(text[i + 1])) {
                        i++
                        current.append(text[i])
                    }
                    val s = cleanAndFormatSentence(current.toString())
                    if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s, terminatorPuncts, closingPuncts)) {
                        sentences.add(s)
                    }
                    current.clear()
                }
            }
            i++
        }

        if (current.isNotBlank()) {
            val s = cleanAndFormatSentence(current.toString())
            if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s, terminatorPuncts, closingPuncts)) {
                sentences.add(s)
            }
        }

        return sentences
    }

    private fun shouldMergeWithNext(
        currentChunk: String,
        nextChunk: String,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): Boolean {
        if (currentChunk.isEmpty() || nextChunk.isEmpty()) return false

        // If next chunk starts with a closing quote/bracket, merge it with previous
        val nextFirstChar = nextChunk.trimStart().firstOrNull()
        if (nextFirstChar != null && closingPuncts.contains(nextFirstChar)) {
            return true
        }

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

    private fun isOnlyPunctuationOrWhitespace(
        text: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): Boolean {
        val allSymbols = terminatorPuncts + closingPuncts + DEFAULT_SECONDARY_PUNCTS
        return text.all { it.isWhitespace() || allSymbols.contains(it) }
    }
}
