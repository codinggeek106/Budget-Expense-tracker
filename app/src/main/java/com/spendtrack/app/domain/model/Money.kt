package com.spendtrack.app.domain.model

import kotlin.math.abs

/** Paise -> display rupees with Indian digit grouping (₹1,00,000.50). Only used at the UI edge. */
object Money {

    fun format(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val magnitude = abs(paise)
        val rupees = groupIndian((magnitude / 100).toString())
        val fraction = magnitude % 100
        return if (fraction == 0L) "$sign₹$rupees" else "$sign₹$rupees.${fraction.toString().padStart(2, '0')}"
    }

    /** 1234567 -> 12,34,567: last three digits, then groups of two. */
    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3)
        val tail = digits.takeLast(3)
        val headGroups = head.reversed().chunked(2).joinToString(",").reversed()
        return "$headGroups,$tail"
    }
}
