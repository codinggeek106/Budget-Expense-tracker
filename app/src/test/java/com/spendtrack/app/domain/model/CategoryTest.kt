package com.spendtrack.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryTest {

    @Test
    fun promptChoicesFallBackToFoodTravelBills() {
        assertEquals(listOf("Food", "Travel", "Bills"), Category.promptChoices(emptyList()))
    }

    @Test
    fun promptChoicesPreferMostUsedAndTopUpWithoutDuplicates() {
        assertEquals(listOf("Groceries", "Food", "Travel"), Category.promptChoices(listOf("Groceries", "Food")))
        // A differently-cased duplicate of a fallback isn't added twice.
        assertEquals(listOf("food", "Travel", "Bills"), Category.promptChoices(listOf("food")))
        assertEquals(listOf("Rent", "Shopping", "Health"), Category.promptChoices(listOf("Rent", "Shopping", "Health", "Food")))
        assertEquals(listOf("Travel", "Food", "Bills"), Category.promptChoices(listOf("Travel")))
    }

    @Test
    fun allChoicesPutsCustomCategoriesBeforeOther() {
        val choices = Category.allChoices(listOf("Rent", "food", "gym", "Rent"))
        assertEquals(
            listOf("Food", "Groceries", "Travel", "Bills", "Shopping", "Health", "Entertainment", "gym", "Rent", "Other"),
            choices,
        )
    }

    @Test
    fun canonicalReusesKnownSpelling() {
        assertEquals("Food", Category.canonical("  food ", emptyList()))
        assertEquals("Rent", Category.canonical("RENT", listOf("Rent")))
        assertEquals("Car Wash", Category.canonical(" Car   Wash ", emptyList()))
    }
}
