package com.keran.appoptmanager.utils

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.util.zip.ZipInputStream

object ZipUtils {
    private const val MAX_CHARS = 8 * 1024 * 1024
    private const val MAX_LINES = 1000
    private const val BUFFER_SIZE = 8192

    fun isZipFile(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.contains("zip") == true ||
               mimeType?.contains("compressed") == true ||
               uri.toString().endsWith(".zip")
    }

    fun readZipEntryNames(context: Context, uri: Uri): List<String> {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            java.util.zip.ZipInputStream(inputStream).use { zip ->
                buildList {
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory) {
                            add(entry.name)
                        }
                    }
                }
            }
        } ?: emptyList()
    }

    fun readZipEntryFullContent(context: Context, uri: Uri, fileName: String): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                generateSequence { zip.nextEntry }
                    .firstOrNull { it.name == fileName }
                    ?.let { 
                        readZipEntryContent(zip).also { zip.closeEntry() }
                    }
            }
        } ?: ""
    }

    fun readZipEntryContent(zipInputStream: ZipInputStream): String = buildString {
        val reader = zipInputStream.bufferedReader()
        val buffer = CharArray(BUFFER_SIZE)
        var totalRead = 0
        var charsRead: Int

        while (reader.read(buffer).also { charsRead = it } != -1 && totalRead < MAX_CHARS) {
            append(buffer, 0, charsRead)
            totalRead += charsRead
        }
    }


    fun readZipEntryContentWithLimit(
        zipInputStream: ZipInputStream,
        shouldStop: (String) -> Boolean
    ): Pair<String, Boolean> {
        val sb = StringBuilder()
        val reader = zipInputStream.bufferedReader()
        var totalRead = 0
        var lineCount = 0
        var stoppedEarly = false

        while (lineCount < MAX_LINES && totalRead < MAX_CHARS) {
            val line = reader.readLine() ?: break
            sb.appendLine(line)
            totalRead += line.length + 1
            lineCount++

            if (shouldStop(line)) {
                stoppedEarly = true
                break
            }
        }
        
        return sb.toString() to !stoppedEarly
    }
}
