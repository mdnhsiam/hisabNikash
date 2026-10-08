package com.hisabnikash.app.core.time

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MonthKeyTest {
    @Test
    fun `storage value always uses ASCII digits regardless of display locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("bn-BD"))
            assertEquals("2025-02", MonthKey(2025, 2).value)
        } finally {
            Locale.setDefault(previous)
        }
    }
    @Test
    fun `Dhaka midnight assigns UTC instants to the correct month`() {
        val dhaka = ZoneId.of("Asia/Dhaka")

        assertEquals(MonthKey(2025, 1), MonthKey.fromInstant(Instant.parse("2025-01-31T17:59:59Z"), dhaka))
        assertEquals(MonthKey(2025, 2), MonthKey.fromInstant(Instant.parse("2025-01-31T18:00:00Z"), dhaka))
    }

    @Test
    fun `previous crosses year boundary`() {
        assertEquals(MonthKey(2024, 12), MonthKey(2025, 1).previous())
        assertEquals("2025-09", MonthKey(2025, 9).value)
    }
}
