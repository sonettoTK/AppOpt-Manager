package com.keran.appoptmanager.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val CONFIG_PATH_KEY = stringPreferencesKey("config_path")
        val AUTO_SAVE_KEY = booleanPreferencesKey("auto_save")
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val BACKGROUND_IMAGE_PATH_KEY = stringPreferencesKey("background_image_path")
        val UI_OPACITY_KEY = floatPreferencesKey("ui_opacity")
        val TOOLBAR_ORDER_KEY = stringPreferencesKey("toolbar_order")

        const val DEFAULT_PATH = "/data/adb/modules/AppOpt/applist.conf"
        const val SECONDARY_DEFAULT_PATH = "/data/adb/modules/AppOpt_Aloazny/applist.prop"
        const val DEFAULT_AUTO_SAVE = true
        const val DEFAULT_THEME_MODE = "system"
        const val DEFAULT_BACKGROUND_IMAGE_PATH = ""
        const val DEFAULT_UI_OPACITY = 1.0f
        const val DEFAULT_TOOLBAR_ORDER = "refresh,save,import,share,search,settings"

        val DEFAULT_PATHS = listOf(DEFAULT_PATH, SECONDARY_DEFAULT_PATH)
    }

    val configPath: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CONFIG_PATH_KEY]
        }

    suspend fun getSavedConfigPath(): String? {
        val savedPath = context.dataStore.data.map { it[CONFIG_PATH_KEY] }.first()
        return savedPath?.trim()?.ifEmpty { null }
    }

    val autoSave: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[AUTO_SAVE_KEY] ?: DEFAULT_AUTO_SAVE
        }
        
    val themeMode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
        }
        
    val backgroundImagePath: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[BACKGROUND_IMAGE_PATH_KEY] ?: DEFAULT_BACKGROUND_IMAGE_PATH
        }
        
    val uiOpacity: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[UI_OPACITY_KEY] ?: DEFAULT_UI_OPACITY
        }

    val toolbarOrder: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            (preferences[TOOLBAR_ORDER_KEY] ?: DEFAULT_TOOLBAR_ORDER).split(",")
        }

    suspend fun setConfigPath(path: String?) {
        context.dataStore.edit { preferences ->
            val normalized = path?.trim()?.ifEmpty { null }
            if (normalized != null) {
                preferences[CONFIG_PATH_KEY] = normalized
            } else {
                preferences.remove(CONFIG_PATH_KEY)
            }
        }
    }

    suspend fun setAutoSave(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SAVE_KEY] = enabled
        }
    }
    
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
    
    suspend fun setBackgroundImagePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[BACKGROUND_IMAGE_PATH_KEY] = path
        }
    }
    
    suspend fun setUiOpacity(opacity: Float) {
        context.dataStore.edit { preferences ->
            preferences[UI_OPACITY_KEY] = opacity
        }
    }

    suspend fun setToolbarOrder(order: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[TOOLBAR_ORDER_KEY] = order.joinToString(",")
        }
    }
}
