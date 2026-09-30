package com.spendtrack.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    @Test
    fun parsesCurrencyMarkers() {
        assertEquals(25_000L, AmountParser.parsePaise("₹250"))
        assertEquals(25_000L, AmountParser.parsePaise("₹ 250"))
        assertEquals(25_000L, AmountParser.parsePaise("Rs.250"))
        assertEquals(25_000L, AmountParser.parsePaise("Rs. 250"))
        assertEquals(25_000L, AmountParser.parsePaise("Rs 250"))
        assertEquals(25_000L, AmountParser.parsePaise("rs. 250"))
        assertEquals(25_000L, AmountParser.parsePaise("INR 250"))
        assertEquals(25_000L, AmountParser.parsePaise("INR250"))
    }

    @Test
    fun parsesCommasAndDecimals() {
        assertEquals(125_050L, AmountParser.parsePaise("₹1,250.50"))
        assertEquals(10_000_000L, AmountParser.parsePaise("₹1,00,000"))
        assertEquals(1_050L, AmountParser.parsePaise("Rs. 10.5"))
        assertEquals(1_005L, AmountParser.parsePaise("INR 10.05"))
        assertEquals(99L, AmountParser.parsePaise("₹0.99"))
        assertEquals(25_000L, AmountParser.parsePaise("250"))
        assertEquals(25_000L, AmountParser.parsePaise("250.00"))
    }

    @Test
    fun rejectsMalformedAmounts() {
        assertNull(AmountParser.parsePaise(""))
        assertNull(AmountParser.parsePaise("₹"))
        assertNull(AmountParser.parsePaise("₹10.555"))
        assertNull(AmountParser.parsePaise("abc"))
        assertNull(AmountParser.parsePaise("₹12345678901234"))
        assertNull(AmountParser.parsePaise("₹10 and ₹20"))
    }

    @Test
    fun findsAmountsInsideText() {
        assertEquals(25_000L, AmountParser.findPaise("You paid ₹250 to someone"))
        assertEquals(125_050L, AmountParser.findPaise("Rs.1,250.50 debited."))
        assertEquals(listOf(25_000L, 1_000L), AmountParser.findAllPaise("₹250 paid, ₹10 cashback"))
        assertNull(AmountParser.findPaise("No money here, 250 items"))
    }

    @Test
    fun doesNotMatchRsInsideWords() {
        assertNull(AmountParser.findPaise("Hers 250"))
        assertNull(AmountParser.findPaise("CINR 250"))
    }

    @Test
    fun trailingPunctuationIsNotPartOfTheAmount() {
        assertEquals(25_000L, AmountParser.findPaise("Paid ₹250."))
        assertEquals(25_050L, AmountParser.findPaise("Paid ₹250.50."))
    }
}
