package com.alexp.anydownload.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log

class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_RESULT) return

        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()

        val result = when (status) {
            PackageInstaller.STATUS_SUCCESS -> InstallResult.Success
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallResult.Failure(
                "Signing key mismatch. Uninstall this app, install the GitHub release build, then update from there.",
            )
            PackageInstaller.STATUS_FAILURE_INVALID -> InstallResult.Failure(
                "Invalid update package. Download again from GitHub Releases.",
            )
            PackageInstaller.STATUS_FAILURE_STORAGE -> InstallResult.Failure(
                "Not enough storage to install the update.",
            )
            else -> InstallResult.Failure(
                message.ifBlank { "Install failed (code $status)." },
            )
        }

        Log.i(TAG, "Install result: $status $message")
        UpdateInstallBus.publish(result)
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "com.alexp.anydownload.INSTALL_RESULT"
        private const val TAG = "InstallResultReceiver"
    }
}

sealed class InstallResult {
    data object Success : InstallResult()
    data class Failure(val message: String) : InstallResult()
}

object UpdateInstallBus {
    private val listeners = mutableSetOf<(InstallResult) -> Unit>()

    fun publish(result: InstallResult) {
        listeners.forEach { it.invoke(result) }
    }

    fun subscribe(listener: (InstallResult) -> Unit) {
        listeners += listener
    }

    fun unsubscribe(listener: (InstallResult) -> Unit) {
        listeners -= listener
    }
}
