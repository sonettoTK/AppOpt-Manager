package com.keran.appoptmanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import com.keran.appoptmanager.data.InstalledApp
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.ToolbarItem
import com.keran.appoptmanager.ui.RuleConfigContext
import com.keran.appoptmanager.ui.Screen
import com.keran.appoptmanager.ui.components.SelectionHeader
import com.keran.appoptmanager.ui.dialogs.UpdateDialog
import com.keran.appoptmanager.ui.components.AppHeader
import com.keran.appoptmanager.ui.screens.AppListScreen
import com.keran.appoptmanager.ui.screens.AppPickerScreen
import com.keran.appoptmanager.ui.screens.ImportConfigScreen
import com.keran.appoptmanager.ui.screens.RuleConfigScreen
import com.keran.appoptmanager.ui.screens.SettingsScreen
import com.keran.appoptmanager.ui.screens.ToolbarSortScreen
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.sub.ImportViewModel
import com.keran.appoptmanager.viewmodel.sub.SelectionViewModel
import com.keran.appoptmanager.viewmodel.sub.SettingsViewModel
import com.keran.appoptmanager.viewmodel.sub.UpdateViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AppNavigation(
    viewModel: AppOptViewModel,
    importViewModel: ImportViewModel,
    updateViewModel: UpdateViewModel,
    settingsViewModel: SettingsViewModel,
    selectionViewModel: SelectionViewModel,
    installedAppRepository: InstalledAppRepository
) {
    var currentScreen by remember { mutableStateOf(Screen.List) }
    var ruleConfigContext by remember { mutableStateOf<RuleConfigContext?>(null) }
    var previousScreen by remember { mutableStateOf(Screen.List) }
    
    val allProcessNames by viewModel.allProcessNames.collectAsStateWithLifecycle()
    val allThreadNames by viewModel.allThreadNames.collectAsStateWithLifecycle()
    val selectionMode by selectionViewModel.selectionMode.collectAsStateWithLifecycle()
    val selectedPackages by selectionViewModel.selectedPackages.collectAsStateWithLifecycle()
    val backgroundImagePath by settingsViewModel.backgroundImagePath.collectAsStateWithLifecycle()
    val uiOpacity by settingsViewModel.uiOpacity.collectAsStateWithLifecycle()
    val importUiState by importViewModel.importUiState.collectAsStateWithLifecycle()
    val configuredPackages by viewModel.uiState.collectAsStateWithLifecycle(initialValue = persistentListOf())
    
    LaunchedEffect(importUiState) {
        if (importUiState.isNotEmpty() && currentScreen != Screen.ImportConfig) {
            currentScreen = Screen.ImportConfig
        }
    }
    
    val localContext = LocalContext.current
    
    LaunchedEffect(Unit) {
        importViewModel.toastEvent.collect { message ->
            android.widget.Toast.makeText(localContext, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val showUpdateDialog by updateViewModel.showUpdateDialog.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val isDownloading by updateViewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by updateViewModel.downloadProgress.collectAsStateWithLifecycle()

    updateInfo?.takeIf { showUpdateDialog }?.let { info ->
        UpdateDialog(
            info = info,
            isDownloading = isDownloading,
            downloadProgress = downloadProgress,
            onDismiss = { updateViewModel.dismissUpdateDialog() },
            onUpdate = { updateViewModel.startDownloadAndInstall() }
        )
    }

    if (backgroundImagePath.isNotEmpty()) {
        val file = File(backgroundImagePath)
        if (file.exists()) {
            Box(modifier = Modifier.fillMaxSize()) {
                coil.compose.AsyncImage(
                    model = file,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    val containerColor = MaterialTheme.colorScheme.background.copy(alpha = if (backgroundImagePath.isNotEmpty()) uiOpacity else 1f)

    var isFabVisible by remember { mutableStateOf(true) }
    var isScrolling by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!isScrolling) {
                    isScrolling = true
                    isFabVisible = false
                }
                return Offset.Zero
            }
            
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                scope.launch {
                    delay(600)
                    isScrolling = false
                    isFabVisible = true
                }
                return Offset.Zero
            }
        }
    }

    BackHandler(enabled = currentScreen != Screen.List) {
        currentScreen = when (currentScreen) {
            Screen.Settings -> Screen.List
            Screen.AppPicker -> Screen.List
            Screen.ImportConfig -> Screen.List
            Screen.ToolbarSort -> Screen.Settings
            Screen.RuleConfig -> previousScreen
            else -> Screen.List
        }
    }

    when (currentScreen) {
        Screen.List -> {
            Scaffold(
                modifier = Modifier.nestedScroll(nestedScrollConnection),
                contentWindowInsets = WindowInsets(0),
                containerColor = containerColor,
                topBar = {
                    if (selectionMode) {
                        val uiState by viewModel.filteredUiState.collectAsStateWithLifecycle()
                        val isAllSelected = selectedPackages.size == uiState.size && uiState.isNotEmpty()

                        SelectionHeader(
                            selectedCount = selectedPackages.size,
                            isAllSelected = isAllSelected,
                            onExitSelectionMode = { selectionViewModel.exitSelectionMode() },
                            onToggleSelectAll = {
                                if (isAllSelected) selectionViewModel.deselectAll() else selectionViewModel.selectAll(uiState.map { it.packageName }.toSet())
                            },
                            onInvertSelection = { selectionViewModel.invertSelection(uiState.map { it.packageName }.toSet()) },
                            onBatchEnableRules = {
                                viewModel.setRulesEnabledForSelectedApps(selectedPackages, true)
                                selectionViewModel.exitSelectionMode()
                            },
                            onBatchDisableRules = {
                                viewModel.setRulesEnabledForSelectedApps(selectedPackages, false)
                                selectionViewModel.exitSelectionMode()
                            },
                            onDeleteSelected = {
                                viewModel.deleteSelectedApps(selectedPackages)
                                selectionViewModel.exitSelectionMode()
                            }
                        )
                    } else {
                        AppHeader(
                            viewModel = viewModel,
                            settingsViewModel = settingsViewModel,
                            onSettingsClick = { currentScreen = Screen.Settings },
                            onImportClick = { currentScreen = Screen.ImportConfig }
                        )
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(
                        visible = isFabVisible && !selectionMode,
                        enter = scaleIn(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = scaleOut(animationSpec = tween(200))
                    ) {
                        FloatingActionButton(
                            onClick = { currentScreen = Screen.AppPicker },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .padding(bottom = 32.dp)
                                .size(56.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add App")
                        }
                    }
                }
            ) { innerPadding ->
                AppListScreen(
                    viewModel = viewModel,
                    selectionViewModel = selectionViewModel,
                    listState = listState,
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 88.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    onEditRules = { },
                    onAddRule = { config ->
                        val iconPath = File(localContext.cacheDir, "app_icons/${config.packageName}.png").absolutePath
                        val app = InstalledApp(
                            name = viewModel.getDisplayName(config),
                            packageName = config.packageName,
                            iconPath = iconPath,
                            isSystem = false
                        )
                        ruleConfigContext = RuleConfigContext(app, persistentListOf())
                        previousScreen = Screen.List
                        currentScreen = Screen.RuleConfig
                    },
                    onImportClick = { currentScreen = Screen.ImportConfig },
                    onAddAppClick = { currentScreen = Screen.AppPicker }
                )
            }
        }
        Screen.Settings -> {
            SettingsScreen(
                viewModel = viewModel,
                settingsViewModel = settingsViewModel,
                onBack = { currentScreen = Screen.List },
                onNavigateToToolbarSort = { currentScreen = Screen.ToolbarSort }
            )
        }
        Screen.ToolbarSort -> {
            ToolbarSortScreen(
                settingsViewModel = settingsViewModel,
                onBack = { currentScreen = Screen.Settings }
            )
        }
        Screen.AppPicker -> {
            AppPickerScreen(
                repository = installedAppRepository,
                configuredPackages = configuredPackages.map { it.packageName }.toSet(),
                containerColor = containerColor,
                uiOpacity = if (backgroundImagePath.isNotEmpty()) uiOpacity else 1f,
                onBack = { currentScreen = Screen.List },
                onAppSelected = { app ->
                    ruleConfigContext = RuleConfigContext(app, persistentListOf())
                    previousScreen = Screen.AppPicker
                    currentScreen = Screen.RuleConfig
                }
            )
        }
        Screen.RuleConfig -> {
            ruleConfigContext?.let { context ->
                RuleConfigScreen(
                    app = context.app,
                    initialRules = context.initialRules,
                    processSuggestions = allProcessNames,
                    threadSuggestions = allThreadNames,
                    onBack = { currentScreen = previousScreen },
                    onSave = { rules ->
                        if (previousScreen == Screen.AppPicker) {
                            viewModel.addApp(context.app)
                        } else {
                            val existing = viewModel.uiState.value
                                .find { it.packageName == context.app.packageName }
                            if (existing != null) {
                                viewModel.updateAppRules(existing, rules)
                            } else {
                                viewModel.addApp(context.app)
                            }
                        }
                        currentScreen = Screen.List
                    }
                )
            }
        }
        Screen.ImportConfig -> {
            ImportConfigScreen(
                viewModel = viewModel,
                importViewModel = importViewModel,
                containerColor = containerColor,
                uiOpacity = if (backgroundImagePath.isNotEmpty()) uiOpacity else 1f,
                onBack = { currentScreen = Screen.List }
            )
        }
    }
}
