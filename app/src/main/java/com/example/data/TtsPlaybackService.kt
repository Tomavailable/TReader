package com.example.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class TtsPlaybackService : Service() {

    private val TAG = "TtsPlaybackService"
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TtsReader:PlaybackWakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START_OR_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "文本朗读"
                val sentence = intent.getStringExtra(EXTRA_SENTENCE) ?: "正在朗读..."
                val progress = intent.getStringExtra(EXTRA_PROGRESS) ?: ""
                val isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, true)

                updateForegroundNotification(title, sentence, progress, isPlaying)

                if (isPlaying) {
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }
            }
            ACTION_PREV -> {
                controller?.onActionPrev()
            }
            ACTION_PLAY_PAUSE -> {
                controller?.onActionTogglePlayPause()
            }
            ACTION_NEXT -> {
                controller?.onActionNext()
            }
            ACTION_STOP -> {
                controller?.onActionStop()
                stopForegroundService()
            }
            ACTION_STOP_SERVICE -> {
                stopForegroundService()
            }
        }

        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock?.isHeld == false) {
                // Keep awake up to 60 minutes during active playback
                wakeLock?.acquire(60 * 60 * 1000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock: ${e.message}")
        }
    }

    private fun updateForegroundNotification(
        title: String,
        sentence: String,
        progress: String,
        isPlaying: Boolean
    ) {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPausePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (isPlaying) "暂停" else "播放"

        val contentSnippet = if (sentence.length > 50) sentence.take(50) + "…" else sentence
        val statusPrefix = if (isPlaying) "▶ 正在朗读" else "⏸ 已暂停"
        val subtitle = if (progress.isNotBlank()) "$statusPrefix ($progress)" else statusPrefix

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("$title · $subtitle")
            .setContentText(contentSnippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(sentence))
            .setContentIntent(openAppIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "上一句", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "下一句", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", stopPendingIntent)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error: ${e.message}")
        }
    }

    private fun stopForegroundService() {
        releaseWakeLock()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "stopForeground error: ${e.message}")
        }
        stopSelf()
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "语音朗读后台播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "在后台或锁屏状态下维持语音合成朗读并提供通知栏控制器"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    interface PlaybackController {
        fun onActionPrev()
        fun onActionTogglePlayPause()
        fun onActionNext()
        fun onActionStop()
    }

    companion object {
        const val CHANNEL_ID = "tts_playback_foreground_channel"
        const val NOTIFICATION_ID = 9021

        const val ACTION_START_OR_UPDATE = "com.example.action.START_OR_UPDATE"
        const val ACTION_PREV = "com.example.action.PREV"
        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SENTENCE = "extra_sentence"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        var controller: PlaybackController? = null

        fun startOrUpdate(
            context: Context,
            title: String,
            sentence: String,
            progress: String,
            isPlaying: Boolean
        ) {
            try {
                val intent = Intent(context, TtsPlaybackService::class.java).apply {
                    action = ACTION_START_OR_UPDATE
                    putExtra(EXTRA_TITLE, title)
                    putExtra(EXTRA_SENTENCE, sentence)
                    putExtra(EXTRA_PROGRESS, progress)
                    putExtra(EXTRA_IS_PLAYING, isPlaying)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("TtsPlaybackService", "Failed to startOrUpdate service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, TtsPlaybackService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e("TtsPlaybackService", "Failed to stop service: ${e.message}")
            }
        }
    }
}
