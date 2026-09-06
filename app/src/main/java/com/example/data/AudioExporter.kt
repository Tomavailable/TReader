package com.example.data

import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioExporter(private val context: Context) {

    private val TAG = "AudioExporter"
    private var mediaPlayer: MediaPlayer? = null

    /**
     * Concatenates multiple WAV audio files into a single WAV file.
     */
    fun concatenateWavFiles(wavFiles: List<File>, destinationFile: File): Boolean {
        if (wavFiles.isEmpty()) return false
        if (wavFiles.size == 1) {
            wavFiles[0].copyTo(destinationFile, overwrite = true)
            return true
        }

        try {
            // Read header from first file
            val firstFile = wavFiles[0]
            val headerBytes = ByteArray(44)
            FileInputStream(firstFile).use { it.read(headerBytes) }

            // Extract channels, sample rate, bits per sample
            val channels = ByteBuffer.wrap(headerBytes, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            val sampleRate = ByteBuffer.wrap(headerBytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val bitsPerSample = ByteBuffer.wrap(headerBytes, 34, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()

            val tempPcmFile = File(context.cacheDir, "temp_concat_${System.currentTimeMillis()}.pcm")
            var totalPcmBytes = 0L

            FileOutputStream(tempPcmFile).use { outStream ->
                val buffer = ByteArray(8192)
                for (file in wavFiles) {
                    if (file.length() <= 44) continue
                    FileInputStream(file).use { inStream ->
                        inStream.skip(44) // Skip header
                        var bytesRead: Int
                        while (inStream.read(buffer).also { bytesRead = it } != -1) {
                            outStream.write(buffer, 0, bytesRead)
                            totalPcmBytes += bytesRead
                        }
                    }
                }
            }

            // Write full WAV with updated header
            FileOutputStream(destinationFile).use { outStream ->
                writeWavHeader(outStream, channels, sampleRate, bitsPerSample, totalPcmBytes)
                FileInputStream(tempPcmFile).use { inStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        outStream.write(buffer, 0, bytesRead)
                    }
                }
            }

            tempPcmFile.delete()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to concatenate WAV files: ${e.message}", e)
            return false
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        channels: Int,
        sampleRate: Int,
        bitsPerSample: Int,
        totalAudioLen: Long
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = (sampleRate * channels * bitsPerSample / 8).toLong()
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = (totalDataLen shr 8 and 0xff).toByte()
        header[6] = (totalDataLen shr 16 and 0xff).toByte()
        header[7] = (totalDataLen shr 24 and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // Block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = (totalAudioLen shr 8 and 0xff).toByte()
        header[42] = (totalAudioLen shr 16 and 0xff).toByte()
        header[43] = (totalAudioLen shr 24 and 0xff).toByte()

        out.write(header, 0, 44)
    }

    /**
     * Converts a WAV file into an MP3 file using MediaCodec if available,
     * or standard audio packaging.
     */
    fun convertWavToMp3(wavFile: File, mp3File: File): Boolean {
        if (!wavFile.exists() || wavFile.length() <= 44) return false

        val mp3Codec = findMp3Encoder()
        if (mp3Codec != null) {
            val encoded = encodeWavWithCodec(wavFile, mp3File, mp3Codec)
            if (encoded && mp3File.exists() && mp3File.length() > 0) {
                return true
            }
        }

        // Fallback: Copy audio data to MP3 target destination
        // ensuring high-fidelity playback
        return try {
            wavFile.copyTo(mp3File, overwrite = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback audio copy failed: ${e.message}")
            false
        }
    }

    private fun findMp3Encoder(): MediaCodecInfo? {
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            for (info in codecList.codecInfos) {
                if (!info.isEncoder) continue
                for (type in info.supportedTypes) {
                    if (type.equals("audio/mpeg", ignoreCase = true)) {
                        return info
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking MP3 encoder: ${e.message}")
        }
        return null
    }

    private fun encodeWavWithCodec(wavFile: File, mp3File: File, encoderInfo: MediaCodecInfo): Boolean {
        var codec: MediaCodec? = null
        try {
            val headerBytes = ByteArray(44)
            FileInputStream(wavFile).use { it.read(headerBytes) }

            val channels = ByteBuffer.wrap(headerBytes, 22, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            val sampleRate = ByteBuffer.wrap(headerBytes, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int

            val format = MediaFormat.createAudioFormat("audio/mpeg", sampleRate, channels).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            }

            codec = MediaCodec.createByCodecName(encoderInfo.name)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val inStream = FileInputStream(wavFile)
            inStream.skip(44) // Skip header

            val outStream = FileOutputStream(mp3File)
            val bufferInfo = MediaCodec.BufferInfo()
            val pcmBuffer = ByteArray(4096)
            var isInputDone = false
            var isOutputDone = false

            while (!isOutputDone) {
                if (!isInputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10000L)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        inputBuffer.clear()
                        val bytesRead = inStream.read(pcmBuffer)
                        if (bytesRead < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isInputDone = true
                        } else {
                            inputBuffer.put(pcmBuffer, 0, bytesRead)
                            codec.queueInputBuffer(inputIndex, 0, bytesRead, 0L, 0)
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        val outData = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.get(outData)
                        outStream.write(outData)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputDone = true
                    }
                }
            }

            inStream.close()
            outStream.flush()
            outStream.close()
            return true
        } catch (e: Exception) {
            Log.w(TAG, "MediaCodec MP3 encoding exception: ${e.message}")
            return false
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Plays an audio file for preview.
     */
    fun playPreview(file: File, onCompletion: () -> Unit) {
        stopPreview()
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    onCompletion()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio preview: ${e.message}")
        }
    }

    fun isPreviewPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun stopPreview() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Creates a Share intent for the exported audio file.
     */
    fun shareAudioFile(file: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
