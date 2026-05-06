package com.keran.appoptmanager.viewmodel

import com.keran.appoptmanager.data.AppConfigRepository
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.data.SettingsRepository
import com.keran.appoptmanager.data.ShellUtils
import com.keran.appoptmanager.model.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class StartupCoordinator(
    private val appConfigRepository: AppConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val installedAppRepository: InstalledAppRepository
) {
    private val _startupStage = MutableStateFlow<StartupStage>(StartupStage.CheckingRoot)
    val startupStage: StateFlow<StartupStage> = _startupStage.asStateFlow()

    private val _configState = MutableStateFlow(ConfigState.Loading)
    val configState: StateFlow<ConfigState> = _configState.asStateFlow()

    private val _packageLoadState = MutableStateFlow(PackageLoadState.Loading)
    val packageLoadState: StateFlow<PackageLoadState> = _packageLoadState.asStateFlow()

    private val _installedPackages = MutableStateFlow<Set<String>>(emptySet())
    val installedPackages: StateFlow<Set<String>> = _installedPackages.asStateFlow()

    private val _hasResolvedFilteredUiState = MutableStateFlow(false)
    val hasResolvedFilteredUiState: StateFlow<Boolean> = _hasResolvedFilteredUiState.asStateFlow()

    private val _startupCompleted = MutableSharedFlow<Unit>()
    val startupCompleted: SharedFlow<Unit> = _startupCompleted.asSharedFlow()

    var currentEffectivePath: String = SettingsRepository.DEFAULT_PATH
        private set

    fun markConfigLoaded() {
        _configState.value = ConfigState.Loaded
    }

    fun markConfigEmpty() {
        _configState.value = ConfigState.Empty
    }

    suspend fun runStartupPipeline(): List<AppConfig> {
        _startupStage.value = StartupStage.CheckingRoot
        val hasRoot = withContext(Dispatchers.IO) {
            ShellUtils.checkRootPermission()
        }
        if (!hasRoot) {
            _startupStage.value = StartupStage.RootDenied
            return emptyList()
        }

        _startupStage.value = StartupStage.LoadingConfig
        refreshInstalledPackages()
        val request = buildLoadRequest()
        val configs = loadConfigInternal(request.requestedPath, tryDefaultPaths = request.tryDefaultPaths)

        _startupStage.value = StartupStage.Ready
        _startupCompleted.emit(Unit)
        return configs
    }

    suspend fun refreshInstalledPackages() {
        _packageLoadState.value = PackageLoadState.Loading
        try {
            val packages = withContext(Dispatchers.IO) {
                installedAppRepository.getInstalledPackageNames()
            }
            _installedPackages.value = packages
            _packageLoadState.value = PackageLoadState.Loaded
        } catch (e: Exception) {
            _installedPackages.value = emptySet()
            _packageLoadState.value = PackageLoadState.Failed
        }
    }

    suspend fun refreshConfig(): List<AppConfig> {
        refreshInstalledPackages()
        val request = buildLoadRequest()
        return loadConfigInternal(request.requestedPath, tryDefaultPaths = request.tryDefaultPaths)
    }

    suspend fun getInstalledPackagesSnapshot(): Set<String> {
        val current = _installedPackages.value
        when (_packageLoadState.value) {
            PackageLoadState.Loaded -> return current
            PackageLoadState.Failed -> return emptySet()
            PackageLoadState.Loading -> Unit
        }

        return try {
            val loaded = withContext(Dispatchers.IO) {
                installedAppRepository.getInstalledPackageNames()
            }
            _installedPackages.value = loaded
            _packageLoadState.value = PackageLoadState.Loaded
            loaded
        } catch (e: Exception) {
            _installedPackages.value = emptySet()
            _packageLoadState.value = PackageLoadState.Failed
            emptySet()
        }
    }

    fun updateResolutionState() {
        val configState = _configState.value
        val packageLoadState = _packageLoadState.value
        val resolved = packageLoadState == PackageLoadState.Failed ||
            (configState == ConfigState.Loaded && packageLoadState == PackageLoadState.Loaded) ||
            configState == ConfigState.Empty ||
            configState == ConfigState.FileNotFound
        if (resolved) {
            _hasResolvedFilteredUiState.value = true
        }
    }

    private suspend fun buildLoadRequest(): LoadRequest {
        val savedPath = settingsRepository.getSavedConfigPath()
        return LoadRequest(
            requestedPath = savedPath ?: SettingsRepository.DEFAULT_PATH,
            tryDefaultPaths = savedPath == null
        )
    }

    private suspend fun loadConfigInternal(path: String, tryDefaultPaths: Boolean = true): List<AppConfig> {
        _configState.value = ConfigState.Loading

        val (effectivePath, fileExists) = resolveConfigPath(path, tryDefaultPaths)
        currentEffectivePath = effectivePath

        if (!fileExists) {
            _configState.value = ConfigState.FileNotFound
            return emptyList()
        }

        val configApps = withContext(Dispatchers.IO) {
            appConfigRepository.loadConfig(effectivePath)
        }

        if (configApps.isEmpty()) {
            _configState.value = ConfigState.Empty
        } else {
            _configState.value = ConfigState.Loaded
        }
        return configApps
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

    private data class LoadRequest(
        val requestedPath: String,
        val tryDefaultPaths: Boolean
    )
}
