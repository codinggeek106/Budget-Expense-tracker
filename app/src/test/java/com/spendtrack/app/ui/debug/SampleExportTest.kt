package com.spendtrack.app.ui.debug

import com.spendtrack.app.data.db.RawNotificationEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class SampleExportTest {

    private val ist = ZoneId.of("Asia/Kolkata")

    @Test
    fun blockHasOneFieldPerLineAndOmitsNullFields() {
        val row = RawNotificationEntity(
            pkg = "com.example",
            title = "Title",
            text = "Line 1\nLine 2",
            bigText = null,
            postedAt = 0L,
        )
        val expected = """
            # 1970-01-01 05:30:00
            pkg: com.example
            posted: 0
            title: Title
            text: Line 1\nLine 2
            expect: ?
        """.trimIndent()
        assertEquals(expected, SampleExport.formatBlock(row, ist))
    }

    @Test
    fun escapeHandlesBackslashesAndCarriageReturns() {
        assertEquals("a\\\\b\\nc\\nd", SampleExport.escape("a\\b\r\nc\rd"))
    }

    @Test
    fun blocksAreSeparatedByBlankLines() {
        val row = RawNotificationEntity(pkg = "p", title = null, text = "t", bigText = null, postedAt = 0L)
        val out = SampleExport.format(listOf(row, row), ist)
        assertEquals(2, out.trimEnd().split("\n\n").size)
    }

    @Test
    fun emptyListFormatsToEmptyString() {
        assertEquals("", SampleExport.format(emptyList(), ist))
    }
}
