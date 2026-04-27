package com.keran.appoptmanager.data

import android.content.Context
import android.net.Uri
import com.keran.appoptmanager.model.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppConfigRepository(private val context: Context) {

    suspend fun loadConfig(path: String): List<AppConfig> = withIoContext {
        val result = ShellUtils.execRootCmd("cat ${shellQuote(path)}")
        if (result.success) ConfigParser.parse(result.output) else emptyList()
    }

    suspend fun checkFileExists(path: String): Boolean = withIoContext {
        val quoted = shellQuote(path)
        val cmd =
            "if [ -e $quoted ]; then echo 1; " +
                "elif ls $quoted >/dev/null 2>&1; then echo 1; " +
                "else echo 0; fi"
        val result = ShellUtils.execRootCmd(cmd)
        result.output.trim() == "1"
    }

    suspend fun readConfigFromUri(uri: Uri): String = withIoContext {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: ""
        }.getOrElse { "" }
    }

    fun getContext(): Context = context

    suspend fun saveConfig(path: String, configs: List<AppConfig>): Boolean = withIoContext {
        val tempFile = File(context.cacheDir, TEMP_CONFIG_FILENAME)

        runCatching {
            tempFile.writeText(ConfigParser.toConfigFile(configs))
            copyFileWithRootPermission(tempFile.absolutePath, path)
        }.getOrElse { false }
        .also { tempFile.delete() }
    }

    private fun copyFileWithRootPermission(sourcePath: String, destPath: String): Boolean {
        val src = shellQuote(sourcePath)
        val dst = shellQuote(destPath)
        val result = ShellUtils.execRootCmd("cp $src $dst && chmod 644 $dst")
        return result.success
    }

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"

    suspend fun exportConfigToCache(configs: List<AppConfig>): File? = withIoContext {
        runCatching {
            val exportDir = File(context.cacheDir, EXPORT_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            File(exportDir, EXPORT_FILENAME).apply {
                writeText(ConfigParser.toConfigFile(configs))
            }
        }.getOrNull()
    }

    private companion object {
        const val TEMP_CONFIG_FILENAME = "temp_config.conf"
        const val EXPORT_DIR_NAME = "exports"
        const val EXPORT_FILENAME = "applist.conf"
    }
}

private suspend inline fun <T> withIoContext(crossinline block: () -> T): T =
    withContext(Dispatchers.IO) { block() }
