package com.alexp.anydownload.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.alexp.anydownload.data.LoginPlatform

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PlatformLoginScreen(
    platform: LoginPlatform,
    onLoginCaptured: () -> Unit,
    onCancel: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("Sign in with your ${platform.label} account") }
    var loginCaptured by remember { mutableStateOf(false) }
    val cookieManager = remember { CookieManager.getInstance() }

    BackHandler(onBack = onCancel)

    fun tryCaptureCookies(source: String) {
        if (loginCaptured) return
        cookieManager.flush()
        if (!platform.isLoggedIn(cookieManager)) return

        loginCaptured = true
        statusMessage = "$source — saving session…"
        onLoginCaptured()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect ${platform.label}") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Use the same username and password as the ${platform.label} app. " +
                        "We capture the browser session — the official app cannot share its login directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp))
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.userAgentString =
                            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                if (url != null) {
                                    tryCaptureCookies("Logged in")
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): Boolean {
                                request?.url?.toString()?.let { url ->
                                    if (platform.isLoggedIn(cookieManager)) {
                                        tryCaptureCookies("Logged in")
                                    }
                                }
                                return false
                            }
                        }

                        clearPlatformCookies(platform, cookieManager)
                        loadUrl(platform.loginUrl)
                    }
                },
            )
        }
    }
}

private fun clearPlatformCookies(platform: LoginPlatform, cookieManager: CookieManager) {
    for (url in platform.cookieUrls()) {
        val cookies = cookieManager.getCookie(url)?.split(';').orEmpty()
        for (part in cookies) {
            val name = part.substringBefore('=').trim()
            if (name.isNotEmpty()) {
                cookieManager.setCookie(url, "$name=; Max-Age=0")
            }
        }
    }
    cookieManager.flush()
}
