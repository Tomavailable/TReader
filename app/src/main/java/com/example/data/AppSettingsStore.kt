package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.ReadingDisplayMode
import com.example.ui.ThemeMode

class AppSettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tts_app_settings", Context.MODE_PRIVATE)

    var speaker1VoiceId: String?
        get() = prefs.getString("speaker1_voice_id", null)
        set(value) = prefs.edit().putString("speaker1_voice_id", value).apply()

    var speaker2VoiceId: String?
        get() = prefs.getString("speaker2_voice_id", null)
        set(value) = prefs.edit().putString("speaker2_voice_id", value).apply()

    var speaker3VoiceId: String?
        get() = prefs.getString("speaker3_voice_id", null)
        set(value) = prefs.edit().putString("speaker3_voice_id", value).apply()

    var speaker1Rate: Float
        get() = prefs.getFloat("speaker1_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speaker1_rate", value).apply()

    var speaker2Rate: Float
        get() = prefs.getFloat("speaker2_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speaker2_rate", value).apply()

    var speaker3Rate: Float
        get() = prefs.getFloat("speaker3_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speaker3_rate", value).apply()

    var speaker1Pitch: Float
        get() = prefs.getFloat("speaker1_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speaker1_pitch", value).apply()

    var speaker2Pitch: Float
        get() = prefs.getFloat("speaker2_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speaker2_pitch", value).apply()

    var speaker3Pitch: Float
        get() = prefs.getFloat("speaker3_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speaker3_pitch", value).apply()

    var activeSpeakerIndex: Int
        get() = prefs.getInt("active_speaker_index", 0)
        set(value) = prefs.edit().putInt("active_speaker_index", value).apply()

    var multiSpeakerEnabled: Boolean
        get() = prefs.getBoolean("multi_speaker_enabled", false)
        set(value) = prefs.edit().putBoolean("multi_speaker_enabled", value).apply()

    var targetRepeatCount: Int
        get() = prefs.getInt("target_repeat_count", 1)
        set(value) = prefs.edit().putInt("target_repeat_count", value).apply()

    var isSplitEnabled: Boolean
        get() = prefs.getBoolean("is_split_enabled", false)
        set(value) = prefs.edit().putBoolean("is_split_enabled", value).apply()

    var splitMode: SplitMode
        get() {
            val name = prefs.getString("split_mode", SplitMode.BREAK_ITERATOR.name)
            return try {
                SplitMode.valueOf(name ?: SplitMode.BREAK_ITERATOR.name)
            } catch (e: Exception) {
                SplitMode.BREAK_ITERATOR
            }
        }
        set(value) = prefs.edit().putString("split_mode", value.name).apply()

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var readingDisplayMode: ReadingDisplayMode
        get() {
            val name = prefs.getString("reading_display_mode", ReadingDisplayMode.BOOK_PAGE.name)
            return try {
                ReadingDisplayMode.valueOf(name ?: ReadingDisplayMode.BOOK_PAGE.name)
            } catch (e: Exception) {
                ReadingDisplayMode.BOOK_PAGE
            }
        }
        set(value) = prefs.edit().putString("reading_display_mode", value.name).apply()

    var selectedLanguage: String
        get() = prefs.getString("selected_language", "all") ?: "all"
        set(value) = prefs.edit().putString("selected_language", value).apply()

    var secondaryPuncts: Set<Char>
        get() {
            val str = prefs.getString("secondary_puncts", null) ?: return TextSegmenter.DEFAULT_SECONDARY_PUNCTS
            return str.toSet()
        }
        set(value) = prefs.edit().putString("secondary_puncts", value.joinToString("")).apply()

    var terminatorPuncts: Set<Char>
        get() {
            val str = prefs.getString("terminator_puncts", null) ?: return TextSegmenter.DEFAULT_TERMINATOR_PUNCTS
            return str.toSet()
        }
        set(value) = prefs.edit().putString("terminator_puncts", value.joinToString("")).apply()

    var closingPuncts: Set<Char>
        get() {
            val str = prefs.getString("closing_puncts", null) ?: return TextSegmenter.DEFAULT_CLOSING_PUNCTS
            return str.toSet()
        }
        set(value) = prefs.edit().putString("closing_puncts", value.joinToString("")).apply()

    var lastOpenedFileName: String?
        get() = prefs.getString("last_file_name", null)
        set(value) = prefs.edit().putString("last_file_name", value).apply()

    var lastOpenedFullText: String?
        get() = prefs.getString("last_full_text", null)
        set(value) = prefs.edit().putString("last_full_text", value).apply()

    var lastOpenedIndex: Int
        get() = prefs.getInt("last_index", 0)
        set(value) = prefs.edit().putInt("last_index", value).apply()
}
