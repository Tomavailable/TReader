package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SplitMode
import com.example.data.TtsVoiceItem

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SettingsBottomSheet(
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val voices by viewModel.ttsManager.availableVoices.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            // Header with clear Close button on top right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "播放与语音设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_btn")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // 1. TTS 语言 (折叠卡片 1)
            // ==========================================
            ExpandableSettingSection(
                title = "TTS语言",
                subtitle = "目标发音、3人轮发音、语速与音调",
                icon = {
                    Icon(
                        Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                // Target TTS Language
                Text(
                    text = "目标朗读语言",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val languages = listOf(
                    "en-US" to "英语 (en-US)",
                    "all" to "全部可用声音"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { (code, label) ->
                        FilterChip(
                            selected = uiState.selectedLanguage == code,
                            onClick = { viewModel.setLanguage(code) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("lang_chip_$code")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Multi-speaker rotation switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "3位发音人轮流朗读",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "每句由发音人1、2、3依次朗读后再推进",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = uiState.multiSpeakerEnabled,
                        onCheckedChange = { viewModel.setMultiSpeakerEnabled(it) },
                        modifier = Modifier.testTag("multi_speaker_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Speaker 1 Selector
                VoiceSelectorItem(
                    speakerNumber = 1,
                    label = "常用发音人 1 (主音)",
                    selectedVoiceId = uiState.speaker1VoiceId,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(0, it) },
                    onAudition = { viewModel.testVoice(0) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelectorItem(
                    speakerNumber = 2,
                    label = "常用发音人 2",
                    selectedVoiceId = uiState.speaker2VoiceId,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(1, it) },
                    onAudition = { viewModel.testVoice(1) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelectorItem(
                    speakerNumber = 3,
                    label = "常用发音人 3",
                    selectedVoiceId = uiState.speaker3VoiceId,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(2, it) },
                    onAudition = { viewModel.testVoice(2) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Speech rate adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "朗读语速",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${String.format("%.2f", uiState.speechRate)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = uiState.speechRate,
                    onValueChange = { viewModel.setSpeechRate(it) },
                    valueRange = 0.5f..2.5f,
                    steps = 19,
                    modifier = Modifier.testTag("speech_rate_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        0.85f to "0.85",
                        0.9f to "0.9",
                        0.95f to "0.95",
                        1.05f to "1.05",
                        1.1f to "1.1",
                        1.2f to "1.2"
                    ).forEach { (r, lbl) ->
                        val isSelected = kotlin.math.abs(uiState.speechRate - r) < 0.02f
                        OutlinedButton(
                            onClick = { viewModel.setSpeechRate(r) },
                            colors = if (isSelected) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                             },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("rate_preset_$lbl")
                        ) {
                            Text(lbl, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pitch adjustment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "朗读音调",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${String.format("%.2f", uiState.pitch)}x",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = uiState.pitch,
                    onValueChange = { viewModel.setPitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    modifier = Modifier.testTag("speech_pitch_slider")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Loop repeat count
                Text(
                    text = "单句循环播放次数",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val loopCounts = listOf(
                    1 to "1x",
                    2 to "2x",
                    3 to "3x",
                    5 to "5x",
                    999 to "∞"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    loopCounts.forEach { (count, label) ->
                        FilterChip(
                            selected = uiState.targetRepeatCount == count,
                            onClick = { viewModel.setTargetRepeatCount(count) },
                            label = { Text(label, fontSize = 13.sp) },
                            modifier = Modifier.testTag("loop_chip_$count")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 2. 词典设置 (折叠卡片 2)
            // ==========================================
            ExpandableSettingSection(
                title = "词典设置",
                subtitle = "查词交互方案、内置 MDX 词典与自动发音",
                icon = {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val mdxPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        viewModel.setMdictFile(context, uri)
                    }
                }

                Text(
                    text = "查词响应模式：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WordLookupMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.wordLookupMode == mode,
                            onClick = { viewModel.setWordLookupMode(mode) },
                            label = {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(mode.title, fontWeight = FontWeight.Bold)
                                    Text(mode.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("lookup_mode_${mode.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "📂 外挂 / 本地 MDX 离线词典配置：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            mdxPickerLauncher.launch(arrayOf("*/*"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "无法打开文件选择器", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("select_mdx_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.mdictMdxUri != null) "已选择: ${uiState.mdictMdxFileName}" else "选择手机本地 .mdx 词典文件",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "🔊 查词自动朗读与默认发音：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("点击单词时自动朗读发音", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = uiState.mdictAutoPronounce,
                        onCheckedChange = { viewModel.setMdictAutoPronounce(it) },
                        modifier = Modifier.testTag("auto_pronounce_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.mdictPronounceAccent == "UK",
                        onClick = { viewModel.setMdictPronounceAccent("UK") },
                        label = { Text("🇬🇧 英国发音 (默认)") },
                        modifier = Modifier.weight(1f).testTag("accent_uk")
                    )
                    FilterChip(
                        selected = uiState.mdictPronounceAccent == "US",
                        onClick = { viewModel.setMdictPronounceAccent("US") },
                        label = { Text("🇺🇸 美国发音") },
                        modifier = Modifier.weight(1f).testTag("accent_us")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "默认调起外部词典软件（方案1默认及方案2快捷调起）：",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DictAppOption.entries.forEach { app ->
                        FilterChip(
                            selected = uiState.defaultDictApp == app,
                            onClick = { viewModel.setDefaultDictApp(app) },
                            label = {
                                Text(app.label, fontWeight = if (uiState.defaultDictApp == app) FontWeight.Bold else FontWeight.Normal)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("default_dict_${app.name}")
                        )

                        if (app == DictAppOption.EUDIC && uiState.defaultDictApp == DictAppOption.EUDIC) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 2.dp, bottom = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "欧路词典小窗调起方案：",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                EudicInvokeMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = uiState.eudicInvokeMode == mode,
                                        onClick = { viewModel.setEudicInvokeMode(mode) },
                                        label = {
                                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                                Text(mode.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text(mode.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("eudic_mode_${mode.name}")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 3. 睡眠模式 (折叠卡片 3)
            // ==========================================
            ExpandableSettingSection(
                title = "睡眠模式",
                subtitle = if (uiState.sleepTimerMinutes > 0) "将在 ${uiState.sleepTimerRemainingSeconds / 60} 分钟后暂停" else "定时停止播放",
                icon = {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Text(
                    text = "定时结束后自动暂停播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val sleepOptions = listOf(
                    0 to "关闭",
                    15 to "15分钟",
                    30 to "30分钟",
                    45 to "45分钟",
                    60 to "60分钟"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sleepOptions.forEach { (mins, lbl) ->
                        FilterChip(
                            selected = uiState.sleepTimerMinutes == mins,
                            onClick = { viewModel.setSleepTimer(mins) },
                            label = { Text(lbl, fontSize = 12.sp) },
                            modifier = Modifier.testTag("sleep_chip_$mins")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 4. 显示模式 (折叠卡片 4)
            // ==========================================
            ExpandableSettingSection(
                title = "显示模式",
                subtitle = "深浅色外观主题",
                icon = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                        label = { Text("跟随系统") },
                        leadingIcon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("theme_chip_system")
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("日间模式") },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("theme_chip_light")
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text("夜间模式") },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.testTag("theme_chip_dark")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 5. 断句逻辑 (折叠卡片 5)
            // ==========================================
            ExpandableSettingSection(
                title = "断句逻辑",
                subtitle = "文本切分与分句模式",
                icon = {
                    Icon(
                        Icons.Default.Spellcheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.splitMode == SplitMode.BREAK_ITERATOR,
                        onClick = { viewModel.setSplitMode(SplitMode.BREAK_ITERATOR) },
                        label = { Text("系统国际断句 (默认推荐)") },
                        modifier = Modifier.fillMaxWidth().testTag("split_chip_break_iterator")
                    )
                    FilterChip(
                        selected = uiState.splitMode == SplitMode.FULL_SENTENCE,
                        onClick = { viewModel.setSplitMode(SplitMode.FULL_SENTENCE) },
                        label = { Text("长句模式 (按句号断句，保留逗号)") },
                        modifier = Modifier.fillMaxWidth().testTag("split_chip_full_sentence")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "智能二次拆分长句",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isSplitEnabled) "已开启：依据标点在中点拆分（150字以上最多拆3段）" else "默认关闭：不二次拆分，保持完整长句",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = uiState.isSplitEnabled,
                            onCheckedChange = { viewModel.toggleSplitEnabled() },
                            modifier = Modifier.testTag("settings_split_enabled_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.splitMode.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uiState.splitMode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 6. 关于我 \^O^/ (折叠卡片 6)
            // ==========================================
            ExpandableSettingSection(
                title = "关于我 \\^O^/",
                subtitle = "应用特色与功能简介",
                icon = {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                initiallyExpanded = false
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "📖 智能朗读与多发音人",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 原生 TTS 语音合成，支持多语言、多发音人轮流朗读与单句循环；可即时微调语速与音高，轻松实现分角色朗读与语言听力训练。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "✂️ 智能断句与抗干扰",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 系统国际断句：准确按语言学小句分割，停顿自然。\n· 长句模式：保留整句完整，平滑清除软换行。\n· 智能规避：全模式避开小数点（3.14）、网址（open.ai）、英文缩写（Dr. Smith）及软换行。\n· 智能拆分：开启拆分后，依据次级标点在中点自动均衡拆分（150字以上最多拆3段）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "📚 离线 MDX 词典与生词查词",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 外挂 MDX 词典：支持选择手机本地 .mdx 词典文件，秒级零延迟查词。\n· 第三方词典调起：支持一键调起欧陆词典等应用内查词小窗。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "💾 本地离线与音频导出",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 历史文章自动存储，断点续读不丢失；支持将朗读合成的音频导出保存，方便离线听书与练习。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "祝您使用愉快！\\^O^/",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceSelectorItem(
    speakerNumber: Int,
    label: String,
    selectedVoiceId: String?,
    voices: List<TtsVoiceItem>,
    selectedLanguage: String,
    onSelectVoice: (String) -> Unit,
    onAudition: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredVoices = remember(voices, selectedLanguage) {
        if (selectedLanguage == "all") voices else {
            val prefix = selectedLanguage.substringBefore("-").lowercase()
            val list = voices.filter { it.locale.language.lowercase().startsWith(prefix) }
            if (list.isNotEmpty()) list else voices
        }
    }

    val currentVoice = remember(selectedVoiceId, filteredVoices, voices) {
        filteredVoices.firstOrNull { it.id == selectedVoiceId }
            ?: voices.firstOrNull { it.id == selectedVoiceId }
            ?: filteredVoices.getOrNull(speakerNumber - 1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .testTag("voice_select_btn_$speakerNumber"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentVoice?.name ?: "默认发音人",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.75f)
                    ) {
                        if (filteredVoices.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("系统默认语音") },
                                onClick = { expanded = false }
                            )
                        } else {
                            filteredVoices.forEach { voice ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            voice.name,
                                            maxLines = 1,
                                            fontSize = 13.sp
                                        )
                                    },
                                    onClick = {
                                        onSelectVoice(voice.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onAudition,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("audition_btn_$speakerNumber")
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "试听",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("试听", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ExpandableSettingSection(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    icon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "折叠" else "展开",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    content()
                }
            }
        }
    }
}

