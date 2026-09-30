package com.spendtrack.app.domain.parser

import com.spendtrack.app.data.db.RawNotificationEntity
import com.spendtrack.app.ui.debug.SampleExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleFixturesTest {

    @Test
    fun parsesPaymentAndNoneBlocks() {
        val samples = SampleFixtures.parse(
            """
            # comment
            pkg: com.example
            posted: 42
            title: T
            text: a\nb
            expect: 1,250.50 | Some Payee

            pkg: com.example
            posted: 43
            bigText: c\\d
            expect: none
            """.trimIndent()
        )
        assertEquals(2, samples.size)
        with(samples[0]) {
            assertEquals(42L, postedAt)
            assertEquals("T", title)
            assertEquals("a\nb", text)
            assertEquals(null, bigText)
            assertEquals(Expectation.Payment(125_050L, "Some Payee"), expect)
        }
        with(samples[1]) {
            assertEquals("c\\d", bigText)
            assertEquals(Expectation.NotAPayment, expect)
        }
    }

    @Test
    fun exportedBlocksRoundTripOnceExpectIsFilled() {
        val row = RawNotificationEntity(
            pkg = "com.phonepe.app",
            title = "Title with \\ backslash",
            text = "Line 1\nLine 2",
            bigText = "Big",
            postedAt = 1_000L,
        )
        val exported = SampleExport.format(listOf(row)).replace("expect: ?", "expect: none")
        val sample = SampleFixtures.parse(exported).single()
        assertEquals(row.title, sample.title)
        assertEquals(row.text, sample.text)
        assertEquals(row.bigText, sample.bigText)
        assertEquals(row.postedAt, sample.postedAt)
    }

    @Test
    fun unfilledExpectationIsRejected() {
        val result = runCatching {
            SampleFixtures.parse("pkg: p\nposted: 1\ntext: t\nexpect: ?")
        }
        assertTrue(result.isFailure)
    }
}
