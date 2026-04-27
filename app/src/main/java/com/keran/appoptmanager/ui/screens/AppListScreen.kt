package com.keran.appoptmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.ui.components.AppCard
import com.keran.appoptmanager.ui.components.AppIndexBar
import com.keran.appoptmanager.ui.components.AppLoadingIndicator
import com.keran.appoptmanager.ui.components.AutoCompleteTextField
import com.keran.appoptmanager.ui.components.ConfirmationBottomSheet
import com.keran.appoptmanager.ui.components.EmptyConfigGuide
import com.keran.appoptmanager.ui.dialogs.EditAppNameDialog
import com.keran.appoptmanager.ui.dialogs.EditRuleDialog
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.ConfigState
import com.keran.appoptmanager.viewmodel.PackageLoadState
import com.keran.appoptmanager.viewmodel.sub.SelectionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    viewModel: AppOptViewModel,
    selectionViewModel: SelectionViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onEditRules: (AppConfig) -> Unit,
    onAddRule: (AppConfig) -> Unit,
    onImportClick: () -> Unit,
    onAddAppClick: () -> Unit
) {
    val uiState by viewModel.filteredUiState.collectAsStateWithLifecycle()
    val allConfigs by viewModel.uiState.collectAsStateWithLifecycle()
    val configState by viewModel.configState.collectAsStateWithLifecycle()
    val packageLoadState by viewModel.packageLoadState.collectAsStateWithLifecycle()
    val hasResolvedFilteredUiState by viewModel.hasResolvedFilteredUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val allProcessNames by viewModel.allProcessNames.collectAsStateWithLifecycle()
    val allThreadNames by viewModel.allThreadNames.collectAsStateWithLifecycle()
    val selectionMode by selectionViewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedPackages by selectionViewModel.selectedPackages.collectAsStateWithLifecycle()
    val iconCacheVersion by viewModel.iconCacheVersion.collectAsStateWithLifecycle()
    val indexMap by viewModel.indexMap.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val context = LocalContext.current
    val vibrator = remember {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
        } catch (e: Exception) {
            null
        }
    }
    
    val indexKeys = remember { listOf("#") + ('A'..'Z').map { it.toString() } }
    var isIndexing by remember { mutableStateOf(false) }
    var currentIndexKey by remember { mutableStateOf<String?>(null) }
    var lastHapticKey by remember { mutableStateOf<String?>(null) }
    var lastHapticTime by remember { mutableStateOf(0L) }
    
    var ruleToDelete by remember { mutableStateOf<Pair<AppConfig, Int>?>(null) }
    var appToDelete by remember { mutableStateOf<AppConfig?>(null) }
    val appToDeleteDisplayName = remember(appToDelete) { 
        appToDelete?.let { viewModel.getDisplayName(it) } ?: ""
    }
    
    var appToEditName by remember { mutableStateOf<AppConfig?>(null) }
    val appToEditDisplayName = remember(appToEditName) { 
        appToEditName?.let { viewModel.getDisplayName(it) } ?: ""
    }
    
    var ruleToEdit by remember { mutableStateOf<Triple<AppConfig, Int, com.keran.appoptmanager.model.Rule>?>(null) }
    
    appToEditName?.let { editingApp ->
        EditAppNameDialog(
            initialName = appToEditDisplayName,
            onDismiss = { appToEditName = null },
            onConfirm = { newName ->
                viewModel.updateAppName(editingApp.packageName, newName)
                appToEditName = null
            }
        )
    }

    ruleToEdit?.let { editing ->
        EditRuleDialog(
            rule = editing,
            processSuggestions = allProcessNames,
            threadSuggestions = allThreadNames,
            onDismiss = { ruleToEdit = null },
            onConfirm = { updatedRule ->
                val (app, index, _) = editing
                viewModel.updateRule(app.packageName, index, updatedRule)
                ruleToEdit = null
            }
        )
    }

    ruleToDelete?.let { pending ->
        ConfirmationBottomSheet(
            title = "删除规则",
            message = "确定要删除这条规则吗？此操作不可撤销。",
            onConfirm = {
                val (app, index) = pending
                viewModel.deleteRule(app.packageName, index)
                ruleToDelete = null
            },
            onDismiss = { ruleToDelete = null }
        )
    }

    appToDelete?.let { pending ->
        ConfirmationBottomSheet(
            title = "删除应用配置",
            message = "确定要删除 $appToDeleteDisplayName 的所有配置吗？此操作将移除该应用及其所有规则。",
            onConfirm = {
                viewModel.deleteApp(pending.packageName)
                appToDelete = null
            },
            onDismiss = { appToDelete = null }
        )
    }

    val sortedList = uiState

    val visibleIndexKeys = remember(indexMap, indexKeys) {
        val ordered = indexKeys.filter { it in indexMap.keys }
        if (ordered.isNotEmpty()) ordered else listOf("#")
    }

    val shouldShowIndexBar by remember {
        derivedStateOf { isIndexing }
    }

    LaunchedEffect(isIndexing, currentIndexKey) {
        val key = currentIndexKey
        val currentTime = System.currentTimeMillis()
        if (isIndexing && key != null && key != lastHapticKey) {
            if (currentTime - lastHapticTime > 20) {
                lastHapticKey = key
                lastHapticTime = currentTime

                try {
                    if (vibrator != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(10)
                        }
                    }
                } catch (e: Exception) {
                }
            }
        }
        if (!isIndexing) {
            lastHapticKey = null
        }
    }

    when (configState) {
        ConfigState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                AppLoadingIndicator()
            }
        }
        ConfigState.FileNotFound -> {
            EmptyConfigGuide(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                title = "配置文件不存在",
                message = "未找到配置文件，请检查路径设置或导入新配置。",
                buttonText = "导入配置",
                onClick = onImportClick
            )
        }
        ConfigState.Empty -> {
             EmptyConfigGuide(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                title = "配置文件为空",
                message = "配置文件内容为空，请添加规则或导入配置。",
                buttonText = "添加应用",
                onClick = onAddAppClick
             )
        }
        ConfigState.Loaded -> {
            when (packageLoadState) {
                PackageLoadState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        AppLoadingIndicator()
                    }
                }

                PackageLoadState.Failed -> {
                    PackageLoadErrorScreen(
                        onRetry = { viewModel.retryLoadPackages() },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding)
                    )
                }

                PackageLoadState.Loaded -> {
                    if (!hasResolvedFilteredUiState) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            AppLoadingIndicator()
                        }
                    } else if (uiState.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("没有找到匹配的应用配置")
                        }
                    } else {
                        Box(modifier = modifier.fillMaxSize()) {
                            val onToggleSelectionLambda: (String) -> Unit = remember(selectionViewModel) { { pkg -> selectionViewModel.toggleSelection(pkg) } }
                            val onSwipeRightLambda: (String) -> Unit = remember(selectionViewModel) { { pkg -> selectionViewModel.enterSelectionMode(pkg) } }
                            val onToggleAppLambda: (String, Boolean) -> Unit = remember(viewModel) { { pkg, enabled -> viewModel.toggleAppEnabled(pkg, enabled) } }
                            val onEditNameLambda: (AppConfig) -> Unit = remember { { config -> appToEditName = config } }
                            val onDeleteAppLambda: (AppConfig) -> Unit = remember { { config -> appToDelete = config } }
                            val onEditRuleLambda: (AppConfig, Int) -> Unit = remember { { config, index -> ruleToEdit = Triple(config, index, config.rules[index]) } }
                            val onDeleteRuleLambda: (AppConfig, Int) -> Unit = remember { { config, index -> ruleToDelete = config to index } }
                            val onToggleRuleLambda: (AppConfig, Int) -> Unit = remember(viewModel) { { config, index ->
                                val newEnabled = !config.rules[index].enabled
                                viewModel.toggleRuleEnabled(config.packageName, index, newEnabled)
                            } }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = contentPadding,
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = uiState,
                                    key = { it.packageName },
                                    contentType = { "AppCard" }
                                ) { app ->
                                    AppCard(
                                        app = app,
                                        displayName = viewModel.getDisplayName(app),
                                        isSelectionMode = selectionMode,
                                        isSelected = app.packageName in selectedPackages,
                                        iconUpdateTrigger = iconCacheVersion,
                                        onToggleSelection = onToggleSelectionLambda,
                                        onSwipeRight = onSwipeRightLambda,
                                        onToggleApp = onToggleAppLambda,
                                        onAddRule = onAddRule,
                                        onEditName = onEditNameLambda,
                                        onDeleteApp = onDeleteAppLambda,
                                        onEditRule = onEditRuleLambda,
                                        onDeleteRule = onDeleteRuleLambda,
                                        onToggleRule = onToggleRuleLambda
                                    )
                                }
                            }

                            if (uiState.isNotEmpty()) {
                                AppIndexBar(
                                    visibleIndexKeys = visibleIndexKeys,
                                    shouldShowIndexBar = shouldShowIndexBar,
                                    currentIndexKey = currentIndexKey,
                                    onIndexSelected = { key ->
                                        currentIndexKey = key
                                        val targetIndex = indexMap[key]
                                        if (targetIndex != null && targetIndex >= 0 && targetIndex < sortedList.size) {
                                            scope.launch {
                                                try {
                                                    listState.scrollToItem(targetIndex)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    },
                                    onDragStateChange = {
                                        isIndexing = it
                                        if (!it) currentIndexKey = null
                                    },
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(top = contentPadding.calculateTopPadding(), bottom = contentPadding.calculateBottomPadding())
                                        .width(40.dp)
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
