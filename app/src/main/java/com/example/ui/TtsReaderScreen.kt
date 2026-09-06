package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.viewinterop.AndroidView
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentParser
import com.example.data.RecentBook

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsReaderScreen(
    viewModel: TtsReaderViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var isPasteDialogOpen by remember { mutableStateOf(false) }

    // Supports TXT, EPUB, SRT, VTT, LRC
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val parsed = DocumentParser.parseUri(context, uri)
                if (parsed.text.isBlank()) {
                    Toast.makeText(context, "未在文件中找到有效可朗读文本", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.loadText(parsed.text, parsed.title)
                    Toast.makeText(context, "已导入 [${parsed.format}]: ${parsed.title} (${parsed.text.length} 字)", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Back handler: returning to home if in playback view
    BackHandler(enabled = uiState.sentences.isNotEmpty()) {
        viewModel.returnToHome()
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Smoothly scroll the active playing sentence directly to the vertical center of the window
    LaunchedEffect(uiState.currentIndex) {
        if (uiState.currentIndex in uiState.sentences.indices) {
            lazyListState.animateScrollToItem(uiState.currentIndex)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            VibrantHeaderBar(
                fileName = uiState.fileName,
                currentIndex = uiState.currentIndex,
                totalSentences = uiState.sentences.size,
                hasRecentBooks = uiState.recentBooks.isNotEmpty(),
                onHomeClick = { viewModel.returnToHome() },
                onRecentBooksClick = { viewModel.setRecentBooksDialogOpen(true) },
                onUploadClick = { filePickerLauncher.launch("*/*") },
                onExportClick = {
                    if (uiState.sentences.isNotEmpty()) {
                        viewModel.setExportDialogOpen(true)
                    } else {
                        Toast.makeText(context, "请先导入文本再导出音频", Toast.LENGTH_SHORT).show()
                    }
                },
                onSettingsClick = { viewModel.setSettingsOpen(true) }
            )
        },
        bottomBar = {
            VibrantFooterPlayerBar(
                uiState = uiState,
                onToggleSplitComma = { viewModel.toggleSplitComma() },
                onPlayPrevious = { viewModel.playPrevious() },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onPlayNext = { viewModel.playNext() },
                onSetRepeatCount = { viewModel.setTargetRepeatCount(it) },
                onSetSpeechRate = { viewModel.setSpeechRate(it) },
                onSetSleepTimer = { viewModel.setSleepTimer(it) },
                onSelectSpeaker = { viewModel.setActiveSpeaker(it) },
                onToggleMultiSpeaker = { viewModel.toggleMultiSpeaker() },
                onOpenSettings = { viewModel.setSettingsOpen(true) },
                onSeek = { targetIndex -> viewModel.jumpToSentence(targetIndex) }
            )
        }
    ) { innerPadding ->
        val bgColor = MaterialTheme.colorScheme.background
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(bgColor)
        ) {
            val halfViewport = maxHeight / 2

            if (uiState.sentences.isEmpty()) {
                VibrantEmptyState(
                    recentBooks = uiState.recentBooks,
                    onUploadClick = { filePickerLauncher.launch("*/*") },
                    onPasteClick = { isPasteDialogOpen = true },
                    onSelectRecentBook = { viewModel.openRecentBook(it) },
                    onDeleteRecentBook = { viewModel.deleteRecentBook(it) }
                )
            } else {
                // BOOK_PAGE: Classic e-reader view with centered active sentence highlighting
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = halfViewport - 28.dp,
                            bottom = halfViewport + 50.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        itemsIndexed(uiState.sentences) { index, sentence ->
                            val isCurrent = index == uiState.currentIndex
                            VibrantSentenceItem(
                                sentence = sentence,
                                index = index,
                                isCurrent = isCurrent,
                                isPlaying = isCurrent && uiState.isPlaying,
                                viewModel = viewModel,
                                onClick = {
                                    viewModel.jumpToSentence(index)
                                    coroutineScope.launch {
                                        lazyListState.animateScrollToItem(index)
                                    }
                                },
                                onLongClick = {
                                    viewModel.translateSentence(context, sentence)
                                }
                            )
                        }
                    }
                }

                // Top subtle gradient mask
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(bgColor, Color.Transparent)
                            )
                        )
                )

                // Bottom subtle gradient mask
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, bgColor)
                            )
                        )
                )
            }
        }
    }

    if (uiState.isSettingsOpen) {
        SettingsBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setSettingsOpen(false) }
        )
    }

    if (uiState.isExportDialogOpen) {
        ExportAudioDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.setExportDialogOpen(false) }
        )
    }


    if (uiState.isRecentBooksDialogOpen) {
        RecentBooksDialog(
            recentBooks = uiState.recentBooks,
            onDismiss = { viewModel.setRecentBooksDialogOpen(false) },
            onSelectBook = { book ->
                viewModel.openRecentBook(book)
                viewModel.setRecentBooksDialogOpen(false)
            },
            onDeleteBook = { id ->
                viewModel.deleteRecentBook(id)
            }
        )
    }

    if (isPasteDialogOpen) {
        PasteTextDialog(
            onDismiss = { isPasteDialogOpen = false },
            onConfirm = { text, title ->
                viewModel.loadText(text, title)
                isPasteDialogOpen = false
            }
        )
    }

    if (uiState.isSentenceInspectionOpen && uiState.selectedSentenceForInspection != null) {
        SentenceInspectionBottomSheet(
            sentence = uiState.selectedSentenceForInspection!!,
            viewModel = viewModel,
            onDismiss = { viewModel.closeSentenceInspection() }
        )
    }

    if (uiState.isLocalMdictBottomSheetOpen && uiState.localMdictResult != null) {
        LocalMdictBottomSheet(
            result = uiState.localMdictResult!!,
            viewModel = viewModel,
            onDismiss = { viewModel.closeLocalMdictBottomSheet() }
        )
    }

    if (uiState.wordLookupMode == WordLookupMode.POPOVER_MENU && !uiState.activePopoverWord.isNullOrEmpty()) {
        val popoverWord = uiState.activePopoverWord!!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag("floating_popover_bar")
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = popoverWord,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    FilterChip(
                        selected = false,
                        onClick = { viewModel.performLocalMdictLookup(popoverWord) },
                        label = { Text("📖 MDX 离线词典", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = { viewModel.launchDictLookup(context, popoverWord, DictAppOption.EUDIC) },
                        label = { Text("📘 欧路词典", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = { viewModel.launchDictLookup(context, popoverWord, DictAppOption.GOOGLE_TRANSLATE) },
                        label = { Text("🌐 谷歌翻译", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = { viewModel.copyWordToClipboard(context, popoverWord) },
                        label = { Text("📋 复制", fontSize = 13.sp) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = { viewModel.pronounceWord(popoverWord) },
                        label = { Text("🔊 发音", fontSize = 13.sp) }
                    )

                    IconButton(
                        onClick = { viewModel.dismissPopover() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top Header Bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VibrantHeaderBar(
    fileName: String,
    currentIndex: Int,
    totalSentences: Int,
    hasRecentBooks: Boolean,
    onHomeClick: () -> Unit,
    onRecentBooksClick: () -> Unit,
    onUploadClick: () -> Unit,
    onExportClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("top_header_bar"),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        navigationIcon = {
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (totalSentences > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .testTag("header_home_btn")
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "返回主页",
                    tint = if (totalSentences > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        title = {
            if (totalSentences > 0) {
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currentIndex + 1} / $totalSentences 句",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        actions = {
            if (hasRecentBooks) {
                IconButton(
                    onClick = onRecentBooksClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("header_recent_books_btn")
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = "最近阅读书籍",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            IconButton(
                onClick = onUploadClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("header_upload_btn")
            ) {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = "上传文档或字幕",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onExportClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("header_export_btn")
            ) {
                Icon(
                    Icons.Default.DownloadForOffline,
                    contentDescription = "下载音频",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("header_settings_btn")
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }
    )
}

/**
 * Home Screen Empty State with TXT / EPUB / SRT / VTT / LRC support
 */
@Composable
private fun VibrantEmptyState(
    recentBooks: List<RecentBook>,
    onUploadClick: () -> Unit,
    onPasteClick: () -> Unit,
    onSelectRecentBook: (RecentBook) -> Unit,
    onDeleteRecentBook: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("empty_state_upload_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Upload and Paste cards in the same row, equal size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Upload File Card (weight 1f)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
                    .clickable { onUploadClick() }
                    .testTag("quick_upload_card"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.UploadFile,
                            contentDescription = "上传文件",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "上传文件",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "TXT · EPUB · 字幕",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Paste Text Card (weight 1f)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(170.dp)
                    .clickable { onPasteClick() }
                    .testTag("quick_paste_card"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = "粘贴文本",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "粘贴文本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "从剪贴板导入",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Books Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "最近阅读书籍",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            if (recentBooks.isNotEmpty()) {
                Text(
                    text = "共 ${recentBooks.size} 本",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (recentBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无最近阅读记录\n上传文件或粘贴文本即可开始",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentBooks, key = { it.id }) { book ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRecentBook(book) }
                            .testTag("home_recent_book_${book.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "读至第 ${book.lastIndex + 1} 句 / 共 ${book.sentenceCount} 句",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (book.snippet.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = book.snippet,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteRecentBook(book.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除记录",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sentence Item View:
 * Supports direct word click lookup, sentence long-press translation, and outer space click play/jump.
 * Increased row height and padding reduce mis-operations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VibrantSentenceItem(
    sentence: String,
    index: Int,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    viewModel: TtsReaderViewModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val highlightBg = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    } else {
        Color.Transparent
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val tokens = remember(sentence) {
        sentence.split(Regex("(?<=\\s)|(?=\\s)"))
    }

    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(highlightBg)
            .pointerInput(sentence) {
                detectTapGestures(
                    onTap = { 
                        // Single tap on blank space/margins jumps to sentence
                        viewModel.dismissPopover()
                        onClick() 
                    },
                    onLongPress = { 
                        viewModel.dismissPopover()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick() 
                    },
                    onDoubleTap = {
                        // Double tap triggers full sentence translation!
                        viewModel.dismissPopover()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.translateSentence(context, sentence)
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("sentence_item_$index"),
        contentAlignment = Alignment.Center
    ) {
        SelectionContainer {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                tokens.forEach { token ->
                    val trimmed = token.trim()
                    val cleanTrimmed = remember(trimmed) { trimmed.replace(Regex("[^a-zA-Z\\u4e00-\\u9fa5]"), "").trim() }
                    if (trimmed.isEmpty()) {
                        Text(
                            text = token,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 30.sp
                            )
                        )
                    } else {
                        val isPopoverActive = uiState.wordLookupMode == WordLookupMode.POPOVER_MENU &&
                                uiState.activePopoverWord == cleanTrimmed &&
                                cleanTrimmed.isNotEmpty()

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isPopoverActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.lookupWord(context, trimmed)
                                }
                                .padding(horizontal = 3.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = token,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = if (isCurrent) 19.sp else 17.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isPopoverActive) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else if (isCurrent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Footer Player Bar:
 * - Row 1: Icon-only Previous, Play/Pause, Next
 * - Row 2 (Buttons row below Play):
 *     1. Sleep Timer (leftmost) -> Expands dropdown: 关闭, 15m, 30m, 45m, 60m
 *     2. Repeat Count -> Expands dropdown: 1x, 2x, 3x, 5x, ∞
 *     3. Playback Speed -> Expands dropdown: 0.85x, 0.9x, 0.95x, 1.0x, 1.05x, 1.1x, 1.2x
 *     4. Comma Split -> Toggles "，"
 * - Bottom Bar (最下面):
 *     Speaker Selector Bar -> Expands dropdown with 3 configured favorite voices & multi-speaker toggle
 */
@Composable
private fun VibrantFooterPlayerBar(
    uiState: ReaderUiState,
    onToggleSplitComma: () -> Unit,
    onPlayPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onSetRepeatCount: (Int) -> Unit,
    onSetSpeechRate: (Float) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onSelectSpeaker: (Int) -> Unit,
    onToggleMultiSpeaker: () -> Unit,
    onOpenSettings: () -> Unit,
    onSeek: (Int) -> Unit
) {
    val totalSentences = uiState.sentences.size
    val currentPosition = (uiState.currentIndex + 1).coerceAtLeast(0)
    val progressFraction = if (totalSentences > 0) {
        (currentPosition.toFloat() / totalSentences.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    var isSleepMenuOpen by remember { mutableStateOf(false) }
    var isRepeatMenuOpen by remember { mutableStateOf(false) }
    var isSpeedMenuOpen by remember { mutableStateOf(false) }
    var isSpeakerMenuOpen by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        shadowElevation = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("bottom_player_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // 1. Progress Bar Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${currentPosition} 句",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )

                    if (totalSentences > 1) {
                        Slider(
                            value = uiState.currentIndex.toFloat().coerceAtLeast(0f),
                            onValueChange = { onSeek(it.toInt()) },
                            valueRange = 0f..(totalSentences - 1).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sentence_scrubber_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }
                }

                Text(
                    text = "${totalSentences} 句",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Playback Control Row: Repeat (icon only), Prev, Play/Pause, Next, Speed (icon only)
            val isRepeatActive = uiState.targetRepeatCount > 1
            val isSpeedCustom = kotlin.math.abs(uiState.speechRate - 1.0f) > 0.02f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button A: 循环 (Icon-only)
                Box {
                    IconButton(
                        onClick = { isRepeatMenuOpen = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_repeat_btn")
                    ) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = "循环次数",
                            tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isRepeatMenuOpen,
                        onDismissRequest = { isRepeatMenuOpen = false }
                    ) {
                        listOf(
                            1 to "单次朗读 (1x - 默认)",
                            2 to "单句循环 2 遍 (2x)",
                            3 to "单句循环 3 遍 (3x)",
                            5 to "单句循环 5 遍 (5x)",
                            999 to "无限循环当前句 (∞)"
                        ).forEach { (count, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    if (uiState.targetRepeatCount == count) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    onSetRepeatCount(count)
                                    isRepeatMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Button B: 上一句
                IconButton(
                    onClick = onPlayPrevious,
                    enabled = uiState.sentences.isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_prev_btn")
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "上一句",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Button C: 播放/暂停
                Surface(
                    onClick = onTogglePlayPause,
                    enabled = uiState.sentences.isNotEmpty(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .testTag("player_play_pause_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "暂停" else "播放",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Button D: 下一句
                IconButton(
                    onClick = onPlayNext,
                    enabled = uiState.sentences.isNotEmpty(),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("player_next_btn")
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "下一句",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Button E: 语速 (Icon-only)
                Box {
                    IconButton(
                        onClick = { isSpeedMenuOpen = true },
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("player_speed_btn")
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = "朗读速度",
                            tint = if (isSpeedCustom) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isSpeedMenuOpen,
                        onDismissRequest = { isSpeedMenuOpen = false }
                    ) {
                        listOf(
                            0.85f to "0.85x",
                            0.90f to "0.90x",
                            0.95f to "0.95x",
                            1.00f to "1.00x (标准)",
                            1.05f to "1.05x",
                            1.10f to "1.10x",
                            1.20f to "1.20x"
                        ).forEach { (rate, label) ->
                            val isSelected = kotlin.math.abs(uiState.speechRate - rate) < 0.02f
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = null)
                                },
                                onClick = {
                                    onSetSpeechRate(rate)
                                    isSpeedMenuOpen = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Quick Action Row Below Play:
            // [定时] | [轮流] | [拆分] | [设置]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: 睡眠模式快捷按钮
                val isSleepActive = uiState.sleepTimerMinutes > 0
                val sleepLabel = if (isSleepActive) {
                    val remMin = (uiState.sleepTimerRemainingSeconds + 59) / 60
                    "${remMin}m"
                } else {
                    "定时"
                }
                Box(modifier = Modifier.weight(1f)) {
                    PlayerQuickButton(
                        isActive = isSleepActive,
                        onClick = { isSleepMenuOpen = true },
                        testTag = "player_sleep_timer_btn"
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = sleepLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    DropdownMenu(
                        expanded = isSleepMenuOpen,
                        onDismissRequest = { isSleepMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("关闭定时 (无限制)") },
                            leadingIcon = {
                                if (!isSleepActive) Icon(Icons.Default.Check, contentDescription = null)
                            },
                            onClick = {
                                onSetSleepTimer(0)
                                isSleepMenuOpen = false
                            }
                        )
                        listOf(15, 30, 45, 60).forEach { mins ->
                            DropdownMenuItem(
                                text = { Text(if (mins == 30) "30 分钟 (默认)" else "$mins 分钟") },
                                leadingIcon = {
                                    if (uiState.sleepTimerMinutes == mins) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    onSetSleepTimer(mins)
                                    isSleepMenuOpen = false
                                }
                            )
                        }
                    }
                }

                // Button 2: 轮流发音按钮
                PlayerQuickButton(
                    isActive = uiState.multiSpeakerEnabled,
                    onClick = onToggleMultiSpeaker,
                    testTag = "player_multi_speaker_btn",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.SyncAlt,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "轮流",
                        fontSize = 11.5.sp,
                        fontWeight = if (uiState.multiSpeakerEnabled) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Button 3: 拆分按钮
                PlayerQuickButton(
                    isActive = uiState.isSplitEnabled,
                    onClick = onToggleSplitComma,
                    testTag = "player_split_btn",
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "拆分",
                        fontSize = 11.5.sp,
                        fontWeight = if (uiState.isSplitEnabled) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Button 4: 设置按钮
                PlayerQuickButton(
                    isActive = uiState.isSettingsOpen,
                    onClick = onOpenSettings,
                    testTag = "player_settings_btn",
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "设置",
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "设置",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. At the very bottom: Speaker Selector Bar (在播放按钮最下面添加一个发音人选项，点击展开设置好的常用发音人)
            val currentSpeakerTitle = if (uiState.multiSpeakerEnabled) {
                "3 位发音人轮流朗读模式 (⇄)"
            } else {
                val index = uiState.activeSpeakerIndex + 1
                val voiceId = when (uiState.activeSpeakerIndex) {
                    1 -> uiState.speaker2VoiceId
                    2 -> uiState.speaker3VoiceId
                    else -> uiState.speaker1VoiceId
                }
                val simpleName = voiceId?.substringAfterLast('-')?.substringAfterLast('#') ?: "默认音色"
                "常用发音人 $index ($simpleName)"
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    onClick = { isSpeakerMenuOpen = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("player_speaker_selector_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentSpeakerTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "展开选择发音人",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = isSpeakerMenuOpen,
                    onDismissRequest = { isSpeakerMenuOpen = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    // Speaker 1
                    val s1Name = uiState.speaker1VoiceId?.substringAfterLast('-') ?: "默认声音"
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("常用发音人 1 (主音)", fontWeight = FontWeight.Bold)
                                Text(s1Name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingIcon = {
                            if (!uiState.multiSpeakerEnabled && uiState.activeSpeakerIndex == 0) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onSelectSpeaker(0)
                            isSpeakerMenuOpen = false
                        }
                    )

                    // Speaker 2
                    val s2Name = uiState.speaker2VoiceId?.substringAfterLast('-') ?: "常用音色 2"
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("常用发音人 2", fontWeight = FontWeight.Bold)
                                Text(s2Name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingIcon = {
                            if (!uiState.multiSpeakerEnabled && uiState.activeSpeakerIndex == 1) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onSelectSpeaker(1)
                            isSpeakerMenuOpen = false
                        }
                    )

                    // Speaker 3
                    val s3Name = uiState.speaker3VoiceId?.substringAfterLast('-') ?: "常用音色 3"
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("常用发音人 3", fontWeight = FontWeight.Bold)
                                Text(s3Name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        leadingIcon = {
                            if (!uiState.multiSpeakerEnabled && uiState.activeSpeakerIndex == 2) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onSelectSpeaker(2)
                            isSpeakerMenuOpen = false
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Round-robin rotation mode
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (uiState.multiSpeakerEnabled) "关闭 3 位发音人轮流朗读" else "开启 3 位发音人轮流朗读",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            onToggleMultiSpeaker()
                            isSpeakerMenuOpen = false
                        }
                    )

                    // Open Settings to change voices
                    DropdownMenuItem(
                        text = {
                            Text("前往设置更换发音人音色...", fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            isSpeakerMenuOpen = false
                            onOpenSettings()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Unified Pill Action Button for the Bottom Player Row
 */
@Composable
private fun PlayerQuickButton(
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentColor = if (isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SentenceInspectionBottomSheet(
    sentence: String,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val words = remember(sentence) {
        sentence.split(Regex("\\s+|(?=[,.;:?!\"()«»—])|(?<=[,.;:?!\"()«»—])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "单词查词与整句翻译助手",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "选中的完整句子：",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.translateSentence(context, sentence)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("translate_sentence_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("长按/点击翻译整句 (离线词典/翻译)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "点击下方任意单词即可即点即查：",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                words.forEach { word ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.lookupWord(context, word)
                        },
                        label = { Text(word, fontSize = 14.sp) },
                        modifier = Modifier.testTag("word_chip_$word")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalMdictBottomSheet(
    result: com.example.data.MdictSearchResult,
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = result.word,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = { viewModel.pronounceWord(result.word, result) },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "朗读",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val htmlData = remember(result.htmlDefinition, isDark) {
                val bgHex = if (isDark) "#18181b" else "#ffffff"
                val textHex = if (isDark) "#f4f4f5" else "#18181b"
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                    <style>
                        body {
                            background-color: $bgHex !important;
                            color: $textHex !important;
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            font-size: 15px;
                            line-height: 1.6;
                            padding: 10px 12px;
                            margin: 0;
                        }
                        a { color: #60a5fa !important; }
                        b, strong { color: if ($isDark) "#60a5fa" else "#2563eb"; }
                    </style>
                </head>
                <body>
                    ${result.htmlDefinition ?: "<p>暂无离线释义</p>"}
                </body>
                </html>
                """.trimIndent()
            }

            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.defaultTextEncodingName = "UTF-8"
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(null, htmlData, "text/html", "utf-8", null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 480.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

