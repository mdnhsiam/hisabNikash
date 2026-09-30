package com.hisabnikash.app.core.money

sealed interface MoneyParseResult {
    data class Success(val minorUnits: Long) : MoneyParseResult
    data class Error(val reason: MoneyParseError) : MoneyParseResult
}

enum class MoneyParseError {
    EMPTY,
    INVALID_FORMAT,
    NOT_POSITIVE,
    TOO_MANY_DECIMALS,
    TOO_LARGE,
}

object MoneyParser {
    const val MAX_MINOR_UNITS: Long = 99_999_999_999_999L

    fun parseToMinorUnits(input: String): MoneyParseResult {
        val normalizedDigits = input.trim().map(::toAsciiDigit).joinToString("")
        if (normalizedDigits.isEmpty()) return MoneyParseResult.Error(MoneyParseError.EMPTY)
        if (normalizedDigits.startsWith('-')) return MoneyParseResult.Error(MoneyParseError.NOT_POSITIVE)

        val parts = normalizedDigits.split('.')
        if (parts.size > 2) return MoneyParseResult.Error(MoneyParseError.INVALID_FORMAT)
        val wholeWithGrouping = parts[0]
        val fraction = parts.getOrElse(1) { "" }
        if (fraction.length > 2) return MoneyParseResult.Error(MoneyParseError.TOO_MANY_DECIMALS)
        if (fraction.any { !it.isDigit() }) return MoneyParseResult.Error(MoneyParseError.INVALID_FORMAT)
        if (!validGrouping(wholeWithGrouping)) return MoneyParseResult.Error(MoneyParseError.INVALID_FORMAT)

        val whole = wholeWithGrouping.replace(",", "")
        if (whole.isEmpty() || whole.any { !it.isDigit() }) return MoneyParseResult.Error(MoneyParseError.INVALID_FORMAT)
        val paddedFraction = fraction.padEnd(2, '0')
        val amount = try {
            Math.addExact(Math.multiplyExact(whole.toLong(), 100L), paddedFraction.ifEmpty { "00" }.toLong())
        } catch (_: ArithmeticException) {
            return MoneyParseResult.Error(MoneyParseError.TOO_LARGE)
        } catch (_: NumberFormatException) {
            return MoneyParseResult.Error(MoneyParseError.TOO_LARGE)
        }

        return when {
            amount <= 0L -> MoneyParseResult.Error(MoneyParseError.NOT_POSITIVE)
            amount > MAX_MINOR_UNITS -> MoneyParseResult.Error(MoneyParseError.TOO_LARGE)
            else -> MoneyParseResult.Success(amount)
        }
    }

    private fun validGrouping(value: String): Boolean {
        if (',' !in value) return value.isNotEmpty() && value.all(Char::isDigit)
        val groups = value.split(',')
        val western = groups.first().length in 1..3 &&
            groups.first().all(Char::isDigit) &&
            groups.drop(1).all { it.length == 3 && it.all(Char::isDigit) }
        val southAsian = groups.size >= 2 &&
            groups.first().length in 1..2 &&
            groups.first().all(Char::isDigit) &&
            groups.last().length == 3 &&
            groups.last().all(Char::isDigit) &&
            groups.drop(1).dropLast(1).all { it.length == 2 && it.all(Char::isDigit) }
        return western || southAsian
    }

    private fun toAsciiDigit(char: Char): Char = when (char) {
        in '০'..'৯' -> ('0'.code + (char.code - '০'.code)).toChar()
        else -> char
    }
}
