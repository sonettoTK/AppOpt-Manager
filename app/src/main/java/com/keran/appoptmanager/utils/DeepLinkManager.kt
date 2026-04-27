package com.keran.appoptmanager.utils

import android.net.Uri
import android.util.Base64
import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object DeepLinkManager {
    private const val SCHEME = "appopt"
    private const val HOST = "share"
    private const val PATH = "/import"
    private const val PARAM_DATA = "data"

    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    @Serializable
    private data class AppConfigDto(
        val id: Int,
        val packageName: String,
        val enabled: Boolean,
        val alias: String? = null,
        val rules: List<RuleDto>
    )

    @Serializable
    private data class RuleDto(
        val target: String,
        val type: RuleType,
        val cores: String,
        val enabled: Boolean
    )

    fun createShareLink(configs: List<AppConfig>): String {
        val dtos = configs.map { it.toDto() }
        val jsonString = json.encodeToString(dtos)
        val compressed = compress(jsonString)
        val base64 = Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP)
        
        return "$SCHEME://$HOST$PATH?$PARAM_DATA=$base64"
    }

    fun parseDeepLink(uri: Uri): List<AppConfig>? {
        if (uri.scheme != SCHEME || uri.host != HOST || uri.path != PATH) return null
        val data = uri.getQueryParameter(PARAM_DATA) ?: return null
        return parseBase64Data(data)
    }

    fun parseShareCode(text: String): List<AppConfig>? {
        return runCatching {
            parseBase64Data(text.trim())
        }.getOrNull()
    }

    fun parseBase64Data(base64Data: String): List<AppConfig>? {
        val cleanData = if (base64Data.startsWith("$SCHEME://")) {
            Uri.parse(base64Data).getQueryParameter(PARAM_DATA) ?: return null
        } else {
            base64Data
        }

        return runCatching {
            val compressed = Base64.decode(cleanData, Base64.URL_SAFE or Base64.NO_WRAP)
            val jsonString = decompress(compressed)
            json.decodeFromString<List<AppConfigDto>>(jsonString).map { it.toDomain() }
        }.recoverCatching {
            val compressed = Base64.decode(cleanData, Base64.URL_SAFE or Base64.NO_WRAP)
            val jsonString = decompress(compressed)
            val single = json.decodeFromString<AppConfigDto>(jsonString)
            listOf(single.toDomain())
        }.getOrNull()
    }

    private fun compress(string: String): ByteArray {
        val os = ByteArrayOutputStream(string.length)
        GZIPOutputStream(os).use { it.write(string.toByteArray()) }
        return os.toByteArray()
    }

    private fun decompress(compressed: ByteArray): String {
        val bis = ByteArrayInputStream(compressed)
        GZIPInputStream(bis).use { return it.reader().readText() }
    }

    private fun AppConfig.toDto(): AppConfigDto {
        return AppConfigDto(
            id = id,
            packageName = packageName,
            enabled = enabled,
            alias = alias,
            rules = rules.map { it.toDto() }
        )
    }

    private fun Rule.toDto(): RuleDto {
        return RuleDto(
            target = target,
            type = type,
            cores = cores,
            enabled = enabled
        )
    }

    private fun AppConfigDto.toDomain(): AppConfig {
        return AppConfig(
            id = id,
            packageName = packageName,
            enabled = enabled,
            rules = rules.map { it.toDomain() }.toImmutableList(),
            alias = alias
        )
    }

    private fun RuleDto.toDomain(): Rule {
        return Rule(
            target = target,
            type = type,
            cores = cores,
            enabled = enabled
        )
    }
}
