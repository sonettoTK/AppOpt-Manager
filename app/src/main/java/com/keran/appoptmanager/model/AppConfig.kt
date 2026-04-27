package com.keran.appoptmanager.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * 可应用于应用程序的规则类型。
 * 
 * - MAIN: 应用于主进程
 * - SUB: 应用于子进程
 * - THREAD: 应用于特定线程
 */
enum class RuleType {
    MAIN, SUB, THREAD
}

/**
 * 表示应用程序组件的CPU亲和性规则。
 * 
 * @property target 目标进程或线程名称
 * @property type 规则类型（主进程、子进程或线程）
 * @property cores 分配的CPU核心（例如："0-3"、"1,3,5"）
 * @property enabled 此规则是否激活
 */
@Immutable
data class Rule(
    val target: String,
    val type: RuleType,
    val cores: String,
    val enabled: Boolean = true
)

/**
 * 用于规则比较和映射的复合键。
 * 
 * @property type 规则类型
 * @property target 目标进程或线程名称
 */
data class RuleKey(val type: RuleType, val target: String)

/**
 * 带有CPU亲和性规则的应用程序配置。
 *
 * @property id 配置的唯一标识符
 * @property packageName Android包名
 * @property enabled 应用程序配置是否激活
 * @property rules 此应用程序的CPU亲和性规则列表
 * @property alias 用户通过编辑功能设置的显示名称。null 时使用系统原始名或包名
 */
@Immutable
data class AppConfig(
    val id: Int,
    val packageName: String,
    val enabled: Boolean,
    val rules: ImmutableList<Rule>,
    val alias: String? = null
)
