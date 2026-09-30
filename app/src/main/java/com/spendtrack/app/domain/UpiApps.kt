package com.spendtrack.app.domain

/** The single allowlist of UPI apps whose notifications are read. */
object UpiApps {
    const val GPAY = "com.google.android.apps.nbu.paisa.user"
    const val PHONEPE = "com.phonepe.app"
    const val PAYTM = "net.one97.paytm"

    val PACKAGES: Set<String> = setOf(GPAY, PHONEPE, PAYTM)

    fun label(pkg: String): String = when (pkg) {
        GPAY -> "GPay"
        PHONEPE -> "PhonePe"
        PAYTM -> "Paytm"
        else -> pkg
    }
}
