package com.alexp.anydownload.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
fun CreditsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Credits",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
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
