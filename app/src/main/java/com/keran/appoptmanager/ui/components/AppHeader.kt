package com.keran.appoptmanager.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keran.appoptmanager.model.ToolbarItem
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.sub.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppHeader(
    viewModel: AppOptViewModel,
    settingsViewModel: SettingsViewModel,
    onSettingsClick: () -> Unit,
    onImportClick: () -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchAnimationReady by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val backgroundImagePath by settingsViewModel.backgroundImagePath.collectAsStateWithLifecycle()
    val uiOpacity by settingsViewModel.uiOpacity.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showShareDialog by remember { mutableStateOf(false) }
    val toolbarOrder by settingsViewModel.toolbarOrder.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val headerAlpha = if (backgroundImagePath.isNotEmpty()) uiOpacity else 0.85f

    LaunchedEffect(Unit) {
        searchAnimationReady = true
    }

    if (showShareDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { androidx.compose.material3.Text("选择分享方式") },
            text = {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showShareDialog = false
                            scope.launch {
                                val file = viewModel.prepareShareConfigFile()
                                if (file != null) {
                                    shareConfigFile(context, file)
                                } else {
                                    android.widget.Toast.makeText(context, "导出失败", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                        androidx.compose.material3.Text("分享配置文件", modifier = Modifier.weight(1f))
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showShareDialog = false
                            scope.launch {
                                val link = viewModel.prepareShareLink()
                                if (link != null) {
                                    clipboardManager.setText(AnnotatedString(link))
                                    android.widget.Toast.makeText(context, "链接已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "生成链接失败 (可能是内容过长)", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(12.dp))
                        androidx.compose.material3.Text("复制分享链接", modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showShareDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    androidx.compose.material3.Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = headerAlpha),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (searchAnimationReady) {
            androidx.compose.animation.AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    if (targetState) {
                        (androidx.compose.animation.slideInHorizontally { width -> width } + androidx.compose.animation.fadeIn()).togetherWith(
                            androidx.compose.animation.slideOutHorizontally { width -> -width } + androidx.compose.animation.fadeOut())
                    } else {
                        (androidx.compose.animation.slideInHorizontally { width -> -width } + androidx.compose.animation.fadeIn()).togetherWith(
                            androidx.compose.animation.slideOutHorizontally { width -> width } + androidx.compose.animation.fadeOut())
                    }
                },
                label = "SearchAnimation"
            ) { searchActive ->
                AppHeaderContent(
                    searchActive = searchActive,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::setSearchQuery,
                    onCloseSearch = {
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                    },
                    toolbarOrder = toolbarOrder,
                    onRefresh = viewModel::refreshConfig,
                    onSave = viewModel::saveConfig,
                    onImport = onImportClick,
                    onShare = { showShareDialog = true },
                    onSearch = { isSearchActive = true },
                    onSettings = onSettingsClick,
                    isRefreshing = isRefreshing,
                    isSaving = isSaving
                )
            }
        } else {
            AppHeaderContent(
                searchActive = isSearchActive,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::setSearchQuery,
                onCloseSearch = {
                    isSearchActive = false
                    viewModel.setSearchQuery("")
                },
                toolbarOrder = toolbarOrder,
                onRefresh = viewModel::refreshConfig,
                onSave = viewModel::saveConfig,
                onImport = onImportClick,
                onShare = { showShareDialog = true },
                onSearch = { isSearchActive = true },
                onSettings = onSettingsClick,
                isRefreshing = isRefreshing,
                isSaving = isSaving
            )
        }
    }
}

@Composable
private fun AppHeaderContent(
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCloseSearch: () -> Unit,
    toolbarOrder: List<ToolbarItem>,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    isRefreshing: Boolean,
    isSaving: Boolean
) {
    if (searchActive) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { androidx.compose.material3.Text("搜索应用...") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        RoundedIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Clear",
                            onClick = { onSearchQueryChange("") },
                            size = 28.dp,
                            iconSize = 18.dp,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            RoundedIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close Search",
                onClick = onCloseSearch,
                size = 36.dp,
                iconSize = 22.dp,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Text(
                text = "AppOpt",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

            ToolbarButtons(
                order = toolbarOrder,
                onRefresh = onRefresh,
                onSave = onSave,
                onImport = onImport,
                onShare = onShare,
                onSearch = onSearch,
                onSettings = onSettings,
                isRefreshing = isRefreshing,
                isSaving = isSaving
            )
        }
    }
}

@Composable
fun ToolbarButtons(
    order: List<ToolbarItem>,
    onRefresh: () -> Unit,
    onSave: () -> Unit,
    onImport: () -> Unit,
    onShare: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    isRefreshing: Boolean = false,
    isSaving: Boolean = false
) {
    androidx.compose.foundation.layout.Row {
        order.forEachIndexed { index, item ->
            if (index > 0) androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
            when (item) {
                ToolbarItem.Refresh -> {
                    if (isRefreshing) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    } else {
                        HeaderNavBtn(Icons.Default.Refresh, "Refresh", onRefresh)
                    }
                }
                ToolbarItem.Save -> {
                    if (isSaving) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    } else {
                        HeaderNavBtn(Icons.Default.Save, "Save", onSave)
                    }
                }
                ToolbarItem.Import -> HeaderNavBtn(Icons.Default.Description, "Import", onImport)
                ToolbarItem.Share -> HeaderNavBtn(Icons.Default.Share, "Share", onShare)
                ToolbarItem.Search -> HeaderNavBtn(Icons.Default.Search, "Search", onSearch)
                ToolbarItem.Settings -> HeaderNavBtn(Icons.Default.Settings, "Settings", onSettings)
            }
        }
    }
}

fun shareConfigFile(context: android.content.Context, file: File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "分享配置文件"))
}
