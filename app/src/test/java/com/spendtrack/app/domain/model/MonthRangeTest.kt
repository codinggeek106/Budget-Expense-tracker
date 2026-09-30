package com.spendtrack.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class MonthRangeTest {

    private val ist = ZoneId.of("Asia/Kolkata")

    private fun millis(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(ist).toInstant().toEpochMilli()

    @Test
    fun boundsAreLocalMidnightsHalfOpen() {
        val range = MonthRange(YearMonth.of(2026, 9), ist)
        assertEquals(millis(2026, 9, 1), range.startMillis)
        assertEquals(millis(2026, 10, 1), range.endMillis)
        assertTrue(range.startMillis in range)
        assertTrue(range.endMillis - 1 in range)
        assertFalse(range.endMillis in range)
        assertFalse(range.startMillis - 1 in range)
    }

    @Test
    fun handlesYearAndLeapFebruary() {
        assertEquals(YearMonth.of(2025, 12), MonthRange(YearMonth.of(2026, 1), ist).previous().month)
        val feb = MonthRange(YearMonth.of(2028, 2), ist)
        assertEquals(29L * 24 * 60 * 60 * 1000, feb.endMillis - feb.startMillis)
    }

    @Test
    fun currentUsesTheClockZone() {
        // 2026-09-30 20:00 UTC is already 1 Oct in India.
        val clock = Clock.fixed(Instant.parse("2026-09-30T20:00:00Z"), ist)
        assertEquals(YearMonth.of(2026, 10), MonthRange.current(clock).month)
    }
}
