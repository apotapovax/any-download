package com.alexp.anydownload

import android.os.Environment
import java.io.File

object DownloadPaths {
    fun downloadsDir(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "AnyDownload",
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun outputTemplate(): String {
        return downloadsDir().absolutePath + "/%(title).100s [%(id)s].%(ext)s"
    }
}

enum class SupportedPlatform(val label: String, val hostPatterns: List<String>) {
    TWITTER("X / Twitter", listOf("twitter.com", "x.com", "t.co")),
    INSTAGRAM("Instagram", listOf("instagram.com", "instagr.am")),
    FACEBOOK("Facebook", listOf("facebook.com", "fb.watch", "fb.com")),
    YOUTUBE("YouTube", listOf("youtube.com", "youtu.be", "youtube-nocookie.com")),
    OTHER("Other", emptyList()),
    ;

    companion object {
        fun detect(url: String): SupportedPlatform {
            val lower = url.lowercase()
            return entries.firstOrNull { platform ->
                platform.hostPatterns.any { host -> lower.contains(host) }
            } ?: OTHER
        }
    }
}

data class FormatOption(
    val id: String,
    val label: String,
    val ext: String?,
)

data class VideoMetadata(
    val url: String,
    val title: String,
    val thumbnail: String?,
    val platform: SupportedPlatform,
    val formats: List<FormatOption>,
    val uploader: String?,
    val durationSeconds: Long?,
)

data class DownloadProgress(
    val percent: Float,
    val etaSeconds: Long?,
)

data class CompletedDownload(
    val title: String,
    val filePath: String,
    val platform: SupportedPlatform,
    val completedAtMillis: Long = System.currentTimeMillis(),
)

sealed class DownloadUiState {
    data object Idle : DownloadUiState()
    data object InitializingEngine : DownloadUiState()
    data class UpdatingEngine(val message: String) : DownloadUiState()
    data object Analyzing : DownloadUiState()
    data class Ready(val metadata: VideoMetadata) : DownloadUiState()
    data class Downloading(val metadata: VideoMetadata, val progress: DownloadProgress) : DownloadUiState()
    data class Success(val metadata: VideoMetadata, val filePath: String) : DownloadUiState()
    data class Error(val message: String, val metadata: VideoMetadata? = null) : DownloadUiState()
}
