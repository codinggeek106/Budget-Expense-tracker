package com.spendtrack.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RawNotificationDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RawNotificationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.rawNotificationDao()
    }

    @After
    fun tearDown() = db.close()

    private fun row(i: Int) = RawNotificationEntity(
        pkg = "com.phonepe.app",
        title = "t$i",
        text = "x$i",
        bigText = null,
        postedAt = i.toLong(),
    )

    @Test
    fun insertAndTrimKeepsNewestRows() = runTest {
        repeat(8) { dao.insertAndTrim(row(it), keep = 5) }

        val rows = dao.observeAll().first()
        assertEquals(5, rows.size)
        assertEquals(listOf("t7", "t6", "t5", "t4", "t3"), rows.map { it.title })
    }

    @Test
    fun defaultCapIs500() = runTest {
        repeat(DEFAULT_OVERFLOW) { dao.insertAndTrim(row(it)) }
        assertEquals(RawNotificationDao.MAX_ROWS, dao.count())
    }

    @Test
    fun clearRemovesEverything() = runTest {
        repeat(3) { dao.insert(row(it)) }
        dao.clear()
        assertEquals(0, dao.count())
    }

    private companion object {
        const val DEFAULT_OVERFLOW = RawNotificationDao.MAX_ROWS + 20
    }
}
