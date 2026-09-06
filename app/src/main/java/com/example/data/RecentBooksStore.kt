package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class RecentBook(
    val id: String,
    val title: String,
    val snippet: String,
    val fullText: String,
    val sentenceCount: Int,
    val lastIndex: Int = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

class RecentBooksStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tts_recent_books", Context.MODE_PRIVATE)

    fun getRecentBooks(): List<RecentBook> {
        val jsonStr = prefs.getString(KEY_BOOKS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<RecentBook>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    RecentBook(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", "未命名文档"),
                        snippet = obj.optString("snippet", ""),
                        fullText = obj.optString("fullText", ""),
                        sentenceCount = obj.optInt("sentenceCount", 0),
                        lastIndex = obj.optInt("lastIndex", 0),
                        lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.lastReadTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRecentBook(title: String, fullText: String, sentenceCount: Int, lastIndex: Int = 0) {
        if (fullText.isBlank()) return
        val currentList = getRecentBooks().toMutableList()
        // Remove existing if matching same title or text
        currentList.removeAll { it.title == title || it.fullText == fullText }

        val snippet = fullText.take(120).replace("\n", " ").trim()
        val newBook = RecentBook(
            id = System.currentTimeMillis().toString(),
            title = title,
            snippet = snippet,
            fullText = fullText,
            sentenceCount = sentenceCount,
            lastIndex = lastIndex,
            lastReadTimestamp = System.currentTimeMillis()
        )
        currentList.add(0, newBook)

        // Keep at most 15 recent books
        val trimmed = currentList.take(15)
        persistList(trimmed)
    }

    fun updateProgress(title: String, lastIndex: Int) {
        val currentList = getRecentBooks().toMutableList()
        val index = currentList.indexOfFirst { it.title == title }
        if (index != -1) {
            val old = currentList[index]
            currentList[index] = old.copy(lastIndex = lastIndex, lastReadTimestamp = System.currentTimeMillis())
            persistList(currentList)
        }
    }

    fun deleteRecentBook(id: String) {
        val currentList = getRecentBooks().toMutableList()
        currentList.removeAll { it.id == id }
        persistList(currentList)
    }

    private fun persistList(list: List<RecentBook>) {
        val jsonArray = JSONArray()
        list.forEach { book ->
            val obj = JSONObject().apply {
                put("id", book.id)
                put("title", book.title)
                put("snippet", book.snippet)
                put("fullText", book.fullText)
                put("sentenceCount", book.sentenceCount)
                put("lastIndex", book.lastIndex)
                put("lastReadTimestamp", book.lastReadTimestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_BOOKS, jsonArray.toString()).apply()
    }

    companion object {
        private const val KEY_BOOKS = "recent_books_json"
    }
}
