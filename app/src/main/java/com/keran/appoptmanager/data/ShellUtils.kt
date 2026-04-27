package com.keran.appoptmanager.data

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object ShellUtils {

    fun checkRootPermission(): Boolean {
        return try {
            val result = execSingleRootCmd("id")
            result.output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun execRootCmd(cmd: String, timeoutMs: Long? = null): ShellResult = execSingleRootCmd(cmd, timeoutMs)

    fun execRootCmds(vararg cmds: String, timeoutMs: Long? = null): ShellResult =
        execSingleRootCmd(cmds.joinToString("; ") { it }, timeoutMs)

    private fun execSingleRootCmd(cmd: String, timeoutMs: Long? = null): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val outputBuffer = StringBuilder()
            val errorBuffer = StringBuilder()

            val outputThread = Thread {
                BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        outputBuffer.appendLine(line)
                    }
                }
            }.apply { start() }

            val errorThread = Thread {
                BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        errorBuffer.appendLine(line)
                    }
                }
            }.apply { start() }

            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("$cmd\n")
                os.writeBytes("exit\n")
                os.flush()
            }

            val finished = timeoutMs?.let { process.waitFor(it, TimeUnit.MILLISECONDS) } ?: run {
                process.waitFor()
                true
            }
            if (!finished) {
                process.destroy()
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                outputThread.join(200)
                errorThread.join(200)
                return ShellResult(
                    false,
                    outputBuffer.toString().trim(),
                    "Command timed out after ${timeoutMs}ms"
                )
            }

            outputThread.join(200)
            errorThread.join(200)

            val output = outputBuffer.toString()
            val error = errorBuffer.toString()
            val success = process.exitValue() == 0

            ShellResult(success, output.trim(), error.trim()).also {
                process.destroy()
            }
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "Unknown error")
        }
    }

    data class ShellResult(
        val success: Boolean,
        val output: String,
        val error: String
    )
}
