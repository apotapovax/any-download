package com.alexp.anydownload.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

enum class ShareTarget(
    val label: String,
    val iconRes: Int,
    val packageNames: List<String>,
) {
    WHATSAPP("WhatsApp", com.alexp.anydownload.R.drawable.ic_whatsapp, listOf("com.whatsapp", "com.whatsapp.w4b")),
    TELEGRAM("Telegram", com.alexp.anydownload.R.drawable.ic_telegram, listOf("org.telegram.messenger", "org.telegram.messenger.web")),
    MESSENGER("Messenger", com.alexp.anydownload.R.drawable.ic_messenger, listOf("com.facebook.orca")),
    ;

    fun resolveInstalledPackage(context: Context): String? {
        val pm = context.packageManager
        return packageNames.firstOrNull { pkg ->
            pm.getLaunchIntentForPackage(pkg) != null
        }
    }
}

object VideoShareHelper {
    fun shareVideo(
        context: Context,
        filePath: String,
        target: ShareTarget?,
    ): Boolean {
        val file = File(filePath)
        if (!file.exists()) return false

        val uri = fileUri(context, file)
        val mime = guessMimeType(file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("video", uri)
        }

        if (target != null) {
            val pkg = target.resolveInstalledPackage(context)
            if (pkg == null) return false
            shareIntent.setPackage(pkg)
            context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(shareIntent)
            return true
        }

        context.startActivity(
            Intent.createChooser(shareIntent, "Share video"),
        )
        return true
    }

    fun installedTargets(context: Context): List<ShareTarget> {
        return ShareTarget.entries.filter { it.resolveInstalledPackage(context) != null }
    }

    private fun fileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    private fun guessMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "3gp" -> "video/3gpp"
            else -> "video/*"
        }
    }
}
