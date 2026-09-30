package com.spendtrack.app.domain.parser

/** What a sample should parse to: an amount and payee, or nothing (`expect: none`). */
sealed interface Expectation {
    data class Payment(val amountPaise: Long, val payee: String) : Expectation
    data object NotAPayment : Expectation
}

data class Sample(
    val source: String,
    val pkg: String,
    val postedAt: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val expect: Expectation,
)

/** Reads the fixture format documented in `src/test/resources/samples/README.md`. */
object SampleFixtures {

    fun load(fileName: String): List<Sample> {
        val stream = SampleFixtures::class.java.getResourceAsStream("/samples/$fileName")
            ?: error("Missing test resource samples/$fileName")
        return parse(stream.bufferedReader(Charsets.UTF_8).readText(), fileName)
    }

    fun parse(content: String, fileName: String = "<inline>"): List<Sample> {
        val blocks = mutableListOf<List<Pair<Int, String>>>()
        var current = mutableListOf<Pair<Int, String>>()
        content.replace("\r\n", "\n").lines().forEachIndexed { index, line ->
            when {
                line.isBlank() -> if (current.isNotEmpty()) {
                    blocks += current
                    current = mutableListOf()
                }
                line.trimStart().startsWith("#") -> Unit
                else -> current += (index + 1) to line
            }
        }
        if (current.isNotEmpty()) blocks += current
        return blocks.map { parseBlock(it, fileName) }
    }

    private fun parseBlock(lines: List<Pair<Int, String>>, fileName: String): Sample {
        val startLine = lines.first().first
        val source = "$fileName:$startLine"
        val fields = lines.associate { (lineNo, line) ->
            val key = line.substringBefore(':', missingDelimiterValue = "").trim()
            require(key.isNotEmpty()) { "$fileName:$lineNo: expected 'key: value', got '$line'" }
            key to line.substringAfter(':').removePrefix(" ")
        }
        fun required(key: String) = fields[key] ?: error("$source: missing '$key:'")
        return Sample(
            source = source,
            pkg = required("pkg").trim(),
            postedAt = required("posted").trim().toLong(),
            title = fields["title"]?.let(::unescape),
            text = fields["text"]?.let(::unescape),
            bigText = fields["bigText"]?.let(::unescape),
            expect = parseExpectation(required("expect").trim(), source),
        )
    }

    private fun parseExpectation(value: String, source: String): Expectation {
        if (value.equals("none", ignoreCase = true)) return Expectation.NotAPayment
        val parts = value.split("|", limit = 2).map { it.trim() }
        require(parts.size == 2 && parts[1].isNotEmpty()) {
            "$source: 'expect:' must be '<amount> | <payee>' or 'none', got '$value'"
        }
        val paise = AmountParser.parsePaise(parts[0]) ?: error("$source: bad amount '${parts[0]}'")
        return Expectation.Payment(paise, parts[1])
    }

    fun unescape(value: String): String = buildString {
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    'n' -> append('\n')
                    '\\' -> append('\\')
                    else -> append(c).append(value[i + 1])
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
