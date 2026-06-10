package com.alexp.anydownload

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnyDownloadApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _engineState = MutableStateFlow<EngineState>(EngineState.Initializing)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    val isEngineReady: Boolean
        get() = _engineState.value is EngineState.Ready

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            _engineState.value = EngineState.Initializing
            runCatching {
                YtdlpEngine.initialize(this@AnyDownloadApp)
            }.onSuccess { result ->
                _engineState.value = EngineState.Ready(
                    ytdlpVersion = result.version,
                    lastUpdateStatus = result.updateStatus,
                    lastUpdateError = result.updateError,
                )
                Log.i(TAG, "Engine ready, yt-dlp ${result.version}, update=${result.updateStatus}")
            }.onFailure { error ->
                Log.e(TAG, "Failed to initialize download engine", error)
                _engineState.value = EngineState.Failed(
                    error.message ?: "Failed to start download engine",
                )
            }
        }
    }

    fun updateYtdlp(channel: YoutubeDL.UpdateChannel, onComplete: (EngineState.Ready) -> Unit) {
        appScope.launch {
            val current = _engineState.value
            val previousVersion = when (current) {
                is EngineState.Ready -> current.ytdlpVersion
                is EngineState.Updating -> current.previousVersion
                is EngineState.Failed -> YtdlpEngine.currentVersion(this@AnyDownloadApp)
                else -> "unknown"
            }

            _engineState.value = EngineState.Updating(previousVersion)

            val result = runCatching {
                YtdlpEngine.update(this@AnyDownloadApp, channel)
            }.getOrElse { error ->
                Log.e(TAG, "Manual yt-dlp update failed", error)
                YtdlpEngine.InitResult(
                    version = previousVersion,
                    updateStatus = null,
                    updateError = error.message ?: "Update failed",
                )
            }

            val ready = EngineState.Ready(
                ytdlpVersion = result.version,
                lastUpdateStatus = result.updateStatus,
                lastUpdateError = result.updateError,
            )
            _engineState.value = ready

            withContext(Dispatchers.Main) {
                onComplete(ready)
            }
        }
    }

    sealed class EngineState {
        data object Initializing : EngineState()
        data class Updating(val previousVersion: String) : EngineState()
        data class Ready(
            val ytdlpVersion: String,
            val lastUpdateStatus: YoutubeDL.UpdateStatus?,
            val lastUpdateError: String? = null,
        ) : EngineState()
        data class Failed(val message: String) : EngineState()
    }

    companion object {
        private const val TAG = "AnyDownloadApp"
    }
}
