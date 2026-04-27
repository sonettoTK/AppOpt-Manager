package com.keran.appoptmanager.utils

import net.sourceforge.pinyin4j.PinyinHelper
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType

object AppSorter {

    private val pinyinFormat = HanyuPinyinOutputFormat().apply {
        caseType = HanyuPinyinCaseType.UPPERCASE
        toneType = HanyuPinyinToneType.WITHOUT_TONE
        vCharType = HanyuPinyinVCharType.WITH_V
    }

    private const val DEFAULT_INITIAL = "#"
    private const val CHINESE_RANGE_START = 0x4e00
    private const val CHINESE_RANGE_END = 0x9fa5

    private val pinyinCache = mutableMapOf<String, String>()
    private val initialCache = mutableMapOf<String, String>()
    private val appInitialCache = mutableMapOf<String, String>()

    private enum class CharType(val priority: Int) {
        DIGIT(0),
        LETTER_OR_CHINESE(1),
        OTHER(2)
    }

    fun <T> sort(list: List<T>, nameSelector: (T) -> String): List<T> =
        list.sortedWith { o1, o2 -> compareNames(nameSelector(o1), nameSelector(o2)) }

    fun getPinyinInitial(name: String): String {
        return initialCache.getOrPut(name) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@getOrPut DEFAULT_INITIAL

            val first = trimmed[0]
            when {
                first.isDigit() -> DEFAULT_INITIAL
                first.isLetter() -> first.uppercaseChar().toString()
                isChineseChar(first) -> extractChineseInitial(first)
                else -> DEFAULT_INITIAL
            }
        }
    }

    fun getAppInitial(name: String): String {
        return appInitialCache.getOrPut(name) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@getOrPut DEFAULT_INITIAL

            val first = trimmed[0]
            when {
                first.isDigit() -> DEFAULT_INITIAL
                first.isLetter() -> first.uppercaseChar().toString()
                isChineseChar(first) -> extractChineseInitial(first)
                else -> DEFAULT_INITIAL
            }
        }
    }

    private fun isChineseChar(c: Char): Boolean =
        c.code in CHINESE_RANGE_START..CHINESE_RANGE_END

    private fun extractChineseInitial(char: Char): String {
        val pinyin = convertToPinyin(char.toString())
        val initial = pinyin.firstOrNull()?.uppercaseChar()
        return if (initial?.isLetter() == true) initial.toString() else DEFAULT_INITIAL
    }

    private fun compareNames(name1: String, name2: String): Int {
        val str1 = name1.trim()
        val str2 = name2.trim()

        return when {
            str1.isEmpty() && str2.isEmpty() -> 0
            str1.isEmpty() -> 1
            str2.isEmpty() -> -1
            else -> compareNonEmpty(str1, str2)
        }
    }

    private fun compareNonEmpty(str1: String, str2: String): Int {
        val type1 = getCharType(str1[0])
        val type2 = getCharType(str2[0])

        return when {
            type1 != type2 -> type1.priority - type2.priority
            type1 == CharType.LETTER_OR_CHINESE ->
                comparePinyin(str1, str2)
            else -> str1.compareTo(str2, ignoreCase = true)
        }
    }

    private fun comparePinyin(str1: String, str2: String): Int {
        val pinyin1 = convertToPinyin(str1)
        val pinyin2 = convertToPinyin(str2)
        return pinyin1.compareTo(pinyin2, ignoreCase = true)
    }

    private fun getCharType(c: Char): CharType = when {
        c.isDigit() -> CharType.DIGIT
        c.isLetter() || c.code in CHINESE_RANGE_START..CHINESE_RANGE_END -> CharType.LETTER_OR_CHINESE
        else -> CharType.OTHER
    }

    private fun convertToPinyin(str: String): String {
        return pinyinCache.getOrPut(str) {
            buildString {
                for (char in str) {
                    append(convertCharToPinyin(char))
                }
            }
        }
    }

    private fun convertCharToPinyin(char: Char): String {
        if (char.code !in CHINESE_RANGE_START..CHINESE_RANGE_END) return char.toString()

        return runCatching {
            PinyinHelper.toHanyuPinyinStringArray(char, pinyinFormat)
                ?.firstOrNull()
                ?: char.toString()
        }.getOrDefault(char.toString())
    }
}
