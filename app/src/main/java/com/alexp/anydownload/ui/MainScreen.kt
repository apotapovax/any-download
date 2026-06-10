package com.alexp.anydownload.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.alexp.anydownload.CompletedDownload
import com.alexp.anydownload.DownloadUiState
import com.alexp.anydownload.MainViewModel
import com.alexp.anydownload.VideoMetadata
import com.alexp.anydownload.data.LoginPlatform
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    darkModeEnabled: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val urlInput by viewModel.urlInput.collectAsState()
    val selectedFormatId by viewModel.selectedFormatId.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.history.collectAsState()
    val ytdlpVersion by viewModel.ytdlpVersion.collectAsState()
    val updateMessage by viewModel.updateMessage.collectAsState()
    val cookieStatus by viewModel.cookieStatus.collectAsState()
    val cookieMessage by viewModel.cookieMessage.collectAsState()
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var activeLogin by remember { mutableStateOf<LoginPlatform?>(null) }
    var drawerDestination by remember { mutableStateOf(DrawerDestination.Download) }

    if (activeLogin != null) {
        PlatformLoginScreen(
            platform = activeLogin!!,
            onLoginCaptured = {
                val platform = activeLogin!!
                viewModel.completeWebViewLogin(platform) { success ->
                    if (success) activeLogin = null
                }
            },
            onCancel = { activeLogin = null },
        )
        return
    }

    val cookiePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { viewModel.importCookies(it) }
    }

    LaunchedEffect(uiState) {
        if (uiState is DownloadUiState.Error) {
            snackbarHostState.showSnackbar((uiState as DownloadUiState.Error).message)
        }
    }

    LaunchedEffect(updateMessage) {
        updateMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearUpdateMessage()
        }
    }

    LaunchedEffect(cookieMessage) {
        cookieMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearCookieMessage()
        }
    }

    val screenTitle = when (drawerDestination) {
        DrawerDestination.Download -> "Any Download"
        DrawerDestination.Accounts -> "Connected accounts"
        DrawerDestination.Engine -> "Download engine"
        DrawerDestination.Credits -> "Credits"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawerSheet(
                selected = drawerDestination,
                onDestinationSelected = { destination ->
                    drawerDestination = destination
                    scope.launch { drawerState.close() }
                },
                darkModeEnabled = darkModeEnabled,
                onDarkModeChanged = onDarkModeChanged,
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(screenTitle) },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            when (drawerDestination) {
                DrawerDestination.Download -> DownloadContent(
                    modifier = Modifier.padding(padding),
                    urlInput = urlInput,
                    selectedFormatId = selectedFormatId,
                    uiState = uiState,
                    history = history,
                    onUrlChange = viewModel::onUrlChanged,
                    onPaste = { viewModel.pasteFromClipboard(context) },
                    onAnalyze = viewModel::analyzeUrl,
                    onFormatSelected = viewModel::onFormatSelected,
                    onDownload = viewModel::startDownload,
                    onReset = viewModel::resetToIdle,
                    context = context,
                )

                DrawerDestination.Accounts -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item {
                        DrawerCookiesCard(
                            status = cookieStatus,
                            onConnect = { activeLogin = it },
                            onDisconnect = viewModel::disconnectPlatform,
                            onImport = {
                                cookiePicker.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                            },
                            onClearAll = viewModel::clearCookies,
                        )
                    }
                }

                DrawerDestination.Engine -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item {
                        DrawerEngineCard(
                            ytdlpVersion = ytdlpVersion,
                            isUpdating = uiState is DownloadUiState.UpdatingEngine ||
                                uiState is DownloadUiState.InitializingEngine,
                            onUpdate = { viewModel.updateYtdlp(useNightly = true) },
                        )
                    }
                }

                DrawerDestination.Credits -> {
                    LaunchedEffect(Unit) {
                        viewModel.checkForAppUpdate()
                    }
                    CreditsContent(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        currentVersionName = viewModel.currentVersionName,
                        appUpdateState = appUpdateState,
                        onCheckForUpdate = viewModel::checkForAppUpdate,
                        onDownloadUpdate = viewModel::downloadAppUpdate,
                        onInstallUpdate = { viewModel.installAppUpdate(context) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadContent(
    modifier: Modifier,
    urlInput: String,
    selectedFormatId: String,
    uiState: DownloadUiState,
    history: List<CompletedDownload>,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit,
    onFormatSelected: (String) -> Unit,
    onDownload: () -> Unit,
    onReset: () -> Unit,
    context: android.content.Context,
) {
    val isBusy = uiState is DownloadUiState.Analyzing ||
        uiState is DownloadUiState.Downloading ||
        uiState is DownloadUiState.InitializingEngine ||
        uiState is DownloadUiState.UpdatingEngine

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            UrlInputCard(
                url = urlInput,
                onUrlChange = onUrlChange,
                onPaste = onPaste,
                onAnalyze = onAnalyze,
                isBusy = isBusy,
            )
        }

        when (val state = uiState) {
            is DownloadUiState.InitializingEngine -> item { LoadingCard("Starting download engine…") }
            is DownloadUiState.UpdatingEngine -> item { LoadingCard(state.message) }
            is DownloadUiState.Analyzing -> item { LoadingCard("Analyzing link…") }
            is DownloadUiState.Ready -> item {
                MetadataCard(
                    metadata = state.metadata,
                    selectedFormatId = selectedFormatId,
                    onFormatSelected = onFormatSelected,
                    onDownload = onDownload,
                )
            }
            is DownloadUiState.Downloading -> item {
                DownloadProgressCard(
                    metadata = state.metadata,
                    progress = state.progress.percent,
                    etaSeconds = state.progress.etaSeconds,
                )
            }
            is DownloadUiState.Success -> item {
                SuccessCard(
                    title = state.metadata.title,
                    filePath = state.filePath,
                    onOpen = { openDownloadedFile(context, state.filePath) },
                    onDone = onReset,
                    context = context,
                )
            }
            is DownloadUiState.Error -> {
                state.metadata?.let { metadata ->
                    item {
                        MetadataCard(
                            metadata = metadata,
                            selectedFormatId = selectedFormatId,
                            onFormatSelected = onFormatSelected,
                            onDownload = onDownload,
                        )
                    }
                }
            }
            DownloadUiState.Idle -> Unit
        }

        if (history.isNotEmpty()) {
            item {
                Text(
                    text = "Recent downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(history, key = { it.filePath }) { item ->
                HistoryItem(
                    item = item,
                    context = context,
                    onOpen = { openDownloadedFile(context, item.filePath) },
                )
            }
        }
    }
}

@Composable
private fun UrlInputCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onPaste: () -> Unit,
    onAnalyze: () -> Unit,
    isBusy: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Paste a video link",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://x.com/... or instagram.com/...") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                singleLine = false,
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onPaste,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Paste")
                }
                Button(
                    onClick = onAnalyze,
                    enabled = !isBusy && url.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                    Spacer(Modifier.size(8.dp))
                    Text("Analyze")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetadataCard(
    metadata: VideoMetadata,
    selectedFormatId: String,
    onFormatSelected: (String) -> Unit,
    onDownload: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            metadata.thumbnail?.let { thumbnail ->
                AsyncImage(
                    model = thumbnail,
                    contentDescription = metadata.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            AssistChip(onClick = {}, label = { Text(metadata.platform.label) })

            Text(
                text = metadata.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            metadata.uploader?.let { uploader ->
                Text(
                    text = uploader,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(text = "Format", style = MaterialTheme.typography.titleSmall)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                metadata.formats.take(8).forEach { format ->
                    FilterChip(
                        selected = selectedFormatId == format.id,
                        onClick = { onFormatSelected(format.id) },
                        label = { Text(format.label) },
                    )
                }
            }

            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Download")
            }
        }
    }
}

@Composable
private fun DownloadProgressCard(
    metadata: VideoMetadata,
    progress: Float,
    etaSeconds: Long?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Downloading", style = MaterialTheme.typography.titleMedium)
            Text(text = metadata.title)
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = buildString {
                    append("${progress.toInt()}%")
                    etaSeconds?.let { append(" · ETA ${it}s") }
                },
            )
        }
    }
}

@Composable
private fun SuccessCard(
    title: String,
    filePath: String,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    context: android.content.Context,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Saved to Downloads/AnyDownload",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = title)
            Text(text = filePath, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpen) { Text("Open file") }
                OutlinedButton(onClick = onDone) { Text("Download another") }
            }
            Text(
                text = "Share",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            ShareVideoButtons(context = context, filePath = filePath)
        }
    }
}

@Composable
private fun HistoryItem(
    item: CompletedDownload,
    context: android.content.Context,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(item.title, fontWeight = FontWeight.Medium)
            Text(text = item.platform.label, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onOpen) { Text("Open") }
            ShareVideoButtons(context = context, filePath = item.filePath)
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(message)
        }
    }
}

private fun openDownloadedFile(context: android.content.Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) return

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open video"))
}
