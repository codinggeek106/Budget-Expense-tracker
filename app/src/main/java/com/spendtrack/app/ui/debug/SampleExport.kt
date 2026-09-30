package com.spendtrack.app.ui.debug

import com.spendtrack.app.data.db.RawNotificationEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats captured notifications as blocks for `app/src/test/resources/samples/<app>.txt`.
 * See `app/src/test/resources/samples/README.md` for the format. Each field sits on one line;
 * backslashes and newlines inside values are escaped as `\\` and `\n`.
 */
object SampleExport {

    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun format(rows: List<RawNotificationEntity>, zone: ZoneId = ZoneId.systemDefault()): String =
        rows.joinToString(separator = "\n\n", postfix = if (rows.isEmpty()) "" else "\n") { formatBlock(it, zone) }

    fun formatBlock(row: RawNotificationEntity, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        appendLine("# ${timeFormat.format(Instant.ofEpochMilli(row.postedAt).atZone(zone))}")
        appendLine("pkg: ${row.pkg}")
        appendLine("posted: ${row.postedAt}")
        row.title?.let { appendLine("title: ${escape(it)}") }
        row.text?.let { appendLine("text: ${escape(it)}") }
        row.bigText?.let { appendLine("bigText: ${escape(it)}") }
        append("expect: ?")
    }

    fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .replace("\n", "\\n")
}
