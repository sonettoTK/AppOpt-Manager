package com.keran.appoptmanager.model

import com.keran.appoptmanager.data.InstalledApp

enum class DiffType {
    NEW,        
    MODIFIED,   
    IDENTICAL   
}

data class RuleDiff(
    val target: String,
    val type: RuleType,
    val oldRule: Rule?,
    val newRule: Rule?
) {
    val isNew: Boolean get() = oldRule == null && newRule != null
    val isModified: Boolean get() = oldRule != null && newRule != null && oldRule != newRule
    val isDeleted: Boolean get() = oldRule != null && newRule == null
    val isSame: Boolean get() = oldRule == newRule
}

data class ImportDiff(
    val appConfig: AppConfig,
    val oldConfig: AppConfig?,
    val installedApp: InstalledApp,
    val type: DiffType,
    val ruleDiffs: List<RuleDiff>
)
