package com.keran.appoptmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keran.appoptmanager.data.AppConfigRepository
import com.keran.appoptmanager.data.ConfigParser
import com.keran.appoptmanager.data.InstalledApp
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.data.SettingsRepository
import com.keran.appoptmanager.data.ShellUtils
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import com.keran.appoptmanager.utils.AppSorter
import com.keran.appoptmanager.utils.DeepLinkManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 应用启动阶段机：从检查 Root 到完全就绪 */
sealed interface StartupStage {
    data object CheckingRoot : StartupStage
    data object RootDenied : StartupStage
    data object LoadingConfig : StartupStage
    data object Ready : StartupStage
}

/** 配置文件数据状态（供列表界面判断显示内容） */
enum class ConfigState { Loading, Loaded, FileNotFound, Empty }

/** 已安装应用列表加载状态 */
enum class PackageLoadState { Loading, Loaded, Failed }

class AppOptViewModel(
    private val appConfigRepository: AppConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val installedAppRepository: InstalledAppRepository
) : ViewModel() {
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

    private val _installedPackages = MutableStateFlow<Set<String>>(emptySet())
    val installedPackages: StateFlow<Set<String>> = _installedPackages.asStateFlow()

    private val _packageLoadState = MutableStateFlow(PackageLoadState.Loading)
    val packageLoadState: StateFlow<PackageLoadState> = _packageLoadState.asStateFlow()

    private val _hasResolvedFilteredUiState = MutableStateFlow(false)
    val hasResolvedFilteredUiState: StateFlow<Boolean> = _hasResolvedFilteredUiState.asStateFlow()

    private val _startupStage = MutableStateFlow<StartupStage>(StartupStage.CheckingRoot)
    val startupStage: StateFlow<StartupStage> = _startupStage.asStateFlow()

    private val _configState = MutableStateFlow(ConfigState.Loading)
    val configState: StateFlow<ConfigState> = _configState.asStateFlow()

    /** 启动完成后触发一次，供其他 ViewModel 响应 */
    private val _startupCompleted = MutableSharedFlow<Unit>()
    val startupCompleted: SharedFlow<Unit> = _startupCompleted.asSharedFlow()

    val filteredUiState: StateFlow<ImmutableList<AppConfig>> = combine(
        _uiState,
        debouncedSearchQuery,
        installedPackages,
        _packageLoadState
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

    private var currentEffectivePath: String = SettingsRepository.DEFAULT_PATH

    private data class LoadRequest(
        val requestedPath: String,
        val tryDefaultPaths: Boolean
    )

    private suspend fun buildLoadRequest(): LoadRequest {
        val savedPath = settingsRepository.getSavedConfigPath()
        return LoadRequest(
            requestedPath = savedPath ?: SettingsRepository.DEFAULT_PATH,
            // 只有未设置自定义路径时，才允许在默认/备用路径中自动兜底
            tryDefaultPaths = savedPath == null
        )
    }

    init {
        observeFilteredUiStateResolution()
        refreshInstalledPackages()
        startStartupPipeline()
    }

    private fun observeFilteredUiStateResolution() {
        viewModelScope.launch {
            combine(_configState, _packageLoadState) { configState, packageLoadState ->
                configState to packageLoadState
            }.collect { (_, packageLoadState) ->
                if (packageLoadState == PackageLoadState.Failed) {
                    _hasResolvedFilteredUiState.value = true
                }
            }
        }

        viewModelScope.launch {
            combine(
                _uiState,
                debouncedSearchQuery,
                installedPackages,
                _packageLoadState,
                _configState
            ) { apps, query, installed, pkgState, configState ->
                FilterResolutionSnapshot(
                    apps = apps,
                    query = query,
                    installedPackages = installed,
                    packageLoadState = pkgState,
                    configState = configState
                )
            }.collect { snapshot ->
                if (
                    snapshot.configState == ConfigState.Loaded &&
                    snapshot.packageLoadState == PackageLoadState.Loaded
                ) {
                    filterAndSortApps(
                        apps = snapshot.apps,
                        query = snapshot.query,
                        installedPackages = snapshot.installedPackages,
                        packageLoadState = snapshot.packageLoadState
                    )
                    _hasResolvedFilteredUiState.value = true
                }
            }
        }
    }

    /** 执行启动管线：检查 Root → 加载配置 → 就绪 */
    private fun startStartupPipeline() {
        viewModelScope.launch {
            // 阶段一：检查 Root 权限
            _startupStage.value = StartupStage.CheckingRoot
            val hasRoot = withContext(Dispatchers.IO) {
                ShellUtils.checkRootPermission()
            }
            if (!hasRoot) {
                _startupStage.value = StartupStage.RootDenied
                return@launch
            }

            // 阶段二：加载配置文件
            _startupStage.value = StartupStage.LoadingConfig
            val request = buildLoadRequest()
            loadConfigInternal(request.requestedPath, tryDefaultPaths = request.tryDefaultPaths)

            // 阶段三：就绪
            _startupStage.value = StartupStage.Ready
            _startupCompleted.emit(Unit)

            // 后台：缓存已安装应用的图标
            if (_configState.value == ConfigState.Loaded) {
                cacheIconsForInstalledApps(_uiState.value)
            }
        }
    }

    /** 从失败状态重试启动管线 */
    fun retryStartup() {
        startStartupPipeline()
    }

    private suspend fun loadConfigInternal(path: String, tryDefaultPaths: Boolean = true) {
        _configState.value = ConfigState.Loading

        val (effectivePath, fileExists) = resolveConfigPath(path, tryDefaultPaths)
        currentEffectivePath = effectivePath

        if (!fileExists) {
            _configState.value = ConfigState.FileNotFound
            _uiState.value = persistentListOf()
            return
        }

        val configApps = appConfigRepository.loadConfig(effectivePath)

        if (configApps.isEmpty()) {
            _configState.value = ConfigState.Empty
            _uiState.value = persistentListOf()
        } else {
            _uiState.value = configApps.toImmutableList()
            _configState.value = ConfigState.Loaded
        }
    }

    private suspend fun resolveConfigPath(path: String, tryDefaultPaths: Boolean): Pair<String, Boolean> {
        if (appConfigRepository.checkFileExists(path)) return path to true
        if (!tryDefaultPaths) return path to false

        return findAvailableDefaultPath()?.let {
            it to true
        } ?: (path to false)
    }

    private suspend fun findAvailableDefaultPath(): String? {
        for (defaultPath in SettingsRepository.DEFAULT_PATHS) {
            if (appConfigRepository.checkFileExists(defaultPath)) {
                return defaultPath
            }
        }
        return null
    }

    private fun refreshInstalledPackages() {
        viewModelScope.launch(Dispatchers.IO) {
            _packageLoadState.value = PackageLoadState.Loading
            try {
                val packages = installedAppRepository.getInstalledPackageNames()
                _installedPackages.value = packages
                _packageLoadState.value = PackageLoadState.Loaded
            } catch (e: Exception) {
                _installedPackages.value = emptySet()
                _packageLoadState.value = PackageLoadState.Failed
            }
        }
    }

    private suspend fun getInstalledPackagesSnapshot(): Set<String> {
        val current = _installedPackages.value
        when (_packageLoadState.value) {
            PackageLoadState.Loaded -> return current
            PackageLoadState.Failed -> return emptySet()
            PackageLoadState.Loading -> Unit
        }

        return try {
            val loaded = installedAppRepository.getInstalledPackageNames()
            _installedPackages.value = loaded
            _packageLoadState.value = PackageLoadState.Loaded
            loaded
        } catch (e: Exception) {
            _installedPackages.value = emptySet()
            _packageLoadState.value = PackageLoadState.Failed
            emptySet()
        }
    }

    fun getDisplayName(app: AppConfig): String {
        return app.alias?.takeIf { it.isNotBlank() }
            ?: installedAppRepository.getAppName(app.packageName)
            ?: app.packageName
    }

    fun saveConfig() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                appConfigRepository.saveConfig(currentEffectivePath, _uiState.value)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun retryLoadPackages() {
        refreshInstalledPackages()
    }

    fun refreshConfig() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshInstalledPackages()
                val request = buildLoadRequest()
                loadConfigInternal(request.requestedPath, tryDefaultPaths = request.tryDefaultPaths)
                if (_configState.value == ConfigState.Loaded) {
                    cacheIconsForInstalledApps(_uiState.value)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addApp(app: InstalledApp) {
        val newApp = AppConfig(
            id = ConfigParser.generateStableId(app.packageName),
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
        _configState.value = ConfigState.Loaded
        saveConfig()
    }

    fun updateAppRules(appConfig: AppConfig, newRules: List<Rule>) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.packageName == appConfig.packageName) {
                    app.copy(rules = newRules.toImmutableList())
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

    fun toggleAppEnabled(id: Int, enabled: Boolean) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.id == id) {
                    app.copy(
                        enabled = enabled,
                        rules = app.rules.map { it.copy(enabled = enabled) }.toImmutableList()
                    )
                } else app
            }.toImmutableList()
        }
        saveConfig()
    }

    fun updateAllConfigs(newConfigs: List<AppConfig>) {
        _uiState.value = newConfigs.toImmutableList()
        _configState.value = if (newConfigs.isEmpty()) ConfigState.Empty else ConfigState.Loaded
    }

    fun toggleRuleEnabled(appId: Int, ruleIndex: Int, enabled: Boolean) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.id == appId) {
                    val updatedRules = app.rules.mapIndexed { index, rule ->
                        if (index == ruleIndex) rule.copy(enabled = enabled) else rule
                    }.toImmutableList()
                    app.copy(rules = updatedRules)
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun deleteSelectedApps(idsToDelete: Set<Int>) {
        if (idsToDelete.isEmpty()) return

        _uiState.update { currentList ->
            currentList.filter { it.id !in idsToDelete }.toImmutableList()
        }

        updateConfigStateAfterDelete()
        saveConfig()
    }

    fun updateAppName(appId: Int, newName: String) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.id == appId) app.copy(alias = newName) else app
            }.toImmutableList()
        }
        saveConfig()
    }

    fun updateRule(appId: Int, ruleIndex: Int, newRule: Rule) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.id == appId) {
                    val updatedRules = app.rules.mapIndexed { index, rule ->
                        if (index == ruleIndex) newRule else rule
                    }.toImmutableList()
                    app.copy(rules = updatedRules)
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun deleteRule(appId: Int, ruleIndex: Int) {
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.id == appId) {
                    if (ruleIndex !in app.rules.indices) return@map app
                    val updatedRules = app.rules.toMutableList().apply { removeAt(ruleIndex) }.toImmutableList()
                    app.copy(rules = updatedRules)
                } else {
                    app
                }
            }.toImmutableList()
        }
        saveConfig()
    }

    fun deleteApp(appId: Int) {
        _uiState.update { currentList ->
            currentList.filter { it.id != appId }.toImmutableList()
        }

        updateConfigStateAfterDelete()
        saveConfig()
    }

    fun setRulesEnabledForSelectedApps(ids: Set<Int>, enabled: Boolean) {
        if (ids.isEmpty()) return

        var anyChange = false
        _uiState.update { currentList ->
            currentList.map { app ->
                if (app.id !in ids) return@map app
                val unchanged =
                    app.enabled == enabled && app.rules.all { it.enabled == enabled }
                if (unchanged) return@map app
                anyChange = true
                app.copy(
                    enabled = enabled,
                    rules = app.rules.map { it.copy(enabled = enabled) }.toImmutableList()
                )
            }.toImmutableList()
        }
        if (anyChange) {
            saveConfig()
        }
    }

    private fun updateConfigStateAfterDelete() {
        if (_uiState.value.isEmpty()) {
            _configState.value = ConfigState.Empty
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

    private suspend fun cacheIconsForInstalledApps(allApps: List<AppConfig>) {
        val installedPackages = getInstalledPackagesSnapshot()
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

    companion object {
        fun provideFactory(
            appConfigRepository: AppConfigRepository,
            settingsRepository: SettingsRepository,
            installedAppRepository: InstalledAppRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppOptViewModel(appConfigRepository, settingsRepository, installedAppRepository) as T
            }
        }
    }
}

private data class FilterResolutionSnapshot(
    val apps: ImmutableList<AppConfig>,
    val query: String,
    val installedPackages: Set<String>,
    val packageLoadState: PackageLoadState,
    val configState: ConfigState
)
