package com.spendtrack.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {

    @Test
    fun wholeRupeesHaveNoDecimals() {
        assertEquals("₹0", Money.format(0))
        assertEquals("₹250", Money.format(25_000))
        assertEquals("₹999", Money.format(99_900))
    }

    @Test
    fun paiseAlwaysShowTwoDigits() {
        assertEquals("₹0.05", Money.format(5))
        assertEquals("₹10.50", Money.format(1_050))
        assertEquals("₹1,250.99", Money.format(125_099))
    }

    @Test
    fun usesIndianGrouping() {
        assertEquals("₹1,000", Money.format(1_000_00))
        assertEquals("₹10,000", Money.format(10_000_00))
        assertEquals("₹1,00,000", Money.format(1_00_000_00))
        assertEquals("₹12,34,567", Money.format(12_34_567_00))
        assertEquals("₹1,23,45,678.90", Money.format(1_23_45_678_90))
    }

    @Test
    fun negativeAmountsKeepTheSignInFront() {
        assertEquals("-₹1,250.50", Money.format(-125_050))
    }
}
