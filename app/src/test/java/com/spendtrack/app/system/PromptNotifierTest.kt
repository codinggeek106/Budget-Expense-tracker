package com.spendtrack.app.system

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
import com.spendtrack.app.domain.model.TxnStatus
import com.spendtrack.app.ui.CategorizeActivity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class PromptNotifierTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private lateinit var db: AppDatabase
    private lateinit var repository: TransactionRepository
    private lateinit var notifier: PromptNotifier

    @Before
    fun setUp() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        NotificationChannels.create(context)
        db = inMemoryDatabase()
        repository = TransactionRepository(db.transactionDao(), db.budgetDao())
        notifier = PromptNotifier(context, repository)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insert(
        category: String? = null,
        status: TxnStatus = TxnStatus.PENDING,
        payee: String = "Ramesh Kumar",
    ) = repository.insert(
        TransactionEntity(
            amountPaise = 125_050,
            payee = payee,
            category = category,
            timestamp = 1_000,
            appPkg = "com.phonepe.app",
            rawText = "r",
            status = status,
        )
    )

    private fun posted(): List<Notification> = shadowOf(manager).allNotifications

    @Test
    fun promptShowsAmountPayeeAndThreeFallbackActions() = runTest {
        val id = insert()
        notifier.prompt(id)

        val n = posted().single()
        assertEquals("Paid ₹1,250.50 to Ramesh Kumar", n.extras.getCharSequence(Notification.EXTRA_TITLE).toString())
        assertEquals("What was this for?", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(NotificationChannels.PAYMENT_PROMPT, n.channelId)
        assertEquals(listOf("Food", "Travel", "Bills"), n.actions.map { it.title.toString() })
        assertTrue(n.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertFalse(n.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
    }

    @Test
    fun actionsCarryTxnIdAndCategoryToTheReceiver() = runTest {
        val id = insert()
        notifier.prompt(id)

        val actions = posted().single().actions
        actions.forEachIndexed { index, action ->
            val intent = shadowOf(action.actionIntent).savedIntent
            assertEquals(CategoryActionReceiver::class.java.name, intent.component!!.className)
            assertEquals(CategoryActionReceiver.ACTION_CATEGORIZE, intent.action)
            assertEquals(id, intent.getLongExtra(CategoryActionReceiver.EXTRA_TXN_ID, -1))
            assertEquals(action.title.toString(), intent.getStringExtra(CategoryActionReceiver.EXTRA_CATEGORY))
            assertTrue(shadowOf(action.actionIntent).isBroadcast)
            assertEquals(PromptNotifier.requestCode(id, index), shadowOf(action.actionIntent).requestCode)
        }
        assertEquals(3, actions.map { shadowOf(it.actionIntent).requestCode }.toSet().size)
    }

    @Test
    fun tappingTheBodyOpensTheCategoryPicker() = runTest {
        val id = insert()
        notifier.prompt(id)

        val content = shadowOf(posted().single().contentIntent)
        assertTrue(content.isActivity)
        assertEquals(CategorizeActivity::class.java.name, content.savedIntent.component!!.className)
        assertEquals(id, content.savedIntent.getLongExtra(CategorizeActivity.EXTRA_TXN_ID, -1))
    }

    @Test
    fun actionsUseMostFrequentCategories() = runTest {
        // Different payee, so no same-payee suggestion applies; this checks frequency alone.
        repeat(3) { insert(category = "Groceries", status = TxnStatus.CATEGORIZED, payee = "Shop") }
        repeat(2) { insert(category = "Rent", status = TxnStatus.CATEGORIZED, payee = "Landlord") }
        val id = insert()
        notifier.prompt(id)
        assertEquals(listOf("Groceries", "Rent", "Food"), posted().single().actions.map { it.title.toString() })
    }

    @Test
    fun cancelRemovesOnlyThatPrompt() = runTest {
        val a = insert()
        val b = insert()
        notifier.prompt(a)
        notifier.prompt(b)
        assertEquals(2, posted().size)

        notifier.cancel(a)
        assertEquals(1, posted().size)
        assertEquals(PromptNotifier.notificationId(b), shadowOf(manager).activeNotifications.single().id)
    }

    @Test
    fun repostingUsesTheSameNotification() = runTest {
        val id = insert()
        notifier.prompt(id)
        notifier.promptAll(listOf(repository.getById(id)!!))
        assertEquals(1, posted().size)
    }

    @Test
    fun noPromptForNonPendingTransactions() = runTest {
        notifier.prompt(insert(category = "Food", status = TxnStatus.CATEGORIZED))
        notifier.prompt(insert(status = TxnStatus.IGNORED))
        notifier.prompt(12345)
        assertTrue(posted().isEmpty())
    }

    @Test
    fun nothingPostedWithoutNotificationPermission() = runTest {
        shadowOf(context as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notifier.prompt(insert())
        assertTrue(posted().isEmpty())
    }
}
