package com.alexp.anydownload.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object AppUpdateInstaller {
    fun updatesDir(context: Context): File {
        return File(context.cacheDir, "updates").also { it.mkdirs() }
    }

    fun pendingApkFile(context: Context): File {
        return File(updatesDir(context), GitHubReleaseClient.APK_ASSET_NAME)
    }

    fun isDebuggableInstall(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun installDownloadedApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return false
        }

        return runCatching {
            installWithPackageInstaller(context, apkFile)
        }.getOrElse {
            installWithIntent(context, apkFile)
        }
    }

    private fun installWithPackageInstaller(context: Context, apkFile: File): Boolean {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)

        session.use { activeSession ->
            apkFile.inputStream().use { input ->
                activeSession.openWrite("base.apk", 0, apkFile.length()).use { output ->
                    input.copyTo(output)
                    activeSession.fsync(output)
                }
            }

            val callbackIntent = Intent(context, InstallResultReceiver::class.java).apply {
                action = InstallResultReceiver.ACTION_INSTALL_RESULT
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, sessionId, callbackIntent, flags)
            activeSession.commit(pendingIntent.intentSender)
        }
        return true
    }

    private fun installWithIntent(context: Context, apkFile: File): Boolean {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return true
    }
}
