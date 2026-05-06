package com.keran.appoptmanager.viewmodel.sub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keran.appoptmanager.data.UpdateRepository
import com.keran.appoptmanager.model.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateInfo?> = _updateInfo.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    fun onStartupCompleted() {
        viewModelScope.launch {
            delay(2000L)
            checkUpdate()
        }
    }

    private fun checkUpdate() {
        viewModelScope.launch {
            updateRepository.checkUpdate()
                .onSuccess { handleUpdateInfo(it) }
                .onFailure { }
        }
    }

    private fun handleUpdateInfo(info: UpdateInfo) {
        val currentVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0.0"

        if (compareVersions(info.versionName, currentVersion) > 0) {
            _updateInfo.value = info
            _showUpdateDialog.value = true
        }
    }

    fun startDownloadAndInstall() {
        val info = _updateInfo.value ?: return
        _isDownloading.value = true
        _downloadProgress.value = 0f

        viewModelScope.launch {
            val downloadDir = context.externalCacheDir ?: context.cacheDir
            val outputFile = File(downloadDir, "AppOptManager_v${info.versionName}.apk")

            updateRepository.downloadApk(
                url = info.downloadUrl,
                outputFile = outputFile,
                onProgress = { progress -> _downloadProgress.value = progress }
            ).onSuccess { file ->
                _isDownloading.value = false
                _showUpdateDialog.value = false
                installApk(file)
            }.onFailure { }
        }
    }

    private fun installApk(file: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        runCatching {
            context.startActivity(intent)
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    private fun compareVersions(newVersion: String, currentVersion: String): Int {
        fun parseVersion(version: String): List<Int> {
            return version.split(".")
                .map { it.toIntOrNull() ?: 0 }
        }

        val newParts = parseVersion(newVersion)
        val currentParts = parseVersion(currentVersion)
        val maxSize = maxOf(newParts.size, currentParts.size)

        for (i in 0 until maxSize) {
            val newPart = newParts.getOrElse(i) { 0 }
            val currentPart = currentParts.getOrElse(i) { 0 }
            if (newPart != currentPart) return newPart.compareTo(currentPart)
        }
        return 0
    }
}
