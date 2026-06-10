package com.alexp.anydownload.update

import org.json.JSONObject

data class UpdateMetadata(
    val versionCode: Int,
    val versionName: String,
    val apkAssetName: String,
    val minSdk: Int,
) {
    fun isNewerThan(currentVersionCode: Int): Boolean = versionCode > currentVersionCode

    companion object {
        fun fromJson(json: String): UpdateMetadata {
            val obj = JSONObject(json)
            return UpdateMetadata(
                versionCode = obj.getInt("versionCode"),
                versionName = obj.getString("versionName"),
                apkAssetName = obj.getString("apkAssetName"),
                minSdk = obj.optInt("minSdk", 26),
            )
        }
    }
}

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseNotes: String,
    val metadata: UpdateMetadata,
    val apkDownloadUrl: String,
)
