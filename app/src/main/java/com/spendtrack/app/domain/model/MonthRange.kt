package com.spendtrack.app.domain.model

import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId

/** A calendar month in [zone] as the half-open epoch-millis range [startMillis, endMillis). */
data class MonthRange(val month: YearMonth, val zone: ZoneId) {

    val startMillis: Long = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis: Long = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

    operator fun contains(epochMillis: Long): Boolean = epochMillis in startMillis until endMillis

    fun previous() = MonthRange(month.minusMonths(1), zone)
    fun next() = MonthRange(month.plusMonths(1), zone)

    companion object {
        fun current(clock: Clock = Clock.systemDefaultZone()) = MonthRange(YearMonth.now(clock), clock.zone)
    }
}
