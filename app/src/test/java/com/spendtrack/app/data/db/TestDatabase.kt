package com.spendtrack.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

fun inMemoryDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
