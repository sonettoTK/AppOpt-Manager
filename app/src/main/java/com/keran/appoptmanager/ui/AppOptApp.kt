package com.keran.appoptmanager.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keran.appoptmanager.data.InstalledApp
import com.keran.appoptmanager.data.InstalledAppRepository
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.ui.components.AppLoadingIndicator
import com.keran.appoptmanager.ui.navigation.AppNavigation
import com.keran.appoptmanager.ui.screens.RootPermissionDeniedScreen
import com.keran.appoptmanager.viewmodel.AppOptViewModel
import com.keran.appoptmanager.viewmodel.StartupStage
import com.keran.appoptmanager.viewmodel.sub.ImportViewModel
import com.keran.appoptmanager.viewmodel.sub.SelectionViewModel
import com.keran.appoptmanager.viewmodel.sub.SettingsViewModel
import com.keran.appoptmanager.viewmodel.sub.UpdateViewModel
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

enum class Screen { List, Settings, AppPicker, RuleConfig, ImportConfig, ToolbarSort }

data class RuleConfigContext(
    val app: InstalledApp,
    val initialRules: PersistentList<Rule> = persistentListOf()
)

@Composable
fun AppOptApp(
    viewModel: AppOptViewModel,
    importViewModel: ImportViewModel,
    updateViewModel: UpdateViewModel,
    settingsViewModel: SettingsViewModel,
    selectionViewModel: SelectionViewModel,
    installedAppRepository: InstalledAppRepository
) {
    val stage by viewModel.startupStage.collectAsStateWithLifecycle()

    when (stage) {
        StartupStage.CheckingRoot,
        StartupStage.LoadingConfig -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AppLoadingIndicator()
            }
        }

        is StartupStage.RootDenied -> {
            RootPermissionDeniedScreen(
                onRetry = viewModel::retryStartup
            )
        }

        is StartupStage.Ready -> {
            AppNavigation(
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
