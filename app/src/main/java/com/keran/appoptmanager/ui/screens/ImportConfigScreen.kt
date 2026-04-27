package com.keran.appoptmanager.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.keran.appoptmanager.model.DiffType
import com.keran.appoptmanager.model.ImportDiff
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleDiff
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.sub.ImportViewModel
import java.io.File
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportConfigScreen(
    viewModel: AppOptViewModel,
    importViewModel: ImportViewModel,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.background,
    uiOpacity: Float = 1f,
    onBack: () -> Unit
) {
    val importUiState by importViewModel.importUiState.collectAsStateWithLifecycle()
    val zipSelectionState by importViewModel.zipSelectionState.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var selectedPackages by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    if (zipSelectionState.showDialog) {
        var selectedFile by remember { mutableStateOf(zipSelectionState.fileNames.firstOrNull() ?: "") }
        
        AlertDialog(
            onDismissRequest = { importViewModel.cancelZipSelection() },
            title = { Text("选择配置子文件") },
            text = {
                Column {
                    Text("检测到压缩包内包含多个配置，请选择您要导入的一项：")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.height(200.dp)
                    ) {
                        items(zipSelectionState.fileNames) { fileName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFile = fileName }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = fileName == selectedFile,
                                    onClick = { selectedFile = fileName }
                                )
                                Text(
                                    text = fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        zipSelectionState.uri?.let { uri ->
                            importViewModel.setCurrentConfigs(viewModel.uiState.value)
                            importViewModel.selectZipFile(uri, selectedFile)
                        }
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { importViewModel.cancelZipSelection() }) {
                    Text("取消")
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }

    val importKey by remember(importUiState) {

        derivedStateOf {
            importUiState.joinToString("|") { diff ->
                "${diff.installedApp.packageName}:${diff.type}"
            }
        }
    }

    LaunchedEffect(importKey) {
        if (importUiState.isEmpty()) {
            query = ""
            selectedPackages = emptySet()
        } else {
            query = ""
            selectedPackages = defaultSelectedPackages(importUiState)
        }
    }

    val filteredDiffs by remember(importUiState, query) {
        derivedStateOf {
            filterImportDiffs(importUiState, query)
        }
    }

    val selectedCount by remember(importUiState, selectedPackages) {
        derivedStateOf {
            countSelectedPackages(importUiState, selectedPackages)
        }
    }
    
    val clipboardManager = LocalClipboardManager.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                importViewModel.setCurrentConfigs(viewModel.uiState.value)
                importViewModel.analyzeImport(it)
            }
        }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("导入配置") },
                navigationIcon = {
                    IconButton(onClick = {
                        importViewModel.clearImportState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = uiOpacity)
                )
            )
        },
        containerColor = containerColor,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        bottomBar = {
            if (importUiState.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val selectedDiffs = filterSelectedDiffs(importUiState, selectedPackages)
                            val currentConfigs = viewModel.uiState.value
                            importViewModel.applyImport(
                                currentConfigs = currentConfigs,
                                onConfigUpdated = { updatedConfigs ->
                                    viewModel.updateAllConfigs(updatedConfigs)
                                },
                                onSaveConfig = { viewModel.saveConfig() }
                            )
                            onBack()
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("确认导入选中项 ($selectedCount)")
                    }
                }
            }
        }
    ) { innerPadding ->
            if (importUiState.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "导入配置方案",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "支持 .conf / .prop / .zip 文件、分享链接或配置文本。系统将自动匹配已安装的应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { launcher.launch("application/octet-stream,application/zip,text/plain,application/x-zip-compressed") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                ) {
                    Text("选择本地文件")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedButton(
                        onClick = {
                            val text = clipboardManager.getText()?.text ?: ""
                            importViewModel.setCurrentConfigs(viewModel.uiState.value)
                            importViewModel.analyzeImportFromShareCode(text)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从分享码导入")
                    }
                    OutlinedButton(
                        onClick = {
                            val text = clipboardManager.getText()?.text ?: ""
                            importViewModel.setCurrentConfigs(viewModel.uiState.value)
                            importViewModel.analyzeImportFromConfigText(text)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("从配置文本导入")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("搜索应用名称或包名...") }
                    )
                }

                item {
                    val visiblePackages = filteredDiffs.map { it.installedApp.packageName }.toSet()
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "当前已选中 $selectedCount 个应用 (共 ${importUiState.size} 个)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    selectedPackages = selectedPackages + visiblePackages
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("一键全选")
                            }

                            TextButton(
                                onClick = {
                                    selectedPackages = selectedPackages - visiblePackages
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("取消全选")
                            }
                        }
                    }
                }

                items(
                    items = filteredDiffs,
                    key = { it.installedApp.packageName }
                ) { diff ->
                    val isSelected = diff.installedApp.packageName in selectedPackages
                    ImportDiffCard(
                        diff = diff,
                        selected = isSelected,
                        uiOpacity = uiOpacity,
                        onSelectedChange = { checked ->
                            selectedPackages = if (checked) {
                                selectedPackages + diff.installedApp.packageName
                            } else {
                                selectedPackages - diff.installedApp.packageName
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ImportDiffCard(
    diff: ImportDiff,
    selected: Boolean,
    uiOpacity: Float,
    onSelectedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = uiOpacity)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (uiOpacity < 1f) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { onSelectedChange(!selected) }
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = onSelectedChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(diff.installedApp.iconPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = diff.installedApp.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = diff.installedApp.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                DiffTag(diff.type)
            }

            val newCount = diff.ruleDiffs.count { it.isNew }
            val modifiedCount = diff.ruleDiffs.count { it.isModified }
            val deletedCount = diff.ruleDiffs.count { it.isDeleted }

            if (newCount + modifiedCount + deletedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "配置差异：新增 $newCount | 修改 $modifiedCount | 删除 $deletedCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            if (diff.ruleDiffs.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "对象",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.2f)
                        )
                        Text(
                            text = "当前",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "导入",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    diff.ruleDiffs.forEach { ruleDiff ->
                        RuleDiffRow(ruleDiff)
                    }
                }
            } else {
                Text(
                    text = "无规则差异",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }
        }
    }
}

@Composable
fun DiffTag(type: DiffType) {
    val (color, text) = when (type) {
        DiffType.NEW -> MaterialTheme.colorScheme.primary to "新增"
        DiffType.MODIFIED -> MaterialTheme.colorScheme.tertiary to "修改"
        DiffType.IDENTICAL -> MaterialTheme.colorScheme.outline to "相同"
    }
    
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun RuleDiffRow(diff: RuleDiff) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            val typeLabel = when (diff.type) {
                RuleType.MAIN -> "主进程"
                RuleType.SUB -> "子进程"
                RuleType.THREAD -> "线程"
            }
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (diff.target.isNotEmpty()) {
                Text(
                    text = diff.target,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            if (diff.oldRule != null) {
                RuleValueContent(diff.oldRule, isOld = true)
            } else {
                Text("-", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (diff.newRule != null) {
                RuleValueContent(diff.newRule, isOld = false, compareTo = diff.oldRule)
            } else {
                Text("删除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RuleValueContent(rule: Rule, isOld: Boolean, compareTo: Rule? = null) {
    Column {
        val coresChanged = !isOld && compareTo != null && compareTo.cores != rule.cores
        val enabledChanged = !isOld && compareTo != null && compareTo.enabled != rule.enabled
        
        val coresColor = if (coresChanged) MaterialTheme.colorScheme.primary else if (isOld) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        val coresWeight = if (coresChanged) FontWeight.Bold else FontWeight.Normal

        Text(
            text = rule.cores,
            style = MaterialTheme.typography.bodySmall,
            color = coresColor,
            fontWeight = coresWeight
        )
        
        if (!rule.enabled) {
            val color = if (enabledChanged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
             Text(
                text = "已禁用",
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = if (enabledChanged) FontWeight.Bold else FontWeight.Normal
            )
        } else if (enabledChanged) {
            Text(
                text = "已启用",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

internal fun defaultSelectedPackages(diffs: List<ImportDiff>): Set<String> {
    return diffs
        .asSequence()
        .filter { it.type != DiffType.IDENTICAL }
        .map { it.installedApp.packageName }
        .toSet()
}

internal fun filterImportDiffs(diffs: List<ImportDiff>, query: String): List<ImportDiff> {
    if (query.isBlank()) return diffs

    return diffs.filter { diff ->
        diff.installedApp.name.contains(query, ignoreCase = true) ||
            diff.installedApp.packageName.contains(query, ignoreCase = true)
    }
}

internal fun countSelectedPackages(diffs: List<ImportDiff>, selectedPackages: Set<String>): Int {
    val allPackages = diffs.map { it.installedApp.packageName }.toSet()
    return selectedPackages.count { it in allPackages }
}

internal fun filterSelectedDiffs(diffs: List<ImportDiff>, selectedPackages: Set<String>): List<ImportDiff> {
    if (selectedPackages.isEmpty()) return emptyList()
    return diffs.filter { diff ->
        diff.installedApp.packageName in selectedPackages
    }
}
