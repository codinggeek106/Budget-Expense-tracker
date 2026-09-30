package com.spendtrack.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Debug log of every notification seen from an allowlisted UPI app, used to collect real parser samples. */
@Entity(tableName = "raw_notifications")
data class RawNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pkg: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val postedAt: Long,
)
