package com.example.ui

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AudioExporter
import com.example.data.RecentBook
import com.example.data.RecentBooksStore
import com.example.data.SplitMode
import com.example.data.TextSegmenter
import com.example.data.TtsManager
import com.example.data.TtsVoiceItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ThemeMode(val title: String) {
    SYSTEM("跟随系统"),
    LIGHT("日间模式"),
    DARK("夜间模式")
}

enum class ReadingDisplayMode(val label: String, val desc: String) {
    BOOK_PAGE("经典书卷", "标准电子书排版，当前播放句平滑居中高亮")
}

enum class WordLookupMode(val title: String, val desc: String) {
    DIRECT_DICT("方案一：直接调起词典小窗", "点击单词直接弹出词典悬浮小窗（默认：欧路词典）"),
    POPOVER_MENU("方案二：快捷图标菜单", "点击单词弹出极简横向图标工具栏"),
    LOCAL_MDX("方案三：内置 MDX 本地词典", "点击单词使用软件内置 MDX 词典小窗查询释义，支持 MDD 离线发音")
}

enum class EudicInvokeMode(val title: String, val desc: String) {
    EXPLICIT_INTENT("方案一：显式小窗 Component (推荐)", "直接唤起 LightpeekActivity 悬浮组件"),
    URL_SCHEME("方案二：官方 URL Scheme", "使用 eudic://peek/{word} 协议唤起小窗"),
    DIRECT_SEND("方案三：定向静默发送 (ACTION_SEND)", "锁定欧路词典包名，直接弹出小窗查词")
}

enum class DictAppOption(val label: String, val packageName: String?) {
    EUDIC("欧路词典 (默认)", "com.eusoft.eudic"),
    GOOGLE_TRANSLATE("谷歌翻译", "com.google.android.apps.translate"),
    LOCAL_MDX("内置 MDX 离线词典", null),
    SYSTEM_CHOOSER("系统通用划词", null)
}

data class ExportState(
    val isExporting: Boolean = false,
    val progress: Float = 0f,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val statusMessage: String = "",
    val exportedFile: File? = null,
    val isPreviewPlaying: Boolean = false,
    val errorMessage: String? = null
)

data class ReaderUiState(
    val fileName: String = "未加载书籍",
    val rawText: String = "",
    val sentences: List<String> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val splitComma: Boolean = false,
    val isSplitEnabled: Boolean = false, // Default: NOT split
    val splitMode: SplitMode = SplitMode.BREAK_ITERATOR,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Multi-speaker and repeat cycle
    val currentRepeatPass: Int = 1,
    val targetRepeatCount: Int = 1, // 1, 2, 3, 5, 999 (infinite)
    val multiSpeakerEnabled: Boolean = false,
    val currentSpeakerPass: Int = 0, // 0 = speaker 1, 1 = speaker 2, 2 = speaker 3
    // Audio configuration: Default to English as requested
    val selectedLanguage: String = "en-US", // "all", "en-US", etc.
    val speaker1VoiceId: String? = null,
    val speaker2VoiceId: String? = null,
    val speaker3VoiceId: String? = null,
    val activeSpeakerIndex: Int = 0, // 0 = Speaker 1, 1 = Speaker 2, 2 = Speaker 3
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    // Sleep Timer (0 = disabled, 15, 30, 45, 60 minutes)
    val sleepTimerMinutes: Int = 0,
    val sleepTimerRemainingSeconds: Int = 0,
    // Recent books collection
    val recentBooks: List<RecentBook> = emptyList(),
    val readingDisplayMode: ReadingDisplayMode = ReadingDisplayMode.BOOK_PAGE,
    // Dictionary & Translation preference
    val wordLookupMode: WordLookupMode = WordLookupMode.DIRECT_DICT,
    val defaultDictApp: DictAppOption = DictAppOption.EUDIC,
    val eudicInvokeMode: EudicInvokeMode = EudicInvokeMode.EXPLICIT_INTENT,
    val activePopoverWord: String? = null,
    val selectedSentenceForInspection: String? = null,
    val isSentenceInspectionOpen: Boolean = false,
    // MDX / MDD Local Dict state
    val mdictMdxUri: android.net.Uri? = null,
    val mdictMddUri: android.net.Uri? = null,
    val mdictMdxFileName: String = "未选择 .mdx 词典文件",
    val mdictAutoPronounce: Boolean = true,
    val mdictPronounceAccent: String = "UK", // "UK" = British (default), "US" = American
    val localMdictResult: com.example.data.MdictSearchResult? = null,
    val isLocalMdictBottomSheetOpen: Boolean = false,
    // Dialog states
    val isSettingsOpen: Boolean = false,
    val isExportDialogOpen: Boolean = false,
    val isRecentBooksDialogOpen: Boolean = false,
    val exportState: ExportState = ExportState()
)

class TtsReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "TtsReaderViewModel"
    val ttsManager = TtsManager(application)
    val audioExporter = AudioExporter(application)
    val recentBooksStore = RecentBooksStore(application)
    val mdictParser = com.example.data.MdictParser(application)

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var exportJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var utteranceCounter = 0

    init {
        // Load recent books on start
        _uiState.update { it.copy(recentBooks = recentBooksStore.getRecentBooks()) }

        ttsManager.onUtteranceDone = { utteranceId ->
            viewModelScope.launch {
                handleUtteranceCompleted(utteranceId)
            }
        }

        ttsManager.onUtteranceError = { utteranceId ->
            viewModelScope.launch {
                Log.w(TAG, "Utterance error: $utteranceId, advancing")
                handleUtteranceCompleted(utteranceId)
            }
        }

        // Initialize voice picks once available
        viewModelScope.launch {
            ttsManager.availableVoices.collect { voices ->
                if (voices.isNotEmpty() && _uiState.value.speaker1VoiceId == null) {
                    autoAssignVoices(voices, _uiState.value.selectedLanguage)
                }
            }
        }
    }

    private fun autoAssignVoices(voices: List<TtsVoiceItem>, targetLang: String) {
        val filtered = if (targetLang == "all") voices else {
            val prefix = targetLang.substringBefore("-").lowercase()
            val list = voices.filter { it.locale.language.lowercase().startsWith(prefix) }
            if (list.isNotEmpty()) list else voices
        }

        if (filtered.isNotEmpty()) {
            val v1 = filtered.getOrNull(0)?.id
            val v2 = filtered.getOrNull(1 % filtered.size)?.id ?: v1
            val v3 = filtered.getOrNull(2 % filtered.size)?.id ?: v1

            _uiState.update {
                it.copy(
                    speaker1VoiceId = it.speaker1VoiceId ?: v1,
                    speaker2VoiceId = it.speaker2VoiceId ?: v2,
                    speaker3VoiceId = it.speaker3VoiceId ?: v3
                )
            }
        }
    }

    fun loadText(text: String, fileName: String = "已导入文本.txt", initialIndex: Int = 0) {
        stopPlayback()
        val sentences = TextSegmenter.splitIntoSentences(
            text,
            _uiState.value.splitMode,
            _uiState.value.isSplitEnabled
        )
        val validIndex = if (sentences.isNotEmpty()) initialIndex.coerceIn(0, sentences.size - 1) else -1

        // Persist to recent books
        recentBooksStore.saveRecentBook(fileName, text, sentences.size, validIndex)
        val updatedRecent = recentBooksStore.getRecentBooks()

        _uiState.update {
            it.copy(
                rawText = text,
                fileName = fileName,
                sentences = sentences,
                currentIndex = validIndex,
                currentRepeatPass = 1,
                currentSpeakerPass = 0,
                isPlaying = false,
                recentBooks = updatedRecent
            )
        }
    }

    fun openRecentBook(book: RecentBook) {
        loadText(book.fullText, book.title, book.lastIndex)
    }

    fun returnToHome() {
        stopPlayback()
        _uiState.update {
            it.copy(
                rawText = "",
                fileName = "未加载书籍",
                sentences = emptyList(),
                currentIndex = 0,
                isPlaying = false
            )
        }
    }

    fun deleteRecentBook(id: String) {
        recentBooksStore.deleteRecentBook(id)
        _uiState.update { it.copy(recentBooks = recentBooksStore.getRecentBooks()) }
    }

    fun setRecentBooksDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isRecentBooksDialogOpen = isOpen) }
    }

    fun toggleSplitEnabled() {
        val nextState = !_uiState.value.isSplitEnabled
        val sentences = TextSegmenter.splitIntoSentences(
            _uiState.value.rawText,
            _uiState.value.splitMode,
            nextState
        )
        val wasPlaying = _uiState.value.isPlaying
        stopPlayback()

        _uiState.update {
            it.copy(
                isSplitEnabled = nextState,
                splitComma = nextState,
                sentences = sentences,
                currentIndex = if (sentences.isNotEmpty()) 0 else -1,
                currentRepeatPass = 1,
                currentSpeakerPass = 0
            )
        }

        if (wasPlaying && sentences.isNotEmpty()) {
            jumpToSentence(0)
        }
    }

    fun toggleSplitMode() {
        val nextMode = if (_uiState.value.splitMode == SplitMode.BREAK_ITERATOR) {
            SplitMode.FULL_SENTENCE
        } else {
            SplitMode.BREAK_ITERATOR
        }
        setSplitMode(nextMode)
    }

    fun toggleSplitComma() {
        toggleSplitEnabled()
    }

    fun setSplitMode(mode: SplitMode) {
        val sentences = TextSegmenter.splitIntoSentences(
            _uiState.value.rawText,
            mode,
            _uiState.value.isSplitEnabled
        )
        val wasPlaying = _uiState.value.isPlaying
        stopPlayback()

        _uiState.update {
            it.copy(
                splitMode = mode,
                sentences = sentences,
                currentIndex = if (sentences.isNotEmpty()) 0 else -1,
                currentRepeatPass = 1,
                currentSpeakerPass = 0
            )
        }

        if (wasPlaying && sentences.isNotEmpty()) {
            jumpToSentence(0)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun setReadingDisplayMode(mode: ReadingDisplayMode) {
        _uiState.update { it.copy(readingDisplayMode = mode) }
    }

    fun togglePlayPause() {
        val state = _uiState.value
        if (state.sentences.isEmpty()) return

        if (state.isPlaying) {
            stopPlayback()
        } else {
            val indexToPlay = if (state.currentIndex in state.sentences.indices) {
                state.currentIndex
            } else {
                0
            }
            _uiState.update {
                it.copy(
                    currentIndex = indexToPlay,
                    isPlaying = true,
                    currentRepeatPass = 1,
                    currentSpeakerPass = 0
                )
            }
            playCurrentSentence()
        }
    }

    fun playPrevious() {
        val state = _uiState.value
        if (state.sentences.isEmpty()) return
        val prevIndex = (state.currentIndex - 1).coerceAtLeast(0)
        jumpToSentence(prevIndex)
    }

    fun playNext() {
        val state = _uiState.value
        if (state.sentences.isEmpty()) return
        val nextIndex = (state.currentIndex + 1).coerceAtMost(state.sentences.size - 1)
        jumpToSentence(nextIndex)
    }

    fun skipForward(count: Int = 5) {
        val state = _uiState.value
        if (state.sentences.isEmpty()) return
        val targetIndex = (state.currentIndex + count).coerceAtMost(state.sentences.size - 1)
        jumpToSentence(targetIndex)
    }

    fun jumpToSentence(index: Int) {
        val state = _uiState.value
        if (index !in state.sentences.indices) return
        stopPlayback()
        _uiState.update {
            it.copy(
                currentIndex = index,
                isPlaying = true,
                currentRepeatPass = 1,
                currentSpeakerPass = 0
            )
        }
        recentBooksStore.updateProgress(state.fileName, index)
        playCurrentSentence()
    }

    private fun playCurrentSentence() {
        val state = _uiState.value
        if (!state.isPlaying || state.currentIndex !in state.sentences.indices) {
            stopPlayback()
            return
        }

        val sentence = state.sentences[state.currentIndex]
        val voiceId = if (state.multiSpeakerEnabled) {
            when (state.currentSpeakerPass) {
                1 -> state.speaker2VoiceId ?: state.speaker1VoiceId
                2 -> state.speaker3VoiceId ?: state.speaker1VoiceId
                else -> state.speaker1VoiceId
            }
        } else {
            when (state.activeSpeakerIndex) {
                1 -> state.speaker2VoiceId ?: state.speaker1VoiceId
                2 -> state.speaker3VoiceId ?: state.speaker1VoiceId
                else -> state.speaker1VoiceId
            }
        }

        // If voices are identical, modulate pitch & rate slightly to give distinct speaker flavors
        var pitch = state.pitch
        var rate = state.speechRate
        if (state.multiSpeakerEnabled) {
            when (state.currentSpeakerPass) {
                1 -> {
                    pitch = (state.pitch * 1.25f).coerceAtMost(2.0f)
                    rate = (state.speechRate * 1.05f).coerceAtMost(2.5f)
                }
                2 -> {
                    pitch = (state.pitch * 0.85f).coerceAtLeast(0.4f)
                    rate = (state.speechRate * 0.95f).coerceAtLeast(0.5f)
                }
            }
        }

        utteranceCounter++
        val utteranceId = "play_${state.currentIndex}_${state.currentRepeatPass}_${state.currentSpeakerPass}_$utteranceCounter"
        ttsManager.speak(sentence, voiceId, rate, pitch, utteranceId)
    }

    private fun handleUtteranceCompleted(utteranceId: String) {
        if (!utteranceId.startsWith("play_")) return
        val state = _uiState.value
        if (!state.isPlaying) return

        if (state.multiSpeakerEnabled && state.currentSpeakerPass < 2) {
            // Advance to next speaker for the same sentence
            _uiState.update { it.copy(currentSpeakerPass = it.currentSpeakerPass + 1) }
            playCurrentSentence()
            return
        }

        // All speakers for this pass have completed
        val maxRepeats = if (state.targetRepeatCount >= 999) Int.MAX_VALUE else state.targetRepeatCount
        if (state.currentRepeatPass < maxRepeats) {
            // Repeat this sentence again
            _uiState.update {
                it.copy(
                    currentRepeatPass = it.currentRepeatPass + 1,
                    currentSpeakerPass = 0
                )
            }
            playCurrentSentence()
            return
        }

        // Move to next sentence
        val nextIndex = state.currentIndex + 1
        if (nextIndex < state.sentences.size) {
            _uiState.update {
                it.copy(
                    currentIndex = nextIndex,
                    currentRepeatPass = 1,
                    currentSpeakerPass = 0
                )
            }
            playCurrentSentence()
        } else {
            // Finished all sentences
            stopPlayback()
        }
    }

    fun stopPlayback() {
        ttsManager.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun setTargetRepeatCount(count: Int) {
        _uiState.update { it.copy(targetRepeatCount = count) }
    }

    fun cycleRepeatCount() {
        val next = when (_uiState.value.targetRepeatCount) {
            1 -> 2
            2 -> 3
            3 -> 5
            5 -> 999
            else -> 1
        }
        setTargetRepeatCount(next)
    }

    fun setMultiSpeakerEnabled(enabled: Boolean) {
        _uiState.update { it.copy(multiSpeakerEnabled = enabled) }
    }

    fun toggleMultiSpeaker() {
        _uiState.update { it.copy(multiSpeakerEnabled = !it.multiSpeakerEnabled) }
    }

    /**
     * Sleep timer: minutes = 0 (disabled), 15, 30, 45, 60.
     */
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _uiState.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0) }
            return
        }
        val totalSeconds = minutes * 60
        _uiState.update { it.copy(sleepTimerMinutes = minutes, sleepTimerRemainingSeconds = totalSeconds) }

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            // Expired -> Pause playback & reset
            stopPlayback()
            _uiState.update { it.copy(sleepTimerMinutes = 0, sleepTimerRemainingSeconds = 0) }
        }
    }

    fun toggleQuickSleepTimer() {
        if (_uiState.value.sleepTimerMinutes > 0) {
            setSleepTimer(0)
        } else {
            setSleepTimer(30) // Default 30 minutes
        }
    }

    fun setLanguage(lang: String) {
        _uiState.update { it.copy(selectedLanguage = lang) }
        autoAssignVoices(ttsManager.availableVoices.value, lang)
    }

    fun setActiveSpeaker(speakerIndex: Int) {
        _uiState.update { it.copy(activeSpeakerIndex = speakerIndex.coerceIn(0, 2), multiSpeakerEnabled = false) }
        if (_uiState.value.isPlaying) {
            playCurrentSentence()
        }
    }

    fun setSpeakerVoice(speakerIndex: Int, voiceId: String?) {
        _uiState.update {
            when (speakerIndex) {
                0 -> it.copy(speaker1VoiceId = voiceId)
                1 -> it.copy(speaker2VoiceId = voiceId)
                2 -> it.copy(speaker3VoiceId = voiceId)
                else -> it
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun setPitch(pitch: Float) {
        _uiState.update { it.copy(pitch = pitch) }
    }

    fun testVoice(speakerIndex: Int) {
        val state = _uiState.value
        val voiceId = when (speakerIndex) {
            1 -> state.speaker2VoiceId ?: state.speaker1VoiceId
            2 -> state.speaker3VoiceId ?: state.speaker1VoiceId
            else -> state.speaker1VoiceId
        }

        var pitch = state.pitch
        var rate = state.speechRate
        if (state.multiSpeakerEnabled) {
            when (speakerIndex) {
                1 -> pitch = (state.pitch * 1.25f).coerceAtMost(2.0f)
                2 -> pitch = (state.pitch * 0.85f).coerceAtLeast(0.4f)
            }
        }

        val testPhrase = if (state.selectedLanguage.startsWith("zh")) {
            "你好，这是第 ${speakerIndex + 1} 位发音人的试听效果。"
        } else {
            "Hello, this is a voice sample for speaker ${speakerIndex + 1}."
        }

        ttsManager.speak(testPhrase, voiceId, rate, pitch, "test_voice_$speakerIndex")
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun setExportDialogOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isExportDialogOpen = isOpen) }
        if (!isOpen) {
            audioExporter.stopPreview()
            _uiState.update {
                it.copy(exportState = it.exportState.copy(isPreviewPlaying = false))
            }
        }
    }


    /**
     * Starts synthesis and export of the entire document to an audio file (MP3 / WAV).
     */
    fun startExport(includeMultiSpeaker: Boolean, repeatPerSentence: Int, targetFormat: String = "mp3") {
        val state = _uiState.value
        if (state.sentences.isEmpty()) return

        stopPlayback()
        exportJob?.cancel()

        val totalSentences = state.sentences.size
        val speakerPasses = if (includeMultiSpeaker) 3 else 1
        val repeats = repeatPerSentence.coerceIn(1, 5)
        val totalSteps = totalSentences * speakerPasses * repeats

        _uiState.update {
            it.copy(
                exportState = ExportState(
                    isExporting = true,
                    progress = 0f,
                    currentStep = 0,
                    totalSteps = totalSteps,
                    statusMessage = "正在准备合成音频...",
                    exportedFile = null,
                    errorMessage = null
                )
            )
        }

        exportJob = viewModelScope.launch {
            val chunkFiles = mutableListOf<File>()
            val cacheDir = getApplication<Application>().cacheDir
            var currentStep = 0

            try {
                for (sIdx in state.sentences.indices) {
                    val sentence = state.sentences[sIdx]

                    for (r in 1..repeats) {
                        for (sp in 0 until speakerPasses) {
                            currentStep++
                            val voiceId = when (sp) {
                                1 -> state.speaker2VoiceId ?: state.speaker1VoiceId
                                2 -> state.speaker3VoiceId ?: state.speaker1VoiceId
                                else -> state.speaker1VoiceId
                            }

                            var pitch = state.pitch
                            var rate = state.speechRate
                            if (includeMultiSpeaker) {
                                when (sp) {
                                    1 -> {
                                        pitch = (state.pitch * 1.25f).coerceAtMost(2.0f)
                                        rate = (state.speechRate * 1.05f).coerceAtMost(2.5f)
                                    }
                                    2 -> {
                                        pitch = (state.pitch * 0.85f).coerceAtLeast(0.4f)
                                        rate = (state.speechRate * 0.95f).coerceAtLeast(0.5f)
                                    }
                                }
                            }

                            val chunkFile = File(cacheDir, "synth_chunk_${sIdx}_${r}_${sp}.wav")
                            _uiState.update {
                                it.copy(
                                    exportState = it.exportState.copy(
                                        currentStep = currentStep,
                                        progress = currentStep.toFloat() / totalSteps.toFloat(),
                                        statusMessage = "合成中: 第 ${sIdx + 1}/$totalSentences 句 (第 $r 遍 · 发音人 ${sp + 1})"
                                    )
                                )
                            }

                            val success = ttsManager.synthesizeToFile(
                                text = sentence,
                                voiceId = voiceId,
                                rate = rate,
                                pitch = pitch,
                                outputFile = chunkFile,
                                utteranceId = "export_chunk_${currentStep}"
                            )

                            if (success && chunkFile.exists() && chunkFile.length() > 0) {
                                chunkFiles.add(chunkFile)
                            }
                            delay(40) // Small pause between synthesis chunks
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        exportState = it.exportState.copy(
                            statusMessage = "正在合并与转码为 $targetFormat 文件..."
                        )
                    )
                }

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val cleanDocName = state.fileName.substringBeforeLast(".").take(12).ifBlank { "tts" }
                val exportDir = File(getApplication<Application>().getExternalFilesDir(null), "exports").apply { mkdirs() }

                val masterWavFile = File(cacheDir, "master_${timeStamp}.wav")
                val combined = audioExporter.concatenateWavFiles(chunkFiles, masterWavFile)

                if (!combined || !masterWavFile.exists()) {
                    throw IllegalStateException("音频文件合并失败")
                }

                val finalAudioFile: File
                if (targetFormat.equals("mp3", ignoreCase = true)) {
                    val mp3File = File(exportDir, "${cleanDocName}_${timeStamp}.mp3")
                    val converted = audioExporter.convertWavToMp3(masterWavFile, mp3File)
                    finalAudioFile = if (converted && mp3File.exists()) mp3File else masterWavFile
                } else {
                    val wavFile = File(exportDir, "${cleanDocName}_${timeStamp}.wav")
                    masterWavFile.copyTo(wavFile, overwrite = true)
                    finalAudioFile = wavFile
                }

                // Cleanup temporary chunk files
                chunkFiles.forEach { it.delete() }
                masterWavFile.delete()

                _uiState.update {
                    it.copy(
                        exportState = it.exportState.copy(
                            isExporting = false,
                            progress = 1f,
                            statusMessage = "音频导出成功！",
                            exportedFile = finalAudioFile
                        )
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Export error: ${e.message}", e)
                chunkFiles.forEach { it.delete() }
                _uiState.update {
                    it.copy(
                        exportState = it.exportState.copy(
                            isExporting = false,
                            errorMessage = "导出失败: ${e.localizedMessage ?: "未知错误"}"
                        )
                    )
                }
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _uiState.update {
            it.copy(
                exportState = it.exportState.copy(
                    isExporting = false,
                    statusMessage = "导出已取消"
                )
            )
        }
    }

    fun togglePreviewPlayback() {
        val file = _uiState.value.exportState.exportedFile ?: return
        if (audioExporter.isPreviewPlaying()) {
            audioExporter.stopPreview()
            _uiState.update {
                it.copy(exportState = it.exportState.copy(isPreviewPlaying = false))
            }
        } else {
            audioExporter.playPreview(file) {
                _uiState.update {
                    it.copy(exportState = it.exportState.copy(isPreviewPlaying = false))
                }
            }
            _uiState.update {
                it.copy(exportState = it.exportState.copy(isPreviewPlaying = true))
            }
        }
    }

    fun createShareIntent(): Intent? {
        val file = _uiState.value.exportState.exportedFile ?: return null
        return audioExporter.shareAudioFile(file)
    }

    fun setWordLookupMode(mode: WordLookupMode) {
        _uiState.update { it.copy(wordLookupMode = mode) }
    }

    fun setDefaultDictApp(app: DictAppOption) {
        _uiState.update { it.copy(defaultDictApp = app) }
    }

    fun dismissPopover() {
        _uiState.update { it.copy(activePopoverWord = null) }
    }

    fun copyWordToClipboard(context: android.content.Context, word: String) {
        dismissPopover()
        val cleanWord = word.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim()
        if (cleanWord.isEmpty()) return
        try {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("word", cleanWord)
            clipboard.setPrimaryClip(clip)
            android.widget.Toast.makeText(context, "已复制: $cleanWord", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy word", e)
        }
    }

    fun openSentenceInspection(sentence: String) {
        _uiState.update {
            it.copy(
                selectedSentenceForInspection = sentence,
                isSentenceInspectionOpen = true
            )
        }
    }

    fun closeSentenceInspection() {
        _uiState.update {
            it.copy(
                selectedSentenceForInspection = null,
                isSentenceInspectionOpen = false
            )
        }
    }

    fun lookupWord(context: android.content.Context, word: String) {
        val cleanWord = word.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim()
        if (cleanWord.isEmpty()) return

        when (_uiState.value.wordLookupMode) {
            WordLookupMode.DIRECT_DICT -> {
                if (_uiState.value.defaultDictApp == DictAppOption.LOCAL_MDX) {
                    performLocalMdictLookup(cleanWord)
                } else {
                    launchDictLookup(context, cleanWord, _uiState.value.defaultDictApp)
                }
            }
            WordLookupMode.POPOVER_MENU -> {
                _uiState.update { it.copy(activePopoverWord = cleanWord) }
            }
            WordLookupMode.LOCAL_MDX -> {
                performLocalMdictLookup(cleanWord)
            }
        }
    }

    fun performLocalMdictLookup(word: String) {
        dismissPopover()
        val cleanWord = word.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim()
        if (cleanWord.isEmpty()) return

        viewModelScope.launch {
            val result = mdictParser.lookupWord(cleanWord)
            _uiState.update { 
                it.copy(
                    localMdictResult = result,
                    isLocalMdictBottomSheetOpen = true
                ) 
            }

            if (_uiState.value.mdictAutoPronounce) {
                pronounceWord(cleanWord, result)
            }
        }
    }

    fun pronounceWord(word: String, result: com.example.data.MdictSearchResult? = null) {
        val targetResult = result ?: _uiState.value.localMdictResult
        val cleanWord = word.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim()
        if (cleanWord.isEmpty()) return

        if (targetResult?.audioBytes != null && targetResult.audioBytes.isNotEmpty()) {
            val success = mdictParser.playAudioBytes(targetResult.audioBytes)
            if (!success) {
                val locale = if (_uiState.value.mdictPronounceAccent == "US") java.util.Locale.US else java.util.Locale.UK
                ttsManager.speakWordWithAccent(cleanWord, locale)
            }
        } else {
            val locale = if (_uiState.value.mdictPronounceAccent == "US") java.util.Locale.US else java.util.Locale.UK
            ttsManager.speakWordWithAccent(cleanWord, locale)
        }
    }

    fun setMdictFile(context: android.content.Context, mdxUri: android.net.Uri, mddUri: android.net.Uri? = null) {
        viewModelScope.launch {
            val fileName = getFileNameFromUri(context, mdxUri) ?: "本地 MDX 词典"
            val success = mdictParser.setDictionaryUri(mdxUri, mddUri)
            _uiState.update { 
                it.copy(
                    mdictMdxUri = mdxUri,
                    mdictMddUri = mddUri,
                    mdictMdxFileName = fileName
                ) 
            }
            if (success) {
                android.widget.Toast.makeText(context, "已离线载入词典: $fileName", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "解析词典索引成功: $fileName", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun closeLocalMdictBottomSheet() {
        _uiState.update { it.copy(isLocalMdictBottomSheetOpen = false) }
    }

    fun setMdictAutoPronounce(enabled: Boolean) {
        _uiState.update { it.copy(mdictAutoPronounce = enabled) }
    }

    fun setMdictPronounceAccent(accent: String) {
        _uiState.update { it.copy(mdictPronounceAccent = accent) }
    }

    fun setEudicInvokeMode(mode: EudicInvokeMode) {
        _uiState.update { it.copy(eudicInvokeMode = mode) }
    }

    private fun getFileNameFromUri(context: android.content.Context, uri: android.net.Uri): String? {
        return try {
            var name: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex)
                    }
                }
            }
            name ?: uri.lastPathSegment
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    fun translateSentence(context: android.content.Context, sentence: String) {
        val cleanText = sentence.trim()
        if (cleanText.isEmpty()) return

        val googlePkg = "com.google.android.apps.translate"

        // 1. Try ACTION_PROCESS_TEXT with Google Translate app
        val processIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, cleanText)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            setPackage(googlePkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val resolveInfos = context.packageManager.queryIntentActivities(processIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(processIntent)
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Google Translate process intent error: ${e.message}")
        }

        // 2. Try ACTION_SEND with Google Translate app
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cleanText)
            setPackage(googlePkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            val resolveInfos = context.packageManager.queryIntentActivities(sendIntent, 0)
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(sendIntent)
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "Google Translate send intent error: ${e.message}")
        }

        // 3. Fallback: Open Google Translate Web URL in browser
        try {
            val encodedText = java.net.URLEncoder.encode(cleanText, "UTF-8")
            val webUri = android.net.Uri.parse("https://translate.google.com/?sl=auto&text=$encodedText")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Google Translate web intent error: ${e.message}")
            launchProcessText(context, cleanText)
        }
    }

    fun launchDictLookup(context: android.content.Context, word: String, dictApp: DictAppOption) {
        dismissPopover()
        val cleanWord = word.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim()
        if (cleanWord.isEmpty()) return

        if (dictApp == DictAppOption.LOCAL_MDX) {
            performLocalMdictLookup(cleanWord)
            return
        }

        val pkg = dictApp.packageName
        if (pkg == "com.eusoft.eudic" || dictApp == DictAppOption.EUDIC) {
            val mode = _uiState.value.eudicInvokeMode
            var success = when (mode) {
                EudicInvokeMode.EXPLICIT_INTENT -> tryEudicExplicitIntent(context, cleanWord)
                EudicInvokeMode.URL_SCHEME -> tryEudicUrlScheme(context, cleanWord)
                EudicInvokeMode.DIRECT_SEND -> tryEudicDirectSend(context, cleanWord)
            }

            if (!success) {
                if (mode != EudicInvokeMode.EXPLICIT_INTENT && tryEudicExplicitIntent(context, cleanWord)) success = true
                else if (mode != EudicInvokeMode.URL_SCHEME && tryEudicUrlScheme(context, cleanWord)) success = true
                else if (mode != EudicInvokeMode.DIRECT_SEND && tryEudicDirectSend(context, cleanWord)) success = true
            }

            if (success) return

            android.widget.Toast.makeText(context, "未找到欧路词典，已切换系统划词/分享", android.widget.Toast.LENGTH_SHORT).show()
        } else if (pkg != null) {
            // Specified package like Google Translate
            val processIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, cleanWord)
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                val resolve = context.packageManager.queryIntentActivities(processIntent, 0)
                if (resolve.isNotEmpty()) {
                    context.startActivity(processIntent)
                    return
                } else {
                    val appName = if (pkg.contains("translate")) "谷歌翻译" else "所选词典"
                    android.widget.Toast.makeText(context, "未安装$appName，已切换系统划词/分享", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.d(TAG, "ProcessText not available for $pkg: ${e.message}")
            }
        }

        // General Fallback
        launchProcessText(context, cleanWord)
    }

    private fun tryEudicExplicitIntent(context: android.content.Context, word: String): Boolean {
        return try {
            val intent = Intent("colordict.intent.action.SEARCH").apply {
                setClassName("com.eusoft.eudic", "com.eusoft.dict.activity.dict.LightpeekActivity")
                putExtra("EXTRA_QUERY", word)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolve = context.packageManager.queryIntentActivities(intent, 0)
            if (resolve.isNotEmpty()) {
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            Log.d(TAG, "Eudic explicit intent failed: ${e.message}")
            false
        }
    }

    private fun tryEudicUrlScheme(context: android.content.Context, word: String): Boolean {
        return try {
            val uri = android.net.Uri.parse("eudic://peek/${android.net.Uri.encode(word)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolve = context.packageManager.queryIntentActivities(intent, 0)
            if (resolve.isNotEmpty()) {
                context.startActivity(intent)
                true
            } else false
        } catch (e: Exception) {
            Log.d(TAG, "Eudic URL scheme failed: ${e.message}")
            false
        }
    }

    private fun tryEudicDirectSend(context: android.content.Context, word: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, word)
                setPackage("com.eusoft.eudic")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolve = context.packageManager.queryIntentActivities(sendIntent, 0)
            if (resolve.isNotEmpty()) {
                context.startActivity(sendIntent)
                true
            } else false
        } catch (e: Exception) {
            Log.d(TAG, "Eudic direct send failed: ${e.message}")
            false
        }
    }

    fun launchProcessText(context: android.content.Context, text: String, targetPackage: String? = null) {
        val cleanText = text.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5\\s]"), "").trim()
        if (cleanText.isEmpty()) return

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_PROCESS_TEXT, cleanText)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            val packageManager = context.packageManager
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            if (resolveInfos.isNotEmpty()) {
                context.startActivity(intent)
                return
            }
        } catch (e: Exception) {
            Log.d(TAG, "ProcessText failed with package $targetPackage: ${e.message}")
        }

        // Fallback: Use ACTION_SEND chooser
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, cleanText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(Intent.createChooser(sendIntent, "选择词典/翻译").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.d(TAG, "Chooser failed: ${e.message}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioExporter.stopPreview()
        ttsManager.shutdown()
    }
}
