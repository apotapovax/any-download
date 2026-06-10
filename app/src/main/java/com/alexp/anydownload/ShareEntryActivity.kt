package com.alexp.anydownload

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Dedicated entry for the system share sheet (Instagram, X, Chrome, etc.).
 * High-priority intent filter so Any Download appears near the top.
 */
class ShareEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedText = extractSharedText(intent)

        val launch = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, sharedText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launch)
        finish()
    }

    private fun extractSharedText(intent: Intent): String? {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { return it }

        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let { return it }

        intent.clipData?.let { clip ->
            if (clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString()?.let { return it }
                clip.getItemAt(0).uri?.toString()?.let { return it }
            }
        }

        return intent.dataString
    }
}
