package com.alexp.anydownload.data

import android.content.Context
import com.alexp.anydownload.CompletedDownload
import com.alexp.anydownload.SupportedPlatform
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DownloadHistoryRepository(context: Context) {
    private val historyFile = File(context.filesDir, HISTORY_FILE_NAME)

    fun load(): List<CompletedDownload> {
        if (!historyFile.exists()) return emptyList()

        return runCatching {
            val array = JSONArray(historyFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    parseEntry(array.getJSONObject(i))?.let { add(it) }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(items: List<CompletedDownload>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("filePath", item.filePath)
                    put("platform", item.platform.name)
                    put("completedAtMillis", item.completedAtMillis)
                },
            )
        }
        historyFile.writeText(array.toString())
    }

    private fun parseEntry(json: JSONObject): CompletedDownload? {
        val filePath = json.optString("filePath")
        if (filePath.isBlank() || !File(filePath).exists()) return null

        val platform = runCatching {
            SupportedPlatform.valueOf(json.getString("platform"))
        }.getOrDefault(SupportedPlatform.OTHER)

        return CompletedDownload(
            title = json.optString("title", "Untitled video"),
            filePath = filePath,
            platform = platform,
            completedAtMillis = json.optLong("completedAtMillis", 0L),
        )
    }

    companion object {
        private const val HISTORY_FILE_NAME = "download_history.json"
    }
}
