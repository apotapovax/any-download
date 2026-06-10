package com.alexp.anydownload.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alexp.anydownload.share.ShareTarget
import com.alexp.anydownload.share.VideoShareHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareVideoButtons(
    context: Context,
    filePath: String,
    modifier: Modifier = Modifier,
) {
    val installed = remember(context) { VideoShareHelper.installedTargets(context) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        installed.forEach { target ->
            FilledTonalButton(
                onClick = { VideoShareHelper.shareVideo(context, filePath, target) },
            ) {
                Icon(
                    painter = painterResource(target.iconRes),
                    contentDescription = target.label,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(target.label)
            }
        }

        OutlinedButton(
            onClick = { VideoShareHelper.shareVideo(context, filePath, target = null) },
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.size(6.dp))
            Text("More")
        }
    }

    if (installed.isEmpty()) {
        Text(
            text = "Install WhatsApp, Telegram, or Messenger to share directly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
