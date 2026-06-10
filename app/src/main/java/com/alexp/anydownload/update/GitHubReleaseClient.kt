package com.alexp.anydownload.update

import com.alexp.anydownload.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

class GitHubReleaseClient {
    suspend fun fetchLatestRelease(): Result<GitHubReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val response = getApi(LATEST_RELEASE_URL)
            parseReleaseResponse(response)
        }
    }

    suspend fun downloadAsset(
        downloadUrl: String,
        destination: java.io.File,
        onProgress: (Float) -> Unit,
    ): Result<java.io.File> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection(downloadUrl)
            connection.requestMethod = "GET"
            applyDownloadHeaders(connection)

            val totalBytes = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            var downloaded = 0L

            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (totalBytes > 0) {
                            onProgress((downloaded.toFloat() / totalBytes.toFloat()) * 100f)
                        }
                    }
                }
            }

            if (destination.length() <= 0L) {
                throw IllegalStateException("Downloaded APK is empty.")
            }

            destination
        }
    }

    private fun parseReleaseResponse(json: String): GitHubReleaseInfo {
        val release = JSONObject(json)
        val tagName = release.getString("tag_name")
        val releaseNotes = release.optString("body").trim()
        val assets = release.getJSONArray("assets")

        var metadata: UpdateMetadata? = null
        var apkDownloadUrl: String? = null

        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.getString("name")
            when (name) {
                UPDATE_METADATA_ASSET -> {
                    val metadataUrl = asset.getString("browser_download_url")
                    metadata = UpdateMetadata.fromJson(getDownload(metadataUrl))
                }
                APK_ASSET_NAME -> {
                    apkDownloadUrl = asset.getString("browser_download_url")
                }
            }
        }

        val resolvedMetadata = metadata
            ?: throw IllegalStateException("Release is missing $UPDATE_METADATA_ASSET.")
        val resolvedApkUrl = apkDownloadUrl
            ?: throw IllegalStateException("Release is missing $APK_ASSET_NAME.")

        return GitHubReleaseInfo(
            tagName = tagName,
            releaseNotes = releaseNotes,
            metadata = resolvedMetadata,
            apkDownloadUrl = resolvedApkUrl,
        )
    }

    private fun getApi(url: String): String {
        val connection = openConnection(url)
        connection.requestMethod = "GET"
        applyApiHeaders(connection)
        return readSuccess(connection)
    }

    private fun getDownload(url: String): String {
        val connection = openConnection(url)
        connection.requestMethod = "GET"
        applyDownloadHeaders(connection)
        return readSuccess(connection)
    }

    private fun readSuccess(connection: HttpURLConnection): String {
        val code = connection.responseCode
        if (code !in 200..299) {
            val errorBody = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            throw IllegalStateException(
                "GitHub request failed ($code): ${errorBody.ifBlank { connection.responseMessage }}",
            )
        }

        return connection.inputStream.bufferedReader().use(BufferedReader::readText)
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
        }
    }

    private fun applyApiHeaders(connection: HttpURLConnection) {
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", USER_AGENT)
    }

    private fun applyDownloadHeaders(connection: HttpURLConnection) {
        connection.setRequestProperty("Accept", "*/*")
        connection.setRequestProperty("User-Agent", USER_AGENT)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 20_000
        private const val READ_TIMEOUT_MS = 120_000
        private const val USER_AGENT = "AnyDownload/${BuildConfig.VERSION_NAME}"

        const val UPDATE_METADATA_ASSET = "update-metadata.json"
        const val APK_ASSET_NAME = "AnyDownload-arm64-v8a.apk"

        private val LATEST_RELEASE_URL =
            "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest"
    }
}
