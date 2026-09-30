package com.spendtrack.app.domain.usecase

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.AppDatabase
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.data.db.inMemoryDatabase
import com.spendtrack.app.domain.model.TxnStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategorizeTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private val dismissed = mutableListOf<Long>()
    private lateinit var categorize: Categorize

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = TransactionRepository(db.transactionDao(), db.budgetDao())
        categorize = Categorize(repository) { dismissed += it }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun pending(category: String? = null) = repository.insert(
        TransactionEntity(
            amountPaise = 100,
            payee = "p",
            category = category,
            timestamp = 1,
            appPkg = "a",
            rawText = "r",
            status = if (category == null) TxnStatus.PENDING else TxnStatus.CATEGORIZED,
        )
    )

    @Test
    fun categorizeUpdatesRowAndDismissesPrompt() = runTest {
        val id = pending()
        assertTrue(categorize(id, "Travel", note = "  cab home  "))
        val row = repository.getById(id)!!
        assertEquals("Travel", row.category)
        assertEquals("cab home", row.note)
        assertEquals(TxnStatus.CATEGORIZED, row.status)
        assertEquals(listOf(id), dismissed)
    }

    @Test
    fun categoryIsCanonicalisedAgainstDefaultsAndExistingCustomOnes() = runTest {
        pending(category = "Rent")
        val a = pending()
        val b = pending()
        categorize(a, " food ")
        categorize(b, "rent")
        assertEquals("Food", repository.getById(a)!!.category)
        assertEquals("Rent", repository.getById(b)!!.category)
    }

    @Test
    fun blankNoteIsNotStored() = runTest {
        val id = pending()
        categorize(id, "Food", note = "   ")
        assertEquals(null, repository.getById(id)!!.note)
    }

    @Test
    fun blankCategoryIsRejected() = runTest {
        val id = pending()
        assertFalse(categorize(id, "   "))
        assertEquals(TxnStatus.PENDING, repository.getById(id)!!.status)
        assertTrue(dismissed.isEmpty())
    }

    @Test
    fun ignoreMarksIgnoredAndDismisses() = runTest {
        val id = pending()
        assertTrue(categorize.ignore(id))
        assertEquals(TxnStatus.IGNORED, repository.getById(id)!!.status)
        assertEquals(listOf(id), dismissed)
    }

    @Test
    fun missingTransactionStillDismissesStalePrompt() = runTest {
        assertFalse(categorize(999, "Food"))
        assertEquals(listOf(999L), dismissed)
    }
}
