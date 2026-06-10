package com.alexp.anydownload

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.alexp.anydownload.ui.MainScreen
import com.alexp.anydownload.ui.theme.AnyDownloadTheme
import com.alexp.anydownload.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val systemDark = isSystemInDarkTheme()
            val initialDark = ThemePreferences.isDarkMode(this) ?: systemDark
            var darkMode by rememberSaveable { mutableStateOf(initialDark) }

            AnyDownloadTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(
                        viewModel = viewModel,
                        darkModeEnabled = darkMode,
                        onDarkModeChanged = { enabled ->
                            darkMode = enabled
                            ThemePreferences.setDarkMode(this, enabled)
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                viewModel.handleIncomingUrl(sharedText)
            }

            Intent.ACTION_VIEW -> {
                viewModel.handleIncomingUrl(intent.dataString)
            }
        }
    }
}
