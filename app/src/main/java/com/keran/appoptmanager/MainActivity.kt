package com.keran.appoptmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.keran.appoptmanager.data.AppConfigRepository
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.data.SettingsRepository
import com.keran.appoptmanager.data.UpdateRepository
import com.keran.appoptmanager.ui.AppOptApp
import com.keran.appoptmanager.ui.theme.AppOptManagerTheme
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.sub.ImportViewModel
import com.keran.appoptmanager.viewmodel.sub.SelectionViewModel
import com.keran.appoptmanager.viewmodel.sub.SettingsViewModel
import com.keran.appoptmanager.viewmodel.sub.UpdateViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppOptViewModel
    private lateinit var importViewModel: ImportViewModel
    private lateinit var updateViewModel: UpdateViewModel
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var selectionViewModel: SelectionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val settingsRepository = SettingsRepository(applicationContext)
        val appConfigRepository = AppConfigRepository(applicationContext)
        val installedAppRepository = InstalledAppRepository(applicationContext)
        val updateRepository = UpdateRepository()

        val appOptFactory = AppOptViewModel.provideFactory(
            appConfigRepository, settingsRepository, installedAppRepository
        )
        viewModel = ViewModelProvider(this, appOptFactory)[AppOptViewModel::class.java]

        val importFactory = ImportViewModel.provideFactory(appConfigRepository, installedAppRepository)
        importViewModel = ViewModelProvider(this, importFactory)[ImportViewModel::class.java]

        val updateFactory = UpdateViewModel.provideFactory(updateRepository, appConfigRepository)
        updateViewModel = ViewModelProvider(this, updateFactory)[UpdateViewModel::class.java]

        val settingsFactory = SettingsViewModel.provideFactory(settingsRepository)
        settingsViewModel = ViewModelProvider(this, settingsFactory)[SettingsViewModel::class.java]

        selectionViewModel = ViewModelProvider(
            this, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[SelectionViewModel::class.java]

        lifecycleScope.launch {
            viewModel.startupCompleted.collect {
                updateViewModel.onStartupCompleted()
            }
        }

        handleIntent(intent)

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
