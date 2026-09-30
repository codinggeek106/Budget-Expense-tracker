package com.spendtrack.app.domain.parser

import com.spendtrack.app.domain.UpiApps
import com.spendtrack.app.domain.model.ParsedPayment

class PaytmParser : PaymentParser {

    override val packageName: String = UpiApps.PAYTM

    override fun parse(title: String?, text: String?, postedAt: Long): ParsedPayment? {
        // TODO(samples): write the success/exclusion regexes against real notifications in
        //  app/src/test/resources/samples/paytm.txt. Do not guess the format.
        return null
    }
}
