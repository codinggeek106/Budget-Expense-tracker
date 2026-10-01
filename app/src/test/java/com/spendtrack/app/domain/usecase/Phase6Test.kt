package com.spendtrack.app.domain.usecase

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spendtrack.app.data.TransactionRepository
import com.spendtrack.app.data.db.AppDatabase
import com.spendtrack.app.data.db.TransactionEntity
import com.spendtrack.app.data.db.inMemoryDatabase
import com.spendtrack.app.domain.model.MonthRange
import com.spendtrack.app.domain.model.TxnStatus
import com.spendtrack.app.system.NotificationChannels
import com.spendtrack.app.system.PromptNotifier
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/** CSV export through the database, and last-category-for-payee suggestions. */
@RunWith(AndroidJUnit4::class)
class Phase6Test {

    private val ist = ZoneId.of("Asia/Kolkata")
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository

    @Before
    fun setUp() {
        db = inMemoryDatabase()
        repository = TransactionRepository(db.transactionDao(), db.budgetDao())
    }

    @After
    fun tearDown() = db.close()

    private fun at(day: Int, month: Int = 9) =
        LocalDateTime.of(2026, month, day, 12, 0).atZone(ist).toInstant().toEpochMilli()

    private suspend fun add(
        payee: String,
        category: String?,
        day: Int,
        month: Int = 9,
        status: TxnStatus = if (category == null) TxnStatus.PENDING else TxnStatus.CATEGORIZED,
    ) = repository.insert(
        TransactionEntity(
            amountPaise = 10_000,
            payee = payee,
            category = category,
            timestamp = at(day, month),
            appPkg = "com.phonepe.app",
            rawText = "r$payee$day$month",
            status = status,
        )
    )

    @Test
    fun exportWritesBomHeaderAndOnlyTheSelectedMonth() = runTest {
        add("Aug", "Food", 31, month = 8)
        add("Sep 1", "Food", 1)
        add("Sep 2", null, 2)
        add("Sep 3", "Bills", 3, status = TxnStatus.IGNORED)

        val out = ByteArrayOutputStream()
        val count = ExportCsv(repository)(MonthRange(YearMonth.of(2026, 9), ist), out, ist)
        val text = out.toString(Charsets.UTF_8)

        assertEquals(3, count)
        assertTrue(text.startsWith("﻿id,date,"))
        val lines = text.removePrefix("﻿").trimEnd().split("\r\n")
        assertEquals(4, lines.size)
        assertEquals(listOf("Sep 1", "Sep 2", "Sep 3"), lines.drop(1).map { it.split(",")[4] })
    }

    @Test
    fun exportEverythingIncludesAllMonths() = runTest {
        add("Aug", "Food", 31, month = 8)
        add("Sep", "Food", 1)
        val out = ByteArrayOutputStream()
        assertEquals(2, ExportCsv(repository)(null, out, ist))
    }

    @Test
    fun lastCategoryForPayeeIsMostRecentCategorizedOne() = runTest {
        add("Ramesh Kumar", "Food", 1)
        add("ramesh kumar ", "Groceries", 5)
        add("Ramesh Kumar", "Bills", 9, status = TxnStatus.IGNORED) // ignored rows don't count
        add("Ramesh Kumar", null, 10) // pending row has no category

        assertEquals("Groceries", repository.getLastCategoryForPayee("  RAMESH KUMAR"))
        assertNull(repository.getLastCategoryForPayee("Someone Else"))
    }

    @Test
    fun promptPutsLastUsedCategoryFirstAndMentionsIt() = runTest {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.create(context)
        repeat(3) { add("Other shop", "Bills", 1 + it) }
        add("Chai Point", "Entertainment", 4)
        val pending = add("Chai Point", null, 6)

        PromptNotifier(context, repository).prompt(pending)

        val n = shadowOf(context.getSystemService(NotificationManager::class.java)).allNotifications.single()
        assertEquals(listOf("Entertainment", "Bills", "Food"), n.actions.map { it.title.toString() })
        assertEquals(
            "What was this for? Last time: Entertainment",
            n.extras.getCharSequence(Notification.EXTRA_TEXT).toString(),
        )
    }
}
