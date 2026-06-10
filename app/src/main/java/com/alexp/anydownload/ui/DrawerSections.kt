package com.alexp.anydownload.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexp.anydownload.CookieStatus
import com.alexp.anydownload.data.LoginPlatform

@Composable
fun DrawerCookiesCard(
    status: CookieStatus,
    onConnect: (LoginPlatform) -> Unit,
    onDisconnect: (LoginPlatform) -> Unit,
    onImport: () -> Unit,
    onClearAll: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (status.isLoaded) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.Cookie, contentDescription = null)
                Text(
                    text = "Connected accounts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(text = status.summary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Tap Connect and sign in once for private links.",
                style = MaterialTheme.typography.bodySmall,
            )
            LoginPlatform.entries.forEach { platform ->
                PlatformConnectRow(
                    platform = platform,
                    isConnected = when (platform) {
                        LoginPlatform.INSTAGRAM -> status.hasInstagram
                        LoginPlatform.FACEBOOK -> status.hasFacebook
                        LoginPlatform.TWITTER -> status.hasTwitter
                    },
                    onConnect = { onConnect(platform) },
                    onDisconnect = { onDisconnect(platform) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Import file")
                }
                if (status.isLoaded) {
                    OutlinedButton(onClick = onClearAll) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerEngineCard(
    ytdlpVersion: String,
    isUpdating: Boolean,
    onUpdate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Download engine",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (ytdlpVersion.isBlank()) "yt-dlp: loading…" else "yt-dlp: $ytdlpVersion",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Update if YouTube shows 403 or \"older than 90 days\" errors.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = onUpdate,
                enabled = !isUpdating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                }
                Text("Update yt-dlp")
            }
        }
    }
}

@Composable
fun PlatformConnectRow(
    platform: LoginPlatform,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = platform.label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        if (isConnected) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("Connected") })
                OutlinedButton(onClick = onDisconnect) {
                    Text("Disconnect")
                }
            }
        } else {
            Button(onClick = onConnect) {
                Text("Connect")
            }
        }
    }
}
