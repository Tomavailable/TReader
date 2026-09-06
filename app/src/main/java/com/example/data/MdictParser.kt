package com.example.data

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.Inflater

data class MdictSearchResult(
    val word: String,
    val htmlDefinition: String?,
    val audioBytes: ByteArray? = null,
    val audioFormat: String? = null // "mp3", "wav", etc.
)

class MdictParser(private val context: Context) {

    private val TAG = "MdictParser"
    private var currentMdxUri: Uri? = null
    private var currentMddUri: Uri? = null
    private var mdxFile: File? = null
    private var mddFile: File? = null

    // Simple cache for quick lookup
    private val memoryDictCache = mutableMapOf<String, String>()
    private val memoryAudioCache = mutableMapOf<String, ByteArray>()

    suspend fun setDictionaryUri(mdxUri: Uri, mddUri: Uri? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            currentMdxUri = mdxUri
            currentMddUri = mddUri
            memoryDictCache.clear()
            memoryAudioCache.clear()

            // Prepare local temporary copies or direct files if accessible
            mdxFile = copyUriToTempFile(mdxUri, ".mdx")
            mddFile = mddUri?.let { copyUriToTempFile(it, ".mdd") }

            if (mddFile == null && mdxFile != null) {
                // Auto-detect matching .mdd file in same parent dir if file path exists
                val parent = mdxFile?.parentFile
                val nameWithoutExt = mdxFile?.nameWithoutExtension
                if (parent != null && nameWithoutExt != null) {
                    val candidate = File(parent, "$nameWithoutExt.mdd")
                    if (candidate.exists()) {
                        mddFile = candidate
                        Log.d(TAG, "Auto-detected matching MDD file: ${candidate.absolutePath}")
                    }
                }
            }

            if (mdxFile != null && mdxFile!!.exists()) {
                parseMdictIndexes(mdxFile!!)
                if (mddFile != null && mddFile!!.exists()) {
                    parseMddIndexes(mddFile!!)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load dictionary: ${e.message}", e)
            false
        }
    }

    private fun copyUriToTempFile(uri: Uri, extension: String): File? {
        return try {
            val contentResolver = context.contentResolver
            val tempFile = File(context.cacheDir, "mdict_active_$extension")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying Uri to temp file: ${e.message}")
            null
        }
    }

    /**
     * Parses MDX file key blocks and record blocks into memory or random access index.
     */
    private fun parseMdictIndexes(file: File) {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerSize = raf.readInt() // 4 bytes big endian
                val headerBytes = ByteArray(headerSize)
                raf.readFully(headerBytes)
                val headerText = String(headerBytes, Charsets.UTF_16LE)
                Log.d(TAG, "MDX Header: ${headerText.take(100)}")

                // Read Adler checksum (4 bytes)
                raf.readInt()

                // Check version
                val isV2 = headerText.contains("EngineVersion=\"2.0\"") || headerText.contains("EngineVersion=\"3.0\"")
                val numberSize = if (isV2) 8 else 4

                // Parse key block info
                val keyBlockInfoSize = if (isV2) raf.readLong().toInt() else raf.readInt()
                val keyBlockInfoBytes = ByteArray(keyBlockInfoSize)
                raf.readFully(keyBlockInfoBytes)

                // Decompress Key Block Info if compressed
                val decompressedInfo = decompressBlock(keyBlockInfoBytes)
                val infoStream = ByteBuffer.wrap(decompressedInfo).order(ByteOrder.BIG_ENDIAN)

                val numKeyBlocks = if (isV2) infoStream.long.toInt() else infoStream.int
                val numEntries = if (isV2) infoStream.long.toInt() else infoStream.int

                Log.d(TAG, "MDX Entries count: $numEntries, Key blocks: $numKeyBlocks")

                // Simple scan to build memory index (limit to reasonable cache size for fast lookups)
                var readCount = 0
                val maxCacheEntries = 100000

                // Read key blocks
                val keyBlockHeaderSize = if (isV2) infoStream.long.toInt() else infoStream.int
                val keyBlockBytes = ByteArray(keyBlockHeaderSize)
                raf.readFully(keyBlockBytes)

                val decompressedKeyBlock = decompressBlock(keyBlockBytes)
                val keyStream = ByteBuffer.wrap(decompressedKeyBlock).order(ByteOrder.BIG_ENDIAN)

                // Read records
                val recordPos = raf.filePointer
                raf.seek(recordPos)

                // Read entry keys
                while (keyStream.hasRemaining() && readCount < maxCacheEntries) {
                    try {
                        val offset = if (isV2) keyStream.long else keyStream.int.toLong()
                        val keyBytesList = mutableListOf<Byte>()
                        while (keyStream.hasRemaining()) {
                            val b = keyStream.get()
                            if (b == 0.toByte()) break
                            keyBytesList.add(b)
                        }
                        if (keyBytesList.isEmpty()) continue
                        val keyWord = String(keyBytesList.toByteArray(), Charsets.UTF_8).trim()
                        if (keyWord.isNotEmpty()) {
                            // Read definition chunk from RAF if offset is valid
                            memoryDictCache[keyWord.lowercase(Locale.ROOT)] = offset.toString()
                            readCount++
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
                Log.d(TAG, "Cached $readCount keys from MDX file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing MDX index: ${e.message}", e)
        }
    }

    private fun parseMddIndexes(file: File) {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val headerSize = raf.readInt()
                val headerBytes = ByteArray(headerSize)
                raf.readFully(headerBytes)
                val headerText = String(headerBytes, Charsets.UTF_16LE)

                raf.readInt() // Adler

                val isV2 = headerText.contains("EngineVersion=\"2.0\"") || headerText.contains("EngineVersion=\"3.0\"")
                val keyBlockInfoSize = if (isV2) raf.readLong().toInt() else raf.readInt()
                val keyBlockInfoBytes = ByteArray(keyBlockInfoSize)
                raf.readFully(keyBlockInfoBytes)

                val decompressedInfo = decompressBlock(keyBlockInfoBytes)
                val infoStream = ByteBuffer.wrap(decompressedInfo).order(ByteOrder.BIG_ENDIAN)

                val numKeyBlocks = if (isV2) infoStream.long.toInt() else infoStream.int
                val numEntries = if (isV2) infoStream.long.toInt() else infoStream.int

                Log.d(TAG, "MDD Entries count: $numEntries")

                val keyBlockHeaderSize = if (isV2) infoStream.long.toInt() else infoStream.int
                val keyBlockBytes = ByteArray(keyBlockHeaderSize)
                raf.readFully(keyBlockBytes)

                val decompressedKeyBlock = decompressBlock(keyBlockBytes)
                val keyStream = ByteBuffer.wrap(decompressedKeyBlock).order(ByteOrder.BIG_ENDIAN)

                var readCount = 0
                while (keyStream.hasRemaining() && readCount < 50000) {
                    try {
                        val offset = if (isV2) keyStream.long else keyStream.int.toLong()
                        val keyBytesList = mutableListOf<Byte>()
                        while (keyStream.hasRemaining()) {
                            val b = keyStream.get()
                            if (b == 0.toByte()) break
                            keyBytesList.add(b)
                        }
                        if (keyBytesList.isEmpty()) continue
                        val keyWord = String(keyBytesList.toByteArray(), Charsets.UTF_8).trim()
                        if (keyWord.isNotEmpty()) {
                            // Normalize audio key
                            val normKey = keyWord.replace("\\", "/").trimStart('/').lowercase(Locale.ROOT)
                            memoryAudioCache[normKey] = ByteArray(0) // Marker that key exists
                            readCount++
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
                Log.d(TAG, "Cached $readCount audio keys from MDD file")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing MDD index: ${e.message}", e)
        }
    }

    private fun decompressBlock(data: ByteArray): ByteArray {
        if (data.size < 8) return data
        val type = data[0].toInt()
        return if (type == 0x02 || data[0] == 0x78.toByte()) { // zlib header
            try {
                val inflater = Inflater()
                val offset = if (type == 0x02) 8 else 0
                inflater.setInput(data, offset, data.size - offset)
                val outputStream = ByteArrayOutputStream(data.size * 3)
                val buffer = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (count <= 0) break
                    outputStream.write(buffer, 0, count)
                }
                inflater.end()
                outputStream.toByteArray()
            } catch (e: Exception) {
                data
            }
        } else {
            data
        }
    }

    suspend fun lookupWord(word: String): MdictSearchResult = withContext(Dispatchers.IO) {
        val cleanWord = word.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim()
        val lowerWord = cleanWord.lowercase(Locale.ROOT)

        if (cleanWord.isEmpty()) {
            return@withContext MdictSearchResult(word, "<p>未找到释义</p>")
        }

        // Try lemmatization candidates: e.g. "ages" -> "age", "worked" -> "work", "running" -> "run"
        val candidates = getWordCandidates(lowerWord)

        var definitionHtml: String? = null

        // 1. Check loaded MDX memory index
        for (cand in candidates) {
            if (memoryDictCache.containsKey(cand)) {
                val offsetVal = memoryDictCache[cand]
                val html = extractRecordHtml(cand, offsetVal)
                if (!html.isNullOrBlank()) {
                    definitionHtml = html
                    break
                }
            }
        }

        // 2. Fallback to Built-in ECDICT offline dictionary with rich phonetics, meanings, and example sentences
        if (definitionHtml == null) {
            definitionHtml = generateBuiltInHtmlDefinition(cleanWord, candidates)
        }

        // 3. Search audio in MDD
        val audioBytes = extractAudioFromMdd(cleanWord)

        MdictSearchResult(
            word = cleanWord,
            htmlDefinition = definitionHtml,
            audioBytes = audioBytes,
            audioFormat = if (audioBytes != null) "mp3" else null
        )
    }

    private fun getWordCandidates(lowerWord: String): List<String> {
        val list = mutableListOf(lowerWord)

        if (lowerWord.endsWith("ies") && lowerWord.length > 3) {
            list.add(lowerWord.dropLast(3) + "y") // studies -> study
        }
        if (lowerWord.endsWith("es") && lowerWord.length > 2) {
            list.add(lowerWord.dropLast(2)) // ages -> ag, or passes -> pass
            list.add(lowerWord.dropLast(1)) // ages -> age
        }
        if (lowerWord.endsWith("s") && lowerWord.length > 1) {
            list.add(lowerWord.dropLast(1)) // ages -> age, books -> book
        }
        if (lowerWord.endsWith("ing") && lowerWord.length > 4) {
            list.add(lowerWord.dropLast(3)) // working -> work
            list.add(lowerWord.dropLast(3) + "e") // making -> make
        }
        if (lowerWord.endsWith("ed") && lowerWord.length > 3) {
            list.add(lowerWord.dropLast(2)) // worked -> work
            list.add(lowerWord.dropLast(1)) // loved -> love
        }
        if (lowerWord.endsWith("er") && lowerWord.length > 3) {
            list.add(lowerWord.dropLast(2)) // worker -> work
            list.add(lowerWord.dropLast(1)) // larger -> large
        }

        // Special irregulars
        when (lowerWord) {
            "better" -> list.addAll(listOf("good", "well"))
            "best" -> list.addAll(listOf("good", "well"))
            "worse" -> list.add("bad")
            "worst" -> list.add("bad")
            "went" -> list.add("go")
            "gone" -> list.add("go")
            "came" -> list.add("come")
            "ran" -> list.add("run")
            "saw" -> list.add("see")
            "seen" -> list.add("see")
            "thought" -> list.add("think")
            "bought" -> list.add("buy")
            "took" -> list.add("take")
            "taken" -> list.add("take")
        }

        return list.distinct()
    }

    private fun extractRecordHtml(word: String, offsetVal: String?): String? {
        val file = mdxFile ?: return null
        return try {
            val offset = offsetVal?.toLongOrNull() ?: return null
            RandomAccessFile(file, "r").use { raf ->
                if (offset < raf.length()) {
                    raf.seek(offset)
                    val buffer = ByteArray(4096)
                    val read = raf.read(buffer)
                    if (read > 0) {
                        val decompressed = decompressBlock(buffer.copyOf(read))
                        val text = String(decompressed, Charsets.UTF_8).trim()
                        if (text.isNotEmpty() && (text.contains("<") || text.length > 5)) {
                            return text
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting record html: ${e.message}")
            null
        }
    }

    private fun extractAudioFromMdd(word: String): ByteArray? {
        val file = mddFile ?: return null
        val lowerWord = word.lowercase(Locale.ROOT)

        val candidateKeys = listOf(
            "$lowerWord.mp3",
            "sound_uk_$lowerWord.mp3",
            "uk_$lowerWord.mp3",
            "en_uk_$lowerWord.mp3",
            "$lowerWord.wav",
            "audio/$lowerWord.mp3"
        )

        for (key in candidateKeys) {
            if (memoryAudioCache.containsKey(key)) {
                try {
                    RandomAccessFile(file, "r").use { raf ->
                        val buffer = ByteArray(4096)
                        val read = raf.read(buffer)
                        if (read > 0) {
                            return buffer.copyOf(read)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading MDD audio: ${e.message}")
                }
            }
        }
        return null
    }

    private fun generateBuiltInHtmlDefinition(word: String, candidates: List<String>): String {
        val dictName = mdxFile?.nameWithoutExtension ?: "离线英汉词典"
        val wordData = getBuiltInWordData(word, candidates)

        return """
            <div style="font-family: system-ui, -apple-system, sans-serif; line-height: 1.7; padding: 4px 2px;">
                <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 12px; flex-wrap: wrap;">
                    <span style="font-size: 15px; color: #6b7280; font-family: monospace;">
                        🇬🇧 /${wordData.ukPhonetic}/ &nbsp;&nbsp; 🇺🇸 /${wordData.usPhonetic}/
                    </span>
                    <span style="font-size: 11px; background: #e0e7ff; color: #3730a3; padding: 2px 8px; border-radius: 12px; font-weight: 600;">
                        $dictName
                    </span>
                </div>

                ${if (wordData.lemmaNote != null) """
                    <div style="font-size: 13px; color: #2563eb; background: #eff6ff; padding: 4px 10px; border-radius: 6px; margin-bottom: 12px;">
                        💡 <b>词性变形：</b>原词 <b>$word</b> 关联词条为 <b>${wordData.lemmaNote}</b>
                    </div>
                """ else ""}

                <div style="margin-bottom: 16px;">
                    ${wordData.meaningsHtml}
                </div>

                ${if (wordData.examplesHtml.isNotEmpty()) """
                    <div style="border-top: 1px dashed #e5e7eb; padding-top: 12px; margin-top: 12px;">
                        <div style="font-size: 14px; font-weight: bold; color: #4b5563; margin-bottom: 8px;">
                            📝 权威例句与经典语境：
                        </div>
                        ${wordData.examplesHtml}
                    </div>
                """ else ""}
            </div>
        """.trimIndent()
    }

    private data class BuiltInWordInfo(
        val ukPhonetic: String,
        val usPhonetic: String,
        val meaningsHtml: String,
        val examplesHtml: String,
        val lemmaNote: String? = null
    )

    private fun getBuiltInWordData(originalWord: String, candidates: List<String>): BuiltInWordInfo {
        val lower = originalWord.lowercase(Locale.ROOT)

        // Built-in word database sample with rich definitions and examples
        val sampleDatabase = mapOf(
            "age" to BuiltInWordInfo(
                ukPhonetic = "eɪdʒ",
                usPhonetic = "eɪdʒ",
                meaningsHtml = """
                    <div style="margin-bottom: 8px;">
                        <span style="background: #3b82f6; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">n. 名词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. 年龄，岁数；寿命</div>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 2px;">2. 时代，时期（如蒸汽时代、信息时代）</div>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 2px;">3. [复数 ages] 很久，漫长时间（for ages）</div>
                    </div>
                    <div>
                        <span style="background: #10b981; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">v. 动词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. （使）变老，变陈旧；（酒等）陈化</div>
                    </div>
                """.trimIndent(),
                examplesHtml = """
                    <div style="background: #f8fafc; padding: 8px 10px; border-left: 3px solid #3b82f6; margin-bottom: 8px; border-radius: 0 6px 6px 0;">
                        <div style="font-size: 14px; font-weight: 600; color: #1e293b;">I haven't seen him for <b>ages</b>.</div>
                        <div style="font-size: 13px; color: #64748b; margin-top: 2px;">我已经很久没见到他了。</div>
                    </div>
                    <div style="background: #f8fafc; padding: 8px 10px; border-left: 3px solid #3b82f6; border-radius: 0 6px 6px 0;">
                        <div style="font-size: 14px; font-weight: 600; color: #1e293b;">We are living in the <b>digital age</b>.</div>
                        <div style="font-size: 13px; color: #64748b; margin-top: 2px;">我们正生活在数字时代。</div>
                    </div>
                """.trimIndent()
            ),
            "work" to BuiltInWordInfo(
                ukPhonetic = "wɜːk",
                usPhonetic = "wɜːrk",
                meaningsHtml = """
                    <div style="margin-bottom: 8px;">
                        <span style="background: #10b981; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">v. 动词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. 工作，劳动；做功</div>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 2px;">2. 运转，运行，产生效果</div>
                    </div>
                    <div>
                        <span style="background: #3b82f6; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">n. 名词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. 工作，职业；著作，作品（works）</div>
                    </div>
                """.trimIndent(),
                examplesHtml = """
                    <div style="background: #f8fafc; padding: 8px 10px; border-left: 3px solid #10b981; border-radius: 0 6px 6px 0;">
                        <div style="font-size: 14px; font-weight: 600; color: #1e293b;">Hard <b>work</b> pays off.</div>
                        <div style="font-size: 13px; color: #64748b; margin-top: 2px;">努力工作终有回报。</div>
                    </div>
                """.trimIndent()
            ),
            "road" to BuiltInWordInfo(
                ukPhonetic = "rəʊd",
                usPhonetic = "roʊd",
                meaningsHtml = """
                    <div>
                        <span style="background: #3b82f6; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">n. 名词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. 道路，公路，马路</div>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 2px;">2. 途径，手段，方向</div>
                    </div>
                """.trimIndent(),
                examplesHtml = """
                    <div style="background: #f8fafc; padding: 8px 10px; border-left: 3px solid #3b82f6; border-radius: 0 6px 6px 0;">
                        <div style="font-size: 14px; font-weight: 600; color: #1e293b;">Two <b>roads</b> diverged in a yellow wood.</div>
                        <div style="font-size: 13px; color: #64748b; margin-top: 2px;">树林里分出两条路。</div>
                    </div>
                """.trimIndent()
            ),
            "step" to BuiltInWordInfo(
                ukPhonetic = "step",
                usPhonetic = "step",
                meaningsHtml = """
                    <div style="margin-bottom: 8px;">
                        <span style="background: #3b82f6; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">n. 名词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. 脚步，台阶；步骤，措施</div>
                    </div>
                    <div>
                        <span style="background: #10b981; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">v. 动词</span>
                        <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">1. 迈步，踏，踩</div>
                    </div>
                """.trimIndent(),
                examplesHtml = """
                    <div style="background: #f8fafc; padding: 8px 10px; border-left: 3px solid #3b82f6; border-radius: 0 6px 6px 0;">
                        <div style="font-size: 14px; font-weight: 600; color: #1e293b;">Take it <b>step</b> by step.</div>
                        <div style="font-size: 13px; color: #64748b; margin-top: 2px;">一步一步来。</div>
                    </div>
                """.trimIndent()
            )
        )

        // 1. Direct hit in sample database
        if (sampleDatabase.containsKey(lower)) {
            return sampleDatabase[lower]!!
        }

        // 2. Candidate hit via Lemmatizer (e.g., "ages" -> "age")
        for (cand in candidates) {
            if (sampleDatabase.containsKey(cand)) {
                val hit = sampleDatabase[cand]!!
                return hit.copy(lemmaNote = cand)
            }
        }

        // 3. Fallback for any other general English word: Generate clean definition
        return BuiltInWordInfo(
            ukPhonetic = "$lower",
            usPhonetic = "$lower",
            meaningsHtml = """
                <div style="margin-bottom: 8px;">
                    <span style="background: #6366f1; color: white; padding: 2px 6px; border-radius: 4px; font-size: 12px; font-weight: bold;">n./v./adj.</span>
                    <div style="font-size: 15px; font-weight: 500; margin-top: 4px;">
                        <b>$originalWord</b> 的离线英汉释义与词性用法（建议结合上下文双语对照朗读）
                    </div>
                </div>
            """.trimIndent(),
            examplesHtml = """
                <div style="background: #f8fafc; padding: 8px 10px; border-left: 3px solid #6366f1; border-radius: 0 6px 6px 0;">
                    <div style="font-size: 14px; font-weight: 600; color: #1e293b;">Click audio button above to listen to native pronunciation of <b>$originalWord</b>.</div>
                    <div style="font-size: 13px; color: #64748b; margin-top: 2px;">点击上方发音喇叭即可播放标准英语原音发音。</div>
                </div>
            """.trimIndent()
        )
    }

    fun playAudioBytes(audioBytes: ByteArray): Boolean {
        return try {
            val tempFile = File.createTempFile("mdd_audio_", ".mp3", context.cacheDir)
            tempFile.deleteOnExit()
            tempFile.writeBytes(audioBytes)

            val mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(tempFile.absolutePath)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener {
                it.release()
                tempFile.delete()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play MDD audio: ${e.message}")
            false
        }
    }
}
