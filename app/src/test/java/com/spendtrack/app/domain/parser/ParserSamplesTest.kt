package com.spendtrack.app.domain.parser

import com.spendtrack.app.domain.UpiApps
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test

/**
 * Runs every real sample in `src/test/resources/samples/<app>.txt` through [ParserRegistry].
 * Each app needs successful payments plus failure/refund/received samples that must return null.
 */
class ParserSamplesTest {

    private val registry = ParserRegistry.default()

    @Ignore("Waiting for real GPay samples in samples/gpay.txt")
    @Test
    fun gpaySamples() = check("gpay.txt", UpiApps.GPAY)

    @Ignore("Waiting for real PhonePe samples in samples/phonepe.txt")
    @Test
    fun phonePeSamples() = check("phonepe.txt", UpiApps.PHONEPE)

    @Ignore("Waiting for real Paytm samples in samples/paytm.txt")
    @Test
    fun paytmSamples() = check("paytm.txt", UpiApps.PAYTM)

    private fun check(file: String, pkg: String) {
        val samples = SampleFixtures.load(file)
        assertTrue("$file has no samples", samples.isNotEmpty())
        assertTrue(
            "$file needs at least one successful payment sample",
            samples.any { it.expect is Expectation.Payment },
        )
        assertTrue(
            "$file needs at least one non-payment sample (failed/refund/received/...)",
            samples.any { it.expect is Expectation.NotAPayment },
        )

        val failures = samples.mapNotNull { sample ->
            if (sample.pkg != pkg) return@mapNotNull "${sample.source}: pkg ${sample.pkg} does not belong in $file"
            val parsed = registry.parse(sample.pkg, sample.title, sample.text, sample.bigText, sample.postedAt)
            when (val expect = sample.expect) {
                Expectation.NotAPayment ->
                    parsed?.let { "${sample.source}: expected null, got ${it.amountPaise} paise to '${it.payee}'" }
                is Expectation.Payment -> when {
                    parsed == null -> "${sample.source}: expected ${expect.amountPaise} paise to '${expect.payee}', got null"
                    parsed.amountPaise != expect.amountPaise || parsed.payee != expect.payee ->
                        "${sample.source}: expected ${expect.amountPaise} paise to '${expect.payee}', " +
                            "got ${parsed.amountPaise} paise to '${parsed.payee}'"
                    parsed.appPkg != pkg || parsed.timestamp != sample.postedAt ->
                        "${sample.source}: wrong appPkg/timestamp (${parsed.appPkg}, ${parsed.timestamp})"
                    parsed.rawText.isBlank() -> "${sample.source}: rawText is blank"
                    else -> null
                }
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n", prefix = "${failures.size} sample(s) failed:\n"))
    }
}
