package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Composable
fun ExportAudioDialog(
    viewModel: TtsReaderViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportState = uiState.exportState
    val context = LocalContext.current

    var selectedFormat by remember { mutableStateOf("mp3") }
    var includeMultiSpeaker by remember { mutableStateOf(uiState.multiSpeakerEnabled) }
    var repeatCount by remember { mutableIntStateOf(uiState.targetRepeatCount.coerceAtMost(3)) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/*")
    ) { uri: Uri? ->
        if (uri != null && exportState.exportedFile != null) {
            saveExportedFileToUri(context, exportState.exportedFile, uri)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!exportState.isExporting) {
                onDismiss()
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "一键导出为音频文件",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!exportState.isExporting) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (exportState.isExporting) {
                    // Exporting in progress
                    ExportInProgressView(exportState = exportState, onCancel = { viewModel.cancelExport() })
                } else if (exportState.exportedFile != null) {
                    // Completed view
                    ExportCompletedView(
                        file = exportState.exportedFile,
                        isPreviewPlaying = exportState.isPreviewPlaying,
                        onTogglePreview = { viewModel.togglePreviewPlayback() },
                        onSaveToDevice = {
                            val defaultName = exportState.exportedFile.name
                            createDocumentLauncher.launch(defaultName)
                        },
                        onShare = {
                            val shareIntent = viewModel.createShareIntent()
                            if (shareIntent != null) {
                                context.startActivity(Intent.createChooser(shareIntent, "分享音频文件"))
                            }
                        }
                    )
                } else {
                    // Configuration options before exporting
                    ExportOptionsView(
                        totalSentences = uiState.sentences.size,
                        selectedFormat = selectedFormat,
                        onSelectFormat = { selectedFormat = it },
                        multiSpeakerAvailable = uiState.multiSpeakerEnabled,
                        includeMultiSpeaker = includeMultiSpeaker,
                        onToggleMultiSpeaker = { includeMultiSpeaker = it },
                        repeatCount = repeatCount,
                        onSelectRepeat = { repeatCount = it }
                    )

                    if (exportState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = exportState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!exportState.isExporting && exportState.exportedFile == null) {
                Button(
                    onClick = {
                        viewModel.startExport(
                            includeMultiSpeaker = includeMultiSpeaker,
                            repeatPerSentence = repeatCount,
                            targetFormat = selectedFormat
                        )
                    },
                    modifier = Modifier.testTag("start_export_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("开始导出为 $selectedFormat")
                }
            } else if (exportState.exportedFile != null) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("完成")
                }
            }
        },
        dismissButton = {
            if (!exportState.isExporting && exportState.exportedFile == null) {
                OutlinedButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    )
}

@Composable
private fun ExportOptionsView(
    totalSentences: Int,
    selectedFormat: String,
    onSelectFormat: (String) -> Unit,
    multiSpeakerAvailable: Boolean,
    includeMultiSpeaker: Boolean,
    onToggleMultiSpeaker: (Boolean) -> Unit,
    repeatCount: Int,
    onSelectRepeat: (Int) -> Unit
) {
    Column {
        Text(
            text = "共 $totalSentences 句文字将被合成并导出为完整音频。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Target format
        Text(
            text = "导出音频格式",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFormat == "mp3",
                onClick = { onSelectFormat("mp3") },
                label = { Text("MP3 格式 (.mp3)") },
                modifier = Modifier.testTag("format_mp3_chip")
            )
            FilterChip(
                selected = selectedFormat == "wav",
                onClick = { onSelectFormat("wav") },
                label = { Text("无损 WAV (.wav)") },
                modifier = Modifier.testTag("format_wav_chip")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multi speaker option
        if (multiSpeakerAvailable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includeMultiSpeaker,
                    onCheckedChange = onToggleMultiSpeaker,
                    modifier = Modifier.testTag("export_multi_speaker_check")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "包含3位发音人轮流朗读",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "每句将按发音人1、2、3依次朗读合成",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Sentence Repeat Count
        Text(
            text = "每句单句重复遍数",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1 to "1遍", 2 to "2遍", 3 to "3遍").forEach { (count, label) ->
                FilterChip(
                    selected = repeatCount == count,
                    onClick = { onSelectRepeat(count) },
                    label = { Text(label) }
                )
            }
        }
    }
}

@Composable
private fun ExportInProgressView(
    exportState: ExportState,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            progress = { exportState.progress },
            modifier = Modifier.size(56.dp),
            strokeWidth = 5.dp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${(exportState.progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = exportState.statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = { exportState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("取消合成")
        }
    }
}

@Composable
private fun ExportCompletedView(
    file: File,
    isPreviewPlaying: Boolean,
    onTogglePreview: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "音频导出成功！",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        val fileSizeFormatted = formatFileSize(file.length())
        Text(
            text = "${file.name} ($fileSizeFormatted)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action 1: In-app preview player
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPreviewPlaying) "正在试听播放..." else "试听合成音频",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onTogglePreview,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPreviewPlaying) "暂停" else "试听")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action 2: Save to device storage
        Button(
            onClick = onSaveToDevice,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("save_to_device_btn"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("保存到手机本地 (MP3)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action 3: System Share
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("share_audio_btn"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("发送 / 分享给好友")
        }
    }
}

private fun saveExportedFileToUri(context: Context, sourceFile: File, targetUri: Uri) {
    try {
        context.contentResolver.openOutputStream(targetUri)?.use { outStream ->
            FileInputStream(sourceFile).use { inStream ->
                val buffer = ByteArray(8192)
                var read: Int
                while (inStream.read(buffer).also { read = it } != -1) {
                    outStream.write(buffer, 0, read)
                }
                outStream.flush()
            }
        }
        Toast.makeText(context, "音频文件保存成功！", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes.toFloat() / (1024 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes.toFloat() / 1024)
        else -> "$bytes B"
    }
}
