package com.keran.appoptmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keran.appoptmanager.data.AppConfigRepository
import com.keran.appoptmanager.data.InstalledApp
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.data.SettingsRepository
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.utils.AppSorter
import com.keran.appoptmanager.utils.DeepLinkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

sealed interface StartupStage {
    data object CheckingRoot : StartupStage
    data object RootDenied : StartupStage
    data object LoadingConfig : StartupStage
    data object Ready : StartupStage
}

enum class ConfigState { Loading, Loaded, FileNotFound, Empty }

enum class PackageLoadState { Loading, Loaded, Failed }

@HiltViewModel
class AppOptViewModel @Inject constructor(
    private val appConfigRepository: AppConfigRepository,
    settingsRepository: SettingsRepository,
    private val installedAppRepository: InstalledAppRepository
) : ViewModel() {

    private val coordinator = StartupCoordinator(
        appConfigRepository, settingsRepository, installedAppRepository
    )

    val startupStage: StateFlow<StartupStage> = coordinator.startupStage
    val configState: StateFlow<ConfigState> = coordinator.configState
    val packageLoadState: StateFlow<PackageLoadState> = coordinator.packageLoadState
    val installedPackages: StateFlow<Set<String>> = coordinator.installedPackages
    val hasResolvedFilteredUiState: StateFlow<Boolean> = coordinator.hasResolvedFilteredUiState
    val startupCompleted = coordinator.startupCompleted

    private val _uiState = MutableStateFlow<ImmutableList<AppConfig>>(persistentListOf())
    val uiState: StateFlow<ImmutableList<AppConfig>> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery: StateFlow<String> = _searchQuery
        .debounce(300)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ""
        )

    val filteredUiState: StateFlow<ImmutableList<AppConfig>> = combine(
        _uiState,
        debouncedSearchQuery,
        coordinator.installedPackages,
        coordinator.packageLoadState
    ) { apps, query, installed, pkgState ->
        filterAndSortApps(apps, query, installed, pkgState)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, persistentListOf())

    private fun filterAndSortApps(
        apps: ImmutableList<AppConfig>,
        query: String,
        installedPackages: Set<String>,
        packageLoadState: PackageLoadState
    ): ImmutableList<AppConfig> {
        val installedApps = when {
            packageLoadState == PackageLoadState.Failed -> apps
            packageLoadState == PackageLoadState.Loading -> apps
            else -> apps.filter { it.packageName in installedPackages }
        }
        val filtered = if (query.isBlank()) installedApps else {
            installedApps.filter {
                val displayName = getDisplayName(it)
                displayName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }
        return AppSorter.sort(filtered) { getDisplayName(it) }.toImmutableList()
    }

    val indexMap: StateFlow<Map<String, Int>> = filteredUiState
        .map { buildIndexMap(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun buildIndexMap(sortedList: ImmutableList<AppConfig>): Map<String, Int> {
        return buildMap {
            sortedList.forEachIndexed { index, app ->
                val displayName = getDisplayName(app)
                val key = AppSorter.getAppInitial(displayName)
                if (key !in this) put(key, index)
            }
        }
    }

    val allProcessNames: StateFlow<List<String>> = _uiState
        .map { extractProcessNames(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun extractProcessNames(apps: ImmutableList<AppConfig>): List<String> =
        apps.flatMap { it.rules }
            .filter { it.type == RuleType.SUB || (it.type == RuleType.THREAD && ":" in it.target) }
            .map { if (":" in it.target) it.target.substringBefore(":") else it.target }
            .distinct()
            .sorted()

    val allThreadNames: StateFlow<List<String>> = _uiState
        .map { extractThreadNames(it) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun extractThreadNames(apps: ImmutableList<AppConfig>): List<String> =
        apps.flatMap { it.rules }
            .filter { it.type == RuleType.THREAD }
            .map { if (":" in it.target) it.target.substringAfter(":") else it.target }
            .distinct()
            .sorted()

    private val _iconCacheVersion = MutableStateFlow(0)
    val iconCacheVersion: StateFlow<Int> = _iconCacheVersion.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val saveMutex = Mutex()

    init {
        viewModelScope.launch {
            combine(
                coordinator.configState,
                coordinator.packageLoadState
            ) { cs, ps -> cs to ps }.collect {
                coordinator.updateResolutionState()
            }
        }
        viewModelScope.launch {
            coordinator.runStartupPipeline().let { configs ->
                if (configs.isNotEmpty()) {
                    _uiState.value = configs.toImmutableList()
                }
            }
            if (coordinator.configState.value == ConfigState.Loaded) {
                cacheIcons(_uiState.value)
            }
        }
    }

    fun retryStartup() {
        viewModelScope.launch {
            coordinator.runStartupPipeline().let { configs ->
                if (configs.isNotEmpty()) {
                    _uiState.value = configs.toImmutableList()
                }
            }
        }
    }

    fun getDisplayName(app: AppConfig): String {
        return app.alias?.takeIf { it.isNotBlank() }
            ?: installedAppRepository.getAppName(app.packageName)
            ?: app.packageName
    }

    fun saveConfig() {
        viewModelScope.launch {
            saveMutex.withLock {
                _isSaving.value = true
                try {
                    appConfigRepository.saveConfig(coordinator.currentEffectivePath, _uiState.value)
                } finally {
                    _isSaving.value = false
                }
            }
        }
    }

    fun retryLoadPackages() {
        viewModelScope.launch {
            coordinator.refreshInstalledPackages()
        }
    }

    fun refreshConfig() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                coordinator.refreshConfig().let { configs ->
                    if (configs.isNotEmpty()) {
                        _uiState.value = configs.toImmutableList()
                    } else {
                        _uiState.value = persistentListOf()
                    }
                }
                if (coordinator.configState.value == ConfigState.Loaded) {
                    cacheIcons(_uiState.value)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addApp(app: InstalledApp) {
        val newApp = AppConfig(
            packageName = app.packageName,
            enabled = true,
            rules = persistentListOf()
        )
        _uiState.update { currentList ->
            if (currentList.any { it.packageName == app.packageName }) {
                currentList
            } else {
                (currentList + newApp).toImmutableList()
            }
        }
        coordinator.markConfigLoaded()
        saveConfig()
    }

    fun updateAppRules(appConfig: AppConfig, newRules: List<Rule>) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == appConfig.packageName) {
                    app.copy(rules = newRules.toPersistentList())
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun removeApp(packageName: String) {
        _uiState.update { currentList ->
            currentList.filter { it.packageName != packageName }.toImmutableList()
        }
        updateConfigStateAfterDelete()
        saveConfig()
    }

    fun toggleAppEnabled(packageName: String, enabled: Boolean) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName) {
                    app.copy(
                        enabled = enabled,
                        rules = app.rules.map { it.copy(enabled = enabled) }.toPersistentList()
                    )
                } else app
            }.toImmutableList()
        }
        saveConfig()
    }

    fun updateAllConfigs(newConfigs: List<AppConfig>) {
        _uiState.value = newConfigs.toImmutableList()
    }

    fun toggleRuleEnabled(packageName: String, ruleIndex: Int, enabled: Boolean) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName && ruleIndex in app.rules.indices) {
                    val updatedRules = app.rules.set(ruleIndex, app.rules[ruleIndex].copy(enabled = enabled))
                    app.copy(rules = updatedRules)
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun deleteSelectedApps(packagesToDelete: Set<String>) {
        if (packagesToDelete.isEmpty()) return

        _uiState.update { currentList ->
            currentList.filter { it.packageName !in packagesToDelete }.toImmutableList()
        }

        updateConfigStateAfterDelete()
        saveConfig()
    }

    fun updateAppName(packageName: String, newName: String) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName) app.copy(alias = newName) else app
            }.toImmutableList()
        }
        saveConfig()
    }

    fun updateRule(packageName: String, ruleIndex: Int, newRule: Rule) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName && ruleIndex in app.rules.indices) {
                    app.copy(rules = app.rules.set(ruleIndex, newRule))
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun deleteRule(packageName: String, ruleIndex: Int) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == packageName) {
                    if (ruleIndex !in app.rules.indices) return@map app
                    val updatedRules = app.rules.removeAt(ruleIndex)
                    app.copy(rules = updatedRules)
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun deleteApp(packageName: String) {
        _uiState.update { currentList ->
            currentList.filter { it.packageName != packageName }.toImmutableList()
        }

        updateConfigStateAfterDelete()
        saveConfig()
    }

    fun setRulesEnabledForSelectedApps(packages: Set<String>, enabled: Boolean) {
        if (packages.isEmpty()) return

        var anyChange = false
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName !in packages) return@map app
                val unchanged =
                    app.enabled == enabled && app.rules.all { it.enabled == enabled }
                if (unchanged) return@map app
                anyChange = true
                app.copy(
                    enabled = enabled,
                    rules = app.rules.map { it.copy(enabled = enabled) }.toPersistentList()
                )
            }.toImmutableList()
        }
        if (anyChange) {
            saveConfig()
        }
    }

    private fun updateConfigStateAfterDelete() {
        if (_uiState.value.isEmpty()) {
            coordinator.markConfigEmpty()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun prepareShareConfigFile(): java.io.File? {
        return appConfigRepository.exportConfigToCache(_uiState.value)
    }

    fun prepareShareLink(): String? {
        val configList = filteredUiState.value
        if (configList.isEmpty()) return null
        return try {
            DeepLinkManager.createShareLink(configList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun cacheIcons(allApps: List<AppConfig>) {
        val installedPackages = coordinator.getInstalledPackagesSnapshot()
        if (installedPackages.isEmpty()) return

        val packageNames = allApps
            .asSequence()
            .map { it.packageName }
            .filter { it in installedPackages }
            .toList()
        if (packageNames.isEmpty()) return

        installedAppRepository.preCacheIconsParallel(packageNames, maxConcurrency = 8)
        _iconCacheVersion.update { it + 1 }
    }


}
