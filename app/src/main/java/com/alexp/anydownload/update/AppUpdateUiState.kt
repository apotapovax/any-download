package com.alexp.anydownload.update

sealed class AppUpdateUiState {
    data object Idle : AppUpdateUiState()
    data object Checking : AppUpdateUiState()
    data object UpToDate : AppUpdateUiState()
    data class Available(
        val versionName: String,
        val releaseNotes: String,
    ) : AppUpdateUiState()
    data class Downloading(val percent: Float) : AppUpdateUiState()
    data class ReadyToInstall(val versionName: String) : AppUpdateUiState()
    data class Error(val message: String) : AppUpdateUiState()
}
