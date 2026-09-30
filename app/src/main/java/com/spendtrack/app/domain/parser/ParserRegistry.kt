package com.spendtrack.app.domain.parser

import com.spendtrack.app.domain.model.ParsedPayment

/** Routes a notification to the parser for its package. */
class ParserRegistry(parsers: List<PaymentParser>) {

    private val byPackage: Map<String, PaymentParser> = parsers.associateBy { it.packageName }

    val packages: Set<String> get() = byPackage.keys

    /**
     * Tries the expanded text first (it is usually the fuller version), then the collapsed text.
     * TODO(samples): confirm this order against the real samples once they exist.
     */
    fun parse(pkg: String, title: String?, text: String?, bigText: String?, postedAt: Long): ParsedPayment? {
        val parser = byPackage[pkg] ?: return null
        val bodies: List<String?> = listOfNotNull(bigText, text).distinct().ifEmpty { listOf(null) }
        return bodies.firstNotNullOfOrNull { parser.parse(title, it, postedAt) }
    }

    companion object {
        fun default() = ParserRegistry(listOf(GPayParser(), PhonePeParser(), PaytmParser()))
    }
}
