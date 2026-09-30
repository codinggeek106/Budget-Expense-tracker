package com.spendtrack.app.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BudgetDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BudgetDao

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        dao = db.budgetDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsertReplacesLimitForSameCategory() = runTest {
        dao.upsert(BudgetEntity("Food", 500_000))
        dao.upsert(BudgetEntity("Bills", 300_000))
        dao.upsert(BudgetEntity("Food", 600_000))
        assertEquals(
            listOf(BudgetEntity("Bills", 300_000), BudgetEntity("Food", 600_000)),
            dao.observeAll().first(),
        )
    }

    @Test
    fun deleteRemovesOnlyThatCategory() = runTest {
        dao.upsert(BudgetEntity("Food", 1))
        dao.upsert(BudgetEntity("Bills", 2))
        dao.delete("Food")
        assertEquals(listOf(BudgetEntity("Bills", 2)), dao.observeAll().first())
    }
}
