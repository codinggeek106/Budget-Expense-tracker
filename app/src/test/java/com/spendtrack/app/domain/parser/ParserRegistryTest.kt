package com.spendtrack.app.domain.parser

import com.spendtrack.app.domain.model.ParsedPayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParserRegistryTest {

    /** Accepts only bodies starting with "ok". Records every body it was given. */
    private class FakeParser(override val packageName: String) : PaymentParser {
        val seen = mutableListOf<String?>()
        override fun parse(title: String?, text: String?, postedAt: Long): ParsedPayment? {
            seen += text
            return if (text?.startsWith("ok") == true) {
                ParsedPayment(100, text, postedAt, packageName, rawTextOf(title, text))
            } else {
                null
            }
        }
    }

    @Test
    fun unknownPackageReturnsNull() {
        val registry = ParserRegistry(listOf(FakeParser("a")))
        assertNull(registry.parse("b", "t", "ok", null, 0))
    }

    @Test
    fun triesBigTextThenText() {
        val parser = FakeParser("a")
        val registry = ParserRegistry(listOf(parser))

        assertEquals("ok text", registry.parse("a", "t", "ok text", "big", 0)?.payee)
        assertEquals(listOf("big", "ok text"), parser.seen)
    }

    @Test
    fun identicalTextAndBigTextAreParsedOnce() {
        val parser = FakeParser("a")
        ParserRegistry(listOf(parser)).parse("a", "t", "same", "same", 0)
        assertEquals(listOf("same"), parser.seen)
    }

    @Test
    fun parserStillCalledWhenThereIsNoBody() {
        val parser = FakeParser("a")
        ParserRegistry(listOf(parser)).parse("a", "t", null, null, 0)
        assertEquals(listOf<String?>(null), parser.seen)
    }

    @Test
    fun defaultRegistryCoversTheAllowlist() {
        assertEquals(com.spendtrack.app.domain.UpiApps.PACKAGES, ParserRegistry.default().packages)
    }
}
