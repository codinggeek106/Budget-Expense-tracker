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

    /** Used to fill the prompt's buttons until the user has categorized enough payments. */
    val PROMPT_FALLBACK: List<String> = listOf(FOOD, TRAVEL, BILLS)

    /** The prompt's quick-action categories: most used first, topped up from [PROMPT_FALLBACK]. */
    fun promptChoices(mostUsed: List<String>, count: Int = 3): List<String> =
        (mostUsed + PROMPT_FALLBACK).distinctBy { it.lowercase() }.take(count)

    /** Every category to offer in the full picker: defaults (Other last), then custom ones in use. */
    fun allChoices(used: List<String>): List<String> {
        val custom = used.filter { u -> DEFAULTS.none { it.equals(u, ignoreCase = true) } }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
        return DEFAULTS.dropLast(1) + custom + OTHER
    }

    /** Trims [input] and reuses the spelling of a matching [known] category, e.g. "food " -> "Food". */
    fun canonical(input: String, known: List<String>): String {
        val trimmed = input.trim().replace(Regex("\\s+"), " ")
        return (DEFAULTS + known).firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: trimmed
    }
}
