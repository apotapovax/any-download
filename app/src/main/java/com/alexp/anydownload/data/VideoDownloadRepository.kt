package com.alexp.anydownload.data

import com.alexp.anydownload.DownloadPaths
import com.alexp.anydownload.DownloadProgress
import com.alexp.anydownload.FormatOption
import com.alexp.anydownload.SupportedPlatform
import com.alexp.anydownload.VideoMetadata
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoDownloadRepository(
    private val cookieRepository: CookieRepository,
) {
    suspend fun fetchMetadata(url: String): VideoMetadata = withContext(Dispatchers.IO) {
        val request = buildInfoRequest(url)
        val info = YoutubeDL.getInstance().getInfo(request)
        info.toMetadata(url)
    }

    suspend fun download(
        url: String,
        formatId: String?,
        onProgress: (DownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val downloadsDir = DownloadPaths.downloadsDir()
        val downloadStartedAt = System.currentTimeMillis()
        val request = buildDownloadRequest(url, formatId)

        val response = YoutubeDL.getInstance().execute(request) { progress, etaInSeconds, _ ->
            onProgress(
                DownloadProgress(
                    percent = progress.toFloat(),
                    etaSeconds = etaInSeconds,
                ),
            )
        }

        if (response.exitCode != 0) {
            val details = response.err?.trim().orEmpty().ifBlank { response.out?.trim().orEmpty() }
            throw IllegalStateException(details.ifBlank { "Download failed." })
        }

        resolveDownloadedFile(
            stdout = response.out.orEmpty(),
            downloadsDir = downloadsDir,
            downloadStartedAt = downloadStartedAt,
        ) ?: throw IllegalStateException(
            "Download finished but file was not found in ${downloadsDir.absolutePath}",
        )
    }

    private fun buildInfoRequest(url: String): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            applyCookies()
        }
    }

    private fun buildDownloadRequest(url: String, formatId: String?): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            applyCookies()
            addOption("-o", DownloadPaths.outputTemplate())
            addOption("--no-mtime")
            addOption("--merge-output-format", "mp4")
            addOption("--restrict-filenames")
            if (formatId.isNullOrBlank() || formatId == BEST_FORMAT_ID) {
                addOption("-f", "bv*+ba/b")
            } else {
                addOption("-f", formatId)
            }
        }
    }

    private fun YoutubeDLRequest.applyCookies() {
        cookieRepository.getCookiesPath()?.let { path ->
            addOption("--cookies", path)
        }
    }

    private fun resolveDownloadedFile(
        stdout: String,
        downloadsDir: File,
        downloadStartedAt: Long,
    ): File? {
        parsePathFromYtDlpOutput(stdout)?.let { path ->
            val file = File(path)
            if (file.exists()) return file
        }

        return downloadsDir.listFiles()
            ?.filter { file ->
                file.isFile &&
                    file.lastModified() >= downloadStartedAt - 5_000 &&
                    file.extension.lowercase() in VIDEO_EXTENSIONS
            }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun parsePathFromYtDlpOutput(stdout: String): String? {
        val patterns = listOf(
            Regex("""\[download\]\s+(.+?) has already been downloaded"""),
            Regex("""\[download\]\s+Destination:\s+(.+)"""),
            Regex("""\[Merger\]\s+Merging formats into "(.+?)""""),
            Regex("""\[ExtractAudio\]\s+Destination:\s+(.+)"""),
            Regex("""(\/storage\/emulated\/0\/Download\/AnyDownload\/.+\.\w+)"""),
        )

        for (line in stdout.lines().asReversed()) {
            for (pattern in patterns) {
                pattern.find(line)?.groupValues?.getOrNull(1)?.let { return it.trim() }
            }
        }
        return null
    }

    private fun VideoInfo.toMetadata(url: String): VideoMetadata {
        val platform = SupportedPlatform.detect(url)
        val formatOptions = buildFormatOptions(formats ?: emptyList())

        return VideoMetadata(
            url = url,
            title = title.orEmpty().ifBlank { "Untitled video" },
            thumbnail = thumbnail,
            platform = platform,
            formats = formatOptions,
            uploader = uploader,
            durationSeconds = duration.takeIf { it > 0 }?.toLong(),
        )
    }

    private fun buildFormatOptions(formats: List<VideoFormat>): List<FormatOption> {
        val merged = linkedMapOf<String, FormatOption>()
        merged[BEST_FORMAT_ID] = FormatOption(
            id = BEST_FORMAT_ID,
            label = "Best available (recommended)",
            ext = "mp4",
        )

        formats
            .asSequence()
            .filter { format ->
                val hasVideo = format.vcodec != null && format.vcodec != "none"
                val hasAudio = format.acodec != null && format.acodec != "none"
                hasVideo || hasAudio
            }
            .sortedWith(
                compareByDescending<VideoFormat> { it.height }
                    .thenByDescending { it.tbr },
            )
            .forEach { format ->
                val id = format.formatId ?: return@forEach
                if (merged.containsKey(id)) return@forEach

                val resolution = when {
                    format.height > 0 -> "${format.height}p"
                    !format.formatNote.isNullOrBlank() -> format.formatNote
                    else -> "unknown"
                }
                val codec = listOfNotNull(format.ext, format.vcodec?.takeIf { it != "none" })
                    .joinToString(" · ")
                    .ifBlank { format.ext ?: "stream" }

                merged[id] = FormatOption(
                    id = id,
                    label = "$resolution · $codec",
                    ext = format.ext,
                )
            }

        return merged.values.toList()
    }

    companion object {
        const val BEST_FORMAT_ID = "best"
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp")
    }
}
