package com.alexp.anydownload.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexp.anydownload.update.AppUpdateUiState

enum class DrawerDestination {
    Download,
    Accounts,
    Engine,
    Credits,
}

@Composable
fun AppDrawerSheet(
    selected: DrawerDestination,
    onDestinationSelected: (DrawerDestination) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier.width(300.dp)) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = "Any Download",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
            )

            NavigationDrawerItem(
                label = { Text("Download") },
                selected = selected == DrawerDestination.Download,
                onClick = { onDestinationSelected(DrawerDestination.Download) },
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Connected accounts") },
                selected = selected == DrawerDestination.Accounts,
                onClick = { onDestinationSelected(DrawerDestination.Accounts) },
                icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Download engine") },
                selected = selected == DrawerDestination.Engine,
                onClick = { onDestinationSelected(DrawerDestination.Engine) },
                icon = { Icon(Icons.Default.Build, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            NavigationDrawerItem(
                label = { Text("Credits") },
                selected = selected == DrawerDestination.Credits,
                onClick = { onDestinationSelected(DrawerDestination.Credits) },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Dark mode",
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = darkModeEnabled, onCheckedChange = onDarkModeChanged)
            }
        }
    }
}

@Composable
fun CreditsContent(
    currentVersionName: String,
    appUpdateState: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Credits",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        AppUpdateCard(
            currentVersionName = currentVersionName,
            state = appUpdateState,
            onCheckForUpdate = onCheckForUpdate,
            onDownloadUpdate = onDownloadUpdate,
            onInstallUpdate = onInstallUpdate,
        )
        Text(
            text = "Supported for personal use",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text("X / Twitter · Instagram · Facebook · YouTube")
        Text(
            text = "Powered by yt-dlp. Share a post from Instagram or X and pick \"Download video\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Any Download — personal sideload app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AppUpdateCard(
    currentVersionName: String,
    state: AppUpdateUiState,
    onCheckForUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onInstallUpdate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                Text(
                    text = "App updates",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Installed: v$currentVersionName",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Checks GitHub Releases for a newer signed APK. Android installs updates — it does not hot-reload app code.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (state) {
                AppUpdateUiState.Idle -> Unit
                AppUpdateUiState.Checking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Checking GitHub…")
                    }
                }
                AppUpdateUiState.UpToDate -> {
                    AssistChip(onClick = {}, label = { Text("Up to date") })
                }
                is AppUpdateUiState.Available -> {
                    AssistChip(onClick = {}, label = { Text("Update available: v${state.versionName}") })
                    if (state.releaseNotes.isNotBlank()) {
                        Text(
                            text = state.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Button(onClick = onDownloadUpdate, modifier = Modifier.fillMaxWidth()) {
                        Text("Download update")
                    }
                }
                is AppUpdateUiState.Downloading -> {
                    Text("Downloading… ${state.percent.toInt()}%")
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { (state.percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is AppUpdateUiState.ReadyToInstall -> {
                    Text("Downloaded v${state.versionName}. Ready to install.")
                    Button(onClick = onInstallUpdate, modifier = Modifier.fillMaxWidth()) {
                        Text("Install update")
                    }
                }
                is AppUpdateUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedButton(
                onClick = onCheckForUpdate,
                enabled = state !is AppUpdateUiState.Checking && state !is AppUpdateUiState.Downloading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Check for updates")
            }
        }
    }
}
