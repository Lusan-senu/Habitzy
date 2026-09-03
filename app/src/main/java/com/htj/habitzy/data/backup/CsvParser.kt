package com.htj.habitzy.data.backup

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Pure CSV helpers used by the CSV importer, factored out for unit testing. */
internal object CsvParser {

    private val dateFormats = listOf(
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy",
        "M/d/yyyy",
    )

    fun splitLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(sb.toString().trim().trim('"'))
                    sb.clear()
                }
                else -> sb.append(ch)
            }
        }
        result.add(sb.toString().trim().trim('"'))
        return result
    }

    fun parseDate(s: String): LocalDate? {
        for (f in dateFormats) {
            try {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern(f))
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun parseBool(v: String?): Boolean? = when (v?.trim()?.lowercase()) {
        "1", "true", "yes", "y", "done", "x", "complete" -> true
        "0", "false", "no", "n", "" -> false
        else -> null
    }
}
