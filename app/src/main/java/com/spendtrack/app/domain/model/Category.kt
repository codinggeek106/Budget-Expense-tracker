package com.spendtrack.app.domain.model

/** Categories are plain strings; these are the built-in ones. Users can add their own via "Other". */
object Category {
    const val FOOD = "Food"
    const val GROCERIES = "Groceries"
    const val TRAVEL = "Travel"
    const val BILLS = "Bills"
    const val SHOPPING = "Shopping"
    const val HEALTH = "Health"
    const val ENTERTAINMENT = "Entertainment"
    const val OTHER = "Other"

    val DEFAULTS: List<String> = listOf(FOOD, GROCERIES, TRAVEL, BILLS, SHOPPING, HEALTH, ENTERTAINMENT, OTHER)
}
