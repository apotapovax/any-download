package com.alexp.anydownload

import android.content.Context
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object YtdlpEngine {
    data class InitResult(
        val version: String,
        val updateStatus: YoutubeDL.UpdateStatus?,
        val updateError: String? = null,
    )

    @Volatile
    private var isInitialized = false

    suspend fun initialize(context: Context): InitResult = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        runCatching {
            update(context, YoutubeDL.UpdateChannel._STABLE)
        }.getOrElse { error ->
            Log.w(TAG, "Startup yt-dlp update skipped", error)
            InitResult(
                version = currentVersion(context),
                updateStatus = null,
                updateError = error.message ?: "Startup update skipped",
            )
        }
    }

    suspend fun update(
        context: Context,
        channel: YoutubeDL.UpdateChannel = YoutubeDL.UpdateChannel._STABLE,
    ): InitResult = withContext(Dispatchers.IO) {
        ensureInitialized(context)

        var status: YoutubeDL.UpdateStatus? = null
        var updateError: String? = null

        try {
            status = YoutubeDL.getInstance().updateYoutubeDL(context, channel)
            Log.i(TAG, "yt-dlp update finished: $status")
        } catch (e: YoutubeDLException) {
            updateError = e.message ?: "Update failed"
            Log.w(TAG, "yt-dlp update failed", e)
        } catch (e: Exception) {
            updateError = e.message ?: "Update failed"
            Log.e(TAG, "yt-dlp update crashed", e)
        }

        InitResult(
            version = currentVersion(context),
            updateStatus = status,
            updateError = updateError,
        )
    }

    private fun ensureInitialized(context: Context) {
        if (isInitialized) return

        synchronized(this) {
            if (isInitialized) return
            YoutubeDL.getInstance().init(context)
            FFmpeg.getInstance().init(context)
            isInitialized = true
            Log.i(TAG, "yt-dlp engine initialized")
        }
    }

    fun currentVersion(context: Context): String {
        return runCatching {
            if (!isInitialized) {
                ensureInitialized(context)
            }
            YoutubeDL.getInstance().versionName(context)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "unknown"
    }

    fun isOutdatedWarning(output: String): Boolean {
        return output.contains("older than 90 days", ignoreCase = true) ||
            output.contains("yt-dlp version", ignoreCase = true) &&
            output.contains("is older", ignoreCase = true)
    }

    private const val TAG = "YtdlpEngine"
}
