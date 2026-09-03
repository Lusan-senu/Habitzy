package com.htj.habitzy.data.backup

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class CsvParserTest {

    @Test
    fun splitLine_splitsByComma() {
        assertThat(CsvParser.splitLine("Run,2024-01-01,1")).containsExactly("Run", "2024-01-01", "1")
    }

    @Test
    fun splitLine_handlesQuotedCommas() {
        assertThat(CsvParser.splitLine("\"Run, fast\",2024-01-01,1"))
            .containsExactly("Run, fast", "2024-01-01", "1")
    }

    @Test
    fun splitLine_trimsWhitespaceAndOuterQuotes() {
        assertThat(CsvParser.splitLine("  Run  , 2024-01-01 ")).containsExactly("Run", "2024-01-01")
    }

    @Test
    fun splitLine_handlesEmptyFields() {
        assertThat(CsvParser.splitLine("a,,b")).containsExactly("a", "", "b")
    }

    @Test
    fun parseDate_acceptsIso() {
        assertThat(CsvParser.parseDate("2024-01-02")).isEqualTo(LocalDate.of(2024, 1, 2))
    }

    @Test
    fun parseDate_acceptsSlashFormats() {
        assertThat(CsvParser.parseDate("2024/01/02")).isEqualTo(LocalDate.of(2024, 1, 2))
        assertThat(CsvParser.parseDate("01/02/2024")).isEqualTo(LocalDate.of(2024, 1, 2))
        assertThat(CsvParser.parseDate("02/01/2024")).isEqualTo(LocalDate.of(2024, 2, 1))
    }

    @Test
    fun parseDate_rejectsGarbage() {
        assertThat(CsvParser.parseDate("not-a-date")).isNull()
        assertThat(CsvParser.parseDate("")).isNull()
    }

    @Test
    fun parseBool_variants() {
        assertThat(CsvParser.parseBool("1")).isTrue()
        assertThat(CsvParser.parseBool("true")).isTrue()
        assertThat(CsvParser.parseBool("Done")).isTrue()
        assertThat(CsvParser.parseBool("0")).isFalse()
        assertThat(CsvParser.parseBool("no")).isFalse()
        assertThat(CsvParser.parseBool("")).isFalse()
        assertThat(CsvParser.parseBool(null)).isNull()
        assertThat(CsvParser.parseBool("maybe")).isNull()
    }
}
