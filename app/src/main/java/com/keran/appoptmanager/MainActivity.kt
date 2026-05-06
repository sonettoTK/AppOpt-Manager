package com.keran.appoptmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.ui.AppOptApp
import com.keran.appoptmanager.ui.theme.AppOptManagerTheme
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.sub.ImportViewModel
import com.keran.appoptmanager.viewmodel.sub.SelectionViewModel
import com.keran.appoptmanager.viewmodel.sub.SettingsViewModel
import com.keran.appoptmanager.viewmodel.sub.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var installedAppRepository: InstalledAppRepository

    private val viewModel: AppOptViewModel by viewModels()
    private val importViewModel: ImportViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val selectionViewModel: SelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val uiOpacity by settingsViewModel.uiOpacity.collectAsStateWithLifecycle()
            val backgroundImagePath by settingsViewModel.backgroundImagePath.collectAsStateWithLifecycle()

            AppOptManagerTheme(
                themeMode = themeMode,
                uiOpacity = if (backgroundImagePath.isNotEmpty()) uiOpacity else 1.0f
            ) {
                AppOptApp(
                    viewModel = viewModel,
                    importViewModel = importViewModel,
                    updateViewModel = updateViewModel,
                    settingsViewModel = settingsViewModel,
                    selectionViewModel = selectionViewModel,
                    installedAppRepository = installedAppRepository
                )
            }
        }

        lifecycleScope.launch {
            viewModel.startupCompleted.collect {
                updateViewModel.onStartupCompleted()
            }
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            importViewModel.handleDeepLink(it, viewModel.uiState.value)
        }
    }
}
