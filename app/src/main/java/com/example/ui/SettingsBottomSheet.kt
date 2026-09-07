package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
                    rate = uiState.speaker1Rate,
                    pitch = uiState.speaker1Pitch,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(0, it) },
                    onRateChange = { viewModel.setSpeakerRate(0, it) },
                    onPitchChange = { viewModel.setSpeakerPitch(0, it) },
                    onAudition = { viewModel.testVoice(0) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelectorItem(
                    speakerNumber = 2,
                    label = "常用发音人 2",
                    selectedVoiceId = uiState.speaker2VoiceId,
                    rate = uiState.speaker2Rate,
                    pitch = uiState.speaker2Pitch,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(1, it) },
                    onRateChange = { viewModel.setSpeakerRate(1, it) },
                    onPitchChange = { viewModel.setSpeakerPitch(1, it) },
                    onAudition = { viewModel.testVoice(1) }
                )

                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelectorItem(
                    speakerNumber = 3,
                    label = "常用发音人 3",
                    selectedVoiceId = uiState.speaker3VoiceId,
                    rate = uiState.speaker3Rate,
                    pitch = uiState.speaker3Pitch,
                    voices = voices,
                    selectedLanguage = uiState.selectedLanguage,
                    onSelectVoice = { viewModel.setSpeakerVoice(2, it) },
                    onRateChange = { viewModel.setSpeakerRate(2, it) },
                    onPitchChange = { viewModel.setSpeakerPitch(2, it) },
                    onAudition = { viewModel.testVoice(2) }
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                subtitle = "查词交互方案、外部词典调用与自动发音",
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

                var isEudicExpanded by remember { mutableStateOf(false) }
                var showAppPickerDialog by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. 欧路词典 (默认) - 点击最右边三角形图标展开或折叠小窗方案
                    val isEudicSelected = uiState.defaultDictApp == DictAppOption.EUDIC
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEudicSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isEudicSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDefaultDictApp(DictAppOption.EUDIC) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isEudicSelected,
                                        onClick = { viewModel.setDefaultDictApp(DictAppOption.EUDIC) }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("欧路词典 (默认)", fontWeight = if (isEudicSelected) FontWeight.Bold else FontWeight.Normal)
                                        Text(
                                            text = "调起模式: ${uiState.eudicInvokeMode.title.substringBefore("：")}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 最右边三角形图标展开或折叠
                                IconButton(
                                    onClick = { isEudicExpanded = !isEudicExpanded },
                                    modifier = Modifier.size(36.dp).testTag("eudic_expand_triangle_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isEudicExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = if (isEudicExpanded) "折叠欧路词典方案" else "展开欧路词典方案",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            AnimatedVisibility(visible = isEudicExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 6.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "欧路词典小窗调起方案选择：",
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

                    // 2. 谷歌翻译
                    val isGoogleSelected = uiState.defaultDictApp == DictAppOption.GOOGLE_TRANSLATE
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGoogleSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isGoogleSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDefaultDictApp(DictAppOption.GOOGLE_TRANSLATE) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isGoogleSelected,
                                onClick = { viewModel.setDefaultDictApp(DictAppOption.GOOGLE_TRANSLATE) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("谷歌翻译", fontWeight = if (isGoogleSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    // 3. 自定义词典软件 (选取手机上已经安装的其他软件)
                    val isCustomSelected = uiState.defaultDictApp == DictAppOption.CUSTOM_APP
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isCustomSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setDefaultDictApp(DictAppOption.CUSTOM_APP)
                                    if (uiState.customDictPackageName == null) {
                                        showAppPickerDialog = true
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isCustomSelected,
                                    onClick = {
                                        viewModel.setDefaultDictApp(DictAppOption.CUSTOM_APP)
                                        if (uiState.customDictPackageName == null) {
                                            showAppPickerDialog = true
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("自定义词典软件", fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Normal)
                                    Text(
                                        text = if (uiState.customDictAppName != null) "已选: ${uiState.customDictAppName}" else "选取手机已安装的任意词典/翻译应用",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showAppPickerDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp).testTag("select_custom_app_btn")
                            ) {
                                Text(if (uiState.customDictAppName != null) "更换" else "选取软件", fontSize = 12.sp)
                            }
                        }
                    }

                    // 4. 系统通用划词
                    val isSystemSelected = uiState.defaultDictApp == DictAppOption.SYSTEM_CHOOSER
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSystemSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isSystemSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDefaultDictApp(DictAppOption.SYSTEM_CHOOSER) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSystemSelected,
                                onClick = { viewModel.setDefaultDictApp(DictAppOption.SYSTEM_CHOOSER) }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("系统通用划词", fontWeight = if (isSystemSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                if (showAppPickerDialog) {
                    InstalledAppPickerDialog(
                        onDismiss = { showAppPickerDialog = false },
                        onAppSelected = { pkg, name ->
                            viewModel.setCustomDictApp(pkg, name)
                        }
                    )
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
                    15 to "15",
                    30 to "30",
                    45 to "45",
                    60 to "60"
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
                        label = { Text("自动", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_chip_system")
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                        label = { Text("夜间", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_chip_dark")
                    )
                    FilterChip(
                        selected = uiState.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                        label = { Text("日间", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(15.dp))
                        },
                        modifier = Modifier.weight(1f).testTag("theme_chip_light")
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

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "⚙️ 自定义标点与规避符号规则",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "您可以自由增加或删除断句和拆分时的标点符号，也可以增删用于规避被分离的闭合后置引号与括号符号。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    CustomPunctRuleSection(
                        title = "1. 二次拆分标点 (长句中顿符号)",
                        desc = "开启二次拆分时，在此类标点处平分或三等分长句",
                        puncts = uiState.secondaryPuncts,
                        onAdd = { viewModel.addSecondaryPunct(it) },
                        onRemove = { viewModel.removeSecondaryPunct(it) }
                    )

                    CustomPunctRuleSection(
                        title = "2. 句末包裹规避符号 (后引号/括号)",
                        desc = "紧随句末标点后的符号，规避将其与句子提前截断分离",
                        puncts = uiState.closingPuncts,
                        onAdd = { viewModel.addClosingPunct(it) },
                        onRemove = { viewModel.removeClosingPunct(it) }
                    )

                    CustomPunctRuleSection(
                        title = "3. 主断句终止符 (长句模式断句标点)",
                        desc = "作为一句话终点依据的主标点符号",
                        puncts = uiState.terminatorPuncts,
                        onAdd = { viewModel.addTerminatorPunct(it) },
                        onRemove = { viewModel.removeTerminatorPunct(it) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.resetPunctuationRules() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("恢复默认符号规则", fontSize = 12.sp)
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
                        text = "📚 词典与生词划词查询",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "· 第三方词典调起：支持一键调起欧路词典等应用内查词小窗，或绑定手机自定义词典软件。\n· 查词响应模式：支持直接调起小窗或快捷图标工具栏。",
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
    rate: Float,
    pitch: Float,
    voices: List<TtsVoiceItem>,
    selectedLanguage: String,
    onSelectVoice: (String) -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onAudition: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAdjustDialog by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "语速 ${String.format("%.2f", rate)}x · 音调 ${String.format("%.2f", pitch)}x",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

                // 设置按钮：只显示设置图标，点击后弹出滑动调节界面
                FilledTonalIconButton(
                    onClick = { showAdjustDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("voice_tune_btn_$speakerNumber")
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "调节语速音调",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 试听按钮
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

    if (showAdjustDialog) {
        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("发音人 $speakerNumber 音效微调", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 语速调节
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("朗读语速", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format("%.2f", rate)}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = rate,
                            onValueChange = onRateChange,
                            valueRange = 0.5f..2.5f,
                            steps = 19
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.85f to "0.85", 1.0f to "1.0", 1.1f to "1.1", 1.25f to "1.25", 1.5f to "1.5").forEach { (r, lbl) ->
                                val isSelected = kotlin.math.abs(rate - r) < 0.02f
                                OutlinedButton(
                                    onClick = { onRateChange(r) },
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(lbl, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // 音调调节
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("朗读音调", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${String.format("%.2f", pitch)}x", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = pitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.5f..2.0f,
                            steps = 14
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0.8f to "0.8低沉", 1.0f to "1.0标准", 1.2f to "1.2清脆", 1.4f to "1.4高亢").forEach { (p, lbl) ->
                                val isSelected = kotlin.math.abs(pitch - p) < 0.02f
                                OutlinedButton(
                                    onClick = { onPitchChange(p) },
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors(),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text(lbl, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // 试听按钮
                    Button(
                        onClick = onAudition,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("试听此音效")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAdjustDialog = false }) {
                    Text("完成")
                }
            }
        )
    }
}

private data class InstalledAppItem(
    val name: String,
    val packageName: String,
    val icon: android.graphics.drawable.Drawable?
)

@Composable
private fun InstalledAppPickerDialog(
    onDismiss: () -> Unit,
    onAppSelected: (packageName: String, appName: String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pm = remember { context.packageManager }
    var searchQuery by remember { mutableStateOf("") }

    val installedApps = remember {
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launcherApps = pm.queryIntentActivities(mainIntent, 0)

            val textIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
            }
            val textApps = pm.queryIntentActivities(textIntent, 0)

            val all = (launcherApps + textApps).distinctBy { it.activityInfo.packageName }
            all.filter { it.activityInfo.packageName != context.packageName }
                .map {
                    InstalledAppItem(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = try { it.loadIcon(pm) } catch (e: Exception) { null }
                    )
                }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("选择已安装的词典/翻译软件", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索软件名称或包名...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                if (filteredApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("未找到相关应用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps.size) { index ->
                            val app = filteredApps[index]
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAppSelected(app.packageName, app.name)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DrawableAppIcon(
                                        drawable = app.icon,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.name, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 14.sp)
                                        Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DrawableAppIcon(
    drawable: android.graphics.drawable.Drawable?,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        if (drawable == null) return@remember null
        try {
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                drawable.bitmap.asImageBitmap()
            } else {
                val w = drawable.intrinsicWidth.coerceIn(48, 144)
                val h = drawable.intrinsicHeight.coerceIn(48, 144)
                val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp.asImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CustomPunctRuleSection(
    title: String,
    desc: String,
    puncts: Set<Char>,
    onAdd: (Char) -> Unit,
    onRemove: (Char) -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            puncts.forEach { ch ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = ch.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "删除 $ch",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemove(ch) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("输入要添加的符号", fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            )
            Button(
                onClick = {
                    inputText.trim().forEach { ch ->
                        onAdd(ch)
                    }
                    inputText = ""
                },
                enabled = inputText.trim().isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("添加", fontSize = 12.sp)
            }
        }
    }
}

