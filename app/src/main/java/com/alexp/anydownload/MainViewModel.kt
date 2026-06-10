package com.alexp.anydownload

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alexp.anydownload.data.CookieRepository
import com.alexp.anydownload.data.DownloadHistoryRepository
import com.alexp.anydownload.data.LoginPlatform
import com.alexp.anydownload.data.VideoDownloadRepository
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val cookieRepository = CookieRepository(application)
    private val historyRepository = DownloadHistoryRepository(application)
    private val repository = VideoDownloadRepository(cookieRepository)
    private val app = application as AnyDownloadApp

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _selectedFormatId = MutableStateFlow(VideoDownloadRepository.BEST_FORMAT_ID)
    val selectedFormatId: StateFlow<String> = _selectedFormatId.asStateFlow()

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Idle)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow(historyRepository.load())
    val history: StateFlow<List<CompletedDownload>> = _history.asStateFlow()

    private val _ytdlpVersion = MutableStateFlow("")
    val ytdlpVersion: StateFlow<String> = _ytdlpVersion.asStateFlow()

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    private val _cookieStatus = MutableStateFlow(cookieRepository.getStatus())
    val cookieStatus: StateFlow<CookieStatus> = _cookieStatus.asStateFlow()

    private val _cookieMessage = MutableStateFlow<String?>(null)
    val cookieMessage: StateFlow<String?> = _cookieMessage.asStateFlow()

    init {
        viewModelScope.launch {
            app.engineState.collect { engine ->
                when (engine) {
                    AnyDownloadApp.EngineState.Initializing -> {
                        _uiState.value = DownloadUiState.InitializingEngine
                    }

                    is AnyDownloadApp.EngineState.Updating -> {
                        _uiState.value = DownloadUiState.UpdatingEngine(
                            "Updating yt-dlp (was ${engine.previousVersion})…",
                        )
                    }

                    is AnyDownloadApp.EngineState.Ready -> {
                        _ytdlpVersion.value = engine.ytdlpVersion
                        _updateMessage.value = when (engine.lastUpdateStatus) {
                            YoutubeDL.UpdateStatus.DONE -> "yt-dlp updated to ${engine.ytdlpVersion}"
                            YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "yt-dlp is up to date (${engine.ytdlpVersion})"
                            else -> engine.lastUpdateError
                        }
                        if (_uiState.value is DownloadUiState.InitializingEngine ||
                            _uiState.value is DownloadUiState.UpdatingEngine
                        ) {
                            _uiState.value = DownloadUiState.Idle
                        }
                    }

                    is AnyDownloadApp.EngineState.Failed -> {
                        _uiState.value = DownloadUiState.Error(engine.message)
                    }
                }
            }
        }
    }

    fun updateYtdlp(useNightly: Boolean = true) {
        if (_uiState.value is DownloadUiState.UpdatingEngine ||
            _uiState.value is DownloadUiState.InitializingEngine
        ) {
            return
        }

        val channel = if (useNightly) {
            YoutubeDL.UpdateChannel._NIGHTLY
        } else {
            YoutubeDL.UpdateChannel._STABLE
        }

        app.updateYtdlp(channel) { ready ->
            _updateMessage.value = when (ready.lastUpdateStatus) {
                YoutubeDL.UpdateStatus.DONE -> "yt-dlp updated to ${ready.ytdlpVersion}"
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> "Already on latest (${ready.ytdlpVersion})"
                else -> ready.lastUpdateError ?: "Update finished"
            }
        }
    }

    fun clearUpdateMessage() {
        _updateMessage.value = null
    }

    fun importCookies(uri: android.net.Uri) {
        viewModelScope.launch {
            cookieRepository.importFromUri(uri)
                .onSuccess { status ->
                    _cookieStatus.value = status
                    _cookieMessage.value = "Cookies imported — ${status.summary}"
                }
                .onFailure { error ->
                    _cookieMessage.value = error.message ?: "Failed to import cookies"
                }
        }
    }

    fun clearCookies() {
        cookieRepository.clear()
        _cookieStatus.value = cookieRepository.getStatus()
        _cookieMessage.value = "All accounts disconnected"
    }

    fun disconnectPlatform(platform: LoginPlatform) {
        cookieRepository.clearPlatform(platform)
        _cookieStatus.value = cookieRepository.getStatus()
        _cookieMessage.value = "${platform.label} disconnected"
    }

    fun completeWebViewLogin(platform: LoginPlatform, onFinished: (Boolean) -> Unit) {
        viewModelScope.launch {
            cookieRepository.saveFromWebViewLogin(platform)
                .onSuccess { status ->
                    _cookieStatus.value = status
                    _cookieMessage.value = "${platform.label} connected — private links enabled"
                    onFinished(true)
                }
                .onFailure { error ->
                    _cookieMessage.value = error.message ?: "Failed to save login"
                    onFinished(false)
                }
        }
    }

    fun clearCookieMessage() {
        _cookieMessage.value = null
    }

    private fun needsCookiesFor(url: String): Boolean {
        return when (SupportedPlatform.detect(url)) {
            SupportedPlatform.INSTAGRAM,
            SupportedPlatform.FACEBOOK,
            -> true
            else -> false
        }
    }

    private fun hasRequiredCookies(url: String): Boolean {
        val status = _cookieStatus.value
        if (!status.isLoaded) return false
        return when (SupportedPlatform.detect(url)) {
            SupportedPlatform.INSTAGRAM -> status.hasInstagram
            SupportedPlatform.FACEBOOK -> status.hasFacebook
            SupportedPlatform.TWITTER -> status.hasTwitter
            else -> true
        }
    }

    fun onUrlChanged(value: String) {
        _urlInput.value = value
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clip = clipboard.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
        val url = extractFirstUrl(text)
        if (url != null) {
            _urlInput.value = url
        }
    }

    fun handleIncomingUrl(raw: String?) {
        val url = extractFirstUrl(raw.orEmpty()) ?: return
        _urlInput.value = url
        analyzeUrl()
    }

    fun onFormatSelected(formatId: String) {
        _selectedFormatId.value = formatId
    }

    fun analyzeUrl() {
        val url = extractFirstUrl(_urlInput.value)
        if (url == null) {
            _uiState.value = DownloadUiState.Error("Paste a valid http(s) link first.")
            return
        }

        if (needsCookiesFor(url) && !hasRequiredCookies(url)) {
            _uiState.value = DownloadUiState.Error(
                message = cookieRequiredMessage(url),
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = DownloadUiState.Analyzing
            runCatching {
                repository.fetchMetadata(url)
            }.onSuccess { metadata ->
                _selectedFormatId.value = VideoDownloadRepository.BEST_FORMAT_ID
                _uiState.value = DownloadUiState.Ready(metadata)
            }.onFailure { error ->
                _uiState.value = DownloadUiState.Error(
                    message = friendlyError(error),
                )
            }
        }
    }

    fun startDownload() {
        val metadata = when (val current = _uiState.value) {
            is DownloadUiState.Ready -> current.metadata
            is DownloadUiState.Error -> current.metadata ?: return
            else -> return
        }

        viewModelScope.launch {
            _uiState.value = DownloadUiState.Downloading(
                metadata = metadata,
                progress = DownloadProgress(0f, null),
            )

            runCatching {
                repository.download(
                    url = metadata.url,
                    formatId = _selectedFormatId.value,
                ) { progress ->
                    _uiState.update {
                        DownloadUiState.Downloading(metadata, progress)
                    }
                }
            }.onSuccess { file ->
                val completed = CompletedDownload(
                    title = metadata.title,
                    filePath = file.absolutePath,
                    platform = metadata.platform,
                )
                _history.update { current ->
                    val updated = listOf(completed) + current
                    historyRepository.save(updated)
                    updated
                }
                _uiState.value = DownloadUiState.Success(metadata, file.absolutePath)
            }.onFailure { error ->
                _uiState.value = DownloadUiState.Error(
                    message = friendlyError(error),
                    metadata = metadata,
                )
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = DownloadUiState.Idle
    }

    private fun friendlyError(error: Throwable): String {
        val message = error.message.orEmpty()
        val cookiesLoaded = _cookieStatus.value.isLoaded
        return when {
            message.contains("login", ignoreCase = true) ||
                message.contains("cookies", ignoreCase = true) ||
                message.contains("rate-limit", ignoreCase = true) ||
                message.contains("authentication", ignoreCase = true) -> {
                if (cookiesLoaded) {
                    "Session expired — tap Connect again for this platform and retry."
                } else {
                    cookieRequiredMessage(_urlInput.value)
                }
            }

            message.contains("Private video", ignoreCase = true) ||
                message.contains("private", ignoreCase = true) -> {
                if (cookiesLoaded) {
                    "This content is private or restricted. Make sure the right account is connected."
                } else {
                    cookieRequiredMessage(_urlInput.value)
                }
            }

            message.contains("Unsupported URL", ignoreCase = true) ->
                "That URL is not supported by the downloader."

            message.contains("403", ignoreCase = true) ||
                message.contains("Forbidden", ignoreCase = true) ||
                message.contains("SABR", ignoreCase = true) ||
                message.contains("older than 90 days", ignoreCase = true) ->
                "YouTube blocked this download. Tap \"Update yt-dlp\" below, wait for it to finish, then try again."

            message.isNotBlank() -> message
            else -> "Something went wrong while processing the link."
        }
    }

    private fun cookieRequiredMessage(url: String): String {
        val platform = SupportedPlatform.detect(url).label
        return "Private $platform link — tap Connect $platform above and sign in first."
    }

    companion object {
        private val URL_REGEX = Regex("""https?://[^\s"'<>]+""")

        fun extractFirstUrl(text: String): String? {
            val trimmed = text.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed.trimEnd('/', '.', ',', ';')
            }
            return URL_REGEX.find(trimmed)?.value?.trimEnd('/', '.', ',', ';')
        }
    }
}
