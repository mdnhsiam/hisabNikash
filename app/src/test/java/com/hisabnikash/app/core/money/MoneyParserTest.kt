package com.hisabnikash.app.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyParserTest {
    @Test
    fun `parses English and Bangla digits into poisha`() {
        assertEquals(MoneyParseResult.Success(123_456), MoneyParser.parseToMinorUnits("1,234.56"))
        assertEquals(MoneyParseResult.Success(123_456), MoneyParser.parseToMinorUnits("১,২৩৪.৫৬"))
        assertEquals(MoneyParseResult.Success(10_000_050), MoneyParser.parseToMinorUnits("১,০০,০০০.৫০"))
        assertEquals(MoneyParseResult.Success(1_200), MoneyParser.parseToMinorUnits("12"))
        assertEquals(MoneyParseResult.Success(1_250), MoneyParser.parseToMinorUnits("12.5"))
    }

    @Test
    fun `accepts the documented maximum amount`() {
        assertEquals(
            MoneyParseResult.Success(99_999_999_999_999),
            MoneyParser.parseToMinorUnits("999,999,999,999.99"),
        )
    }

    @Test
    fun `rejects empty zero negative excess precision and overflow`() {
        val invalidInputs = listOf(
            "",
            "0",
            "0.00",
            "-1",
            "12.345",
            "1,000,000,000,000.00",
            "not money",
        )

        invalidInputs.forEach { input ->
            assertTrue("Expected '$input' to be rejected", MoneyParser.parseToMinorUnits(input) is MoneyParseResult.Error)
        }
    }
}
