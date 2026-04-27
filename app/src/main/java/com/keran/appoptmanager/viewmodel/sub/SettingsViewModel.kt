package com.keran.appoptmanager.viewmodel.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.keran.appoptmanager.data.SettingsRepository
import com.keran.appoptmanager.model.ToolbarItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _configPath = MutableStateFlow<String?>(null)
    val configPath: StateFlow<String?> = _configPath.asStateFlow()

    private val _autoSave = MutableStateFlow(SettingsRepository.DEFAULT_AUTO_SAVE)
    val autoSave: StateFlow<Boolean> = _autoSave.asStateFlow()

    private val _themeMode = MutableStateFlow(SettingsRepository.DEFAULT_THEME_MODE)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _backgroundImagePath = MutableStateFlow(SettingsRepository.DEFAULT_BACKGROUND_IMAGE_PATH)
    val backgroundImagePath: StateFlow<String> = _backgroundImagePath.asStateFlow()

    private val _uiOpacity = MutableStateFlow(SettingsRepository.DEFAULT_UI_OPACITY)
    val uiOpacity: StateFlow<Float> = _uiOpacity.asStateFlow()

    private val _toolbarOrder = MutableStateFlow<List<ToolbarItem>>(ToolbarItem.DEFAULT_ORDER)
    val toolbarOrder: StateFlow<List<ToolbarItem>> = _toolbarOrder.asStateFlow()

    init {
        collectSettings()
    }

    private fun collectSettings() {
        viewModelScope.launch {
            settingsRepository.configPath.collect { path ->
                _configPath.value = path
            }
        }
        viewModelScope.launch { settingsRepository.autoSave.collect { _autoSave.value = it } }
        viewModelScope.launch { settingsRepository.themeMode.collect { _themeMode.value = it } }
        viewModelScope.launch { settingsRepository.backgroundImagePath.collect { _backgroundImagePath.value = it } }
        viewModelScope.launch { settingsRepository.uiOpacity.collect { _uiOpacity.value = it } }
        viewModelScope.launch { collectToolbarOrder() }
    }

    private suspend fun collectToolbarOrder() {
        settingsRepository.toolbarOrder.collect { orderIds ->
            val items = orderIds.mapNotNull { ToolbarItem.fromId(it) }
            if (items.isNotEmpty()) {
                _toolbarOrder.value = items
            }
        }
    }

    fun setConfigPath(path: String?) {
        viewModelScope.launch {
            settingsRepository.setConfigPath(path)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoSave(enabled)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setBackgroundImagePath(path: String) {
        viewModelScope.launch {
            settingsRepository.setBackgroundImagePath(path)
        }
    }

    fun setUiOpacity(opacity: Float) {
        viewModelScope.launch {
            settingsRepository.setUiOpacity(opacity)
        }
    }

    fun updateToolbarOrder(newOrder: List<ToolbarItem>) {
        _toolbarOrder.value = newOrder
    }

    fun saveToolbarOrder() {
        viewModelScope.launch {
            settingsRepository.setToolbarOrder(_toolbarOrder.value.map { it.id })
        }
    }

    companion object {
        fun provideFactory(
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(settingsRepository) as T
            }
        }
    }
}
