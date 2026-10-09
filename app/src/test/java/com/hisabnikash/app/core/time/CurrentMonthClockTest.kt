package com.hisabnikash.app.core.time

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class CurrentMonthClockTest {
    @Test fun `resume refresh uses Dhaka boundary and Bengali dates normalize`() {
        var now = Instant.parse("2025-01-31T17:59:59Z")
        val clock = CurrentMonthClock { now }
        assertEquals(MonthKey(2025, 1), clock.month.value)
        now = Instant.parse("2025-01-31T18:00:00Z")
        clock.refresh()
        assertEquals(MonthKey(2025, 2), clock.month.value)
        assertEquals(java.time.LocalDate.of(2025, 2, 1), DateInput.parse("২০২৫-০২-০১"))
        assertNull(DateInput.parse("2025-99-99"))
    }

    @Test fun `date input rejects years outside the displayed four digit format`() {
        assertNull(DateInput.parse("+999999999-12-31"))
        assertNull(DateInput.parse("0000-01-01"))
    }
}
