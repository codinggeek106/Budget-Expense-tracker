package com.spendtrack.app.domain.parser

/** Rupee amounts in notification text -> paise. Never goes through floating point. */
object AmountParser {

    private const val NUMBER = """[0-9][0-9,]*(?:\.[0-9]{1,2})?(?![0-9])"""

    /** A currency marker (₹, Rs, Rs., INR) followed by an amount. */
    private val CURRENCY_AMOUNT = Regex("""(?:₹|(?<![A-Za-z])(?:Rs\.?|INR))\s*($NUMBER)""", RegexOption.IGNORE_CASE)

    private val BARE_NUMBER = Regex("""^$NUMBER$""")

    /** Longest integer part accepted (13 digits keeps paise well inside Long). */
    private const val MAX_INTEGER_DIGITS = 13

    /** The first currency-marked amount in [input], in paise, or null if there is none. */
    fun findPaise(input: String): Long? =
        CURRENCY_AMOUNT.find(input)?.groupValues?.get(1)?.let(::toPaise)

    /** Every currency-marked amount in [input], in paise, in order of appearance. */
    fun findAllPaise(input: String): List<Long> =
        CURRENCY_AMOUNT.findAll(input).mapNotNull { toPaise(it.groupValues[1]) }.toList()

    /**
     * Parses an amount with an optional currency marker, e.g. "₹1,250.50", "Rs. 99", "INR 10.5", "250".
     * Returns null if [amount] is not exactly one amount.
     */
    fun parsePaise(amount: String): Long? {
        val trimmed = amount.trim()
        CURRENCY_AMOUNT.matchEntire(trimmed)?.let { return toPaise(it.groupValues[1]) }
        return if (BARE_NUMBER.matches(trimmed)) toPaise(trimmed) else null
    }

    private fun toPaise(number: String): Long? {
        val digits = number.replace(",", "")
        val integerPart = digits.substringBefore('.')
        val fraction = digits.substringAfter('.', missingDelimiterValue = "")
        if (integerPart.isEmpty() || integerPart.length > MAX_INTEGER_DIGITS) return null
        val paise = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 10
            2 -> fraction.toLong()
            else -> return null
        }
        return integerPart.toLong() * 100 + paise
    }
}
