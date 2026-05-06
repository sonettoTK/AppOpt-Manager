package com.keran.appoptmanager.viewmodel.sub

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keran.appoptmanager.data.AppConfigRepository
import com.keran.appoptmanager.data.ConfigParser
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.DiffType
import com.keran.appoptmanager.model.ImportDiff
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleDiff
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.utils.DeepLinkManager
import com.keran.appoptmanager.utils.ZipUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    private val installedAppRepository: InstalledAppRepository
) : ViewModel() {

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    private val _importUiState = MutableStateFlow<List<ImportDiff>>(emptyList())
    val importUiState: StateFlow<List<ImportDiff>> = _importUiState.asStateFlow()

    private var currentConfigsCache: List<AppConfig> = emptyList()

    fun setCurrentConfigs(configs: List<AppConfig>) {
        currentConfigsCache = configs
    }

    data class ZipSelectionState(
        val showDialog: Boolean = false,
        val uri: Uri? = null,
        val fileNames: List<String> = emptyList()
    )

    private val _zipSelectionState = MutableStateFlow(ZipSelectionState())
    val zipSelectionState: StateFlow<ZipSelectionState> = _zipSelectionState.asStateFlow()

    fun analyzeImport(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                if (ZipUtils.isZipFile(appConfigRepository.getContext(), uri)) {
                    val entryNames = ZipUtils.readZipEntryNames(appConfigRepository.getContext(), uri)
                    val validEntries = mutableListOf<String>()
                    
                    for (entryName in entryNames) {
                        if (!ConfigParser.hasValidExtension(entryName)) continue
                        
                        val content = ZipUtils.readZipEntryFullContent(appConfigRepository.getContext(), uri, entryName)
                        if (content.isNotEmpty() && ConfigParser.hasValidConfigLine(content)) {
                            validEntries.add(entryName)
                        }
                    }
                    
                    if (validEntries.isEmpty()) {
                        emitToast("压缩包中未找到有效配置文件")
                        return@launch
                    }
                    
                    if (validEntries.size == 1) {
                        val content = ZipUtils.readZipEntryFullContent(appConfigRepository.getContext(), uri, validEntries[0])
                        val configs = ConfigParser.parse(content)
                        if (configs.isEmpty()) {
                            emitToast("配置文件格式错误")
                            return@launch
                        }
                        processImportedConfigs(configs)
                    } else {
                        _zipSelectionState.value = ZipSelectionState(
                            showDialog = true,
                            fileNames = validEntries,
                            uri = uri
                        )
                    }
                } else {
                    val content = appConfigRepository.readConfigFromUri(uri)
                    if (content.isEmpty()) {
                        emitToast("文件内容为空")
                        return@launch
                    }
                    
                    val configs = ConfigParser.parse(content)
                    if (configs.isEmpty()) {
                        emitToast("配置文件格式错误")
                        return@launch
                    }
                    
                    processImportedConfigs(configs)
                }
            }.onFailure {
                emitToast("读取文件失败: ${it.message}")
            }
        }
    }
    
    fun analyzeImportFromShareCode(text: String) {
        if (text.isBlank()) {
            emitToast("剪贴板内容为空")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val configs = DeepLinkManager.parseShareCode(text)
                if (configs.isNullOrEmpty()) {
                    emitToast("无法解析分享码")
                    return@launch
                }
                processImportedConfigs(configs)
            }.onFailure {
                emitToast("解析分享码失败: ${it.message}")
            }
        }
    }
    
    fun analyzeImportFromConfigText(text: String) {
        if (text.isBlank()) {
            emitToast("剪贴板内容为空")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val configs = ConfigParser.parse(text)
                if (configs.isEmpty()) {
                    emitToast("无法解析配置文本")
                    return@launch
                }
                processImportedConfigs(configs)
            }.onFailure {
                emitToast("解析配置文本失败: ${it.message}")
            }
        }
    }

    fun selectZipFile(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelZipSelection()

            runCatching {
                val content = ZipUtils.readZipEntryFullContent(appConfigRepository.getContext(), uri, fileName)

                if (content.isEmpty()) {
                    emitToast("无法读取文件内容")
                } else {
                    ConfigParser.parse(content).let {
                        if (it.isEmpty()) emitToast("配置文件格式错误或内容为空")
                        else processImportedConfigs(it)
                    }
                }
            }.onFailure { emitToast("读取文件失败: ${it.message}") }
        }
    }

    fun cancelZipSelection() {
        _zipSelectionState.value = ZipSelectionState()
    }

    fun handleDeepLink(intent: Intent, currentConfigs: List<AppConfig> = currentConfigsCache) {
        val uri = intent.data ?: return
        currentConfigsCache = currentConfigs
        viewModelScope.launch {
            val configs = DeepLinkManager.parseDeepLink(uri)
            if (configs != null && configs.isNotEmpty()) {
                processImportedConfigs(configs)
            } else {
                _toastEvent.emit("链接解析失败或无效")
            }
        }
    }

    private suspend fun processImportedConfigs(importedConfigs: List<AppConfig>) {
        val installedApps = installedAppRepository.getInstalledApps().associateBy { it.packageName }
        val currentMap = currentConfigsCache.associateBy { it.packageName }

        val diffs = importedConfigs.mapNotNull { config ->
            val app = installedApps[config.packageName] ?: return@mapNotNull null

            val oldConfig = currentMap[config.packageName]
            val oldRules = oldConfig?.rules ?: emptyList()
            val newRules = config.rules.toList()

            val ruleDiffs = buildRuleDiffs(oldRules.toList(), newRules)
                .asSequence()
                .filterNot { it.isSame }
                .sortedWith(ruleDiffOrdering())
                .toList()

            val type = when {
                oldConfig == null -> DiffType.NEW
                ruleDiffs.isEmpty() -> DiffType.IDENTICAL
                else -> DiffType.MODIFIED
            }

            ImportDiff(config, oldConfig, app, type, ruleDiffs)
        }

        _importUiState.value = sortImportDiffs(diffs)
    }

    fun applyImport(
        currentConfigs: List<AppConfig>,
        selectedPackages: Set<String>,
        onConfigUpdated: (List<AppConfig>) -> Unit,
        onSaveConfig: () -> Unit
    ) {
        if (selectedPackages.isEmpty()) {
            _importUiState.value = emptyList()
            return
        }

        val currentMap = currentConfigs.associateBy { it.packageName }.toMutableMap()

        for (diff in _importUiState.value) {
            val pkg = diff.appConfig.packageName
            if (pkg !in selectedPackages) continue

            val oldConfig = currentMap[pkg]
            currentMap[pkg] = diff.appConfig.copy(
                alias = oldConfig?.alias ?: diff.appConfig.alias
            )
        }

        val updatedList = currentMap.values.sortedBy { (it.alias ?: it.packageName).trim() }
        onConfigUpdated(updatedList)
        onSaveConfig()
        _importUiState.value = emptyList()
    }

    fun clearImportState() {
        _importUiState.value = emptyList()
    }

    private fun emitToast(message: String) {
        viewModelScope.launch { _toastEvent.emit(message) }
    }
}

