package com.keran.appoptmanager.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

data class InstalledApp(
    val name: String,
    val packageName: String,
    val iconPath: String,
    val isSystem: Boolean
)

class InstalledAppRepository(private val context: Context) {
    private companion object {
        const val PACKAGE_LIST_TIMEOUT_MS = 8_000L
    }

    private val iconCacheDir = File(context.cacheDir, "app_icons")
    private val appNameCache = ConcurrentHashMap<String, String>()
    private val missingAppNames = ConcurrentHashMap.newKeySet<String>()

    init {
        if (!iconCacheDir.exists()) {
            iconCacheDir.mkdirs()
        }
    }

    suspend fun getInstalledApps(): List<InstalledApp> {
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledPackages(0)

            val installed = packages.mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null

                val appName = appInfo.loadLabel(pm).toString()
                val packageName = packageInfo.packageName
                appNameCache[packageName] = appName

                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

                InstalledApp(
                    name = appName,
                    packageName = packageName,
                    iconPath = File(iconCacheDir, "$packageName.png").absolutePath,
                    isSystem = (isSystem && !isUpdatedSystem)
                )
            }.sortedBy { it.name }

            preCacheIconsParallel(installed.map { it.packageName })
            installed
        }
    }

    suspend fun getInstalledPackageNames(): Set<String> {
        return withContext(Dispatchers.IO) {
            runPackageQueryWithTimeout(PACKAGE_LIST_TIMEOUT_MS) {
                context.packageManager.getInstalledApplications(0)
                    .asSequence()
                    .map { it.packageName }
                    .toSet()
            }.also { packageNames ->
                if (packageNames.isEmpty()) {
                    throw IllegalStateException("Installed package list is empty")
                }
                if (packageNames.size < 5) {
                    throw IllegalStateException("Installed package list is too small, QUERY_ALL_PACKAGES permission may be denied")
                }
            }
        }
    }

    private fun <T> runPackageQueryWithTimeout(
        timeoutMs: Long,
        query: () -> T
    ): T {
        val task = FutureTask(query)
        val worker = Thread(task, "AppOptManager-PackageQuery").apply {
            isDaemon = true
            start()
        }

        return try {
            task.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            task.cancel(true)
            worker.interrupt()
            throw IllegalStateException("Timed out while loading installed packages", e)
        } catch (e: InterruptedException) {
            task.cancel(true)
            worker.interrupt()
            Thread.currentThread().interrupt()
            throw IllegalStateException("Interrupted while loading installed packages", e)
        } catch (e: ExecutionException) {
            throw IllegalStateException("Failed to load installed packages", e.cause ?: e)
        }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getAppName(packageName: String): String? {
        appNameCache[packageName]?.let { return it }
        if (missingAppNames.contains(packageName)) return null

        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = appInfo.loadLabel(pm).toString()
            appNameCache[packageName] = appName
            appName
        } catch (e: PackageManager.NameNotFoundException) {
            missingAppNames.add(packageName)
            null
        }
    }

    suspend fun ensureIconCached(packageName: String) {
        withContext(Dispatchers.IO) {
            val iconFile = File(iconCacheDir, "$packageName.png")
            if (iconFile.exists()) return@withContext

            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val drawable = appInfo.loadIcon(pm)
                saveIconToCache(drawable, iconFile)
            } catch (e: Exception) {
                // Ignore if app not found or icon load fails
            }
        }
    }

    suspend fun preCacheIcons(packageNames: List<String>, batchSize: Int = 10, onBatchProcessed: (Int) -> Unit = {}) {
        withContext(Dispatchers.IO) {
            packageNames.chunked(batchSize).forEach { batch ->
                batch.map { packageName ->
                    async {
                        ensureIconCachedInternal(packageName)
                        packageName
                    }
                }.awaitAll()
                onBatchProcessed(batch.size)
            }
        }
    }

    private suspend fun ensureIconCachedInternal(packageName: String) {
        val iconFile = File(iconCacheDir, "$packageName.png")
        if (iconFile.exists()) return

        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val drawable = appInfo.loadIcon(pm)
            saveIconToCache(drawable, iconFile)
        } catch (e: Exception) {
            // Ignore if app not found or icon load fails
        }
    }

    suspend fun preCacheIconsParallel(packageNames: List<String>, maxConcurrency: Int = 8): Int {
        if (packageNames.isEmpty()) return 0

        return withContext(Dispatchers.IO) {
            val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrency)
            packageNames.map { packageName ->
                async {
                    semaphore.acquire()
                    try {
                        ensureIconCachedInternal(packageName)
                        1
                    } finally {
                        semaphore.release()
                    }
                }
            }.sumOf { it.await() }
        }
    }


    private fun saveIconToCache(drawable: Drawable, file: File) {
        try {
            val bitmap = if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth.takeIf { it > 0 } ?: 48,
                    drawable.intrinsicHeight.takeIf { it > 0 } ?: 48,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bitmap
            }

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {
        }
    }
}
