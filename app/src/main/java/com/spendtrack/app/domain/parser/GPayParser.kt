package com.spendtrack.app.domain.parser

import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.ParsedPayment

class GPayParser : PaymentParser {

    override val packageName: String = UpiApps.GPAY

    override fun parse(title: String?, text: String?, postedAt: Long): ParsedPayment? {
        // TODO(samples): write the success/exclusion regexes against real notifications in
        //  app/src/test/resources/samples/gpay.txt. Do not guess the format.
        return null
    }
}