internal fun sortImportDiffs(diffs: List<ImportDiff>): List<ImportDiff> {
    fun typePriority(type: DiffType): Int = when (type) {
        DiffType.NEW -> 0
        DiffType.MODIFIED -> 1
        DiffType.IDENTICAL -> 2
    }

    return diffs.sortedWith(
        compareBy<ImportDiff> { typePriority(it.type) }
            .thenBy { it.installedApp.name }
            .thenBy { it.installedApp.packageName }
    )
}

internal fun buildRuleDiffs(oldRules: List<Rule>, newRules: List<Rule>): List<RuleDiff> {
    fun ruleKey(rule: Rule) = com.keran.appoptmanager.model.RuleKey(rule.type, rule.target)

    val oldMap = oldRules.associateBy { ruleKey(it) }
    val newMap = newRules.associateBy { ruleKey(it) }

    val keyComparator = compareBy<com.keran.appoptmanager.model.RuleKey>({ it.type.ordinal }, { it.target })
    val allKeys = (oldMap.keys + newMap.keys).toSortedSet(keyComparator)

    return allKeys.map { key ->
        RuleDiff(
            target = key.target,
            type = key.type,
            oldRule = oldMap[key],
            newRule = newMap[key]
        )
    }
}

internal fun ruleDiffOrdering(): Comparator<RuleDiff> {
    fun kindPriority(diff: RuleDiff): Int = when {
        diff.isNew -> 0
        diff.isModified -> 1
        diff.isDeleted -> 2
        else -> 3
    }

    return compareBy<RuleDiff> { kindPriority(it) }.thenBy { it.target }
}
