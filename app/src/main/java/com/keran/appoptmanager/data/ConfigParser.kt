package com.keran.appoptmanager.data

import com.keran.appoptmanager.model.AppConfig
import com.keran.appoptmanager.model.Rule
import com.keran.appoptmanager.model.RuleType
import kotlinx.collections.immutable.toPersistentList
import java.util.regex.Pattern

object ConfigParser {

    val CONFIG_REGEX = Pattern.compile("^([^={}:\\s]+)(?::([^={}\\s]+))?(?:\\{([^}]+)\\})?=([0-9,-]+)$")
    private val VALID_EXTENSIONS = setOf("conf", "txt", "prop", "cfg", "config")
    private const val DISABLED_PREFIX = "!"
    private const val COMMENT_PREFIX = "#"
    private const val ALIAS_DIRECTIVE_PREFIX = "#@alias:"
    private const val MAIN_PROCESS_LABEL = "主进程"
    private const val TARGET_SEPARATOR = ":"
    private const val MIN_FORMATS_FOR_VALIDATION = 2

    enum class ConfigFormat {
        MAIN,
        SUB,
        THREAD,
        SUB_THREAD
    }

    fun hasValidExtension(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in VALID_EXTENSIONS
    }

    fun hasValidConfigLine(content: String): Boolean {
        return detectConfigFormats(content).size >= MIN_FORMATS_FOR_VALIDATION
    }

    fun detectConfigFormats(content: String): Set<ConfigFormat> {
        val formats = mutableSetOf<ConfigFormat>()

        for (line in content.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith(COMMENT_PREFIX)) continue

            val parseLine = if (trimmed.startsWith(DISABLED_PREFIX)) {
                trimmed.substring(1).trim()
            } else trimmed

            val matcher = CONFIG_REGEX.matcher(parseLine)
            if (matcher.find()) {
                val format = determineFormat(
                    hasSubProcess = matcher.group(2) != null,
                    hasThread = matcher.group(3) != null
                )
                formats.add(format)

                if (formats.size >= MIN_FORMATS_FOR_VALIDATION) break
            }
        }
        return formats
    }

    private fun determineFormat(hasSubProcess: Boolean, hasThread: Boolean): ConfigFormat = when {
        hasSubProcess && hasThread -> ConfigFormat.SUB_THREAD
        hasSubProcess -> ConfigFormat.SUB
        hasThread -> ConfigFormat.THREAD
        else -> ConfigFormat.MAIN
    }

    fun detectFormat(line: String): ConfigFormat? {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith(COMMENT_PREFIX)) return null

        val parseLine = if (trimmed.startsWith(DISABLED_PREFIX)) {
            trimmed.substring(1).trim()
        } else trimmed

        val matcher = CONFIG_REGEX.matcher(parseLine)
        if (!matcher.find()) return null

        return determineFormat(
            hasSubProcess = matcher.group(2) != null,
            hasThread = matcher.group(3) != null
        )
    }

    fun parse(content: String): List<AppConfig> {
        val appRules = LinkedHashMap<String, MutableList<Rule>>()
        val aliases = LinkedHashMap<String, String>()
        var pendingAlias: String? = null

        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                if (line.startsWith(ALIAS_DIRECTIVE_PREFIX)) {
                    val (pkg, alias) = parseAliasDirective(line) ?: return@forEach
                    if (pkg != null) {
                        aliases[pkg] = alias
                    } else {
                        pendingAlias = alias
                    }
                    return@forEach
                }
                if (line.startsWith(COMMENT_PREFIX)) {
                    pendingAlias = null
                    return@forEach
                }

                val ruleInfo = processRuleLine(line, appRules) ?: run {
                    pendingAlias = null
                    return@forEach
                }
                pendingAlias?.let { alias ->
                    aliases.putIfAbsent(ruleInfo.packageName, alias)
                    pendingAlias = null
                }
            }

        return buildAppConfigs(appRules, aliases)
    }

    private fun parseAliasDirective(line: String): Pair<String?, String>? {
        val payload = line.removePrefix(ALIAS_DIRECTIVE_PREFIX).trim()
        if (payload.isEmpty()) return null

        val separatorIndex = payload.indexOf('=')
        return if (separatorIndex > 0) {
            val pkg = payload.substring(0, separatorIndex).trim()
            val alias = payload.substring(separatorIndex + 1).trim()
            if (pkg.isEmpty() || alias.isEmpty()) null else pkg to alias
        } else {
            null to payload
        }
    }

    private fun processRuleLine(
        line: String,
        appRules: MutableMap<String, MutableList<Rule>>
    ): RuleInfo? {
        val (isEnabled, parseLine) = extractEnabledState(line)
        val ruleInfo = parseRuleLine(parseLine) ?: return null

        appRules.getOrPut(ruleInfo.packageName) { mutableListOf() }
            .add(Rule(ruleInfo.target, ruleInfo.type, ruleInfo.cores, isEnabled))
        return ruleInfo
    }

    private fun buildAppConfigs(
        appRules: Map<String, List<Rule>>,
        aliases: Map<String, String>
    ): List<AppConfig> {
        return appRules.map { (packageName, rules) ->
            AppConfig(
                packageName = packageName,
                enabled = true,
                rules = rules.toPersistentList(),
                alias = aliases[packageName]
            )
        }
    }

    private data class RuleInfo(
        val packageName: String,
        val target: String,
        val type: RuleType,
        val cores: String
    )

    private fun extractEnabledState(line: String): Pair<Boolean, String> {
        return if (line.startsWith(DISABLED_PREFIX)) {
            false to line.substring(1).trim()
        } else {
            true to line
        }
    }

    private fun parseRuleLine(line: String): RuleInfo? {
        val matcher = CONFIG_REGEX.matcher(line)
        if (!matcher.find()) return null

        val packageName = matcher.group(1) ?: return null
        val processName = matcher.group(2)
        val threadName = matcher.group(3)
        val cores = matcher.group(4) ?: return null

        val (target, type) = buildTargetAndType(processName, threadName)
        return RuleInfo(packageName, target, type, cores)
    }

    private fun buildTargetAndType(processName: String?, threadName: String?): Pair<String, RuleType> = when {
        processName != null && threadName != null ->
            "$processName$TARGET_SEPARATOR$threadName" to RuleType.THREAD
        processName != null ->
            processName to RuleType.SUB
        threadName != null ->
            threadName to RuleType.THREAD
        else ->
            MAIN_PROCESS_LABEL to RuleType.MAIN
    }

    fun toConfigFile(apps: List<AppConfig>): String = buildString {
        for (app in apps) {
            val alias = app.alias?.trim()?.ifEmpty { null }
            if (alias != null) {
                appendLine("$COMMENT_PREFIX $alias")
                appendLine("$ALIAS_DIRECTIVE_PREFIX${app.packageName}=$alias")
            }

            for (rule in app.rules) {
                if (!rule.enabled) append(DISABLED_PREFIX)
                append(app.packageName)
                appendRuleSuffix(rule)
                appendLine("=${rule.cores}")
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendRuleSuffix(rule: Rule) {
        when (rule.type) {
            RuleType.MAIN -> {}
            RuleType.SUB -> append(":${rule.target}")
            RuleType.THREAD -> append(buildThreadSuffix(rule.target))
        }
    }

    private fun buildThreadSuffix(target: String): String {
        return if (target.contains(TARGET_SEPARATOR)) {
            val (process, thread) = target.split(TARGET_SEPARATOR, limit = 2)
            ":$process{$thread}"
        } else {
            "{$target}"
        }
    }
}
