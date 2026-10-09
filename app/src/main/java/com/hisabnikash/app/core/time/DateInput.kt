package com.hisabnikash.app.core.time

import java.time.LocalDate

object DateInput {
    fun parse(value: String): LocalDate? = runCatching {
        val normalized = value.trim().map { char ->
            val digit = Character.digit(char, 10)
            if (digit in 0..9) ('0'.code + digit).toChar() else char
        }.joinToString("")
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(normalized))
        LocalDate.parse(normalized).also { require(it.year in 1..9999) }
    }.getOrNull()
}
